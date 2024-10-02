package cz.johnslovakia.gameapi.users.quests;

import cz.johnslovakia.gameapi.users.GamePlayer;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
public class PlayerQuestData {

    private final Quest quest;

    private final GamePlayer gamePlayer;
    @Setter
    private Status status;
    private int progress;
    @Setter
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

    public void increaseProgress(){
        if (progress < quest.getCompletionGoal()) {
            progress++;
        }
    }

    public boolean isCompleted(){
        return quest.getCompletionGoal() >= progress;
    }

    public enum Status{
        NOT_STARTED, IN_PROGRESS, COMPLETED;
    }
}
