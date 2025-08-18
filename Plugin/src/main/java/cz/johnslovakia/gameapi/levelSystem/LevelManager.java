package cz.johnslovakia.gameapi.levelSystem;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.datastorage.JSConfigs;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.resources.Resource;

import cz.johnslovakia.gameapi.utils.rewards.Reward;
import cz.johnslovakia.gameapi.utils.rewards.RewardItem;
import cz.johnslovakia.gameapi.utils.rewards.unclaimed.UnclaimedReward;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Getter
@NoArgsConstructor
public class LevelManager {

    public static void saveLevelManager(LevelManager levelManager) {
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();

        String json = gson.toJson(levelManager);
        new JSConfigs(Minigame.getInstance().getDatabase().getConnection()).saveConfig("LevelManager", json);
    }

    public static LevelManager loadOrCreateLevelManager() {
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();

        String json = new JSConfigs(Minigame.getInstance().getDatabase().getConnection()).loadConfig("LevelManager");

        if (json != null) {
            return gson.fromJson(json, LevelManager.class);
        } else {
            LevelManager defaultManager = createDefaultLevelManager();
            saveLevelManager(defaultManager);
            return defaultManager;
        }
    }

    private static LevelManager createDefaultLevelManager(){
        LevelManager levelManager = new LevelManager();
        levelManager.addLevelRange(new LevelRange(0, 10, 5000, new Reward(new RewardItem("Coins", 1000))));
        levelManager.addLevelRange(new LevelRange(11, 20, 7500, new Reward(new RewardItem("Coins", 1500))));
        levelManager.addLevelRange(new LevelRange(21, 30, 7500, new Reward(new RewardItem("Coins", 2000))));
        levelManager.addLevelRange(new LevelRange(31, 40, 10000, new Reward(new RewardItem("Coins", 2500))));
        levelManager.addLevelRange(new LevelRange(41, 50, 10000, new Reward(new RewardItem("Coins", 2500))));
        levelManager.addLevelRange(new LevelRange(51, 60, 12500, new Reward(new RewardItem("Coins", 3000))));
        levelManager.addLevelRange(new LevelRange(61, 70, 12500, new Reward(new RewardItem("Coins", 3000))));
        levelManager.addLevelRange(new LevelRange(71, 80, 15000, new Reward(new RewardItem("Coins", 3500))));
        levelManager.addLevelRange(new LevelRange(81, 90, 15000, new Reward(new RewardItem("Coins", 4000))));
        levelManager.addLevelRange(new LevelRange(91, 100, 20000, new Reward(new RewardItem("Coins", 4000))));
        levelManager.addLevelEvolution(new LevelEvolution(0, "\uE000"));
        levelManager.addLevelEvolution(new LevelEvolution(10, "\uE001"));
        levelManager.addLevelEvolution(new LevelEvolution(20, "\uE002"));
        levelManager.addLevelEvolution(new LevelEvolution(30, "\uE003"));
        levelManager.addLevelEvolution(new LevelEvolution(40, "\uE004"));
        levelManager.addLevelEvolution(new LevelEvolution(50, "\uE005"));
        levelManager.addLevelEvolution(new LevelEvolution(60, "\uE006"));
        levelManager.addLevelEvolution(new LevelEvolution(70, "\uE007"));
        levelManager.addLevelEvolution(new LevelEvolution(80, "\uE008"));
        levelManager.addLevelEvolution(new LevelEvolution(90, "\uE009"));
        levelManager.addLevelEvolution(new LevelEvolution(100, "\uE00A"));

        DailyMeter dailyMeter = new DailyMeter();
        dailyMeter.addDailyMeterLevel(500, new Reward(RewardItem.builder("Coins").setRandomAmountRange(250, 500).build()));
        dailyMeter.addDailyMeterLevel(750, new Reward(RewardItem.builder("Coins").setRandomAmountRange(300, 500).build()));
        dailyMeter.addDailyMeterLevel(1000, new Reward(RewardItem.builder("Coins").setRandomAmountRange(400, 700).build()));
        dailyMeter.addDailyMeterLevel(1500, new Reward(RewardItem.builder("Coins").setRandomAmountRange(400, 700).build()));
        dailyMeter.addDailyMeterLevel(2000, new Reward(RewardItem.builder("Coins").setRandomAmountRange(500, 750).build()));
        dailyMeter.addDailyMeterLevel(2500, new Reward(RewardItem.builder("Coins").setRandomAmountRange(600, 750).build()));
        dailyMeter.addDailyMeterLevel(3000, new Reward(RewardItem.builder("Coins").setRandomAmountRange(750, 1000).build()));
        levelManager.setDailyMeter(dailyMeter);


        return levelManager;
    }




    private final List<LevelRange> levelRanges = new ArrayList<>();
    private final List<LevelReward> levelRewards = new ArrayList<>();
    private final List<LevelEvolution> levelEvolutions = new ArrayList<>();
    @Setter
    private DailyMeter dailyMeter;

