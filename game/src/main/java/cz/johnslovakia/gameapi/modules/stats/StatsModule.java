package cz.johnslovakia.gameapi.modules.stats;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.Shared;
import cz.johnslovakia.gameapi.database.DatabaseMigrationHelper;
import cz.johnslovakia.gameapi.database.Type;
import cz.johnslovakia.gameapi.events.GameJoinEvent;
import cz.johnslovakia.gameapi.events.GameQuitEvent;
import cz.johnslovakia.gameapi.modules.Module;
import cz.johnslovakia.gameapi.modules.game.GameInstance;
import cz.johnslovakia.gameapi.modules.game.GameState;
import cz.johnslovakia.gameapi.modules.game.lobby.LobbyLocation;
import cz.johnslovakia.gameapi.users.PlayerIdentity;

import cz.johnslovakia.gameapi.utils.BatchConfig;
import cz.johnslovakia.gameapi.utils.CachedBatchStorage;
import cz.johnslovakia.gameapi.utils.ConfigAPI;
import cz.johnslovakia.gameapi.utils.GameUtils;
import lombok.AccessLevel;
import lombok.Getter;

import me.zort.sqllib.SQLDatabaseConnection;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class StatsModule implements Module, Listener {

    @Getter
    private List<Stat> stats = new ArrayList<>();

    private CachedBatchStorage<String, Map<String, Integer>> storage;

    @Getter
    private LifetimeStatsHologram lifetimeStatsHologram;
    @Getter
    private TopStatsHologram topStatsHologram;
    @Getter
    private StatsTable statsTable;
    @Getter(AccessLevel.PACKAGE)
    private boolean fixedBillboardDisplay = true;

    public StatsModule(boolean fixedBillboardDisplay) {
        this.fixedBillboardDisplay = fixedBillboardDisplay;
    }

    public StatsModule() {
    }

    @Override
    public void initialize() {
        this.storage = new CachedBatchStorage<>(
                "player_stats",
                BatchConfig.builder("player_stats")
                        .maxBatchSize(50)
                        .flushIntervalSeconds(30)
                        .build(),
                this::loadStatsFromDB,
                this::saveStatsToDB,
                this::mergeStatsMaps
        );

        this.statsTable = new StatsTable(this);
        this.lifetimeStatsHologram = new LifetimeStatsHologram(this);
        this.topStatsHologram = new TopStatsHologram(this);

        DatabaseMigrationHelper.ensureNicknameUnique(Minigame.getInstance().getFullName() + "_stats");
    }

    @Override
    public void terminate() {
        if (topStatsHologram != null) topStatsHologram.shutdown();
        if (lifetimeStatsHologram != null) lifetimeStatsHologram.removeAll();
        if (storage != null) storage.shutdown();
        stats = null;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        String nickname = e.getPlayer().getName();
        storage.flushAndInvalidate(nickname);
    }

    @EventHandler
    public void onPlayerQuit(GameQuitEvent e) {
        removeHolograms(e.getGamePlayer());
    }

    @EventHandler
    public void onPlayerJoin(GameJoinEvent e) {
        Bukkit.getScheduler().runTaskLater(Minigame.getInstance().getPlugin(), task -> {
            if (!e.getGamePlayer().isOnline()) {
                task.cancel();
                return;
            }

            if (e.getGame().getState().equals(GameState.STARTING) || e.getGame().getState().equals(GameState.WAITING)) {
                String nickname = e.getGamePlayer().getName();
                storage.get(nickname).thenAccept(playerStats -> {
                    ConfigAPI config = new ConfigAPI(GameAPI.getInstance().getMinigameDataFolder().toString(), "config.yml", Minigame.getInstance().getPlugin());
                    GameInstance game = e.getGame();

                    LobbyLocation statsHologramLoc = GameUtils.getLobbyLocation(config.getConfig(), game, "statsHologram");
                    if (statsHologramLoc != null) {
                        Bukkit.getScheduler().runTask(Minigame.getInstance().getPlugin(), t -> {
                            lifetimeStatsHologram.create(e.getGamePlayer(), statsHologramLoc.getLocation());
                        });
                    }
                    LobbyLocation topStatsHologramLoc = GameUtils.getLobbyLocation(config.getConfig(), game, "topStatsHologram");
                    if (topStatsHologramLoc != null) {
                        Bukkit.getScheduler().runTask(Minigame.getInstance().getPlugin(), t -> {
                            topStatsHologram.create(e.getGamePlayer(), topStatsHologramLoc.getLocation());
                        });
                    }
                });
            }
        }, 30L);
    }

    public void removeHolograms(PlayerIdentity playerIdentity) {
        lifetimeStatsHologram.remove(playerIdentity);
        topStatsHologram.remove(playerIdentity);
    }

    public void createDatabaseTable() {
        statsTable.createTable();

        registerStat(new Stat("Winstreak").hideFromPlayer());
        for (Stat stat : stats) {
            statsTable.createNewColumn(Type.INT, stat.getName().replace(" ", "_"));
        }

        topStatsHologram.registerStats();
    }

    public void registerStat(Stat... stats) {
        Collections.addAll(this.stats, stats);
    }

    public void registerStat(String... stats) {
        for (String name : stats) {
            registerStat(new Stat(name));
        }
    }

    private Map<String, Map<String, Integer>> loadStatsFromDB(Set<String> nicknames) throws SQLException {
        Map<String, Map<String, Integer>> results = new HashMap<>();

        if (nicknames.isEmpty() || stats.isEmpty()) {
            return results;
        }

        String placeholders = String.join(",", Collections.nCopies(nicknames.size(), "?"));
        StringBuilder sqlBuilder = new StringBuilder("SELECT `Nickname`");
        for (Stat stat : stats) {
            sqlBuilder.append(", `").append(stat.getName().replace(" ", "_")).append("`");
        }
        sqlBuilder.append(" FROM ").append(statsTable.quotedTableName());
        sqlBuilder.append(" WHERE `Nickname` IN (").append(placeholders).append(")");

        try (SQLDatabaseConnection dbConn = Shared.getInstance().getDatabase().getConnection();
             PreparedStatement stmt = dbConn.getConnection().prepareStatement(sqlBuilder.toString())) {

            int i = 1;
            for (String nickname : nicknames) {
                stmt.setString(i++, nickname);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String nickname = rs.getString("Nickname");

                    Map<String, Integer> statsMap = new HashMap<>();
                    for (Stat stat : stats) {
                        try {
                            int value = rs.getInt(stat.getName().replace(" ", "_"));
                            statsMap.put(stat.getName(), value);
                        } catch (SQLException e) {
                            statsMap.put(stat.getName(), 0);
                        }
                    }

                    results.put(nickname, statsMap);
                }
            }
        }

        for (String nickname : nicknames) {
            results.putIfAbsent(nickname, new HashMap<>());
        }

        return results;
    }

    private void saveStatsToDB(Map<String, CachedBatchStorage.PendingChange<String, Map<String, Integer>>> changes) throws SQLException {
        if (changes.isEmpty()) return;

        Map<String, CachedBatchStorage.PendingChange<String, Map<String, Integer>>> setChanges = new HashMap<>();
        Map<String, CachedBatchStorage.PendingChange<String, Map<String, Integer>>> deltaChanges = new HashMap<>();

        for (Map.Entry<String, CachedBatchStorage.PendingChange<String, Map<String, Integer>>> entry : changes.entrySet()) {
            if (entry.getValue().isSet()) {
                setChanges.put(entry.getKey(), entry.getValue());
            } else {
                deltaChanges.put(entry.getKey(), entry.getValue());
            }
        }

        try (SQLDatabaseConnection dbConn = Minigame.getInstance().getDatabase().getConnection()) {
            Connection conn = dbConn.getConnection();
            conn.setAutoCommit(false);

            try {
                if (!deltaChanges.isEmpty()) executeBatch(conn, deltaChanges, false);
                if (!setChanges.isEmpty()) executeBatch(conn, setChanges, true);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private void executeBatch(Connection conn, Map<String, CachedBatchStorage.PendingChange<String, Map<String, Integer>>> changes, boolean isSet) throws SQLException {
        Set<String> allStatNames = new HashSet<>();
        for (CachedBatchStorage.PendingChange<String, Map<String, Integer>> change : changes.values()) {
            allStatNames.addAll(change.getDelta().keySet());
        }
        if (allStatNames.isEmpty()) return;

        List<String> statNamesList = new ArrayList<>(allStatNames);

        StringBuilder sql = new StringBuilder("INSERT INTO ").append(statsTable.quotedTableName()).append(" (`Nickname`");
        StringBuilder values = new StringBuilder(" VALUES (?");
        StringBuilder onDuplicate = new StringBuilder(" ON DUPLICATE KEY UPDATE ");

        for (int i = 0; i < statNamesList.size(); i++) {
            String col = statNamesList.get(i).replace(" ", "_");
            sql.append(", `").append(col).append("`");
            values.append(", ?");
            if (i > 0) onDuplicate.append(", ");

            if (isSet) {
                onDuplicate.append("`").append(col).append("` = VALUES(`").append(col).append("`)");
            } else {
                onDuplicate.append("`").append(col).append("` = `").append(col).append("` + VALUES(`").append(col).append("`)");
            }
        }
        sql.append(")").append(values).append(")").append(onDuplicate);

        try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (CachedBatchStorage.PendingChange<String, Map<String, Integer>> change : changes.values()) {
                Map<String, Integer> statsMap = change.getDelta();
                int paramIndex = 1;
                stmt.setString(paramIndex++, change.getKey());
                for (String statName : statNamesList) {
                    stmt.setInt(paramIndex++, statsMap.getOrDefault(statName, 0));
                }
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    private Map<String, Integer> mergeStatsMaps(Map<String, Integer> existing, Map<String, Integer> delta) {
        Map<String, Integer> result = new HashMap<>(existing);
        for (Map.Entry<String, Integer> entry : delta.entrySet()) {
            result.merge(entry.getKey(), entry.getValue(), Integer::sum);
        }
        return result;
    }

    public void increasePlayerStat(String nickname, String statName, int amount) {
        Map<String, Integer> delta = new HashMap<>();
        delta.put(statName, amount);
        storage.modify(nickname, delta);
    }

    public void increasePlayerStat(String nickname, Stat stat, int amount) {
        increasePlayerStat(nickname, stat.getName(), amount);
    }

    public void setPlayerStat(String nickname, String statName, int value) {
        Map<String, Integer> current = storage.getCached(nickname);
        Map<String, Integer> newMap = current != null ? new HashMap<>(current) : new HashMap<>();
        newMap.put(statName, value);
        storage.set(nickname, newMap);
    }

    public void setPlayerStat(String nickname, Stat stat, int value) {
        setPlayerStat(nickname, stat.getName(), value);
    }

    public int getPlayerStat(String nickname, String statName) {
        Map<String, Integer> statsMap = storage.getCached(nickname);
        if (statsMap == null) {
            storage.get(nickname);
            return 0;
        }
        return statsMap.getOrDefault(statName, 0);
    }

    public int getPlayerStat(String nickname, Stat stat) {
        return getPlayerStat(nickname, stat.getName());
    }

    public void increasePlayerStat(PlayerIdentity playerIdentity, String statName, int amount) {
        increasePlayerStat(playerIdentity.getName(), statName, amount);
    }

    public void increasePlayerStat(PlayerIdentity playerIdentity, Stat stat, int amount) {
        increasePlayerStat(playerIdentity.getName(), stat.getName(), amount);
    }

    public void setPlayerStat(PlayerIdentity playerIdentity, String statName, int value) {
        setPlayerStat(playerIdentity.getName(), statName, value);
    }

    public void setPlayerStat(PlayerIdentity playerIdentity, Stat stat, int value) {
        setPlayerStat(playerIdentity.getName(), stat.getName(), value);
    }

    public int getPlayerStat(PlayerIdentity playerIdentity, String statName) {
        return getPlayerStat(playerIdentity.getName(), statName);
    }

    public int getPlayerStat(PlayerIdentity playerIdentity, Stat stat) {
        return getPlayerStat(playerIdentity.getName(), stat.getName());
    }
}