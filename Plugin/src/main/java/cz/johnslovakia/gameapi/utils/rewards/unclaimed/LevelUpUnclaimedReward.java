package cz.johnslovakia.gameapi.utils.rewards.unclaimed;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import cz.johnslovakia.gameapi.users.GamePlayer;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class LevelUpUnclaimedReward extends UnclaimedReward{

    private final int level;

    public LevelUpUnclaimedReward(GamePlayer gamePlayer, LocalDateTime createdAt, String rewardJson, JsonObject data, Type type) {
        super(gamePlayer, createdAt, rewardJson, data, type);

        //JsonObject dataJsonObject = JsonParser.parseString(data).getAsJsonObject();
        this.level = data.get("level").getAsInt();
    }
}