    public int getMaxLevel(){
        return levelRanges.stream()
                .mapToInt(LevelRange::endLevel)
                .max()
                .orElse(0);
    }

    public void addLevelRange(LevelRange range) {
        levelRanges.add(range);
    }

    public void addLevelReward(LevelReward levelReward){
        levelRewards.add(levelReward);
    }

    public void addLevelEvolution(LevelEvolution evolution) {
        levelEvolutions.add(evolution);
        levelEvolutions.sort(Comparator.comparingInt(LevelEvolution::startLevel));
    }

    public LevelProgress getLevelProgress(GamePlayer gamePlayer) {
        int totalXp = gamePlayer.getPlayerData().getBalance(Resource.getResourceByName("ExperiencePoints"));
        int xpSum = 0;

        for (LevelRange range : levelRanges) {
            for (int lvl = range.startLevel(); lvl <= range.endLevel(); lvl++) {
                int xpPerLevel = range.neededXP();

                if (totalXp < xpSum + xpPerLevel) {
                    int xpOnCurrent = totalXp - xpSum;
                    return new LevelProgress(gamePlayer.getPlayerData().getLevel(), range, getLevelEvolution(gamePlayer.getPlayerData().getLevel()), xpOnCurrent, xpPerLevel);
                }

                xpSum += xpPerLevel;
            }
        }

        LevelRange last = levelRanges.get(levelRanges.size() - 1);
        return new LevelProgress(gamePlayer.getPlayerData().getLevel(), last, getLevelEvolution(gamePlayer.getPlayerData().getLevel()), 0, last.neededXP());
    }

    public LevelEvolution getNextEvolution(int level){
        for (LevelEvolution evo : levelEvolutions) {
            if (evo.startLevel() > level) {
                return evo;
            }
        }
        return null;
    }

    public void isLevelUp(GamePlayer gamePlayer) {
        int currentLevel = gamePlayer.getPlayerData().getLevel();

        if (currentLevel >= getMaxLevel())
            return;

        Resource resource = Resource.getResourceByName("ExperiencePoints");
        int xp = gamePlayer.getPlayerData().getBalance(resource);
        int newLevel = getLevelForXp(xp);

        if (newLevel > currentLevel) {
            onLevelUp(gamePlayer, currentLevel, newLevel);
            gamePlayer.getPlayerData().setLevel(newLevel);
        }
    }

    public LevelRange getLevelRange(int level) {
        int currentLevel = 1;

        for (LevelRange range : levelRanges) {
            int start = currentLevel;
            int end = currentLevel + range.getLength() - 1;

            if (level >= start && level <= end) {
                return range;
            }

            currentLevel = end + 1;
        }

        return null;
    }

    public LevelEvolution getLevelEvolution(int level) {
        LevelEvolution result = levelEvolutions.get(0);
        for (LevelEvolution evolution : levelEvolutions) {
            if (level >= evolution.startLevel()) {
                result = evolution;
            } else {
                break;
            }
        }
        return result;
    }

    public int getLevelForXp(int totalXp) {
        int xpSum = 0;
        int level = 1;

        for (LevelRange range : levelRanges) {
            for (int i = 0; i < range.getLength(); i++) {
                xpSum += range.neededXP();
                if (totalXp < xpSum) {
                    return level;
                }
                level++;
            }
        }

        return level;
    }

    public Reward getReward(int level){
        /*return levelRewards.stream()
                .filter(lr -> lr.level() == level)
                .map(LevelReward::reward)
                .findFirst()
                .orElse(getLevelRange(level).reward());*/

        LevelReward levelReward = levelRewards.stream()
                .filter(lr -> lr.level() == level)
                .findFirst()
                .orElse(null);
        if (levelReward != null){
            Reward merged = new Reward();
            levelReward.reward().getRewardItems().forEach(merged::addRewardItem);
            getLevelRange(level).reward().getRewardItems().forEach(merged::addRewardItem);
            return merged;
        }else{
            return getLevelRange(level).reward();
        }


    }

    public void onLevelUp(GamePlayer gamePlayer, int currentLevel, int newLevel){
        Player player = gamePlayer.getOnlinePlayer();
        //ToDo: new Sound


        new BukkitRunnable(){
            @Override
            public void run() {
                gamePlayer.getOnlinePlayer().playSound(gamePlayer.getOnlinePlayer(), "jsplugins:completed", 20.0F, 20.0F);
                MessageManager.get(player, "chat.level.levelUp")
                        .replace("%level%", String.valueOf(newLevel))
                        .send();

                for (int lvl = currentLevel + 1; lvl <= newLevel; lvl++) {
                    Reward reward = getReward(lvl);

                    if (reward != null) {
                        JsonObject json = new JsonObject();
                        json.addProperty("level", lvl);

                        reward.setAsClaimable(gamePlayer, UnclaimedReward.Type.LEVELUP, json);
                    }
                }
            }
        }.runTaskLater(Minigame.getInstance().getPlugin(), 1L);
    }
}

