package cz.johnslovakia.gameapi.rewards.unclaimed;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import cz.johnslovakia.gameapi.Shared;
import cz.johnslovakia.gameapi.modules.Module;
import cz.johnslovakia.gameapi.users.PlayerIdentity;
import cz.johnslovakia.gameapi.users.PlayerIdentityRegistry;
import cz.johnslovakia.gameapi.utils.Logger;

import me.zort.sqllib.SQLDatabaseConnection;
import me.zort.sqllib.api.data.QueryRowsResult;
import me.zort.sqllib.api.data.Row;

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

    private Map<PlayerIdentity, List<UnclaimedReward>> cache = new ConcurrentHashMap<>();

    @Override
    public void initialize() {

    }

    @Override
    public void terminate() {
        cache = null;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        loadUnclaimedRewards(PlayerIdentityRegistry.get(e.getPlayer()));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        cache.remove(PlayerIdentityRegistry.get(e.getPlayer()));
    }


    public void addUnclaimedReward(PlayerIdentity playerIdentity, UnclaimedReward unclaimedReward){
        cache.computeIfAbsent(playerIdentity, key -> new ArrayList<>())
                .add(unclaimedReward);
    }

    public void removeUnclaimedReward(PlayerIdentity playerIdentity, UnclaimedReward unclaimedReward){
        List<UnclaimedReward> rewards = cache.get(playerIdentity);
        if (rewards == null) return;

        rewards.remove(unclaimedReward);
        if (rewards.isEmpty()) {
            cache.remove(playerIdentity);
        }
    }

    public List<UnclaimedReward> getPlayerUnclaimedRewards(PlayerIdentity playerIdentity) {
        List<UnclaimedReward> rewards = cache.get(playerIdentity);
        if (rewards == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(rewards);
    }

    public List<UnclaimedReward> getPlayerUnclaimedRewardsByType(PlayerIdentity playerIdentity, UnclaimedRewardType type){
        return getPlayerUnclaimedRewards(playerIdentity).stream()
                .filter(r -> r.getType() == type)
                .collect(Collectors.toList());
    }

    public CompletableFuture<List<UnclaimedReward>> loadUnclaimedRewards(PlayerIdentity playerIdentity) {
        return CompletableFuture.supplyAsync(() -> {
            List<UnclaimedReward> unclaimedRewards = new ArrayList<>();

            try {
                SQLDatabaseConnection connection = Shared.getInstance().getDatabase().getConnection();

                QueryRowsResult<Row> result = connection.select()
                        .from("unclaimed_rewards")
                        .where().isEqual("Nickname", playerIdentity.getName())
                        .obtainAll();

                if (result.isEmpty()) {
                    return unclaimedRewards;
                }

                for (Row row : result) {
                    try {
                        LocalDateTime createdAt = null;
                        Object timestampObj = row.get("created_at");

                        if (timestampObj instanceof Timestamp timestamp) {
                            createdAt = timestamp.toLocalDateTime();
                        }

                        if (createdAt == null) {
                            Logger.log("Missing created_at for reward of player " + playerIdentity.getName(), Logger.LogType.WARNING);
                            continue;
                        }

                        String rewardJson = row.getString("reward_json");
                        JsonObject dataJson = JsonParser.parseString(row.getString("data_json")).getAsJsonObject();
                        UnclaimedRewardType type = UnclaimedRewardType.valueOf(row.getString("type"));

                        UnclaimedReward unclaimedReward = switch (type) {
                            case QUEST -> new QuestUnclaimedReward(playerIdentity, createdAt, rewardJson, dataJson, type);
                            case DAILYMETER -> new DailyMeterUnclaimedReward(playerIdentity, createdAt, rewardJson, dataJson, type);
                            case LEVELUP -> new LevelUpUnclaimedReward(playerIdentity, createdAt, rewardJson, dataJson, type);
                            default -> throw new IllegalArgumentException("Unknown reward type: " + type);
                        };

                        unclaimedRewards.add(unclaimedReward);
                        addUnclaimedReward(playerIdentity, unclaimedReward);
                    } catch (Exception e) {
                        Logger.log("Failed to parse unclaimed reward for player " + playerIdentity.getName() + ": " + e.getMessage(), Logger.LogType.ERROR);
                    }
                }

            } catch (Exception exception) {
                Logger.log("Failed to load unclaimed rewards for player " + playerIdentity.getName() + ": " + exception.getMessage(), Logger.LogType.ERROR);
                exception.printStackTrace();
            }

            return unclaimedRewards;
        });
    }
}
