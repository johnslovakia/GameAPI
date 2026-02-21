package cz.johnslovakia.gameapi.modules.stats;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.Shared;
import cz.johnslovakia.gameapi.database.DatabaseMigrationHelper;
import cz.johnslovakia.gameapi.database.Type;
import cz.johnslovakia.gameapi.events.GameJoinEvent;
import cz.johnslovakia.gameapi.modules.Module;
import cz.johnslovakia.gameapi.modules.game.GameInstance;
import cz.johnslovakia.gameapi.modules.game.GameState;
import cz.johnslovakia.gameapi.modules.game.lobby.LobbyLocation;
import cz.johnslovakia.gameapi.users.PlayerIdentity;
import cz.johnslovakia.gameapi.users.PlayerIdentityRegistry;

import cz.johnslovakia.gameapi.utils.BatchConfig;
import cz.johnslovakia.gameapi.utils.CachedBatchStorage;
import cz.johnslovakia.gameapi.utils.ConfigAPI;
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
    private CachedBatchStorage<PlayerIdentity, Map<String, Integer>> storage;

    @Getter
    private StatsHolograms statsHolograms;
    @Getter
    private StatsTable statsTable;


    @Override
    public void initialize() {
        this.storage = new CachedBatchStorage<>(
                "player_stats",
                BatchConfig.builder("player_stats")
                        .maxBatchSize(50)
                        .flushIntervalSeconds(60)
                        .build(),
                this::loadStatsFromDB,
                this::saveStatsToDB,
                this::mergeStatsMaps
        );

        this.statsHolograms = new StatsHolograms(this);
        this.statsTable = new StatsTable(this);

        DatabaseMigrationHelper.ensureNicknameUnique(Minigame.getInstance().getName() + "_stats");
    }

    @Override
    public void terminate() {
        if (storage != null) {
            storage.shutdown();
        }
        stats = null;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        PlayerIdentity playerIdentity = PlayerIdentityRegistry.get(e.getPlayer());
        storage.invalidate(playerIdentity);
    }

    @EventHandler
    public void onPlayerJoin(GameJoinEvent e) {
        if (e.getGame().getState().equals(GameState.STARTING) || e.getGame().getState().equals(GameState.WAITING)) {
            storage.get(e.getGamePlayer()).thenAccept(stats -> {
                ConfigAPI config = new ConfigAPI(GameAPI.getInstance().getMinigameDataFolder().toString(), "config.yml", Minigame.getInstance().getPlugin());
                GameInstance game = e.getGame();

                LobbyLocation statsHologram = config.getLobbyLocation(game, "statsHologram");
                if (statsHologram != null) {
                    Bukkit.getScheduler().runTask(Minigame.getInstance().getPlugin(), task -> {
                        statsHolograms.createPlayerStatisticsHologram(e.getGamePlayer(), statsHologram.getLocation());
                    });
                }
                LobbyLocation topStatsHologram = config.getLobbyLocation(game, "topStatsHologram");
                if (topStatsHologram != null) {
                    Bukkit.getScheduler().runTask(Minigame.getInstance().getPlugin(), task -> {
                        statsHolograms.getTopStatsHologram().createTopStatisticsHologram(e.getGamePlayer(), topStatsHologram.getLocation());
                    });
                }
            });
        }
    }

    //TODO: .
    public void createDatabaseTable(){
        statsTable.createTable();

        registerStat(new Stat("Winstreak").hideFromPlayer());
        //statsTable.createNewColumn(Type.INT, "Winstreak");
        for (Stat stat : stats){
            statsTable.createNewColumn(Type.INT, stat.getName().replace(" ", "_"));
        }
    }

    public void registerStat(Stat... stats) {
        Collections.addAll(this.stats, stats);
    }

    public void registerStat(String... stats) {
        for (String name : stats) {
            registerStat(new Stat(name));
        }
    }

    private Map<PlayerIdentity, Map<String, Integer>> loadStatsFromDB(Set<PlayerIdentity> playerIdentities) throws SQLException {
        Map<PlayerIdentity, Map<String, Integer>> results = new HashMap<>();

        if (playerIdentities.isEmpty() || stats.isEmpty()) {
            return results;
        }

        String placeholders = String.join(",", Collections.nCopies(playerIdentities.size(), "?"));
        StringBuilder sqlBuilder = new StringBuilder("SELECT Nickname");
        for (Stat stat : stats) {
            sqlBuilder.append(", ").append(stat.getName().replace(" ", "_"));
        }
        sqlBuilder.append(" FROM ").append(Minigame.getInstance().getName() + "_stats");
        sqlBuilder.append(" WHERE Nickname IN (").append(placeholders).append(")");

        try (SQLDatabaseConnection dbConn = Shared.getInstance().getDatabase().getConnection();
             PreparedStatement stmt = dbConn.getConnection().prepareStatement(sqlBuilder.toString())) {

            int i = 1;
            Map<String, PlayerIdentity> nicknameMap = new HashMap<>();
            for (PlayerIdentity playerIdentity : playerIdentities) {
                stmt.setString(i++, playerIdentity.getName());
                nicknameMap.put(playerIdentity.getName(), playerIdentity);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String nickname = rs.getString("Nickname");
                    PlayerIdentity playerIdentity = nicknameMap.get(nickname);
                    if (playerIdentity == null) continue;

                    Map<String, Integer> statsMap = new HashMap<>();
                    for (Stat stat : stats) {
                        try {
                            int value = rs.getInt(stat.getName().replace(" ", "_"));
                            statsMap.put(stat.getName(), value);
                        } catch (SQLException e) {
                            statsMap.put(stat.getName(), 0);
                        }
                    }

                    results.put(playerIdentity, statsMap);
                }
            }
        }

        for (PlayerIdentity playerIdentity : playerIdentities) {
            results.putIfAbsent(playerIdentity, new HashMap<>());
        }

        return results;
    }

    private void saveStatsToDB(Map<PlayerIdentity, CachedBatchStorage.PendingChange<PlayerIdentity, Map<String, Integer>>> changes) throws SQLException {
        if (changes.isEmpty()) return;

        Set<String> allStatNames = new HashSet<>();
        for (CachedBatchStorage.PendingChange<PlayerIdentity, Map<String, Integer>> change : changes.values()) {
            allStatNames.addAll(change.getDelta().keySet());
        }
        if (allStatNames.isEmpty()) return;

        StringBuilder sql = new StringBuilder("INSERT INTO " + Minigame.getInstance().getName() + "_stats (Nickname");
        StringBuilder values = new StringBuilder(" VALUES (?");
        StringBuilder onDuplicate = new StringBuilder(" ON DUPLICATE KEY UPDATE ");
        List<String> statNamesList = new ArrayList<>(allStatNames);

        for (int i = 0; i < statNamesList.size(); i++) {
            String statName = statNamesList.get(i).replace(" ", "_");
            sql.append(", ").append(statName);
            values.append(", ?");
            if (i > 0) onDuplicate.append(", ");
            onDuplicate.append(statName).append(" = ").append(statName).append(" + VALUES(").append(statName).append(")");
        }
        sql.append(")").append(values).append(")").append(onDuplicate);

        try (SQLDatabaseConnection dbConn = Minigame.getInstance().getDatabase().getConnection()) {
            Connection conn = dbConn.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
                for (CachedBatchStorage.PendingChange<PlayerIdentity, Map<String, Integer>> change : changes.values()) {
                    String nickname = change.getKey().getName();
                    Map<String, Integer> statsMap = change.getDelta();
                    int paramIndex = 1;
                    stmt.setString(paramIndex++, nickname);
                    for (String statName : statNamesList) {
                        stmt.setInt(paramIndex++, statsMap.getOrDefault(statName, 0));
                    }
                    stmt.addBatch();
                }

                stmt.executeBatch();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private Map<String, Integer> mergeStatsMaps(Map<String, Integer> existing, Map<String, Integer> delta) {
        Map<String, Integer> result = new HashMap<>(existing);

        for (Map.Entry<String, Integer> entry : delta.entrySet()) {
            result.merge(entry.getKey(), entry.getValue(), Integer::sum);
        }

        return result;
    }



    public void increasePlayerStat(PlayerIdentity playerIdentity, String statName, int amount) {
        Map<String, Integer> delta = new HashMap<>();
        delta.put(statName, amount);

        storage.modify(playerIdentity, delta);
    }

    public void increasePlayerStat(PlayerIdentity playerIdentity, Stat stat, int amount) {
        increasePlayerStat(playerIdentity, stat.getName(), amount);
    }

    public void setPlayerStat(PlayerIdentity playerIdentity, String statName, int value) {
        Map<String, Integer> newMap = new HashMap<>();
        newMap.put(statName, value);

        Map<String, Integer> current = storage.getCached(playerIdentity);
        if (current != null) {
            newMap.putAll(current);
        }
        newMap.put(statName, value);

        storage.set(playerIdentity, newMap);
    }

    public int getPlayerStat(PlayerIdentity playerIdentity, String statName) {
        Map<String, Integer> statsMap = storage.getCached(playerIdentity);

        if (statsMap == null) {
            storage.get(playerIdentity);
            return 0;
        }

        return statsMap.getOrDefault(statName, 0);
    }
}