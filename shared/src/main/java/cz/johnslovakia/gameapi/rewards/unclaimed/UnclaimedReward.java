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

import java.time.LocalDateTime;

@Getter @Setter
public class UnclaimedReward {

    private final PlayerIdentity playerIdentity;
    private final LocalDateTime createdAt;
    private final String rewardJson;
    private final JsonObject data;
    private final UnclaimedRewardType type;

    private final Reward reward;

    @Setter
    private int bonus;
    @Setter
    private boolean claimed = false;

    public UnclaimedReward(PlayerIdentity playerIdentity, LocalDateTime createdAt, String rewardJson, JsonObject data, UnclaimedRewardType type) {
        this.playerIdentity = playerIdentity;
        this.createdAt = createdAt;
        this.rewardJson = rewardJson;
        this.data = data;
        this.type = type;

        Gson gson = new GsonBuilder()
                .create();
        this.reward = gson.fromJson(rewardJson, Reward.class);

    }

    public UnclaimedReward(PlayerIdentity playerIdentity, LocalDateTime createdAt, Reward reward, JsonObject data, UnclaimedRewardType type) {
        this.playerIdentity = playerIdentity;
        this.createdAt = createdAt;
        this.reward = reward;
        this.data = data;
        this.type = type;

        Gson gson = new GsonBuilder()
                .create();
        this.rewardJson = gson.toJson(reward, Reward.class);
    }

    public void claim(){
        //TODO: přepsat na lepší systém bonusů, neukládá se bonus
        getReward().applyReward(playerIdentity, true, bonus);
        UnclaimedRewardsTable.removeUnclaimedReward(this);
        ModuleManager.getModule(UnclaimedRewardsModule.class).removeUnclaimedReward(playerIdentity, this);
    }
}