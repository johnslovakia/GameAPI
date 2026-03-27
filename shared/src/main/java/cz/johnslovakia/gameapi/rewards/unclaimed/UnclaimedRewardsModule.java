package cz.johnslovakia.gameapi.rewards.unclaimed;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import cz.johnslovakia.gameapi.Shared;
import cz.johnslovakia.gameapi.modules.Module;
import cz.johnslovakia.gameapi.users.PlayerIdentity;
import cz.johnslovakia.gameapi.utils.Logger;

import me.zort.sqllib.SQLDatabaseConnection;
import me.zort.sqllib.api.data.QueryRowsResult;
import me.zort.sqllib.api.data.Row;

import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class UnclaimedRewardsModule implements Listener, Module {

    private Map<String, List<UnclaimedReward>> cache = new ConcurrentHashMap<>();

    @Override
    public void initialize() {}

    @Override
    public void terminate() {
        cache = null;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        loadUnclaimedRewards(e.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        cache.remove(e.getPlayer().getName());
    }


    public void addUnclaimedReward(OfflinePlayer player, UnclaimedReward unclaimedReward) {
        cache.computeIfAbsent(player.getName(), key -> new ArrayList<>())
                .add(unclaimedReward);
    }

    public void removeUnclaimedReward(OfflinePlayer player, UnclaimedReward unclaimedReward) {
        List<UnclaimedReward> rewards = cache.get(player.getName());
        if (rewards == null) return;

        rewards.remove(unclaimedReward);
        if (rewards.isEmpty()) {
            cache.remove(player.getName());
        }
    }
    public List<UnclaimedReward> getPlayerUnclaimedRewards(OfflinePlayer player) {
        List<UnclaimedReward> rewards = cache.get(player.getName());
        return rewards != null ? new ArrayList<>(rewards) : new ArrayList<>();
    }

    public List<UnclaimedReward> getPlayerUnclaimedRewardsByType(PlayerIdentity playerIdentity, UnclaimedRewardType type) {
        return getPlayerUnclaimedRewardsByType(playerIdentity.getOfflinePlayer(), type);
    }

    public List<UnclaimedReward> getPlayerUnclaimedRewardsByType(OfflinePlayer player, UnclaimedRewardType type) {
        return getPlayerUnclaimedRewards(player).stream()
                .filter(r -> r.getType() == type)
                .collect(Collectors.toList());
    }

    public CompletableFuture<List<UnclaimedReward>> getOrLoadUnclaimedRewards(OfflinePlayer player) {
        List<UnclaimedReward> cached = cache.get(player.getName());
        if (cached != null) {
            return CompletableFuture.completedFuture(new ArrayList<>(cached));
        }
        return fetchFromDatabase(player);
    }

    public CompletableFuture<List<UnclaimedReward>> getOrLoadUnclaimedRewardsByType(OfflinePlayer player, UnclaimedRewardType type) {
        return getOrLoadUnclaimedRewards(player).thenApply(rewards ->
                rewards.stream()
                        .filter(r -> r.getType() == type)
                        .collect(Collectors.toList())
        );
    }

    public CompletableFuture<List<UnclaimedReward>> getOrLoadUnclaimedRewardsByType(PlayerIdentity playerIdentity, UnclaimedRewardType type) {
        return getOrLoadUnclaimedRewardsByType(playerIdentity.getOfflinePlayer(), type);
    }


    /*public CompletableFuture<List<UnclaimedReward>> loadUnclaimedRewards(OfflinePlayer player) {
        return fetchFromDatabase(player).thenApply(rewards -> {
            rewards.forEach(r -> addUnclaimedReward(player, r));
            return rewards;
        });
    }*/

    public CompletableFuture<List<UnclaimedReward>> loadUnclaimedRewards(OfflinePlayer player) {
        return fetchFromDatabase(player).thenApply(rewards -> {
            cache.put(player.getName(), new ArrayList<>(rewards));
            return rewards;
        });
    }

    private CompletableFuture<List<UnclaimedReward>> fetchFromDatabase(OfflinePlayer player) {
        return CompletableFuture.supplyAsync(() -> {
            List<UnclaimedReward> unclaimedRewards = new ArrayList<>();

            if (Shared.getInstance().getDatabase() == null) return unclaimedRewards;

            try (SQLDatabaseConnection connection = Shared.getInstance().getDatabase().getConnection()) {
                if (connection == null) return unclaimedRewards;

                QueryRowsResult<Row> result = connection.select()
                        .from("unclaimed_rewards")
                        .where().isEqual("Nickname", player.getName())
                        .obtainAll();

                if (result.isEmpty()) return unclaimedRewards;

                for (Row row : result) {
                    try {
                        Object timestampObj = row.get("created_at");
                        if (!(timestampObj instanceof Timestamp)) {
                            Logger.log("Missing created_at for reward of player " + player.getName(), Logger.LogType.WARNING);
                            continue;
                        }

                        LocalDateTime createdAt = ((Timestamp) timestampObj).toLocalDateTime();
                        String rewardJson = row.getString("reward_json");
                        JsonObject dataJson = JsonParser.parseString(row.getString("data_json")).getAsJsonObject();
                        UnclaimedRewardType type = UnclaimedRewardType.valueOf(row.getString("type"));

                        UnclaimedReward unclaimedReward = switch (type) {
                            case QUEST      -> new QuestUnclaimedReward(player, createdAt, rewardJson, dataJson, type);
                            case DAILYMETER -> new DailyMeterUnclaimedReward(player, createdAt, rewardJson, dataJson, type);
                            case LEVELUP    -> new LevelUpUnclaimedReward(player, createdAt, rewardJson, dataJson, type);
                            default         -> throw new IllegalArgumentException("Unknown reward type: " + type);
                        };

                        unclaimedRewards.add(unclaimedReward);

                    } catch (Exception e) {
                        Logger.log("Failed to parse unclaimed reward for player " + player.getName() + ": " + e.getMessage(), Logger.LogType.ERROR);
                    }
                }

            } catch (Exception e) {
                Logger.log("Failed to load unclaimed rewards for player " + player.getName() + ": " + e.getMessage(), Logger.LogType.ERROR);
                e.printStackTrace();
            }

            return unclaimedRewards;
        });
    }
}