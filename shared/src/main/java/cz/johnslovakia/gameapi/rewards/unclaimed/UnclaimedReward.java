package cz.johnslovakia.gameapi.rewards.unclaimed;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import cz.johnslovakia.gameapi.database.UnclaimedRewardsTable;
import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.rewards.Reward;
import cz.johnslovakia.gameapi.users.PlayerIdentity;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.OfflinePlayer;

import java.time.LocalDateTime;
import java.util.Objects;

@Getter @Setter
public class UnclaimedReward {

    private final OfflinePlayer offlinePlayer;
    private final LocalDateTime createdAt;
    private final String rewardJson;
    private final JsonObject data;
    private final UnclaimedRewardType type;

    private final Reward reward;

    @Setter
    private int bonus;
    @Setter
    private boolean claimed = false;

    public UnclaimedReward(OfflinePlayer offlinePlayer, LocalDateTime createdAt, String rewardJson, JsonObject data, UnclaimedRewardType type) {
        this.offlinePlayer = offlinePlayer;
        this.createdAt = createdAt;
        this.rewardJson = rewardJson;
        this.data = data;
        this.type = type;

        Gson gson = new GsonBuilder().create();
        this.reward = gson.fromJson(rewardJson, Reward.class);

    }

    public UnclaimedReward(OfflinePlayer offlinePlayer, LocalDateTime createdAt, Reward reward, JsonObject data, UnclaimedRewardType type) {
        this.offlinePlayer = offlinePlayer;
        this.createdAt = createdAt;
        this.reward = reward;
        this.data = data;
        this.type = type;

        Gson gson = new GsonBuilder().create();
        this.rewardJson = gson.toJson(reward, Reward.class);
    }

    public void claim(){
        //TODO: přepsat na lepší systém bonusů, neukládá se bonus
        if (!ModuleManager.getModule(UnclaimedRewardsModule.class).getPlayerUnclaimedRewards(offlinePlayer).contains(this)) return;
        getReward().applyReward(offlinePlayer, true, bonus);
        UnclaimedRewardsTable.removeUnclaimedReward(this);
        ModuleManager.getModule(UnclaimedRewardsModule.class).removeUnclaimedReward(offlinePlayer, this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UnclaimedReward that = (UnclaimedReward) o;
        return type == that.type
                && Objects.equals(offlinePlayer.getName(), that.offlinePlayer.getName())
                && Objects.equals(createdAt, that.createdAt)
                && Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(offlinePlayer.getName(), type, createdAt, data);
    }
}