package cz.johnslovakia.gameapi.users.quests;

import cz.johnslovakia.gameapi.users.GamePlayer;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Date;

@Getter
public class PlayerQuestData {

    private final Quest quest;

    private final GamePlayer gamePlayer;
    @Setter
    private Status status;
    private int progress;
    @Setter
    private LocalDate startDate;

    public PlayerQuestData(Quest quest, GamePlayer gamePlayer, LocalDate startDate, int progress) {
        this.gamePlayer = gamePlayer;
        this.quest = quest;
        this.status = Status.IN_PROGRESS;
        this.startDate = startDate;
        this.progress = progress;
    }

    public PlayerQuestData(Quest quest, GamePlayer gamePlayer, LocalDate startDate, Status status) {
        this.gamePlayer = gamePlayer;
        this.quest = quest;
        this.status = status;
        this.progress = quest.getCompletionGoal();
        this.startDate = startDate;
    }

    public void increaseProgress(){
        if (progress < quest.getCompletionGoal()) {
            progress++;
        }
    }

    public boolean isCompleted(){
        return progress >= quest.getCompletionGoal();
    }

    public enum Status{
        IN_PROGRESS, COMPLETED;
    }
}
