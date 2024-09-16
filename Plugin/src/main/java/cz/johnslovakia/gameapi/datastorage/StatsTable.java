package cz.johnslovakia.gameapi.datastorage;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.users.stats.Stat;
import cz.johnslovakia.gameapi.utils.Logger;
import me.zort.sqllib.SQLDatabaseConnection;
import me.zort.sqllib.api.data.QueryResult;
import me.zort.sqllib.api.data.Row;

import java.sql.*;
import java.util.*;

public class StatsTable {


    private Minigame minigame;
    private String TABLE_NAME;

    public StatsTable() {
        this.minigame = GameAPI.getInstance().getMinigame();
        this.TABLE_NAME = GameAPI.getInstance().getMinigame().getMinigameName() + "_stats";
    }
    public void createTable() {
        StringBuilder stats_s = new StringBuilder("`id` INT AUTO_INCREMENT PRIMARY KEY, `Nickname` VARCHAR(32) NOT NULL");

        List<String> stats = new ArrayList<>();
        for (Stat stat : GameAPI.getInstance().getStatsManager().getStats()){
            stats.add(stat.getName());
        }

        for (String s : stats) {
            stats_s.append(", `").append(s).append("` int NOT NULL");
        }

        SQLDatabaseConnection connection = GameAPI.getInstance().getMinigame().getDatabase();
        String finalStats_s = stats_s.toString();
        QueryResult result = connection.exec(() ->
                "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + "("
                        + finalStats_s + ");");

        if (!result.isSuccessful()) {
            Logger.log("Failed to create " + TABLE_NAME + " table!", Logger.LogType.ERROR);
            Logger.log(result.getRejectMessage(), Logger.LogType.ERROR);
        }
    }

    public void createMySQLUser(String nick) {

        List<String> stats = new ArrayList<>();
        for (Stat stat : GameAPI.getInstance().getStatsManager().getStats()){
            stats.add(stat.getName());
        }

        String stats_s = "`Nickname`";
        String stats_s_1 = "";
        int i = 0;
        for (String s : stats) {
            i++;
            stats_s = stats_s + ", `" + s + "`";
            if (stats.size() == i) {
                stats_s_1 = stats_s_1 + "?";
            } else {
                stats_s_1 = stats_s_1 + "?, ";
            }
        }

        String query = "INSERT IGNORE INTO `" + TABLE_NAME + "` (" + stats_s + ") VALUES (?, " + stats_s_1 + ")";
        minigame.getDatabase().exec(query);
    }


    public void setStat(String nick, String statName, int count) {
        minigame.getDatabase().update()
                .table(TABLE_NAME)
                .set(statName, count)
                .where().isEqual("Nickname", nick)
                .execute();
    }

    public void addStat(String nick, String statName, int count) {
        setStat(nick, statName, getStat(nick, statName) + 1);
    }

    public void removeStat(String nick, String statName, int count) {
        int i = getStat(nick, statName);
        if (i <= 0) {
            return;
        }
        if ((count - 1) < 0) {
            return;
        }
        setStat(nick, statName, (i - 1));
    }

    public int getStat(String nick, String statName) {
        Optional<Row> result = minigame.getDatabase().select()
                .from(TABLE_NAME)
                .where().isEqual("Nickname", nick)
                .obtainOne();

        if (result.isPresent()){
            if (result.get().get(statName) != null){
                return result.get().getInt(statName);
            }
        }
        return 0;
    }

    public HashMap<String, Integer> getAllStats(String nick) {
        HashMap<String, Integer> stats = new HashMap<String, Integer>();

        List<String> stats2 = new ArrayList<>();
        for (Stat stat : GameAPI.getInstance().getStatsManager().getStats()){
            stats2.add(stat.getName());
        }


        String query = "SELECT * FROM `" + TABLE_NAME + "` WHERE nick=?";

        PreparedStatement p = null;
        ResultSet rs = null;
        try {
            p = minigame.getDatabase().getConnection().prepareStatement(query);

            p.setString(1, nick);
            rs = p.executeQuery();

            if (rs.next()) {
                for (String s : stats2) {
                    stats.put(s, rs.getInt(s));
                }
            } else {
                for (String s : stats2) {
                    stats.put(s, 0);
                }
            }
        } catch (SQLException e) {
            e.getStackTrace();
            for (String s : stats2) {
                stats.put(s, 0);
            }
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (p != null) {
                try {
                    p.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return stats;
    }

    public HashMap<String, Integer> topStats(String type, int limit) {

        String query = "SELECT `" + type + "`, Nickname FROM `" + TABLE_NAME + "` ORDER BY `" + type + "` DESC LIMIT " + limit + ";";

        PreparedStatement p = null;
        ResultSet rs = null;
        try {
            p = minigame.getDatabase().getConnection().prepareStatement(query);
            rs = p.executeQuery();

            HashMap<String, Integer> statsList = new HashMap<>();
            while (rs.next()) {
                statsList.put(rs.getString("Nickname"), rs.getInt(type));
            }

            return sortByValue(statsList);
        } catch (SQLException e) {
            e.getStackTrace();
            return new HashMap<>();
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (p != null) {
                try {
                    p.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static HashMap<String, Integer> sortByValue(HashMap<String, Integer> hm)
    {
        // Create a list from elements of HashMap
        List<Map.Entry<String, Integer> > list =
                new LinkedList<Map.Entry<String, Integer> >(hm.entrySet());

        // Sort the list
        Collections.sort(list, new Comparator<Map.Entry<String, Integer> >() {
            public int compare(Map.Entry<String, Integer> o1,
                               Map.Entry<String, Integer> o2)
            {
                return (o1.getValue()).compareTo(o2.getValue());
            }
        });

        // put data from sorted list to hashmap
        HashMap<String, Integer> temp = new LinkedHashMap<String, Integer>();
        for (Map.Entry<String, Integer> aa : list) {
            temp.put(aa.getKey(), aa.getValue());
        }
        return temp;
    }
}

