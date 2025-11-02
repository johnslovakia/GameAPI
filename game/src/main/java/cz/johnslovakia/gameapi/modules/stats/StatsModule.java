package cz.johnslovakia.gameapi.modules.stats;

import cz.johnslovakia.gameapi.Shared;
import cz.johnslovakia.gameapi.modules.Module;
import cz.johnslovakia.gameapi.users.PlayerIdentity;
import cz.johnslovakia.gameapi.users.PlayerIdentityRegistry;

import me.zort.sqllib.SQLDatabaseConnection;
import me.zort.sqllib.api.data.Row;
import me.zort.sqllib.internal.query.UpdateQuery;
import me.zort.sqllib.transaction.Transaction;
import me.zort.sqllib.transaction.TransactionFlow;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class StatsModule implements Module, Listener {

    private List<Stat> stats = new ArrayList<>();
    private Map<PlayerIdentity, Map<String, Integer>> cache = new HashMap<>();
    private Set<PlayerIdentity> dirtyPlayers = ConcurrentHashMap.newKeySet();

    private BukkitTask autoSaveTask;


    @Override
    public void initialize() {
        autoSaveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(
                Shared.getInstance().getPlugin(),
                this::saveDirtyPlayers,
                20L * 60 * 5,
                20L * 60 * 5);
    }

    @Override
    public void terminate() {
        stats = null;
        cache = null;
        dirtyPlayers = null;
        autoSaveTask.cancel();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        PlayerIdentity playerIdentity = PlayerIdentityRegistry.get(e.getPlayer());
        savePlayerStatsAsync(playerIdentity);

        dirtyPlayers.remove(playerIdentity);
        cache.remove(playerIdentity);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        PlayerIdentity playerIdentity = PlayerIdentityRegistry.get(e.getPlayer());

        loadPlayerStats(playerIdentity);
    }

    public void registerStat(Stat... stats) {
        Collections.addAll(this.stats, stats);
    }

    public void registerStat(String... stats) {
        for (String name : stats) {
            registerStat(new Stat(name));
        }
    }

    private void saveDirtyPlayers(){
        for (PlayerIdentity playerIdentity : dirtyPlayers){
            savePlayerStatsAsync(playerIdentity);
            dirtyPlayers.remove(playerIdentity);
        }
    }

    private CompletableFuture<Map<String, Long>> loadPlayerStats(PlayerIdentity playerIdentity) {
        return CompletableFuture.supplyAsync(() ->
                cache.computeIfAbsent(playerIdentity, key -> {
                    Optional<Row> result = Shared.getInstance().getDatabase().getConnection()
                            .select()
                            .from(TABLE_NAME)
                            .where().isEqual("Nickname", playerIdentity.getOnlinePlayer().getName())
                            .obtainOne();

                    Map<String, Long> statsMap = new HashMap<>();
                    if (result.isEmpty()) return statsMap;

                    Row row = result.get();
                    for (Stat stat : stats) {
                        statsMap.put(stat.getName(), row.getLong(stat.getName()));
                    }

                    return statsMap;
                })
        );
    }

    public void savePlayerStatsAsync(PlayerIdentity playerIdentity){
        CompletableFuture.runAsync(() -> {
            try {
                Map<String, Integer> stats = cache.get(playerIdentity);
                if (stats == null) return;

                SQLDatabaseConnection connection = Shared.getInstance().getDatabase().getConnection();

                Transaction transaction = connection.beginTransaction();
                TransactionFlow.Builder flow = transaction.flow()
                        .commitOnSuccess(true)
                        .rollbackOnFailure(true)
                        .autoClose(true);

                for (Map.Entry<String, Integer> entry : stats.entrySet()) {
                    flow.step(new UpdateQuery()
                            .table(TABLE_NAME)
                            .set(entry.getKey(), entry.getValue())
                            .where().isEqual("Nickname", playerIdentity.getName()));
                }

                flow.create().execute();
            } catch (Exception e) {
                Bukkit.getLogger().severe("Failed to save player data for " + playerIdentity.getName());
                e.printStackTrace();
            }
        });
    }

    public void increasePlayerStat(PlayerIdentity playerIdentity, Stat stat, int amount) {
        increasePlayerStat(playerIdentity, stat.getName(), amount);
    }

    public void increasePlayerStat(PlayerIdentity playerIdentity, String statName, int amount) {
        cache.computeIfAbsent(playerIdentity, p -> new HashMap<>()).merge(statName, amount, Integer::sum);
        dirtyPlayers.add(playerIdentity);
    }

    public long getPlayerStat(PlayerIdentity player, String statName) {
        Map<String, Integer> playerStats = cache.get(player);
        if (playerStats == null) return 0L;

        return playerStats.getOrDefault(statName, 0);
    }
}