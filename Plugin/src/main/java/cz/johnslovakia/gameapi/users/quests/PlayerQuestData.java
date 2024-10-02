package cz.johnslovakia.gameapi.users.quests;

import cz.johnslovakia.gameapi.users.GamePlayer;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class PlayerQuestData {

    private Quest quest;

    private GamePlayer gamePlayer;
    private Status status;
    private int progress;
    private LocalDate completionDate;

    public PlayerQuestData(Quest quest, GamePlayer gamePlayer, int progress) {
        this.gamePlayer = gamePlayer;
        this.quest = quest;
        this.status = Status.IN_PROGRESS;
        this.progress = progress;
    }

    public PlayerQuestData(Quest quest, GamePlayer gamePlayer, LocalDate completionDate) {
        this.gamePlayer = gamePlayer;
        this.quest = quest;
        this.status = Status.COMPLETED;
        this.progress = quest.getCompletionGoal();
        this.completionDate = completionDate;
    }

    public PlayerQuestData(Quest quest, GamePlayer gamePlayer) {
        this.quest = quest;
        this.gamePlayer = gamePlayer;
        this.status = Status.NOT_STARTED;
    }

    private boolean isCompleted(){
        return quest.getCompletionGoal() >= progress;
    }

    public enum Status{
        NOT_STARTED, IN_PROGRESS, COMPLETED;
    }
}
