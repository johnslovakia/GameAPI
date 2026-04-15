package cz.johnslovakia.gameapi.modules.stats;

import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.Core;
import cz.johnslovakia.gameapi.users.PlayerIdentity;
import cz.johnslovakia.gameapi.utils.Logger;

import lombok.Getter;
import me.zort.sqllib.SQLDatabaseConnection;

import java.sql.*;
import java.util.*;

@Getter
public class StatsTable {

    private final StatsModule statsModule;
    private final String lifetimeTableName;
    private final String periodTableName;

    public StatsTable(StatsModule statsModule) {
        this.statsModule = statsModule;
        String base = Minigame.getInstance().getFullName() + "_stats";
        this.lifetimeTableName = base;
        this.periodTableName = base + "_period";
    }

    public String quotedLifetimeTable() {
        return "`" + lifetimeTableName + "`";
    }

    public String quotedPeriodTable() {
        return "`" + periodTableName + "`";
    }

    public void createLifetimeTable() {
        if (Core.getInstance().getDatabase() == null) {
            Logger.log("You don't have the database set up in the config.yml!", Logger.LogType.ERROR);
            return;
        }

        StringBuilder columns = new StringBuilder("`id` INT AUTO_INCREMENT PRIMARY KEY, `Nickname` VARCHAR(32) NOT NULL UNIQUE");
        for (Stat stat : statsModule.getStats()) {
            if (stat.getName().equalsIgnoreCase("Winstreak")) continue;
            columns.append(", `").append(stat.getName().replace(" ", "_")).append("` INT DEFAULT 0");
        }

        try (SQLDatabaseConnection dbConn = Core.getInstance().getDatabase().getConnection()) {
            if (dbConn == null) return;
            try (Statement stmt = dbConn.getConnection().createStatement()) {
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS " + quotedLifetimeTable() + " (" + columns + ")");
            }
        } catch (SQLException e) {
            Logger.log("Failed to create " + lifetimeTableName + " table!", Logger.LogType.ERROR);
            e.printStackTrace();
        }
    }

    public void createPeriodTable() {
        if (Core.getInstance().getDatabase() == null) return;

        StringBuilder columns = new StringBuilder(
                "`Nickname` VARCHAR(32) NOT NULL, " +
                "`period_type` VARCHAR(8) NOT NULL, " +
                "`period_key` VARCHAR(12) NOT NULL"
        );

        for (Stat stat : statsModule.getStats()) {
            if (stat.getName().equalsIgnoreCase("Winstreak")) continue;
            columns.append(", `").append(stat.getName().replace(" ", "_")).append("` INT DEFAULT 0");
        }
        columns.append(", PRIMARY KEY (`Nickname`, `period_type`, `period_key`)");
        columns.append(", INDEX `idx_period` (`period_type`, `period_key`)");

        try (SQLDatabaseConnection dbConn = Core.getInstance().getDatabase().getConnection()) {
            if (dbConn == null) return;
            try (Statement stmt = dbConn.getConnection().createStatement()) {
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS " + quotedPeriodTable() + " (" + columns + ")");
            }
        } catch (SQLException e) {
            Logger.log("Failed to create " + periodTableName + " table!", Logger.LogType.ERROR);
            e.printStackTrace();
        }
    }

