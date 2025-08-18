package cz.johnslovakia.gameapi.users.achievements;

import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.quests.Quest;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter @Setter
public class PlayerAchievementData {

    private final Achievement achievement;
    private final GamePlayer gamePlayer;

    private Status status;
    private AchievementStage stage;
    private int progress;
    private LocalDate completionDate;

    public PlayerAchievementData(Achievement achievement, GamePlayer gamePlayer, AchievementStage stage, int progress) {
        this.gamePlayer = gamePlayer;
        this.achievement = achievement;
        this.stage = stage;
        this.progress = progress;

        this.status = Status.LOCKED;
    }

    public PlayerAchievementData(Achievement achievement, GamePlayer gamePlayer, Status status) {
        this.achievement = achievement;
        this.gamePlayer = gamePlayer;
        this.status = status;
    }

    public void increaseProgress(){
        if (progress < stage.goal()) {
            progress++;
        }
    }

    public enum Status{
        LOCKED, UNLOCKED;
    }
}
