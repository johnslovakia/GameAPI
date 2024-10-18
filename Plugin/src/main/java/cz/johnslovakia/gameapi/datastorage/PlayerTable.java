package cz.johnslovakia.gameapi.datastorage;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.messages.Language;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.utils.Logger;
import me.zort.sqllib.SQLDatabaseConnection;
import me.zort.sqllib.api.data.QueryResult;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PlayerTable {

    public static final String TABLE_NAME = "gameapi_playertable";

    private GamePlayer gamePlayer;
    private Map<Type, List<String>> rows = null;

    public PlayerTable(GamePlayer gamePlayer) {
        this.gamePlayer = gamePlayer;
    }

    public PlayerTable() {
    }

    public PlayerTable addRow(Type type, String name){
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

    public void newUser(GamePlayer gamePlayer){
        SQLDatabaseConnection connection = GameAPI.getInstance().getMinigame().getDatabase();
        if (connection == null){
            return;
        }

        QueryResult result = connection.insert()
                .into(TABLE_NAME, "Nickname", "Language")
                .values(gamePlayer.getOnlinePlayer().getName(), Language.getDefaultLanguage())
                .execute();
    }

    public void createTable() {

        StringBuilder rows_s = new StringBuilder("id INT AUTO_INCREMENT PRIMARY KEY," +
                "Nickname VARCHAR(24) UNIQUE NOT NULL," +
                "Language VARCHAR(32) NOT NULL");

        if (rows != null) {
            for (Type type : rows.keySet()) {
                if (type == Type.INT) {
                    for (String s : rows.get(type)) {
                        rows_s.append(", ").append(s).append(" int NOT NULL");
                    }
                } else if (type == Type.JSON) {
                    for (String s : rows.get(type)) {
                        rows_s.append(", ").append(s).append(" JSON");
                    }
                } else if (type == Type.VARCHAR128) {
                    for (String s : rows.get(type)) {
                        rows_s.append(", ").append(s).append(" VARCHAR(128) NOT NULL");
                    }
                } else if (type == Type.VARCHAR256) {
                    for (String s : rows.get(type)) {
                        rows_s.append(", ").append(s).append(" VARCHAR(256) NOT NULL");
                    }
                } else if (type == Type.VARCHAR512) {
                    for (String s : rows.get(type)) {
                        rows_s.append(", ").append(s).append(" VARCHAR(512) NOT NULL");
                    }
                } else if (type == Type.VARCHAR1024) {
                    for (String s : rows.get(type)) {
                        rows_s.append(", ").append(s).append(" VARCHAR(1024) NOT NULL");
                    }
                } else if (type == Type.VARCHAR2048) {
                    for (String s : rows.get(type)) {
                        rows_s.append(", ").append(s).append(" VARCHAR(2048) NOT NULL");
                    }
                }
            }
        }


        SQLDatabaseConnection connection = GameAPI.getInstance().getMinigame().getDatabase();
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
}
