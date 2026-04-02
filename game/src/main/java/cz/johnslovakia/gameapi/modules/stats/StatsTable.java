package cz.johnslovakia.gameapi.modules.stats;

import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.Shared;
import cz.johnslovakia.gameapi.database.Type;
import cz.johnslovakia.gameapi.users.PlayerIdentity;
import cz.johnslovakia.gameapi.utils.Logger;

import lombok.Getter;

import java.sql.*;
import java.util.*;

import me.zort.sqllib.SQLDatabaseConnection;

@Getter
public class StatsTable {

    private final StatsModule statsModule;
    private final String TABLE_NAME;

    public StatsTable(StatsModule statsModule) {
        this.statsModule = statsModule;
        this.TABLE_NAME = Minigame.getInstance().getFullName() + "_stats";
    }

    public String quotedTableName() {
        return "`" + TABLE_NAME + "`";
    }

    public void createTable() {
        if (Shared.getInstance().getDatabase() == null) {
            Logger.log("You don't have the database set up in the config.yml!", Logger.LogType.ERROR);
            return;
        }

        StringBuilder columns = new StringBuilder("`id` INT AUTO_INCREMENT PRIMARY KEY, `Nickname` VARCHAR(32) NOT NULL");

        for (Stat stat : statsModule.getStats()) {
            if (stat.getName().equalsIgnoreCase("Winstreak")) {
                continue;
            }
            columns.append(", `")
                    .append(stat.getName().replace(" ", "_"))
                    .append("` INT DEFAULT 0");
        }

        try (SQLDatabaseConnection dbConn = Shared.getInstance().getDatabase().getConnection()) {
            if (dbConn == null) return;

            Connection conn = dbConn.getConnection();
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS " + quotedTableName() + "(" + columns + ")");
            }
        } catch (SQLException e) {
            Logger.log("Failed to create " + TABLE_NAME + " table!", Logger.LogType.ERROR);
            e.printStackTrace();
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
                    if (rs.next() && rs.getInt(1) > 0) {
                        return this;
                    }
                }
            }

            try (Statement alterStmt = conn.createStatement()) {
                String sql = "ALTER TABLE " + quotedTableName() + " ADD `" + name + "` "
                        + type.getB() + (type == Type.INT ? " DEFAULT 0" : "");
                alterStmt.executeUpdate(sql);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return this;
    }

    public void newUser(PlayerIdentity playerIdentity) {
        if (Shared.getInstance().getDatabase() == null) return;

        try (SQLDatabaseConnection dbConn = Shared.getInstance().getDatabase().getConnection()) {
            if (dbConn == null) return;
            Connection conn = dbConn.getConnection();

            try (PreparedStatement checkStmt = conn.prepareStatement(
                    "SELECT 1 FROM " + quotedTableName() + " WHERE `Nickname` = ? LIMIT 1"
            )) {
                checkStmt.setString(1, playerIdentity.getOnlinePlayer().getName());
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) return;
                }
            }

            try (PreparedStatement insertStmt = conn.prepareStatement(
                    "INSERT INTO " + quotedTableName() + " (`Nickname`) VALUES (?)"
            )) {
                insertStmt.setString(1, playerIdentity.getOnlinePlayer().getName());
                insertStmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setStat(String nick, String statName, int count) {
        statName = statName.replace(" ", "_");

        try (SQLDatabaseConnection dbConn = Shared.getInstance().getDatabase().getConnection()) {
            if (dbConn == null) return;
            Connection conn = dbConn.getConnection();

            try (PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE " + quotedTableName() + " SET `" + statName + "` = ? WHERE `Nickname` = ?"
            )) {
                stmt.setInt(1, count);
                stmt.setString(2, nick);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addStat(String nick, String statName, int count) {
        statName = statName.replace(" ", "_");
        setStat(nick, statName, getStat(nick, statName) + count);
    }

    public void removeStat(String nick, String statName, int count) {
        statName = statName.replace(" ", "_");
        int current = getStat(nick, statName);
        if (current <= 0) return;
        setStat(nick, statName, Math.max(0, current - count));
    }

    public int getStat(String nick, String statName) {
        statName = statName.replace(" ", "_");

        try (SQLDatabaseConnection dbConn = Shared.getInstance().getDatabase().getConnection()) {
            if (dbConn == null) return 0;
            Connection conn = dbConn.getConnection();

            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT `" + statName + "` FROM " + quotedTableName() + " WHERE `Nickname` = ? LIMIT 1"
            )) {
                stmt.setString(1, nick);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0;
    }

    public Map<String, Integer> getAllStats(String nick) {
        Map<String, Integer> stats = new HashMap<>();
        List<String> columns = new ArrayList<>();
        for (Stat stat : statsModule.getStats()) {
            columns.add(stat.getName().replace(" ", "_"));
        }

        try (SQLDatabaseConnection dbConn = Shared.getInstance().getDatabase().getConnection()) {
            if (dbConn == null) {
                for (String s : columns) stats.put(s, 0);
                return stats;
            }

            Connection conn = dbConn.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT * FROM " + quotedTableName() + " WHERE `Nickname` = ? LIMIT 1"
            )) {
                stmt.setString(1, nick);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        for (String s : columns) {
                            try {
                                stats.put(s, rs.getInt(s));
                            } catch (SQLException e) {
                                stats.put(s, 0);
                            }
                        }
                    } else {
                        for (String s : columns) stats.put(s, 0);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            for (String s : columns) stats.put(s, 0);
        }

        return stats;
    }

    public LinkedHashMap<String, Integer> topStats(String type, int limit) {
        type = type.replace(" ", "_");
        LinkedHashMap<String, Integer> result = new LinkedHashMap<>();

        try (SQLDatabaseConnection dbConn = Shared.getInstance().getDatabase().getConnection()) {
            if (dbConn == null) return result;

            Connection conn = dbConn.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT `Nickname`, `" + type + "` FROM " + quotedTableName() + " ORDER BY `" + type + "` DESC LIMIT ?"
            )) {
                stmt.setInt(1, limit);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        result.put(rs.getString("Nickname"), rs.getInt(type));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }
}