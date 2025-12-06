package cz.johnslovakia.gameapi.modules.quests;

import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerIdentity;
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
    private LocalDate startDate;

    public PlayerQuestData(GamePlayer gamePlayer, Quest quest, LocalDate startDate, int progress) {
        this.gamePlayer = gamePlayer;
        this.quest = quest;
        this.status = Status.IN_PROGRESS;
        this.startDate = startDate;
        this.progress = progress;
    }

    public PlayerQuestData(GamePlayer gamePlayer, Quest quest, LocalDate startDate, Status status) {
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
