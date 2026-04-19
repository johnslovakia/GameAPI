package cz.johnslovakia.gameapi.modules.stats;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.Core;
import cz.johnslovakia.gameapi.database.DatabaseMigrationHelper;
import cz.johnslovakia.gameapi.database.Type;
import cz.johnslovakia.gameapi.events.GameJoinEvent;
import cz.johnslovakia.gameapi.events.GameQuitEvent;
import cz.johnslovakia.gameapi.modules.Module;
import cz.johnslovakia.gameapi.modules.game.GameInstance;
import cz.johnslovakia.gameapi.modules.game.GameState;
import cz.johnslovakia.gameapi.modules.game.lobby.LobbyLocation;
import cz.johnslovakia.gameapi.modules.game.lobby.LobbyModule;
import cz.johnslovakia.gameapi.users.PlayerIdentity;

import cz.johnslovakia.gameapi.utils.BatchConfig;
import cz.johnslovakia.gameapi.utils.CachedBatchStorage;
import cz.johnslovakia.gameapi.utils.ConfigAPI;
import cz.johnslovakia.gameapi.utils.GameUtils;
import lombok.AccessLevel;
import lombok.Getter;

import me.zort.sqllib.SQLDatabaseConnection;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class StatsModule implements Module, Listener {

    @Getter
    private List<Stat> stats = new ArrayList<>();

    private CachedBatchStorage<String, Map<String, Integer>> lifetimeStorage;
    private CachedBatchStorage<String, Map<String, Integer>> periodStorage;

    @Getter
    private PlayerStatsHologram playerStatsHologram;
    @Getter
    private TopStatsHologram topStatsHologram;
    @Getter
    private StatsTable statsTable;
    @Getter(AccessLevel.PACKAGE)
    private boolean fixedBillboardDisplay = true;
    @Getter
    private StatPeriod defaultDisplayPeriod = StatPeriod.LIFETIME;

    public StatsModule() {}

    public StatsModule(boolean fixedBillboardDisplay) {
        this.fixedBillboardDisplay = fixedBillboardDisplay;
    }

    public StatsModule withDefaultDisplayPeriod(StatPeriod period) {
        this.defaultDisplayPeriod = period;
        return this;
    }

    @Override
    public void initialize() {
        BatchConfig batchConfig = BatchConfig.builder("player_stats")
                .maxBatchSize(50)
                .flushIntervalSeconds(30)
                .build();

        lifetimeStorage = new CachedBatchStorage<>(
                "player_stats_lifetime",
                batchConfig,
                this::loadLifetimeFromDB,
                this::saveLifetimeToDB,
                this::mergeStats
        );

        periodStorage = new CachedBatchStorage<>(
                "player_stats_period",
                batchConfig,
                this::loadPeriodFromDB,
                this::savePeriodToDB,
                this::mergeStats
        );

        this.statsTable = new StatsTable(this);
        this.playerStatsHologram = new PlayerStatsHologram(this);
        this.topStatsHologram = new TopStatsHologram(this);

        DatabaseMigrationHelper.ensureNicknameUnique(Minigame.getInstance().getFullName() + "_stats");
    }

    @Override
    public void terminate() {
        if (topStatsHologram != null) topStatsHologram.shutdown();
        if (playerStatsHologram != null) playerStatsHologram.removeAll();
        if (lifetimeStorage != null) lifetimeStorage.shutdown();
        if (periodStorage != null) periodStorage.shutdown();
        stats = null;
    }

    @EventHandler (priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent e) {
        String nickname = e.getPlayer().getName();
        lifetimeStorage.flushAndInvalidate(nickname);
        for (StatPeriod period : StatPeriod.values()) {
            if (period == StatPeriod.LIFETIME) continue;
            periodStorage.flushAndInvalidate(periodKey(nickname, period));
        }
    }

    @EventHandler
    public void onGamePlayerQuit(GameQuitEvent e) {
        removeHolograms(e.getGamePlayer());
    }

    @EventHandler
    public void onPlayerJoin(GameJoinEvent e) {
        Bukkit.getScheduler().runTaskLater(Minigame.getInstance().getPlugin(), task -> {
            if (!e.getGamePlayer().isOnline()) {
                task.cancel();
                return;
            }
            preloadAll(e.getGamePlayer()).thenRun(() -> {
                if (e.getGame().getState().equals(GameState.STARTING) || e.getGame().getState().equals(GameState.WAITING)) {
                    ConfigAPI config = new ConfigAPI(
                            GameAPI.getInstance().getMinigameDataFolder().toString(),
                            "config.yml",
                            Minigame.getInstance().getPlugin());
                    GameInstance game = e.getGame();

                    LobbyLocation statsLoc = GameUtils.getLobbyLocation(config.getConfig(), game, "statsHologram");
                    if (statsLoc != null) {
                        if (game.getModule(LobbyModule.class).getLobbyLocation().getGame() == null) {
                            statsLoc.setGame(null);
                        }
                        Bukkit.getScheduler().runTask(Minigame.getInstance().getPlugin(), t ->
                                playerStatsHologram.create(e.getGamePlayer(), statsLoc.getLocation()));
                    }

                    LobbyLocation topLoc = GameUtils.getLobbyLocation(config.getConfig(), game, "topStatsHologram");
                    if (topLoc != null) {
                        if (game.getModule(LobbyModule.class).getLobbyLocation().getGame() == null) {
                            topLoc.setGame(null);
                        }
                        Bukkit.getScheduler().runTask(Minigame.getInstance().getPlugin(), t ->
                                topStatsHologram.create(e.getGamePlayer(), topLoc.getLocation()));
                    }
                }
            });
        }, 30L);
    }

    public CompletableFuture<?> preloadAll(OfflinePlayer offlinePlayer) {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        futures.add(lifetimeStorage.get(offlinePlayer.getName()));
        for (StatPeriod period : StatPeriod.values()) {
            if (period == StatPeriod.LIFETIME) continue;
            futures.add(periodStorage.get(periodKey(offlinePlayer.getName(), period)));
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    public CompletableFuture<?> preloadAll(PlayerIdentity playerIdentity) {
        return preloadAll(playerIdentity.getOfflinePlayer());
    }

    public void removeHolograms(PlayerIdentity playerIdentity) {
        playerStatsHologram.remove(playerIdentity);
        topStatsHologram.remove(playerIdentity);
    }

    public void createDatabaseTable() {
        statsTable.createLifetimeTable();
        statsTable.createPeriodTable();

        registerStat(new Stat("Winstreak").hideFromPlayer());

        for (Stat stat : stats) {
            String col = stat.getName().replace(" ", "_");
            statsTable.createNewColumn(col, false);
            if (!stat.getName().equalsIgnoreCase("Winstreak")) {
                statsTable.createNewColumn(col, true);
            }
        }

        topStatsHologram.registerStats();
    }

    public void registerStat(Stat... stats) {
        Collections.addAll(this.stats, stats);
    }

    public void registerStat(String... stats) {
        for (String name : stats) registerStat(new Stat(name));
    }

    private String periodKey(String nickname, StatPeriod period) {
        return nickname + ":" + period.name() + ":" + period.getCurrentPeriodKey();
    }

    private Map<String, Map<String, Integer>> loadLifetimeFromDB(Set<String> nicknames) throws SQLException {
        Map<String, Map<String, Integer>> results = new HashMap<>();
        if (nicknames.isEmpty() || stats.isEmpty()) return results;

        String placeholders = String.join(",", Collections.nCopies(nicknames.size(), "?"));
        StringBuilder sql = new StringBuilder("SELECT `Nickname`");
        for (Stat stat : stats) sql.append(", `").append(stat.getName().replace(" ", "_")).append("`");
        sql.append(" FROM ").append(statsTable.quotedLifetimeTable())
           .append(" WHERE `Nickname` IN (").append(placeholders).append(")");

        try (SQLDatabaseConnection dbConn = Core.getInstance().getDatabase().getConnection();
             PreparedStatement stmt = dbConn.getConnection().prepareStatement(sql.toString())) {
            int i = 1;
            for (String n : nicknames) stmt.setString(i++, n);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String nickname = rs.getString("Nickname");
                    Map<String, Integer> map = new HashMap<>();
                    for (Stat stat : stats) {
                        try { map.put(stat.getName(), rs.getInt(stat.getName().replace(" ", "_"))); }
                        catch (SQLException ignored) { map.put(stat.getName(), 0); }
                    }
                    results.put(nickname, map);
                }
            }
        }

        for (String n : nicknames) results.putIfAbsent(n, new HashMap<>());
        return results;
    }

    private Map<String, Map<String, Integer>> loadPeriodFromDB(Set<String> compositeKeys) throws SQLException {
        Map<String, Map<String, Integer>> results = new HashMap<>();
        if (compositeKeys.isEmpty() || stats.isEmpty()) return results;

        Map<String, Set<String>> byPeriodTypeAndKey = new LinkedHashMap<>();
        for (String ck : compositeKeys) {
            String[] parts = ck.split(":", 3);
            String groupKey = parts[1] + ":" + parts[2];
            byPeriodTypeAndKey.computeIfAbsent(groupKey, k -> new HashSet<>()).add(parts[0]);
        }

        for (Map.Entry<String, Set<String>> entry : byPeriodTypeAndKey.entrySet()) {
            String[] gk = entry.getKey().split(":", 2);
            String periodType = gk[0];
            String periodKeyStr = gk[1];
            Set<String> nicknames = entry.getValue();

            String placeholders = String.join(",", Collections.nCopies(nicknames.size(), "?"));
            StringBuilder sql = new StringBuilder("SELECT `Nickname`");
            for (Stat stat : stats) {
                if (!stat.getName().equalsIgnoreCase("Winstreak"))
                    sql.append(", `").append(stat.getName().replace(" ", "_")).append("`");
            }
            sql.append(" FROM ").append(statsTable.quotedPeriodTable())
               .append(" WHERE `period_type` = ? AND `period_key` = ? AND `Nickname` IN (").append(placeholders).append(")");

            try (SQLDatabaseConnection dbConn = Core.getInstance().getDatabase().getConnection();
                 PreparedStatement stmt = dbConn.getConnection().prepareStatement(sql.toString())) {
                stmt.setString(1, periodType);
                stmt.setString(2, periodKeyStr);
                int i = 3;
                for (String n : nicknames) stmt.setString(i++, n);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String nickname = rs.getString("Nickname");
                        Map<String, Integer> map = new HashMap<>();
                        for (Stat stat : stats) {
                            if (stat.getName().equalsIgnoreCase("Winstreak")) continue;
                            try { map.put(stat.getName(), rs.getInt(stat.getName().replace(" ", "_"))); }
                            catch (SQLException ignored) { map.put(stat.getName(), 0); }
                        }
                        results.put(nickname + ":" + periodType + ":" + periodKeyStr, map);
                    }
                }
            }

            for (String n : nicknames) results.putIfAbsent(n + ":" + periodType + ":" + periodKeyStr, new HashMap<>());
        }

        return results;
    }

    private void saveLifetimeToDB(Map<String, CachedBatchStorage.PendingChange<String, Map<String, Integer>>> changes) throws SQLException {
        if (changes.isEmpty()) return;
        Map<String, CachedBatchStorage.PendingChange<String, Map<String, Integer>>> setChanges = new HashMap<>();
        Map<String, CachedBatchStorage.PendingChange<String, Map<String, Integer>>> deltaChanges = new HashMap<>();
        for (Map.Entry<String, CachedBatchStorage.PendingChange<String, Map<String, Integer>>> e : changes.entrySet()) {
            (e.getValue().isSet() ? setChanges : deltaChanges).put(e.getKey(), e.getValue());
        }
        try (SQLDatabaseConnection dbConn = Minigame.getInstance().getDatabase().getConnection()) {
            Connection conn = dbConn.getConnection();
            conn.setAutoCommit(false);
            try {
                if (!deltaChanges.isEmpty()) executeLifetimeBatch(conn, deltaChanges, false);
                if (!setChanges.isEmpty()) executeLifetimeBatch(conn, setChanges, true);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private void executeLifetimeBatch(Connection conn, Map<String, CachedBatchStorage.PendingChange<String, Map<String, Integer>>> changes, boolean isSet) throws SQLException {
        Set<String> allStats = new HashSet<>();
        for (CachedBatchStorage.PendingChange<String, Map<String, Integer>> c : changes.values()) allStats.addAll(c.getDelta().keySet());
        if (allStats.isEmpty()) return;

        List<String> statList = new ArrayList<>(allStats);
        StringBuilder sql = new StringBuilder("INSERT INTO ").append(statsTable.quotedLifetimeTable()).append(" (`Nickname`");
        StringBuilder vals = new StringBuilder(" VALUES (?");
        StringBuilder dup = new StringBuilder(" ON DUPLICATE KEY UPDATE ");

        for (int i = 0; i < statList.size(); i++) {
            String col = statList.get(i).replace(" ", "_");
            sql.append(", `").append(col).append("`");
            vals.append(", ?");
            if (i > 0) dup.append(", ");
            if (isSet) dup.append("`").append(col).append("` = VALUES(`").append(col).append("`)");
            else dup.append("`").append(col).append("` = `").append(col).append("` + VALUES(`").append(col).append("`)");
        }
        sql.append(")").append(vals).append(")").append(dup);

        try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (CachedBatchStorage.PendingChange<String, Map<String, Integer>> c : changes.values()) {
                int p = 1;
                stmt.setString(p++, c.getKey());
                for (String s : statList) stmt.setInt(p++, c.getDelta().getOrDefault(s, 0));
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    private void savePeriodToDB(Map<String, CachedBatchStorage.PendingChange<String, Map<String, Integer>>> changes) throws SQLException {
        if (changes.isEmpty()) return;
        Map<String, CachedBatchStorage.PendingChange<String, Map<String, Integer>>> setChanges = new HashMap<>();
        Map<String, CachedBatchStorage.PendingChange<String, Map<String, Integer>>> deltaChanges = new HashMap<>();
        for (Map.Entry<String, CachedBatchStorage.PendingChange<String, Map<String, Integer>>> e : changes.entrySet()) {
            (e.getValue().isSet() ? setChanges : deltaChanges).put(e.getKey(), e.getValue());
        }
        try (SQLDatabaseConnection dbConn = Minigame.getInstance().getDatabase().getConnection()) {
            Connection conn = dbConn.getConnection();
            conn.setAutoCommit(false);
            try {
                if (!deltaChanges.isEmpty()) executePeriodBatch(conn, deltaChanges, false);
                if (!setChanges.isEmpty()) executePeriodBatch(conn, setChanges, true);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private void executePeriodBatch(Connection conn, Map<String, CachedBatchStorage.PendingChange<String, Map<String, Integer>>> changes, boolean isSet) throws SQLException {
        Set<String> allStats = new HashSet<>();
        for (CachedBatchStorage.PendingChange<String, Map<String, Integer>> c : changes.values()) allStats.addAll(c.getDelta().keySet());
        if (allStats.isEmpty()) return;

        List<String> statList = new ArrayList<>(allStats);
        StringBuilder sql = new StringBuilder("INSERT INTO ").append(statsTable.quotedPeriodTable()).append(" (`Nickname`, `period_type`, `period_key`");
        StringBuilder vals = new StringBuilder(" VALUES (?, ?, ?");
        StringBuilder dup = new StringBuilder(" ON DUPLICATE KEY UPDATE ");

        for (int i = 0; i < statList.size(); i++) {
            String col = statList.get(i).replace(" ", "_");
            sql.append(", `").append(col).append("`");
            vals.append(", ?");
            if (i > 0) dup.append(", ");
            if (isSet) dup.append("`").append(col).append("` = VALUES(`").append(col).append("`)");
            else dup.append("`").append(col).append("` = `").append(col).append("` + VALUES(`").append(col).append("`)");
        }
        sql.append(")").append(vals).append(")").append(dup);

        try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (Map.Entry<String, CachedBatchStorage.PendingChange<String, Map<String, Integer>>> entry : changes.entrySet()) {
                String[] parts = entry.getKey().split(":", 3);
                int p = 1;
                stmt.setString(p++, parts[0]);
                stmt.setString(p++, parts[1]);
                stmt.setString(p++, parts[2]);
                for (String s : statList) stmt.setInt(p++, entry.getValue().getDelta().getOrDefault(s, 0));
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    private Map<String, Integer> mergeStats(Map<String, Integer> existing, Map<String, Integer> delta) {
        Map<String, Integer> result = new HashMap<>(existing);
        for (Map.Entry<String, Integer> e : delta.entrySet()) result.merge(e.getKey(), e.getValue(), Integer::sum);
        return result;
    }

    private CachedBatchStorage<String, Map<String, Integer>> storageFor(StatPeriod period) {
        return period == StatPeriod.LIFETIME ? lifetimeStorage : periodStorage;
    }

    private String keyFor(String nickname, StatPeriod period) {
        return period == StatPeriod.LIFETIME ? nickname : periodKey(nickname, period);
    }

    public void increasePlayerStat(String nickname, String statName, int amount) {
        Map<String, Integer> delta = Map.of(statName, amount);
        lifetimeStorage.modify(nickname, delta);
        if (statName.equalsIgnoreCase("Winstreak")) return;
        for (StatPeriod period : StatPeriod.values()) {
            if (period == StatPeriod.LIFETIME) continue;
            periodStorage.modify(periodKey(nickname, period), delta);
        }
    }

    public void increasePlayerStat(String nickname, Stat stat, int amount) {
        increasePlayerStat(nickname, stat.getName(), amount);
    }

    public void setPlayerStat(String nickname, String statName, int value) {
        Map<String, Integer> current = lifetimeStorage.getCached(nickname);
        Map<String, Integer> updated = current != null ? new HashMap<>(current) : new HashMap<>();
        updated.put(statName, value);
        lifetimeStorage.set(nickname, updated);
    }

    public void setPlayerStat(String nickname, Stat stat, int value) {
        setPlayerStat(nickname, stat.getName(), value);
    }

    public int getPlayerStat(String nickname, String statName) {
        return getPlayerStat(nickname, statName, StatPeriod.LIFETIME);
    }

    public int getPlayerStat(String nickname, String statName, StatPeriod period) {
        String key = keyFor(nickname, period);
        Map<String, Integer> map = storageFor(period).getCached(key);
        if (map == null) {
            storageFor(period).get(key);
            return 0;
        }
        return map.getOrDefault(statName, 0);
    }

    public int getPlayerStat(String nickname, Stat stat) {
        return getPlayerStat(nickname, stat.getName(), StatPeriod.LIFETIME);
    }

    public int getPlayerStat(String nickname, Stat stat, StatPeriod period) {
        return getPlayerStat(nickname, stat.getName(), period);
    }

    public void increasePlayerStat(PlayerIdentity p, String statName, int amount) { increasePlayerStat(p.getName(), statName, amount); }
    public void increasePlayerStat(PlayerIdentity p, Stat stat, int amount) { increasePlayerStat(p.getName(), stat.getName(), amount); }
    public void setPlayerStat(PlayerIdentity p, String statName, int value) { setPlayerStat(p.getName(), statName, value); }
    public void setPlayerStat(PlayerIdentity p, Stat stat, int value) { setPlayerStat(p.getName(), stat.getName(), value); }
    public int getPlayerStat(PlayerIdentity p, String statName) { return getPlayerStat(p.getName(), statName, StatPeriod.LIFETIME); }
    public int getPlayerStat(PlayerIdentity p, Stat stat) { return getPlayerStat(p.getName(), stat.getName(), StatPeriod.LIFETIME); }
    public int getPlayerStat(PlayerIdentity p, String statName, StatPeriod period) { return getPlayerStat(p.getName(), statName, period); }
    public int getPlayerStat(PlayerIdentity p, Stat stat, StatPeriod period) { return getPlayerStat(p.getName(), stat.getName(), period); }
}