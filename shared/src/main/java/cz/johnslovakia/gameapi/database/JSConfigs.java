package cz.johnslovakia.gameapi.database;

import cz.johnslovakia.gameapi.Shared;
import cz.johnslovakia.gameapi.utils.Logger;
import me.zort.sqllib.SQLDatabaseConnection;
import me.zort.sqllib.api.data.QueryResult;
import me.zort.sqllib.pool.SQLConnectionPool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class JSConfigs {

    public static void createTable() {
        if (Shared.getInstance().getDatabase() == null) return;

        try (SQLDatabaseConnection connection = Shared.getInstance().getDatabase().getPool().getResource()) {

            QueryResult result = connection.exec(() ->
                    "CREATE TABLE IF NOT EXISTS jsConfigs (" +
                            "id INT AUTO_INCREMENT PRIMARY KEY," +
                            "`key` VARCHAR(100) NOT NULL UNIQUE," +
                            "value LONGTEXT NOT NULL," +
                            "last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                            ");"
            );

            if (!result.isSuccessful()) {
                Logger.log("Failed to create game_configs table!", Logger.LogType.ERROR);
                Logger.log(result.getRejectMessage(), Logger.LogType.ERROR);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public JSConfigs() {
    }

    public void saveConfig(String key, String json) {
        try (SQLDatabaseConnection dbConn = Shared.getInstance().getDatabase().getPool().getResource()) {
            Connection conn = dbConn.getConnection();

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO jsConfigs (`key`, value) VALUES (?, ?) " +
                            "ON DUPLICATE KEY UPDATE value = VALUES(value), last_updated = CURRENT_TIMESTAMP"
            )) {
                ps.setString(1, key);
                ps.setString(2, json);
                ps.executeUpdate();
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public String loadConfig(String key) {
        try (SQLDatabaseConnection dbConn = Shared.getInstance().getDatabase().getPool().getResource()) {
            Connection conn = dbConn.getConnection();

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT value FROM jsConfigs WHERE `key` = ?"
            )) {
                ps.setString(1, key);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("value");
                    }
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return null;
    }
}
