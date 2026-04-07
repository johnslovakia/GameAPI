package cz.johnslovakia.gameapi.modules.levels;

import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.resources.ResourcesModule;

import cz.johnslovakia.gameapi.utils.Logger;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.OfflinePlayer;

import java.util.concurrent.CompletableFuture;

@Getter
public class PlayerLevelData {

    @Getter(AccessLevel.PRIVATE)
    private final LevelModule levelModule = ModuleManager.getModule(LevelModule.class);
    @Getter(AccessLevel.PRIVATE)
    private final OfflinePlayer offlinePlayer;

    private int level;
    private int xpOnCurrentLevel;
    private int xpToNextLevel;
    @Setter
    private int dailyXP;

    private LevelRange levelRange;
    private LevelEvolution levelEvolution;

    public PlayerLevelData(OfflinePlayer offlinePlayer, int level, int dailyXP) {
        this.offlinePlayer = offlinePlayer;
        this.level = level;
        this.dailyXP = dailyXP;
    }

    public CompletableFuture<Void> calculate(int totalXp) {
        return CompletableFuture.runAsync(() -> applyXp(totalXp));
    }

    public void calculateSync(int totalXp) {
        applyXp(totalXp);
    }

    private void applyXp(int totalXp) {
        int xpSum = 0;

        for (LevelRange range : levelModule.getLevelRanges()) {
            for (int lvl = range.startLevel(); lvl <= range.endLevel(); lvl++) {
                int xpPerLevel = range.getXPForLevel(lvl);

                if (totalXp < xpSum + xpPerLevel) {
                    this.levelRange = range;
                    this.levelEvolution = levelModule.getLevelEvolution(level);
                    this.xpOnCurrentLevel = totalXp - xpSum;
                    this.xpToNextLevel = xpPerLevel;
                    return;
                }

                xpSum += xpPerLevel;
            }
        }

        this.levelRange = levelModule.getLevelRanges().getLast();
        this.levelEvolution = levelModule.getLevelEvolution(level);
        this.xpOnCurrentLevel = 0;
        this.xpToNextLevel = 0;
    }

    public CompletableFuture<Void> calculate() {
        return ModuleManager.getModule(ResourcesModule.class)
                .getPlayerBalance(offlinePlayer, "ExperiencePoints")
                .thenAccept(this::applyXp)
                .exceptionally(ex -> {
                    Logger.log("calculate: FAILED - " + ex.getMessage(), Logger.LogType.ERROR);
                    ex.printStackTrace();
                    return null;
                });
    }

    public void setLevel(int level) {
        this.level = level;
        calculate();
    }
}