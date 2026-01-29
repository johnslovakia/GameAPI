package cz.johnslovakia.gameapi.database;

import cz.johnslovakia.gameapi.Shared;
import cz.johnslovakia.gameapi.users.PlayerIdentity;
import cz.johnslovakia.gameapi.utils.Logger;

import me.zort.sqllib.SQLDatabaseConnection;
import me.zort.sqllib.api.data.QueryResult;
import me.zort.sqllib.api.data.Row;
import me.zort.sqllib.pool.SQLConnectionPool;

import java.sql.*;
import java.util.*;

public class MinigameTable {

    private final String namespace;

    private Map<Type, List<String>> rows = new HashMap<>();

    public MinigameTable(String namespace, Map<Type, List<String>> rows) {
        this.namespace = namespace;
        this.rows = rows;
    }

    public MinigameTable(String namespace) {
        this.namespace = namespace;
    }

    public MinigameTable addColumn(Type type, String name){
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

    private static String getDefaultForType(Type type) {
        if (type.name().toLowerCase().contains("varchar")){
            return " DEFAULT NULL";
        }else if (type == Type.INT){
            return " NOT NULL DEFAULT 0";
        }else {
            return "";
        }
    }

    public MinigameTable createNewColumn(Type type, String name) {
        try (SQLDatabaseConnection dbConn = Shared.getInstance().getDatabase().getConnection()) {
            if (dbConn == null) return this;

            Connection conn = dbConn.getConnection();
            String checkSql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND COLUMN_NAME = ?";

            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, Shared.getInstance().getDatabase().getDatabase());
                checkStmt.setString(2, namespace);
                checkStmt.setString(3, name);

                try (ResultSet rs = checkStmt.executeQuery()) {
                    boolean exists = false;
                    if (rs.next()) {
                        exists = rs.getInt(1) > 0;
                    }

                    if (!exists) {
                        String sql = "ALTER TABLE `" + namespace + "` ADD `" + name + "` "
                                + type.b
                                + getDefaultForType(type);
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
        try (SQLDatabaseConnection connection = Shared.getInstance().getDatabase().getPool().getResource()) {

            Optional<Row> result = connection.select()
                    .from(namespace)
                    .where()
                    .isEqual("Nickname", playerIdentity.getOnlinePlayer().getName())
                    .obtainOne();

            if (result.isEmpty()) {
                connection.insert()
                        .into(namespace, "Nickname")
                        .values(playerIdentity.getOnlinePlayer().getName())
                        .execute();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void createTable() {
        StringBuilder rows_s = new StringBuilder(
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                        "Nickname VARCHAR(24) UNIQUE NOT NULL," +
                        "Cosmetics JSON," +
                        "Quests JSON," +
                        "Achievements JSON," +
                        "KitInventories JSON"
        );

        if (rows != null) {
            for (Type type : rows.keySet()) {
                for (String s : rows.get(type)) {
                    switch (type) {
                        case INT -> rows_s.append(", ").append(s).append(" INT DEFAULT 0");
                        case JSON -> rows_s.append(", ").append(s).append(" JSON");
                        case VARCHAR128 -> rows_s.append(", ").append(s).append(" VARCHAR(128)");
                        case VARCHAR256 -> rows_s.append(", ").append(s).append(" VARCHAR(256)");
                        case VARCHAR512 -> rows_s.append(", ").append(s).append(" VARCHAR(512)");
                        case VARCHAR1024 -> rows_s.append(", ").append(s).append(" VARCHAR(1024)");
                        case VARCHAR2048 -> rows_s.append(", ").append(s).append(" VARCHAR(2048)");
                    }
                }
            }
        }

        if (Shared.getInstance().getDatabase() == null) {
            Logger.log("You don't have the database set up in the config.yml!", Logger.LogType.ERROR);
            return;
        }

        try (SQLDatabaseConnection connection = Shared.getInstance().getDatabase().getPool().getResource()) {

            QueryResult result = connection.exec(() ->
                    "CREATE TABLE IF NOT EXISTS " + namespace + " (" + rows_s + ")"
            );

            if (!result.isSuccessful()) {
                Logger.log("Failed to create " + namespace + " table!", Logger.LogType.ERROR);
                Logger.log(result.getRejectMessage(), Logger.LogType.ERROR);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public String getTableName() {
        return namespace;
    }
}
