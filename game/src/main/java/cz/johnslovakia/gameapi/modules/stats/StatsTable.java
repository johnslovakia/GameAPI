package cz.johnslovakia.gameapi.modules.stats;

import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.Shared;
import cz.johnslovakia.gameapi.database.Type;
import cz.johnslovakia.gameapi.users.PlayerIdentity;
import cz.johnslovakia.gameapi.utils.Logger;

import lombok.Getter;
import me.zort.sqllib.SQLDatabaseConnection;
import me.zort.sqllib.api.data.QueryResult;
import me.zort.sqllib.api.data.Row;

import java.sql.*;
import java.util.*;

@Getter
public class StatsTable {

    private final StatsModule statsModule;
    private final String TABLE_NAME;

    public StatsTable(StatsModule statsModule) {
        this.statsModule = statsModule;
        this.TABLE_NAME = Minigame.getInstance().getName() + "_stats";
    }
    public void createTable() {
        if (Shared.getInstance().getDatabase() == null) {
            Logger.log("You don't have the database set up in the config.yml!", Logger.LogType.ERROR);
            return;
        }

        StringBuilder stats_s = new StringBuilder("`id` INT AUTO_INCREMENT PRIMARY KEY, `Nickname` VARCHAR(32) NOT NULL");

        List<String> stats = new ArrayList<>();
        for (Stat stat : statsModule.getStats()){
            if (stat.getName().equalsIgnoreCase("Winstreak")){
                continue;
            }
            stats.add(stat.getName().replace(" ", "_"));
        }

        //stats.add("Winstreak");
        //stats.add("LastDailyWinReward");


        for (String s : stats) {
            stats_s.append(", `").append(s).append("` int DEFAULT 0");
        }

        SQLDatabaseConnection connection = Shared.getInstance().getDatabase().getConnection();
        if (connection == null){
            return;
        }
        String finalStats_s = stats_s.toString();
        QueryResult result = connection.exec(() ->
                "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + "("
                        + finalStats_s + ");");

        if (!result.isSuccessful()) {
            Logger.log("Failed to create " + TABLE_NAME + " table!", Logger.LogType.ERROR);
            Logger.log(result.getRejectMessage(), Logger.LogType.ERROR);
        }
    }

    public StatsTable createNewColumn(Type type, String name) {
        SQLDatabaseConnection connection = Shared.getInstance().getDatabase().getConnection();
        if (connection != null) {
            Connection conn = Shared.getInstance().getDatabase().getConnection().getConnection();

            try (
                    PreparedStatement checkStmt = conn.prepareStatement(
                            "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND COLUMN_NAME = ?"
                    )
            ) {
                checkStmt.setString(1, Shared.getInstance().getDatabase().getDatabase());
                checkStmt.setString(2, TABLE_NAME);
                checkStmt.setString(3, name);

                ResultSet rs = checkStmt.executeQuery();
                boolean exists = false;

                if (rs.next()) {
                    exists = rs.getInt(1) > 0;
                }

                rs.close();

                if (!exists) {
                    try (Statement alterStmt = conn.createStatement()) {
                        String sql = "ALTER TABLE `" + TABLE_NAME + "` ADD `" + name + "` " + type.getB() + (type == Type.INT ? " DEFAULT 0" : "");
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

    public void newUser(PlayerIdentity playerIdentity) {
        SQLDatabaseConnection connection = Shared.getInstance().getDatabase().getConnection();
        if (connection == null){
            return;
        }

        Optional<Row> result = connection.select()
                .from(TABLE_NAME)
                .where().isEqual("Nickname", playerIdentity.getOnlinePlayer().getName())
                .obtainOne();

        if (result.isEmpty()) {
            connection.insert()
                    .into(TABLE_NAME, "Nickname")
                    .values(playerIdentity.getOnlinePlayer().getName())
                    .execute();
        }
    }


    public void setStat(String nick, String statName, int count) {
        statName = statName.replace(" ", "_");
        Shared.getInstance().getDatabase().getConnection().update()
                .table(TABLE_NAME)
                .set(statName, count)
                .where().isEqual("Nickname", nick)
                .execute();
    }

    public void addStat(String nick, String statName, int count) {
        statName = statName.replace(" ", "_");
        setStat(nick, statName, getStat(nick, statName) + 1);
    }

    public void removeStat(String nick, String statName, int count) {
        statName = statName.replace(" ", "_");
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
        statName = statName.replace(" ", "_");
        Optional<Row> result = Shared.getInstance().getDatabase().getConnection().select()
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
        for (Stat stat : statsModule.getStats()){
            stats2.add(stat.getName().replace(" ", "_"));
        }


        String query = "SELECT * FROM `" + TABLE_NAME + "` WHERE nick=?";

        PreparedStatement p = null;
        ResultSet rs = null;
        try {
            p = Shared.getInstance().getDatabase().getConnection().getConnection().prepareStatement(query);

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
            p = Shared.getInstance().getDatabase().getConnection().getConnection().prepareStatement(query);
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

