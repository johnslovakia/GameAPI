package cz.johnslovakia.gameapi.database;

import cz.johnslovakia.gameapi.Shared;
import cz.johnslovakia.gameapi.modules.messages.Language;
import cz.johnslovakia.gameapi.users.PlayerIdentity;
import cz.johnslovakia.gameapi.utils.Logger;
import me.zort.sqllib.SQLDatabaseConnection;
import me.zort.sqllib.api.data.QueryResult;
import me.zort.sqllib.api.data.Row;

import java.sql.*;
import java.util.*;

public class PlayerTable {

    public static final String TABLE_NAME = "gameapi_playertable";

    private Map<Type, List<String>> rows;

    public PlayerTable() {
    }

    public PlayerTable addColumn(Type type, String name){
        if (rows == null) rows = new HashMap<>();

        List<String> rs = new ArrayList<>();
        if (rows.get(type) != null){
            rs.addAll(rows.get(type));
            rs.add(name);
        }else{
            rs.add(name);
        }

        rows.put(type, rs);
        return this;
    }

    public PlayerTable createNewColumn(Type type, String name) {
        createNewColumn(type, name, null);
        return this;
    }

    public PlayerTable createNewColumn(Type type, String name, String def) {
        try (SQLDatabaseConnection dbConn = Shared.getInstance().getDatabase().getConnection()) {
            if (dbConn == null) return this;

            Connection conn = dbConn.getConnection();
            String checkSql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND COLUMN_NAME = ?";

            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, Shared.getInstance().getDatabase().getDatabase());
                checkStmt.setString(2, TABLE_NAME);
                checkStmt.setString(3, name);

                try (ResultSet rs = checkStmt.executeQuery()) {
                    boolean exists = false;
                    if (rs.next()) {
                        exists = rs.getInt(1) > 0;
                    }

                    if (!exists) {
                        String sql = "ALTER TABLE `" + TABLE_NAME + "` ADD `" + name + "` " + type.b +
                                (type == Type.INT ? " DEFAULT " + (def != null ? def : "0") : "");
                        try (Statement alterStmt = conn.createStatement()) {
                            alterStmt.executeUpdate(sql);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return this;
    }

    public void newUser(PlayerIdentity playerIdentity) {
        if (Shared.getInstance().getDatabase() == null) {
            Logger.log("You don't have the database set up in the config.yml!", Logger.LogType.ERROR);
            return;
        }

        try (SQLDatabaseConnection connection = Shared.getInstance().getDatabase().getConnection()) {
            if (connection == null) return;

            Optional<Row> result = connection.select()
                    .from(TABLE_NAME)
                    .where().isEqual("Nickname", playerIdentity.getOnlinePlayer().getName())
                    .obtainOne();

            if (result.isEmpty()) {
                connection.insert()
                        .into(TABLE_NAME, "Nickname", "Language")
                        .values(
                                playerIdentity.getOnlinePlayer().getName(),
                                Language.getDefaultLanguage().getName()
                        )
                        .execute();
            }
        }catch (Exception exception){
            exception.printStackTrace();
        }
    }

    public void createTable() {
        StringBuilder rows_s = new StringBuilder(
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                        "Nickname VARCHAR(24) UNIQUE NOT NULL," +
                        "Language VARCHAR(32) DEFAULT 'English'"
        );

        if (rows != null) {
            for (Type type : rows.keySet()) {
                for (String s : rows.get(type)) {
                    if (type == Type.INT) {
                        rows_s.append(", ").append(s).append(" INT DEFAULT 0");
                    } else if (type == Type.JSON) {
                        rows_s.append(", ").append(s).append(" JSON");
                    } else if (type == Type.VARCHAR128) {
                        rows_s.append(", ").append(s).append(" VARCHAR(128)");
                    } else if (type == Type.VARCHAR256) {
                        rows_s.append(", ").append(s).append(" VARCHAR(256)");
                    } else if (type == Type.VARCHAR512) {
                        rows_s.append(", ").append(s).append(" VARCHAR(512)");
                    } else if (type == Type.VARCHAR1024) {
                        rows_s.append(", ").append(s).append(" VARCHAR(1024)");
                    } else if (type == Type.VARCHAR2048) {
                        rows_s.append(", ").append(s).append(" VARCHAR(2048)");
                    }
                }
            }
        }

        if (Shared.getInstance().getDatabase() == null) return;

        try (SQLDatabaseConnection connection = Shared.getInstance().getDatabase().getConnection()) {
            if (connection == null) return;

            QueryResult result = connection.exec(() ->
                    "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" + rows_s + ");"
            );

            if (!result.isSuccessful()) {
                Logger.log("Failed to create " + TABLE_NAME + " table!", Logger.LogType.ERROR);
                Logger.log(result.getRejectMessage(), Logger.LogType.ERROR);
            }
        }catch (Exception exception){
            exception.printStackTrace();
        }
    }
}
