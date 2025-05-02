package cz.johnslovakia.gameapi.users.achievements;

import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.quests.Quest;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
public class PlayerAchievementData {

    private final Achievement achievement;

    private final GamePlayer gamePlayer;
    @Setter
    private Status status;
    private int progress;
    @Setter
    private LocalDate completionDate;

    public PlayerAchievementData(Achievement achievement, GamePlayer gamePlayer, int progress) {
        this.gamePlayer = gamePlayer;
        this.achievement = achievement;
        this.status = Status.LOCKED;
        this.progress = progress;
    }

    public PlayerAchievementData(Achievement achievement, GamePlayer gamePlayer, Status status) {
        this.achievement = achievement;
        this.gamePlayer = gamePlayer;
        this.status = status;
    }

    public void increaseProgress(){
        if (progress < achievement.getCompletionGoal()) {
            progress++;
        }
    }

    public boolean isCompleted(){
        return progress >= achievement.getCompletionGoal();
    }

    public enum Status{
        LOCKED, UNLOCKED;
    }
}
