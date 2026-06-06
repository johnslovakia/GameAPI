package cz.johnslovakia.gameapi.database;

import cz.johnslovakia.gameapi.Core;
import cz.johnslovakia.gameapi.utils.Logger;
import me.zort.sqllib.SQLDatabaseConnection;
import me.zort.sqllib.api.data.QueryResult;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class JSConfigs {

    private static final String TABLE_NAME = "jsConfigs";
    private static volatile boolean tableReady = false;

    public static void createTable() {
        ensureTable();
    }

    private static boolean ensureTable() {
        if (tableReady) return true;

        Core core = Core.getInstance();
        if (core == null || core.getDatabase() == null) return false;

        synchronized (JSConfigs.class) {
            if (tableReady) return true;

            try (SQLDatabaseConnection connection = core.getDatabase().getPool().getResource()) {

                QueryResult result = connection.exec(() ->
                        "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                                "id INT AUTO_INCREMENT PRIMARY KEY," +
                                "`key` VARCHAR(100) NOT NULL UNIQUE," +
                                "value LONGTEXT NOT NULL," +
                                "last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                                ");"
                );

                if (!result.isSuccessful()) {
                    Logger.log("Failed to create " + TABLE_NAME + " table!", Logger.LogType.ERROR);
                    Logger.log(result.getRejectMessage(), Logger.LogType.ERROR);
                    return false;
                }

                tableReady = true;
                return true;

            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }
    }


    public JSConfigs() {
    }

    public void saveConfig(String key, String json) {
        if (!ensureTable()) return;

        try (SQLDatabaseConnection dbConn = Core.getInstance().getDatabase().getPool().getResource()) {
            Connection conn = dbConn.getConnection();

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO " + TABLE_NAME + " (`key`, value) VALUES (?, ?) " +
                            "ON DUPLICATE KEY UPDATE value = VALUES(value), last_updated = CURRENT_TIMESTAMP"
            )) {
                ps.setString(1, key);
                ps.setString(2, json);
                ps.executeUpdate();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String loadConfig(String key) {
        if (!ensureTable()) return null;

        try (SQLDatabaseConnection dbConn = Core.getInstance().getDatabase().getPool().getResource()) {
            Connection conn = dbConn.getConnection();

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT value FROM " + TABLE_NAME + " WHERE `key` = ?"
            )) {
                ps.setString(1, key);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("value");
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }
}
