package cz.johnslovakia.gameapi.rewards.unclaimed;

import com.google.gson.JsonObject;
import cz.johnslovakia.gameapi.modules.quests.QuestType;
import cz.johnslovakia.gameapi.users.PlayerIdentity;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class QuestUnclaimedReward extends UnclaimedReward{

    private final String questName;
    private final QuestType questType;

    public QuestUnclaimedReward(PlayerIdentity playerIdentity, LocalDateTime createdAt, String rewardJson, JsonObject data, UnclaimedRewardType type) {
        super(playerIdentity, createdAt, rewardJson, data, type);

        //JsonObject dataJsonObject = JsonParser.parseString(data).getAsJsonObject();
        this.questName = data.get("questName").getAsString();
        this.questType = QuestType.valueOf(data.get("questType").getAsString());
    }
}
