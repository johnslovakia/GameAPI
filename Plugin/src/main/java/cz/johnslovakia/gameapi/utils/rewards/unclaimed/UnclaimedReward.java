package cz.johnslovakia.gameapi.utils.rewards.unclaimed;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import cz.johnslovakia.gameapi.datastorage.UnclaimedRewardsTable;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.resources.Resource;
import cz.johnslovakia.gameapi.utils.rewards.Reward;
import cz.johnslovakia.gameapi.utils.rewards.RewardItem;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
public class UnclaimedReward {

    private final GamePlayer gamePlayer;
    private final LocalDateTime createdAt;
    private final String rewardJson;
    private final JsonObject data;
    private final Type type;

    private final Reward reward;

    @Setter
    private int bonus;
    @Setter
    private boolean claimed = false;

    public UnclaimedReward(GamePlayer gamePlayer, LocalDateTime createdAt, String rewardJson, JsonObject data, Type type) {
        this.gamePlayer = gamePlayer;
        this.createdAt = createdAt;
        this.rewardJson = rewardJson;
        this.data = data;
        this.type = type;

        Gson gson = new GsonBuilder()
                .create();
        this.reward = gson.fromJson(rewardJson, Reward.class);

    }

    public UnclaimedReward(GamePlayer gamePlayer, LocalDateTime createdAt, Reward reward, JsonObject data, Type type) {
        this.gamePlayer = gamePlayer;
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
        getReward().applyReward(gamePlayer, true, bonus);
        UnclaimedRewardsTable.removeUnclaimedReward(this);
        gamePlayer.getPlayerData().getUnclaimedRewards().remove(this);
    }


    public enum Type{
        QUEST, BATTLEPASS, DAILYMETER, LEVELUP;
    }
}