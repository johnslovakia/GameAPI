package cz.johnslovakia.gameapi.database;

import cz.johnslovakia.gameapi.Shared;
import cz.johnslovakia.gameapi.utils.Logger;
import me.zort.sqllib.SQLDatabaseConnection;

import java.sql.*;

public class DatabaseMigrationHelper {

    public static boolean ensureNicknameUnique(String tableName) {
        SQLDatabaseConnection connection = Shared.getInstance().getDatabase().getConnection();
        if (connection == null) {
            Logger.log("Database connection is null!", Logger.LogType.ERROR);
            return false;
        }

        Connection conn = connection.getConnection();

        try {
            if (!tableExists(conn, tableName)) {
                Logger.log("Table " + tableName + " does not exist yet. Skipping UNIQUE constraint migration.", Logger.LogType.INFO);
                return true;
            }

            if (hasUniqueConstraint(conn, tableName, "Nickname")) {
                Logger.log("UNIQUE constraint on Nickname already exists in " + tableName, Logger.LogType.INFO);
                return true;
            }

            if (hasDuplicateNicknames(conn, tableName)) {
                Logger.log("Found duplicate nicknames in " + tableName + ". Cleaning up...", Logger.LogType.WARNING);
                cleanupDuplicates(conn, tableName);
            }

            try (Statement stmt = conn.createStatement()) {
                String sql = "ALTER TABLE `" + tableName + "` ADD UNIQUE KEY `unique_nickname` (`Nickname`)";
                stmt.executeUpdate(sql);
                Logger.log("Successfully added UNIQUE constraint to Nickname in " + tableName, Logger.LogType.INFO);
                return true;
            }

        } catch (SQLException e) {
            Logger.log("Error while ensuring UNIQUE constraint on " + tableName + ": " + e.getMessage(), Logger.LogType.ERROR);
            e.printStackTrace();
            return false;
        }
    }

    private static boolean tableExists(Connection conn, String tableName) throws SQLException {
        String database = Shared.getInstance().getDatabase().getDatabase();

        String query = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES " +
                "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?";

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, database);
            stmt.setString(2, tableName);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    private static boolean hasUniqueConstraint(Connection conn, String tableName, String columnName) throws SQLException {
        String database = Shared.getInstance().getDatabase().getDatabase();

        String query = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS " +
                "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND COLUMN_NAME = ? AND NON_UNIQUE = 0";

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, database);
            stmt.setString(2, tableName);
            stmt.setString(3, columnName);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    private static boolean hasDuplicateNicknames(Connection conn, String tableName) throws SQLException {
        String query = "SELECT COUNT(*) FROM (SELECT Nickname FROM `" + tableName + "` GROUP BY Nickname HAVING COUNT(*) > 1) as duplicates";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    private static void cleanupDuplicates(Connection conn, String tableName) throws SQLException {
        String deleteSql = "DELETE t1 FROM `" + tableName + "` t1 " +
                "INNER JOIN `" + tableName + "` t2 " +
                "WHERE t1.Nickname = t2.Nickname AND t1.id < t2.id";

        try (Statement stmt = conn.createStatement()) {
            int deleted = stmt.executeUpdate(deleteSql);
            if (deleted > 0) {
                Logger.log("Cleaned up " + deleted + " duplicate records from " + tableName, Logger.LogType.INFO);
            }
        }
    }
}