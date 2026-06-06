package cz.johnslovakia.gameapi.modules.resources.storage;

import cz.johnslovakia.gameapi.Core;
import cz.johnslovakia.gameapi.database.JSConfigs;
import cz.johnslovakia.gameapi.database.PlayerTable;
import cz.johnslovakia.gameapi.utils.BatchConfig;
import cz.johnslovakia.gameapi.utils.CachedBatchStorage;
import cz.johnslovakia.gameapi.utils.Logger;
import me.zort.sqllib.SQLDatabaseConnection;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.zip.CRC32;

public class JsonResourceStorage implements ResourceStorage {

    public static final String RESOURCE_COLUMN = "Resources";
    private static final String CONFIG_TABLE = "jsConfigs";

    private static volatile boolean columnReady = false;

    private final String resourceName;
    private final Set<String> legacyBatchedTables = new LinkedHashSet<>();

    private CachedBatchStorage<String, Integer> storage;
    private final Listener playerQuitListener;

    private volatile boolean migrationsChecked = false;

    public JsonResourceStorage(String resourceName, Collection<String> legacyBatchedTables) {
        this.resourceName = resourceName;
        if (legacyBatchedTables != null) {
            legacyBatchedTables.stream()
                    .filter(table -> table != null && !table.isBlank())
                    .forEach(this.legacyBatchedTables::add);
        }

        this.storage = new CachedBatchStorage<>(
                "resource-json-" + resourceName,
                BatchConfig.builder("resource-json-" + resourceName)
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

    public void addLegacyBatchedTables(Collection<String> tables) {
        if (tables == null) return;

        tables.stream()
                .filter(table -> table != null && !table.isBlank())
                .forEach(this.legacyBatchedTables::add);
        migrationsChecked = false;
    }

    public List<String> getLegacyBatchedTables() {
        return List.copyOf(legacyBatchedTables);
    }

    @Override
    public void onEnable() {
        ensureReady();
    }

    private void ensureReady() {
        ensureResourcesColumn();
        if (!migrationsChecked) {
            synchronized (this) {
                if (!migrationsChecked) {
                    migrationsChecked = migrateLegacyTables();
                }
            }
        }
    }

    private static boolean ensureResourcesColumn() {
        if (columnReady) return true;

        try {
            new PlayerTable().createTable();

            try (SQLDatabaseConnection dbConn = Core.getInstance().getDatabase().getConnection()) {
                if (dbConn == null) return false;

                Connection conn = dbConn.getConnection();
                String checkSql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND COLUMN_NAME = ?";

                try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                    checkStmt.setString(1, Core.getInstance().getDatabase().getDatabase());
                    checkStmt.setString(2, PlayerTable.TABLE_NAME);
                    checkStmt.setString(3, RESOURCE_COLUMN);

                    try (ResultSet rs = checkStmt.executeQuery()) {
                        if (rs.next() && rs.getInt(1) == 0) {
                            try (Statement alterStmt = conn.createStatement()) {
                                alterStmt.executeUpdate("ALTER TABLE `" + PlayerTable.TABLE_NAME + "` ADD `" + RESOURCE_COLUMN + "` JSON");
                            }
                        }
                    }
                }
            }

            columnReady = true;
            return true;
        } catch (Exception e) {
            Logger.log("Failed to prepare " + PlayerTable.TABLE_NAME + "." + RESOURCE_COLUMN + " column.", Logger.LogType.ERROR);
            e.printStackTrace();
            return false;
        }
    }

    private Map<String, Integer> loadBalanceFromDB(Set<String> nicknames) throws SQLException {
        ensureReady();

        Map<String, Integer> results = new HashMap<>();
        if (nicknames.isEmpty()) return results;

        String placeholders = String.join(",", Collections.nCopies(nicknames.size(), "?"));
        String sql = "SELECT Nickname, `" + RESOURCE_COLUMN + "` FROM `" + PlayerTable.TABLE_NAME + "` WHERE Nickname IN (" + placeholders + ")";

        try (SQLDatabaseConnection dbConn = Core.getInstance().getDatabase().getConnection()) {
            if (dbConn == null) return results;

            try (PreparedStatement stmt = dbConn.getConnection().prepareStatement(sql)) {
                int i = 1;
                for (String nickname : nicknames) {
                    stmt.setString(i++, nickname);
                }

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        results.put(rs.getString("Nickname"), getResourceValue(rs.getString(RESOURCE_COLUMN)));
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
        ensureReady();

        try (SQLDatabaseConnection dbConn = Core.getInstance().getDatabase().getConnection()) {
            if (dbConn == null) return;

            Connection conn = dbConn.getConnection();
            conn.setAutoCommit(false);

            try {
                try (PreparedStatement ensureRowStmt = conn.prepareStatement(
                             "INSERT INTO `" + PlayerTable.TABLE_NAME + "` (Nickname, `" + RESOURCE_COLUMN + "`) VALUES (?, ?) " +
                                     "ON DUPLICATE KEY UPDATE Nickname = Nickname"
                     );
                     PreparedStatement selectStmt = conn.prepareStatement(
                             "SELECT `" + RESOURCE_COLUMN + "` FROM `" + PlayerTable.TABLE_NAME + "` WHERE Nickname = ? FOR UPDATE"
                     );
                     PreparedStatement updateStmt = conn.prepareStatement(
                             "UPDATE `" + PlayerTable.TABLE_NAME + "` SET `" + RESOURCE_COLUMN + "` = ? WHERE Nickname = ?"
                     )) {

                    for (CachedBatchStorage.PendingChange<String, Integer> change : changes.values()) {
                        applyChange(ensureRowStmt, selectStmt, updateStmt, change);
                    }
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

    private void applyChange(
            PreparedStatement ensureRowStmt,
            PreparedStatement selectStmt,
            PreparedStatement updateStmt,
            CachedBatchStorage.PendingChange<String, Integer> change
    ) throws SQLException {
        String nickname = change.getKey();

        ensureRowStmt.setString(1, nickname);
        ensureRowStmt.setString(2, "{}");
        ensureRowStmt.executeUpdate();

        selectStmt.setString(1, nickname);
        JSONObject resources = new JSONObject();
        try (ResultSet rs = selectStmt.executeQuery()) {
            if (rs.next()) {
                resources = parseResources(rs.getString(RESOURCE_COLUMN));
            }
        }

        int current = resources.optInt(resourceName, 0);
        int updated = change.isSet() ? change.getDelta() : current + change.getDelta();
        resources.put(resourceName, updated);

        updateStmt.setString(1, resources.toString());
        updateStmt.setString(2, nickname);
        updateStmt.executeUpdate();
    }

    private boolean migrateLegacyTables() {
        if (legacyBatchedTables.isEmpty()) return true;
        if (!ensureResourcesColumn()) return false;

        boolean allDone = true;
        for (String tableName : legacyBatchedTables) {
            if (!isValidIdentifier(tableName) || !isValidIdentifier(resourceName)) {
                Logger.log("Skipping resource migration for invalid identifier: " + tableName + "." + resourceName, Logger.LogType.WARNING);
                continue;
            }

            try {
                String migrationKey = migrationKey(tableName);
                MigrationClaim claim = claimMigration(migrationKey);
                if (claim == MigrationClaim.DONE) continue;
                if (claim == MigrationClaim.RUNNING) {
                    allDone = false;
                    continue;
                }

                if (!legacyColumnExists(tableName)) {
                    Logger.log("Resource migration skipped for " + resourceName + ": " + tableName + "." + resourceName + " does not exist yet.", Logger.LogType.WARNING);
                    clearMigrationClaim(migrationKey);
                    allDone = false;
                    continue;
                }

                migrateLegacyTable(tableName, migrationKey);
                Logger.log("Migrated resource " + resourceName + " from " + tableName + " into " + PlayerTable.TABLE_NAME + "." + RESOURCE_COLUMN + ".", Logger.LogType.INFO);
            } catch (Exception e) {
                allDone = false;
                Logger.log("Failed to migrate resource " + resourceName + " from " + tableName + ".", Logger.LogType.ERROR);
                e.printStackTrace();
            }
        }
        return allDone;
    }

    private MigrationClaim claimMigration(String migrationKey) throws SQLException {
        JSConfigs.createTable();

        try (SQLDatabaseConnection dbConn = Core.getInstance().getDatabase().getConnection()) {
            if (dbConn == null) return MigrationClaim.RUNNING;

            Connection conn = dbConn.getConnection();
            conn.setAutoCommit(false);

            try {
                try (PreparedStatement selectStmt = conn.prepareStatement(
                             "SELECT value FROM `" + CONFIG_TABLE + "` WHERE `key` = ? FOR UPDATE"
                     );
                     PreparedStatement insertStmt = conn.prepareStatement(
                             "INSERT INTO `" + CONFIG_TABLE + "` (`key`, value) VALUES (?, ?)"
                     );
                     PreparedStatement updateStmt = conn.prepareStatement(
                             "UPDATE `" + CONFIG_TABLE + "` SET value = ?, last_updated = CURRENT_TIMESTAMP WHERE `key` = ?"
                     )) {

                    selectStmt.setString(1, migrationKey);
                    try (ResultSet rs = selectStmt.executeQuery()) {
                        if (rs.next()) {
                            String value = rs.getString("value");
                            if ("done".equalsIgnoreCase(value)) {
                                conn.commit();
                                return MigrationClaim.DONE;
                            }
                            if (value != null && value.startsWith("running:") && !isStaleClaim(value)) {
                                conn.commit();
                                return MigrationClaim.RUNNING;
                            }

                            updateStmt.setString(1, runningMarker());
                            updateStmt.setString(2, migrationKey);
                            updateStmt.executeUpdate();
                            conn.commit();
                            return MigrationClaim.CLAIMED;
                        }
                    }

                    insertStmt.setString(1, migrationKey);
                    insertStmt.setString(2, runningMarker());
                    insertStmt.executeUpdate();
                    conn.commit();
                    return MigrationClaim.CLAIMED;
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private void clearMigrationClaim(String migrationKey) {
        try (SQLDatabaseConnection dbConn = Core.getInstance().getDatabase().getConnection()) {
            if (dbConn == null) return;

            try (PreparedStatement stmt = dbConn.getConnection().prepareStatement(
                    "DELETE FROM `" + CONFIG_TABLE + "` WHERE `key` = ? AND value LIKE 'running:%'"
            )) {
                stmt.setString(1, migrationKey);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private boolean legacyColumnExists(String tableName) throws SQLException {
        try (SQLDatabaseConnection dbConn = Core.getInstance().getDatabase().getConnection()) {
            if (dbConn == null) return false;

            String sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND COLUMN_NAME = ?";
            try (PreparedStatement stmt = dbConn.getConnection().prepareStatement(sql)) {
                stmt.setString(1, Core.getInstance().getDatabase().getDatabase());
                stmt.setString(2, tableName);
                stmt.setString(3, resourceName);

                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next() && rs.getInt(1) > 0;
                }
            }
        }
    }

    private void migrateLegacyTable(String tableName, String migrationKey) throws SQLException {
        String legacySql = "SELECT Nickname, `" + resourceName + "` FROM `" + tableName + "` WHERE `" + resourceName + "` <> 0";

        try (SQLDatabaseConnection dbConn = Core.getInstance().getDatabase().getConnection()) {
            if (dbConn == null) return;

            Connection conn = dbConn.getConnection();
            List<CachedBatchStorage.PendingChange<String, Integer>> legacyChanges = new ArrayList<>();
            try (Statement legacyStmt = conn.createStatement();
                 ResultSet rs = legacyStmt.executeQuery(legacySql)) {
                while (rs.next()) {
                    String nickname = rs.getString("Nickname");
                    int amount = rs.getInt(resourceName);
                    legacyChanges.add(new CachedBatchStorage.PendingChange<>(nickname, amount, System.currentTimeMillis()));
                }
            }

            conn.setAutoCommit(false);

            try (PreparedStatement ensureRowStmt = conn.prepareStatement(
                         "INSERT INTO `" + PlayerTable.TABLE_NAME + "` (Nickname, `" + RESOURCE_COLUMN + "`) VALUES (?, ?) " +
                                 "ON DUPLICATE KEY UPDATE Nickname = Nickname"
                 );
                 PreparedStatement selectStmt = conn.prepareStatement(
                         "SELECT `" + RESOURCE_COLUMN + "` FROM `" + PlayerTable.TABLE_NAME + "` WHERE Nickname = ? FOR UPDATE"
                 );
                 PreparedStatement updateStmt = conn.prepareStatement(
                         "UPDATE `" + PlayerTable.TABLE_NAME + "` SET `" + RESOURCE_COLUMN + "` = ? WHERE Nickname = ?"
                 );
                 PreparedStatement completeStmt = conn.prepareStatement(
                         "UPDATE `" + CONFIG_TABLE + "` SET value = ?, last_updated = CURRENT_TIMESTAMP WHERE `key` = ?"
                 )) {

                for (CachedBatchStorage.PendingChange<String, Integer> change : legacyChanges) {
                    applyChange(ensureRowStmt, selectStmt, updateStmt, change);
                }

                completeStmt.setString(1, "done");
                completeStmt.setString(2, migrationKey);
                completeStmt.executeUpdate();

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private int getResourceValue(String rawJson) {
        return parseResources(rawJson).optInt(resourceName, 0);
    }

    private static JSONObject parseResources(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return new JSONObject();
        }

        try {
            return new JSONObject(rawJson);
        } catch (Exception ignored) {
            return new JSONObject();
        }
    }

    private static boolean isValidIdentifier(String identifier) {
        return identifier != null && identifier.matches("[A-Za-z0-9_]+");
    }

    private String migrationKey(String tableName) {
        CRC32 crc = new CRC32();
        crc.update((resourceName + "|" + tableName).toLowerCase().getBytes(StandardCharsets.UTF_8));
        return "resourceMigration." + Long.toHexString(crc.getValue());
    }

    private static String runningMarker() {
        return "running:" + System.currentTimeMillis();
    }

    private static boolean isStaleClaim(String marker) {
        try {
            long startedAt = Long.parseLong(marker.substring("running:".length()));
            return System.currentTimeMillis() - startedAt > 10 * 60 * 1000L;
        } catch (Exception ignored) {
            return true;
        }
    }

    private enum MigrationClaim {
        CLAIMED,
        DONE,
        RUNNING
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
