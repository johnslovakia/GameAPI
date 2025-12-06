package cz.johnslovakia.gameapi.database;

import cz.johnslovakia.gameapi.Shared;
import cz.johnslovakia.gameapi.users.PlayerIdentity;
import cz.johnslovakia.gameapi.utils.Logger;

import me.zort.sqllib.SQLDatabaseConnection;
import me.zort.sqllib.api.data.QueryResult;
import me.zort.sqllib.api.data.Row;

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

    public MinigameTable createNewColumn(Type type, String name) {
        SQLDatabaseConnection connection = Shared.getInstance().getDatabase().getConnection();
        if (connection != null) {
            Connection conn = Shared.getInstance().getDatabase().getConnection().getConnection();

            try (
                    PreparedStatement checkStmt = conn.prepareStatement(
                            "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND COLUMN_NAME = ?"
                    )
            ) {
                checkStmt.setString(1, Shared.getInstance().getDatabase().getDatabase());
                checkStmt.setString(2, namespace);
                checkStmt.setString(3, name);

                ResultSet rs = checkStmt.executeQuery();
                boolean exists = false;

                if (rs.next()) {
                    exists = rs.getInt(1) > 0;
                }

                rs.close();

                if (!exists) {
                    try (Statement alterStmt = conn.createStatement()) {
                        String sql = "ALTER TABLE `" + namespace + "` ADD `" + name + "` " + type.b + (type == Type.INT ? " DEFAULT 0" : "");
                        alterStmt.executeUpdate(sql);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return this;
    }

    public void newUser(PlayerIdentity playerIdentity){
        SQLDatabaseConnection connection = Shared.getInstance().getDatabase().getConnection();
        if (connection == null){
            return;
        }

        Optional<Row> result = connection.select()
                .from(namespace)
                .where().isEqual("Nickname", playerIdentity.getOnlinePlayer().getName())
                .obtainOne();

        if (result.isEmpty()) {
            connection.insert()
                    .into(namespace, "Nickname")
                    .values(playerIdentity.getOnlinePlayer().getName())
                    .execute();
        }
    }

    public void createTable() {
        StringBuilder rows_s = new StringBuilder("id INT AUTO_INCREMENT PRIMARY KEY," +
                "Nickname VARCHAR(24) UNIQUE NOT NULL," +
                "Cosmetics JSON," +
                "Quests JSON," +
                "Achievements JSON," +
                "KitInventories JSON");

        if (rows != null) {
            for (Type type : rows.keySet()) {
                if (type == Type.INT) {
                    for (String s : rows.get(type)) {
                        rows_s.append(", ").append(s).append(" int DEFAULT 0");
                    }
                } else if (type == Type.JSON) {
                    for (String s : rows.get(type)) {
                        rows_s.append(", ").append(s).append(" JSON");
                    }
                } else if (type == Type.VARCHAR128) {
                    for (String s : rows.get(type)) {
                        rows_s.append(", ").append(s).append(" VARCHAR(128)");
                    }
                } else if (type == Type.VARCHAR256) {
                    for (String s : rows.get(type)) {
                        rows_s.append(", ").append(s).append(" VARCHAR(256)");
                    }
                } else if (type == Type.VARCHAR512) {
                    for (String s : rows.get(type)) {
                        rows_s.append(", ").append(s).append(" VARCHAR(512)");
                    }
                } else if (type == Type.VARCHAR1024) {
                    for (String s : rows.get(type)) {
                        rows_s.append(", ").append(s).append(" VARCHAR(1024)");
                    }
                } else if (type == Type.VARCHAR2048) {
                    for (String s : rows.get(type)) {
                        rows_s.append(", ").append(s).append(" VARCHAR(2048)");
                    }
                }
            }
        }


        if (Shared.getInstance().getDatabase() == null){
            Logger.log("You don't have the database set up in the config.yml!", Logger.LogType.ERROR);
            return;
        }
        SQLDatabaseConnection connection = Shared.getInstance().getDatabase().getConnection();
        if (connection == null){
            return;
        }
        QueryResult result = connection.exec(() ->
                "CREATE TABLE IF NOT EXISTS " + namespace + "("
                        + rows_s + ");");

        if (!result.isSuccessful()) {
            Logger.log("Failed to create " + namespace + " table!", Logger.LogType.ERROR);
            Logger.log(result.getRejectMessage(), Logger.LogType.ERROR);
        }
    }

    public String getTableName() {
        return namespace;
    }
}