    public void createNewColumn(String name, boolean period) {
        String table = period ? periodTableName : lifetimeTableName;
        String quotedTable = period ? quotedPeriodTable() : quotedLifetimeTable();

        try (SQLDatabaseConnection dbConn = Core.getInstance().getDatabase().getConnection()) {
            if (dbConn == null) return;
            Connection conn = dbConn.getConnection();

            try (PreparedStatement check = conn.prepareStatement(
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND COLUMN_NAME = ?"
            )) {
                check.setString(1, Core.getInstance().getDatabase().getDatabase());
                check.setString(2, table);
                check.setString(3, name);
                try (ResultSet rs = check.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) return;
                }
            }

            try (Statement alter = conn.createStatement()) {
                alter.executeUpdate("ALTER TABLE " + quotedTable + " ADD `" + name + "` INT DEFAULT 0");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void newUser(PlayerIdentity playerIdentity) {
        if (Core.getInstance().getDatabase() == null) return;

        try (SQLDatabaseConnection dbConn = Core.getInstance().getDatabase().getConnection()) {
            if (dbConn == null) return;
            Connection conn = dbConn.getConnection();

            try (PreparedStatement check = conn.prepareStatement(
                    "SELECT 1 FROM " + quotedLifetimeTable() + " WHERE `Nickname` = ? LIMIT 1"
            )) {
                check.setString(1, playerIdentity.getOnlinePlayer().getName());
                try (ResultSet rs = check.executeQuery()) {
                    if (rs.next()) return;
                }
            }

            try (PreparedStatement insert = conn.prepareStatement(
                    "INSERT INTO " + quotedLifetimeTable() + " (`Nickname`) VALUES (?)"
            )) {
                insert.setString(1, playerIdentity.getOnlinePlayer().getName());
                insert.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public LinkedHashMap<String, Integer> topStats(String statName, int limit, StatPeriod period) {
        String col = statName.replace(" ", "_");
        LinkedHashMap<String, Integer> result = new LinkedHashMap<>();

        try (SQLDatabaseConnection dbConn = Core.getInstance().getDatabase().getConnection()) {
            if (dbConn == null) return result;
            Connection conn = dbConn.getConnection();

            if (period == StatPeriod.LIFETIME) {
                try (PreparedStatement stmt = conn.prepareStatement(
                        "SELECT `Nickname`, `" + col + "` FROM " + quotedLifetimeTable()
                        + " WHERE `" + col + "` > 0 ORDER BY `" + col + "` DESC LIMIT ?"
                )) {
                    stmt.setInt(1, limit);
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) result.put(rs.getString("Nickname"), rs.getInt(col));
                    }
                }
            } else {
                try (PreparedStatement stmt = conn.prepareStatement(
                        "SELECT `Nickname`, `" + col + "` FROM " + quotedPeriodTable()
                        + " WHERE `period_type` = ? AND `period_key` = ? AND `" + col + "` > 0"
                        + " ORDER BY `" + col + "` DESC LIMIT ?"
                )) {
                    stmt.setString(1, period.name());
                    stmt.setString(2, period.getCurrentPeriodKey());
                    stmt.setInt(3, limit);
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) result.put(rs.getString("Nickname"), rs.getInt(col));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }

    public int getPlayerRank(String nickname, String statName, StatPeriod period) {
        String col = statName.replace(" ", "_");

        try (SQLDatabaseConnection dbConn = Core.getInstance().getDatabase().getConnection()) {
            if (dbConn == null) return -1;
            Connection conn = dbConn.getConnection();

            if (period == StatPeriod.LIFETIME) {
                try (PreparedStatement check = conn.prepareStatement(
                        "SELECT `" + col + "` FROM " + quotedLifetimeTable() + " WHERE `Nickname` = ? LIMIT 1"
                )) {
                    check.setString(1, nickname);
                    try (ResultSet rs = check.executeQuery()) {
                        if (!rs.next() || rs.getInt(1) <= 0) return -1;
                    }
                }
                try (PreparedStatement rank = conn.prepareStatement(
                        "SELECT COUNT(*) + 1 AS `rank` FROM " + quotedLifetimeTable()
                        + " WHERE `" + col + "` > (SELECT `" + col + "` FROM " + quotedLifetimeTable()
                        + " WHERE `Nickname` = ? LIMIT 1)"
                )) {
                    rank.setString(1, nickname);
                    try (ResultSet rs = rank.executeQuery()) {
                        if (rs.next()) return rs.getInt("rank");
                    }
                }
            } else {
                String periodKey = period.getCurrentPeriodKey();
                try (PreparedStatement check = conn.prepareStatement(
                        "SELECT `" + col + "` FROM " + quotedPeriodTable()
                        + " WHERE `Nickname` = ? AND `period_type` = ? AND `period_key` = ? LIMIT 1"
                )) {
                    check.setString(1, nickname);
                    check.setString(2, period.name());
                    check.setString(3, periodKey);
                    try (ResultSet rs = check.executeQuery()) {
                        if (!rs.next() || rs.getInt(1) <= 0) return -1;
                    }
                }
                try (PreparedStatement rank = conn.prepareStatement(
                        "SELECT COUNT(*) + 1 AS `rank` FROM " + quotedPeriodTable()
                        + " WHERE `period_type` = ? AND `period_key` = ? AND `" + col + "` > "
                        + "(SELECT `" + col + "` FROM " + quotedPeriodTable()
                        + " WHERE `Nickname` = ? AND `period_type` = ? AND `period_key` = ? LIMIT 1)"
                )) {
                    rank.setString(1, period.name());
                    rank.setString(2, periodKey);
                    rank.setString(3, nickname);
                    rank.setString(4, period.name());
                    rank.setString(5, periodKey);
                    try (ResultSet rs = rank.executeQuery()) {
                        if (rs.next()) return rs.getInt("rank");
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return -1;
    }

    public int getPlayerRank(String nickname, String statName) {
        return getPlayerRank(nickname, statName, StatPeriod.LIFETIME);
    }
}