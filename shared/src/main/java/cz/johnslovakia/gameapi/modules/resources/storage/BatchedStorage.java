package cz.johnslovakia.gameapi.modules.resources.storage;

import cz.johnslovakia.gameapi.Core;
import cz.johnslovakia.gameapi.utils.BatchConfig;
import cz.johnslovakia.gameapi.utils.CachedBatchStorage;
import me.zort.sqllib.SQLDatabaseConnection;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class BatchedStorage implements ResourceStorage {

    private final String resourceName;
    private final String tableName;

    private CachedBatchStorage<String, Integer> storage;

    private final Listener playerQuitListener;

    public BatchedStorage(String resourceName, String tableName) {
        this.resourceName = resourceName;
        this.tableName = tableName;

        this.storage = new CachedBatchStorage<>(
                "resource-" + resourceName,
                BatchConfig.builder("resource-" + resourceName)
                        .maxBatchSize(50)
                        .flushIntervalSeconds(30)
                        .debugEnabled(false)
                        .build(),
                this::loadBalanceFromDB,
                this::saveBalanceToDB,
                Integer::sum
        );
        
        this.playerQuitListener = new Listener() {
            @EventHandler
            public void onQuit(PlayerQuitEvent event) {
                CachedBatchStorage<String, Integer> current = storage;
                if (current != null) {
                    current.flushAndInvalidate(event.getPlayer().getName());
                }
            }
        };
        Bukkit.getPluginManager().registerEvents(playerQuitListener, Core.getInstance().getPlugin());
    }

    @Override
    public void onEnable() {
        try (SQLDatabaseConnection dbConn = Core.getInstance().getDatabase().getConnection()) {
            if (dbConn == null) return;

            Connection conn = dbConn.getConnection();
            String checkSql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND COLUMN_NAME = ?";

            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, Core.getInstance().getDatabase().getDatabase());
                checkStmt.setString(2, tableName);
                checkStmt.setString(3, resourceName);

                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) == 0) {
                        String sql = "ALTER TABLE `" + tableName + "` ADD `" + resourceName + "` INT DEFAULT 0";
                        try (Statement alterStmt = conn.createStatement()) {
                            alterStmt.executeUpdate(sql);
                        }
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    private Map<String, Integer> loadBalanceFromDB(Set<String> nicknames) throws SQLException {
        Map<String, Integer> results = new HashMap<>();

        if (nicknames.isEmpty()) return results;

        String placeholders = String.join(",", Collections.nCopies(nicknames.size(), "?"));
        String sql = "SELECT Nickname, " + resourceName +
                " FROM " + tableName +
                " WHERE Nickname IN (" + placeholders + ")";

        try (SQLDatabaseConnection dbConn = Core.getInstance().getDatabase().getConnection()) {
            if (dbConn == null) return results;

            try (PreparedStatement stmt = dbConn.getConnection().prepareStatement(sql)) {
                int i = 1;
                for (String nickname : nicknames) {
                    stmt.setString(i++, nickname);
                }
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        results.put(rs.getString("Nickname"), rs.getInt(resourceName));
                    }
                }
            }
        }
        
        for (String nickname : nicknames) {
            results.putIfAbsent(nickname, 0);
        }

        return results;
    }
    
    private void saveBalanceToDB(Map<String, CachedBatchStorage.PendingChange<String, Integer>> changes)
            throws SQLException {
        if (changes.isEmpty()) return;

        try (SQLDatabaseConnection dbConn = Core.getInstance().getDatabase().getConnection()) {
            if (dbConn == null) return;

            Connection conn = dbConn.getConnection();
            conn.setAutoCommit(false);
            
            String setSql = "INSERT INTO " + tableName + " (Nickname, " + resourceName + ")" +
                    " VALUES (?, ?)" +
                    " ON DUPLICATE KEY UPDATE " + resourceName + " = VALUES(" + resourceName + ")";
            String deltaSql = "INSERT INTO " + tableName + " (Nickname, " + resourceName + ")" +
                    " VALUES (?, ?)" +
                    " ON DUPLICATE KEY UPDATE " + resourceName + " = " + resourceName + " + VALUES(" + resourceName + ")";

            try (PreparedStatement setStmt = conn.prepareStatement(setSql);
                 PreparedStatement deltaStmt = conn.prepareStatement(deltaSql)) {

                int setCount = 0, deltaCount = 0;

                for (CachedBatchStorage.PendingChange<String, Integer> change : changes.values()) {
                    if (change.isSet()) {
                        setStmt.setString(1, change.getKey());
                        setStmt.setInt(2, change.getDelta());
                        setStmt.addBatch();
                        setCount++;
                    } else {
                        deltaStmt.setString(1, change.getKey());
                        deltaStmt.setInt(2, change.getDelta());
                        deltaStmt.addBatch();
                        deltaCount++;
                    }
                }

                if (setCount > 0) setStmt.executeBatch();
                if (deltaCount > 0) deltaStmt.executeBatch();

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }
    
    @Override
    public void deposit(OfflinePlayer player, int amount) {
        CachedBatchStorage<String, Integer> current = storage;
        if (current != null) {
            current.modify(player.getName(), amount);
        }
    }

    @Override
    public void withdraw(OfflinePlayer player, int amount) {
        CachedBatchStorage<String, Integer> current = storage;
        if (current != null) {
            current.modify(player.getName(), -amount);
        }
    }

    @Override
    public CompletableFuture<Integer> getBalance(OfflinePlayer player) {
        CachedBatchStorage<String, Integer> current = storage;
        return current != null ? current.get(player.getName()) : CompletableFuture.completedFuture(0);
    }

    @Override
    public int getBalanceCached(OfflinePlayer player) {
        CachedBatchStorage<String, Integer> current = storage;
        if (current == null) return 0;

        Integer cached = current.getCached(player.getName());
        return cached != null ? cached : 0;
    }

    @Override
    public CompletableFuture<Void> preload(Iterable<? extends OfflinePlayer> players) {
        CachedBatchStorage<String, Integer> current = storage;
        if (current == null) return CompletableFuture.completedFuture(null);

        Set<String> nicknames = new HashSet<>();
        players.forEach(p -> nicknames.add(p.getName()));
        return current.preload(nicknames);
    }

    @Override
    public void shutdown() {
        shutdown(false);
    }

    @Override
    public void shutdownSilently() {
        shutdown(true);
    }

    private void shutdown(boolean silent) {
        HandlerList.unregisterAll(playerQuitListener);
        CachedBatchStorage<String, Integer> current = storage;
        if (current != null) {
            storage = null;
            if (silent) {
                current.shutdownSilently();
            } else {
                current.shutdown();
            }
        }
    }
}
