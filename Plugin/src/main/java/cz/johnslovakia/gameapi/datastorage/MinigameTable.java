package cz.johnslovakia.gameapi.datastorage;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.messages.Language;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.utils.Logger;
import me.zort.sqllib.SQLDatabaseConnection;
import me.zort.sqllib.api.data.QueryResult;
import me.zort.sqllib.api.data.Row;

import java.util.*;

public class MinigameTable {

    private String TABLE_NAME;
    private Minigame minigame;

    private Map<Type, List<String>> rows = new HashMap<>();

    public MinigameTable(Minigame minigame, Map<Type, List<String>> rows) {
        this.minigame = minigame;
        this.rows = rows;
        this.TABLE_NAME = minigame.getName();
    }

    public MinigameTable(Minigame minigame) {
        this.minigame = minigame;
        this.TABLE_NAME = minigame.getName();
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
        SQLDatabaseConnection connection = GameAPI.getInstance().getMinigame().getDatabase().getConnection();
        if (connection != null) {
            QueryResult result = connection.exec(() ->
                    "ALTER TABLE " + TABLE_NAME +
                            " ADD IF NOT EXISTS " + name + " " + type.getB() + (type.equals(Type.INT) ? " DEFAULT 0" : ""));

            if (!result.isSuccessful()) {
                Logger.log("Failed to add new column " + TABLE_NAME + " table!", Logger.LogType.ERROR);
                Logger.log(result.getRejectMessage(), Logger.LogType.ERROR);
            }
        }
        return this;
    }

    public void newUser(GamePlayer gamePlayer){
        SQLDatabaseConnection connection = GameAPI.getInstance().getMinigame().getDatabase().getConnection();
        if (connection == null){
            return;
        }

        Optional<Row> result = connection.select()
                .from(TABLE_NAME)
                .where().isEqual("Nickname", gamePlayer.getOnlinePlayer().getName())
                .obtainOne();

        if (result.isEmpty()) {
            connection.insert()
                    .into(TABLE_NAME, "Nickname")
                    .values(gamePlayer.getOnlinePlayer().getName())
                    .execute();
        }
    }

    public void createTable() {
        StringBuilder rows_s = new StringBuilder("id INT AUTO_INCREMENT PRIMARY KEY," +
                "Nickname VARCHAR(24) UNIQUE NOT NULL," +
                "Cosmetics JSON," +
                "Quests JSON," +
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


        if (GameAPI.getInstance().getMinigame().getDatabase() == null){
            Logger.log("You don't have the database set up in the config.yml!", Logger.LogType.ERROR);
            return;
        }
        SQLDatabaseConnection connection = GameAPI.getInstance().getMinigame().getDatabase().getConnection();
        if (connection == null){
            return;
        }
        QueryResult result = connection.exec(() ->
                "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + "("
                        + rows_s + ");");

        if (!result.isSuccessful()) {
            Logger.log("Failed to create " + TABLE_NAME + " table!", Logger.LogType.ERROR);
            Logger.log(result.getRejectMessage(), Logger.LogType.ERROR);
        }
    }

    public String getTableName() {
        return TABLE_NAME;
    }
}
