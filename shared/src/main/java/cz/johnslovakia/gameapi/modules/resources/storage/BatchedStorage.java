package cz.johnslovakia.gameapi.modules.resources.storage;

import cz.johnslovakia.gameapi.Shared;
import cz.johnslovakia.gameapi.database.Type;
import cz.johnslovakia.gameapi.users.PlayerIdentity;
import cz.johnslovakia.gameapi.users.PlayerIdentityRegistry;
import cz.johnslovakia.gameapi.utils.BatchConfig;
import cz.johnslovakia.gameapi.utils.CachedBatchStorage;
import me.zort.sqllib.SQLDatabaseConnection;
import org.bukkit.Bukkit;
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

    private CachedBatchStorage<PlayerIdentity, Integer> storage;

    private final Listener playerQuitListener;

    public BatchedStorage(String resourceName, String tableName) {
        this.resourceName = resourceName;
        this.tableName = tableName;

        this.storage = new CachedBatchStorage<>(
                "resource-" + resourceName,
                BatchConfig.builder("resource-" + resourceName)
                        .maxBatchSize(50)
                        .flushIntervalSeconds(180)
                        .build(),
                this::loadBalanceFromDB,
                this::saveBalanceToDB,
                Integer::sum
        );

        this.playerQuitListener = new Listener() {
            @EventHandler
            public void onQuit(PlayerQuitEvent event) {
                PlayerIdentity identity = PlayerIdentityRegistry.get(event.getPlayer());
                storage.invalidate(identity);
            }
        };
        Bukkit.getPluginManager().registerEvents(playerQuitListener, Shared.getInstance().getPlugin());
    }

    @Override
    public void onEnable() {
        SQLDatabaseConnection connection = Shared.getInstance().getDatabase().getConnection();
        if (connection != null) {
            Connection conn = Shared.getInstance().getDatabase().getConnection().getConnection();

            try (
                    PreparedStatement checkStmt = conn.prepareStatement(
                            "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND COLUMN_NAME = ?"
                    )
            ) {
                checkStmt.setString(1, Shared.getInstance().getDatabase().getDatabase());
                checkStmt.setString(2, tableName);
                checkStmt.setString(3, resourceName);

                ResultSet rs = checkStmt.executeQuery();
                boolean exists = false;

                if (rs.next()) {
                    exists = rs.getInt(1) > 0;
                }

                rs.close();

                if (!exists) {
                    try (Statement alterStmt = conn.createStatement()) {
                        String sql = "ALTER TABLE `" + tableName + "` ADD `" + resourceName + "` INT DEFAULT 0";
                        alterStmt.executeUpdate(sql);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private Map<PlayerIdentity, Integer> loadBalanceFromDB(Set<PlayerIdentity> playerIdentities) throws SQLException {
        Map<PlayerIdentity, Integer> results = new HashMap<>();

        if (playerIdentities.isEmpty()) {
            return results;
        }


        String placeholders = String.join(",", Collections.nCopies(playerIdentities.size(), "?"));
        String sql = "SELECT Nickname, " + resourceName +
                " FROM " + tableName +
                " WHERE Nickname IN (" + placeholders + ")";

        try (Connection conn = Shared.getInstance().getDatabase().getConnection().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            int i = 1;
            Map<String, PlayerIdentity> nicknameMap = new HashMap<>();
            for (PlayerIdentity playerIdentity : playerIdentities) {
                stmt.setString(i++, playerIdentity.getName());
                nicknameMap.put(playerIdentity.getName(), playerIdentity);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String nickname = rs.getString("Nickname");
                    PlayerIdentity playerIdentity = nicknameMap.get(nickname);
                    if (playerIdentity == null) continue;

                    int balance = rs.getInt(resourceName);
                    results.put(playerIdentity, balance);
                }
            }
        }

        for (PlayerIdentity playerIdentity : playerIdentities) {
            results.putIfAbsent(playerIdentity, 0);
        }

        return results;
    }

    private void saveBalanceToDB(Map<PlayerIdentity, CachedBatchStorage.PendingChange<PlayerIdentity, Integer>> changes)
            throws SQLException {
        if (changes.isEmpty()) return;

        try (SQLDatabaseConnection sql = Shared.getInstance().getDatabase().getConnection()) {
            Connection conn = sql.getConnection();
            conn.setAutoCommit(false);

            String setSql = "INSERT INTO " + tableName + " (Nickname, " + resourceName + ")" +
                    " VALUES (?, ?)" +
                    " ON DUPLICATE KEY UPDATE " + resourceName + " = VALUES(" + resourceName + ")";
            String modifySql = "INSERT INTO " + tableName + " (Nickname, " + resourceName + ")" +
                    " VALUES (?, ?)" +
                    " ON DUPLICATE KEY UPDATE " + resourceName + " = " + resourceName + " + VALUES(" + resourceName + ")";

            try (PreparedStatement setStmt = conn.prepareStatement(setSql);
                 PreparedStatement modifyStmt = conn.prepareStatement(modifySql)) {

                int setCount = 0;
                int modifyCount = 0;

                for (CachedBatchStorage.PendingChange<PlayerIdentity, Integer> change : changes.values()) {
                    PlayerIdentity player = change.getKey();
                    int value = change.getDelta();

                    if (change.isSet()) {
                        setStmt.setString(1, player.getName());
                        setStmt.setInt(2, value);
                        setStmt.addBatch();
                        setCount++;
                    } else {
                        modifyStmt.setString(1, player.getName());
                        modifyStmt.setInt(2, value);
                        modifyStmt.addBatch();
                        modifyCount++;
                    }
                }

                if (setCount > 0) {
                    setStmt.executeBatch();
                }
                if (modifyCount > 0) {
                    modifyStmt.executeBatch();
                }

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
    public void deposit(PlayerIdentity playerIdentity, int amount) {
        storage.modify(playerIdentity, amount);
    }

    @Override
    public void withdraw(PlayerIdentity playerIdentity, int amount) {
        storage.modify(playerIdentity, -amount);
    }

    @Override
    public CompletableFuture<Integer> getBalance(PlayerIdentity playerIdentity) {
        return storage.get(playerIdentity);
    }

    @Override
    public int getBalanceCached(PlayerIdentity playerIdentity) {
        Integer cached = storage.getCached(playerIdentity);
        if (cached == null) {
            if (playerIdentity.getOnlinePlayer() != null)
                playerIdentity.getOnlinePlayer().sendMessage("An error occurred. Your data wasn’t preloaded and isn’t available in the cache.");
            return 0;
        }

        return cached;
    }

    @Override
    public CompletableFuture<Void> preload(Iterable<PlayerIdentity> players) {
        Set<PlayerIdentity> playerSet = new HashSet<>();
        players.forEach(playerSet::add);
        return storage.preload(playerSet);
    }

    @Override
    public void shutdown() {
        storage.shutdown();
        storage = null;
        HandlerList.unregisterAll(playerQuitListener);
    }
}
