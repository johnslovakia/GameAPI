package cz.johnslovakia.gameapi.utils.rewards.unclaimed;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.quests.QuestType;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class QuestUnclaimedReward extends UnclaimedReward{

    private final String questName;
    private final QuestType questType;

    public QuestUnclaimedReward(GamePlayer gamePlayer, LocalDateTime createdAt, String rewardJson, JsonObject data, Type type) {
        super(gamePlayer, createdAt, rewardJson, data, type);

        //JsonObject dataJsonObject = JsonParser.parseString(data).getAsJsonObject();
        this.questName = data.get("questName").getAsString();
        this.questType = QuestType.valueOf(data.get("questType").getAsString());
    }
}
