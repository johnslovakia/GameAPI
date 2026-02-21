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

        for (Stat stat : statsModule.getStats()) {
            if (stat.getName().equalsIgnoreCase("Winstreak")) {
                continue;
            }
            stats_s.append(", `")
                    .append(stat.getName().replace(" ", "_"))
                    .append("` INT DEFAULT 0");
        }

        try (SQLDatabaseConnection connection = Shared.getInstance().getDatabase().getConnection()) {
            if (connection == null) {
                return;
            }

            String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + "(" + stats_s + ");";
            QueryResult result = connection.exec(() -> sql);

            if (!result.isSuccessful()) {
                Logger.log("Failed to create " + TABLE_NAME + " table!", Logger.LogType.ERROR);
                Logger.log(result.getRejectMessage(), Logger.LogType.ERROR);
            }
        }
    }

    public StatsTable createNewColumn(Type type, String name) {
        try (SQLDatabaseConnection dbConn = Shared.getInstance().getDatabase().getConnection()) {
            if (dbConn == null) return this;
            Connection conn = dbConn.getConnection();

            try (PreparedStatement checkStmt = conn.prepareStatement(
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND COLUMN_NAME = ?"
            )) {
                checkStmt.setString(1, Shared.getInstance().getDatabase().getDatabase());
                checkStmt.setString(2, TABLE_NAME);
                checkStmt.setString(3, name);

                try (ResultSet rs = checkStmt.executeQuery()) {
                    boolean exists = false;
                    if (rs.next()) {
                        exists = rs.getInt(1) > 0;
                    }

                    if (!exists) {
                        try (Statement alterStmt = conn.createStatement()) {
                            String sql = "ALTER TABLE `" + TABLE_NAME + "` ADD `" + name + "` "
                                    + type.getB() + (type == Type.INT ? " DEFAULT 0" : "");
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
        if (Shared.getInstance().getDatabase() == null) return;

        try (SQLDatabaseConnection connection = Shared.getInstance().getDatabase().getConnection()) {
            if (connection == null) return;

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
    }

    public void setStat(String nick, String statName, int count) {
        statName = statName.replace(" ", "_");

        try (SQLDatabaseConnection connection = Shared.getInstance().getDatabase().getConnection()) {
            if (connection == null) return;

            connection.update()
                    .table(TABLE_NAME)
                    .set(statName, count)
                    .where().isEqual("Nickname", nick)
                    .execute();
        }
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

        try (SQLDatabaseConnection connection = Shared.getInstance().getDatabase().getConnection()) {
            if (connection == null) return 0;

            Optional<Row> result = connection.select()
                    .from(TABLE_NAME)
                    .where().isEqual("Nickname", nick)
                    .obtainOne();

            if (result.isPresent()) {
                Object value = result.get().get(statName);
                if (value != null) {
                    return result.get().getInt(statName);
                }
            }
        }

        return 0;
    }

    public HashMap<String, Integer> getAllStats(String nick) {
        HashMap<String, Integer> stats = new HashMap<>();
        List<String> stats2 = new ArrayList<>();
        for (Stat stat : statsModule.getStats()) {
            stats2.add(stat.getName().replace(" ", "_"));
        }

        String query = "SELECT * FROM `" + TABLE_NAME + "` WHERE nick=?";

        try (SQLDatabaseConnection dbConn = Shared.getInstance().getDatabase().getConnection()) {
            if (dbConn == null) {
                for (String s : stats2) stats.put(s, 0);
                return stats;
            }

            Connection conn = dbConn.getConnection();
            try (PreparedStatement p = conn.prepareStatement(query)) {
                p.setString(1, nick);
                try (ResultSet rs = p.executeQuery()) {
                    if (rs.next()) {
                        for (String s : stats2) {
                            stats.put(s, rs.getInt(s));
                        }
                    } else {
                        for (String s : stats2) {
                            stats.put(s, 0);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            for (String s : stats2) stats.put(s, 0);
        }

        return stats;
    }

    public HashMap<String, Integer> topStats(String type, int limit) {
        String query = "SELECT `" + type + "`, Nickname FROM `" + TABLE_NAME + "` ORDER BY `" + type + "` DESC LIMIT " + limit;

        HashMap<String, Integer> statsList = new HashMap<>();

        try (SQLDatabaseConnection dbConn = Shared.getInstance().getDatabase().getConnection()) {
            if (dbConn == null) return new HashMap<>();

            Connection conn = dbConn.getConnection();
            try (PreparedStatement p = conn.prepareStatement(query);
                 ResultSet rs = p.executeQuery()) {

                while (rs.next()) {
                    statsList.put(rs.getString("Nickname"), rs.getInt(type));
                }

                return sortByValue(statsList);

            }
        } catch (SQLException e) {
            e.printStackTrace();
            return new HashMap<>();
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
                return (o2.getValue()).compareTo(o1.getValue());
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

