package cz.johnslovakia.gameapi.levelSystem;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.datastorage.JSConfigs;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.resources.Resource;

import cz.johnslovakia.gameapi.users.resources.ResourcesManager;
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
import java.util.Arrays;
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
        levelManager.addLevelRange(new LevelRange(21, 30, 7500, new Reward(new RewardItem("Coins", 1500))));
        levelManager.addLevelRange(new LevelRange(31, 40, 10000, new Reward(new RewardItem("Coins", 2500))));
        levelManager.addLevelRange(new LevelRange(41, 50, 10000, new Reward(new RewardItem("Coins", 2500))));
        levelManager.addLevelRange(new LevelRange(51, 60, 12500, new Reward(new RewardItem("Coins", 4000))));
        levelManager.addLevelRange(new LevelRange(61, 70, 15000, new Reward(new RewardItem("Coins", 4000))));
        levelManager.addLevelRange(new LevelRange(71, 80, 15000, new Reward(new RewardItem("Coins", 5000))));
        levelManager.addLevelRange(new LevelRange(81, 90, 20000, new Reward(new RewardItem("Coins", 5000))));
        levelManager.addLevelRange(new LevelRange(91, 100, 25000, new Reward(new RewardItem("Coins", 7500))));
        levelManager.addLevelEvolution(new LevelEvolution(0, "\uE000", 1027, 1028));
        levelManager.addLevelEvolution(new LevelEvolution(10, "\uE001", 1029, 1030));
        levelManager.addLevelEvolution(new LevelEvolution(20, "\uE002", 1031, 1032));
        levelManager.addLevelEvolution(new LevelEvolution(30, "\uE003", 1033, 1034));
        levelManager.addLevelEvolution(new LevelEvolution(40, "\uE004", 1035, 1036));
        levelManager.addLevelEvolution(new LevelEvolution(50, "\uE005", 1037, 1038));
        levelManager.addLevelEvolution(new LevelEvolution(60, "\uE006", 1039, 1040));
        levelManager.addLevelEvolution(new LevelEvolution(70, "\uE007", 1041, 1042));
        levelManager.addLevelEvolution(new LevelEvolution(80, "\uE008", 1043, 1044));
        levelManager.addLevelEvolution(new LevelEvolution(90, "\uE009", 1045, 1046));
        levelManager.addLevelEvolution(new LevelEvolution(100, "\uE00A", 1047, 1048));
        
        levelManager.addLevelReward(new LevelReward(new Reward(RewardItem.builder("CosmeticTokens").setAmount(2).build()),2));
        levelManager.addLevelReward(new LevelReward(new Reward(RewardItem.builder("CosmeticTokens").setAmount(4).build()),5, 10, 15, 20, 25, 30, 35, 40, 45));
        levelManager.addLevelReward(new LevelReward(new Reward(RewardItem.builder("CosmeticTokens").setAmount(8).build()),50, 55, 60, 65, 70, 75, 80, 85, 90, 95));
        levelManager.addLevelReward(new LevelReward(new Reward(RewardItem.builder("CosmeticTokens").setAmount(25).build()),100));

        DailyMeter dailyMeter = new DailyMeter();
        dailyMeter.addDailyMeterLevel(500, new Reward(RewardItem.builder("Coins").setRandomAmountRange(250, 500).build()));
        dailyMeter.addDailyMeterLevel(750, new Reward(RewardItem.builder("Coins").setRandomAmountRange(400, 600).build(), RewardItem.builder("CosmeticTokens").setAmount(1).setChance(4).build()));
        dailyMeter.addDailyMeterLevel(1000, new Reward(RewardItem.builder("Coins").setRandomAmountRange(500, 700).build(), RewardItem.builder("CosmeticTokens").setAmount(1).setChance(8).build()));
        dailyMeter.addDailyMeterLevel(1250, new Reward(RewardItem.builder("Coins").setRandomAmountRange(600, 700).build(), RewardItem.builder("CosmeticTokens").setAmount(1).setChance(12).build()));
        dailyMeter.addDailyMeterLevel(1500, new Reward(RewardItem.builder("Coins").setRandomAmountRange(700, 900).build(), RewardItem.builder("CosmeticTokens").setAmount(1).setChance(18).build()));
        dailyMeter.addDailyMeterLevel(2000, new Reward(RewardItem.builder("Coins").setRandomAmountRange(700, 900).build(), RewardItem.builder("CosmeticTokens").setAmount(1).setChance(24).build()));
        dailyMeter.addDailyMeterLevel(2500, new Reward(RewardItem.builder("Coins").setRandomAmountRange(800, 1000).build(), RewardItem.builder("CosmeticTokens").setRandomAmountRange(1, 2).setChance(30).build()));
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
        int totalXp = gamePlayer.getPlayerData().getBalance(ResourcesManager.getResourceByName("ExperiencePoints"));
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

        Resource resource = ResourcesManager.getResourceByName("ExperiencePoints");
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
                .filter(lr -> Arrays.stream(lr.level()).anyMatch(l -> l == level))
                .findFirst()
                .orElse(null);
        if (levelReward != null){
            Reward merged = new Reward();
            getLevelRange(level).reward().getRewardItems().forEach(merged::addRewardItem);
            levelReward.reward().getRewardItems().forEach(merged::addRewardItem);
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

