package cz.johnslovakia.gameapi.rewards.unclaimed;

import com.google.gson.JsonObject;
import cz.johnslovakia.gameapi.users.PlayerIdentity;
import lombok.Getter;
import org.bukkit.entity.Player;

import java.time.LocalDateTime;

@Getter
public class DailyMeterUnclaimedReward extends UnclaimedReward{

    private final int tier;

    public DailyMeterUnclaimedReward(PlayerIdentity playerIdentity, LocalDateTime createdAt, String rewardJson, JsonObject data, UnclaimedRewardType type) {
        super(playerIdentity, createdAt, rewardJson, data, type);

        //JsonObject dataJsonObject = JsonParser.parseString(data).getAsJsonObject();
        this.tier = data.get("tier").getAsInt();
    }
}
