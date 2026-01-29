package cz.johnslovakia.gameapi.modules.levels;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import cz.johnslovakia.gameapi.Shared;
import cz.johnslovakia.gameapi.database.JSConfigs;
import cz.johnslovakia.gameapi.database.PlayerTable;
import cz.johnslovakia.gameapi.events.DailyXPGainEvent;
import cz.johnslovakia.gameapi.modules.dailyRewardTrack.DailyRewardTrackModule;
import cz.johnslovakia.gameapi.modules.messages.MessageModule;
import cz.johnslovakia.gameapi.modules.Module;
import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.resources.ResourcesModule;
import cz.johnslovakia.gameapi.rewards.Reward;
import cz.johnslovakia.gameapi.rewards.RewardItem;
import cz.johnslovakia.gameapi.rewards.unclaimed.UnclaimedRewardType;
import cz.johnslovakia.gameapi.users.PlayerIdentity;
import cz.johnslovakia.gameapi.users.PlayerIdentityRegistry;

import cz.johnslovakia.gameapi.utils.CharRepo;
import cz.johnslovakia.gameapi.utils.Logger;
import cz.johnslovakia.gameapi.utils.StringUtils;
import cz.johnslovakia.gameapi.utils.TextBackground;
import cz.johnslovakia.gameapi.utils.chatHead.ChatHeadAPI;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.zort.sqllib.SQLDatabaseConnection;
import me.zort.sqllib.api.data.Row;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@NoArgsConstructor
public class LevelModule implements Module, Listener {

    @Getter(AccessLevel.PACKAGE)
    private List<LevelRange> levelRanges = new ArrayList<>();
    @Getter(AccessLevel.PACKAGE)
    private List<LevelReward> levelRewards = new ArrayList<>();
    @Getter(AccessLevel.PACKAGE)
    private List<LevelEvolution> levelEvolutions = new ArrayList<>();

    @Getter
    public transient Map<PlayerIdentity, PlayerLevelData> cache = new ConcurrentHashMap<>();


    @Override
    public void initialize() {

    }

    @Override
    public void terminate() {
        levelRanges = null;
        levelRewards = null;
        levelEvolutions = null;
        cache = null;
    }

    /*@EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        PlayerIdentity playerIdentity = PlayerIdentityRegistry.get(e.getPlayer());
        cache.remove(PlayerIdentityRegistry.get(e.getPlayer()));
    }*/

    public CompletableFuture<PlayerLevelData> loadPlayerData(PlayerIdentity playerIdentity) {
        if (!ModuleManager.getInstance().hasModule(LevelModule.class)) {
            Logger.log("LevelModule is not registered!", Logger.LogType.ERROR);
        }

        PlayerLevelData cached = cache.get(playerIdentity);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }

        return CompletableFuture.supplyAsync(() -> {
            if (Shared.getInstance().getDatabase() == null) return null;

            try (SQLDatabaseConnection connection = Shared.getInstance().getDatabase().getConnection()) {
                if (connection == null) return null;

                Optional<Row> result = connection.select()
                        .from(PlayerTable.TABLE_NAME)
                        .where().isEqual("Nickname", playerIdentity.getOnlinePlayer().getName())
                        .obtainOne();

                return result.orElse(null);
            } catch (Exception e) {
                Logger.log("Failed to load player level data for " + playerIdentity.getName() + ": " + e.getMessage(), Logger.LogType.ERROR);
                e.printStackTrace();
                return null;
            }
        }).thenCompose(row -> {
            if (row == null) {
                PlayerLevelData defaultData = new PlayerLevelData(playerIdentity, 1, 0);
                cache.put(playerIdentity, defaultData);
                return CompletableFuture.completedFuture(defaultData);
            }

            int dailyXP = row.getInt("DailyXP");

            return ModuleManager.getModule(ResourcesModule.class)
                    .getPlayerBalance(playerIdentity, "ExperiencePoints")
                    .thenCompose(xp -> {
                        int level = calculateLevelFromXp(xp);
                        PlayerLevelData data = new PlayerLevelData(playerIdentity, level, dailyXP);

                        return data.calculate().thenApply(v -> {
                            float xpProgress = (float) data.getXpOnCurrentLevel() / data.getXpToNextLevel();
                            Player player = playerIdentity.getOnlinePlayer();
                            if (player != null) {
                                player.setExp(Math.min(xpProgress, 1.0f));
                                player.setLevel(level);
                            }

                            cache.put(playerIdentity, data);
                            return data;
                        });
                    });
        });
    }

    public PlayerLevelData getPlayerData(PlayerIdentity playerIdentity) {
        PlayerLevelData data = cache.get(playerIdentity);
        if (data == null)
            playerIdentity.getOnlinePlayer().sendMessage("§cUnable to retrieve your Player Level Data at the moment. Sorry for the inconvenience.");

        return data;
    }

    public int getPlayerLevel(PlayerIdentity playerIdentity){
        return getPlayerData(playerIdentity).getLevel();
    }

    public Component getPlayerLevelColored(PlayerIdentity playerIdentity){
        PlayerLevelData data = getPlayerData(playerIdentity);
        if (data == null) return Component.text("§c0");

        if (data.getLevel() >= 100){
            return Component.text()
                    .append(Component.text("1", TextColor.fromHexString("#5fb243")))
                    .append(Component.text("0", TextColor.fromHexString("#d86dd8")))
                    .append(Component.text("0", TextColor.fromHexString("#e14d45")))
                    .build();
        }

        return Component.text(data.getLevel())
                .color(data.getLevelEvolution().color());
    }

    public Component getLevelColored(int level){
        if (level >= 100){
            return Component.text()
                    .append(Component.text("1", TextColor.fromHexString("#5fb243")))
                    .append(Component.text("0", TextColor.fromHexString("#d86dd8")))
                    .append(Component.text("0", TextColor.fromHexString("#e14d45")))
                    .build();
        }

        return Component.text(level)
                .color(getLevelEvolution(level).color());
    }

    public LevelRange getPlayerLevelRange(PlayerIdentity playerIdentity){
        return getPlayerData(playerIdentity).getLevelRange();
    }
    public LevelRange getPlayerNextLevelRange(PlayerIdentity playerIdentity){
        return getLevelRange(getPlayerData(playerIdentity).getLevel() + 1);
    }

    public void addDailyXP(PlayerIdentity playerIdentity, int amount) {
        PlayerLevelData data = getPlayerData(playerIdentity);

        int oldXP = data.getDailyXP();
        int newXP = oldXP + amount;
        data.setDailyXP(newXP);

        DailyXPGainEvent event = new DailyXPGainEvent(playerIdentity, oldXP, newXP, amount);
        Bukkit.getPluginManager().callEvent(event);

        Bukkit.getScheduler().runTaskAsynchronously(Shared.getInstance().getPlugin(), task -> {
            CompletableFuture.runAsync(() -> {
                try (SQLDatabaseConnection dbConn = Shared.getInstance().getDatabase().getConnection()) {
                    if (dbConn == null) return;

                    Connection conn = dbConn.getConnection();
                    String sql = "UPDATE " + PlayerTable.TABLE_NAME + " SET DailyXP = ? WHERE Nickname = ?";

                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setInt(1, data.getDailyXP());
                        ps.setString(2, playerIdentity.getName());
                        ps.executeUpdate();
                    }

                } catch (Exception e) {
                    Bukkit.getLogger().severe("Failed to save player data for " + playerIdentity.getName());
                    e.printStackTrace();
                }
            });
        });
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

    public LevelEvolution getNextLevelEvolution(int level){
        for (LevelEvolution evo : levelEvolutions) {
            if (evo.startLevel() > level) {
                return evo;
            }
        }
        return null;
    }

    private int calculateLevelFromXp(int totalXp) {
        int xpSum = 0;
        int currentLevel = 1;

        for (LevelRange range : levelRanges) {
            for (int lvl = range.startLevel(); lvl <= range.endLevel(); lvl++) {
                int xpNeeded = range.getXPForLevel(lvl);

                if (totalXp < xpSum + xpNeeded) {
                    return currentLevel;
                }

                xpSum += xpNeeded;
                currentLevel++;
            }
        }

        return currentLevel;
    }

    public Reward getRewardForLevel(int level){
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

    public void checkLevelUp(PlayerIdentity playerIdentity) {
        PlayerLevelData data = getPlayerData(playerIdentity);
        if (data == null) return;
        int currentLevel = data.getLevel();

        if (currentLevel >= levelRanges.getLast().endLevel())
            return;

        ModuleManager.getModule(ResourcesModule.class).getPlayerBalance(playerIdentity, "ExperiencePoints").thenAccept(xp -> {
            int newLevel = calculateLevelFromXp(xp);

            if (newLevel > currentLevel) {
                performLevelUp(playerIdentity, currentLevel, newLevel);
                data.setLevel(newLevel);
            }
        });
    }

    private void performLevelUp(PlayerIdentity playerIdentity, int currentLevel, int newLevel){
        //ToDo: new Sound

        new BukkitRunnable(){
            @Override
            public void run() {
                Player player = playerIdentity.getOnlinePlayer();
                if (player != null) {
                    player.playSound(playerIdentity.getOnlinePlayer(), "jsplugins:completed", 20.0F, 20.0F);
                    ModuleManager.getModule(MessageModule.class).get(playerIdentity, "chat.level.levelUp")
                            .replace("%level%", String.valueOf(newLevel))
                            .send();
                    levelUpBanner(playerIdentity, newLevel);
                    player.setLevel(newLevel);
                }

                for (int lvl = currentLevel + 1; lvl <= newLevel; lvl++) {
                    Reward reward = getRewardForLevel(lvl);

                    if (reward != null) {
                        JsonObject json = new JsonObject();
                        json.addProperty("level", lvl);

                        reward.setAsClaimable(playerIdentity, UnclaimedRewardType.LEVELUP, json);
                    }
                }
            }
        }.runTaskLater(Shared.getInstance().getPlugin(), 1L);
    }

    public void levelUpBanner(PlayerIdentity playerIdentity, int level) {
        Component text = Component.text("§f\uE00B ").font(Key.key("jsplugins", "actionbar_offset"))
                .shadowColor(ShadowColor.shadowColor(0))
                .append(ModuleManager.getModule(MessageModule.class).get(playerIdentity, "chat.actionbar.level_up").getTranslated())
                .append(getLevelColored(level))
                .append(getLevelEvolution(level).getIcon().font(Key.key("jsplugins", "actionbar_offset")))
                .append(Component.text(" §f\uE00B").font(Key.key("jsplugins", "actionbar_offset")));
        playerIdentity.getOnlinePlayer().sendActionBar(TextBackground.getTextWithBackground(text));
    }



    public static Builder builder() {
        return new Builder();
    }

    public static LevelModule createDefault() {
        return Builder.createDefault().build();
    }

    public static LevelModule loadOrCreateLevelModule() {
        try {
            Gson gson = new GsonBuilder()
                    .setPrettyPrinting()
                    .registerTypeAdapter(TextColor.class, new TextColorAdapter())
                    .create();

            String json = new JSConfigs().loadConfig("LevelModule");

            if (json != null && json.contains("\"color\"") && json.contains("\"scaling\"")) {
                return gson.fromJson(json, LevelModule.class);
            } else {
                Logger.log("Old or missing LevelModule format detected - creating new default config", Logger.LogType.WARNING);
                LevelModule defaultManager = LevelModule.createDefault();
                saveLevelModule(defaultManager);
                return defaultManager;
            }
        } catch (Exception ex) {
            Logger.log("Failed to load LevelModule, creating default: " + ex.getMessage(), Logger.LogType.ERROR);
            LevelModule defaultManager = LevelModule.createDefault();
            saveLevelModule(defaultManager);
            ex.printStackTrace();
            return defaultManager;
        }
    }

    public static void saveLevelModule(LevelModule levelManager) {
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(TextColor.class, new TextColorAdapter())
                .create();

        String json = gson.toJson(levelManager);
        new JSConfigs().saveConfig("LevelModule", json);
    }

    public static class Builder {

        private final LevelModule levelModule;
        private final List<LevelRangeBuilder> pendingRanges = new ArrayList<>();
        private final List<LevelRewardBuilder> pendingRewards = new ArrayList<>();

        private Builder() {
            this.levelModule = new LevelModule();
        }

        public static Builder createDefault() {
            return new Builder()
                    .addLevelRange(0, 10, 20000, LevelRange.XPScaling.AGGRESSIVE_EXPONENTIAL).withReward("Coins", 1000)
                    .addLevelRange(11, 20, 20000, LevelRange.XPScaling.AGGRESSIVE_EXPONENTIAL).withReward("Coins", 2000)
                    .addLevelRange(21, 30, 20000, LevelRange.XPScaling.AGGRESSIVE_EXPONENTIAL).withReward("Coins", 3000)
                    .addLevelRange(31, 40, 20000, LevelRange.XPScaling.AGGRESSIVE_EXPONENTIAL).withReward("Coins", 3000)
                    .addLevelRange(41, 50, 20000, LevelRange.XPScaling.AGGRESSIVE_EXPONENTIAL).withReward("Coins", 4000)
                    .addLevelRange(51, 60, 20000, LevelRange.XPScaling.AGGRESSIVE_EXPONENTIAL).withReward("Coins", 4000)
                    .addLevelRange(61, 70, 20000, LevelRange.XPScaling.AGGRESSIVE_EXPONENTIAL).withReward("Coins", 5000)
                    .addLevelRange(71, 80, 20000, LevelRange.XPScaling.AGGRESSIVE_EXPONENTIAL).withReward("Coins", 5000)
                    .addLevelRange(81, 90, 20000, LevelRange.XPScaling.AGGRESSIVE_EXPONENTIAL).withReward("Coins", 7500)
                    .addLevelRange(91, 100, 20000, LevelRange.XPScaling.AGGRESSIVE_EXPONENTIAL).withReward("Coins", 10000)

                    .addLevelEvolution(0, "\uE000", TextColor.fromHexString("#707070"),1027, 1028)
                    .addLevelEvolution(10, "\uE001", TextColor.fromHexString("#d0d0d0"), 1029, 1030)
                    .addLevelEvolution(20, "\uE002", TextColor.fromHexString("#4bd81c"), 1031, 1032)
                    .addLevelEvolution(30, "\uE003", TextColor.fromHexString("#63dfdf"), 1033, 1034)
                    .addLevelEvolution(40, "\uE004", TextColor.fromHexString("#3080d0"), 1035, 1036)
                    .addLevelEvolution(50, "\uE005", TextColor.fromHexString("#e06ef4"), 1037, 1038)
                    .addLevelEvolution(60, "\uE006", TextColor.fromHexString("#8d39bb"), 1039, 1040)
                    .addLevelEvolution(70, "\uE007", TextColor.fromHexString("#8d39bb"), 1041, 1042)
                    .addLevelEvolution(80, "\uE008", TextColor.fromHexString("#dc9935"), 1043, 1044)
                    .addLevelEvolution(90, "\uE009", TextColor.fromHexString("#c94646"), 1045, 1046)
                    .addLevelEvolution(100, "\uE00A", NamedTextColor.GOLD, 1047, 1048)

                    .addLevelReward(2).withReward("CosmeticTokens", 2)
                    .addLevelReward(5, 10, 15, 20, 25, 30, 35, 40, 45).withReward("CosmeticTokens", 4)
                    .addLevelReward(50, 55, 60, 65, 70, 75, 80, 85, 90, 95).withReward("CosmeticTokens", 8)
                    .addLevelReward(100).withReward("CosmeticTokens", 25)
                    .done();
        }

        public LevelRangeBuilder addLevelRange(int startLevel, int endLevel, int xpPerLevel) {
            LevelRangeBuilder rangeBuilder = new LevelRangeBuilder(
                    this, startLevel, endLevel, xpPerLevel, LevelRange.XPScaling.FLAT
            );
            pendingRanges.add(rangeBuilder);
            return rangeBuilder;
        }

        public LevelRangeBuilder addLevelRange(int startLevel, int endLevel, int baseXP, LevelRange.XPScaling scaling) {
            LevelRangeBuilder rangeBuilder = new LevelRangeBuilder(
                    this, startLevel, endLevel, baseXP, scaling
            );
            pendingRanges.add(rangeBuilder);
            return rangeBuilder;
        }

        public static class LevelRangeBuilder {
            private final Builder parent;
            private final int startLevel;
            private final int endLevel;
            private final int baseXP;
            private final LevelRange.XPScaling scaling;
            private Reward reward;

            private LevelRangeBuilder(Builder parent, int startLevel, int endLevel, int baseXP, LevelRange.XPScaling scaling) {
                this.parent = parent;
                this.startLevel = startLevel;
                this.endLevel = endLevel;
                this.baseXP = baseXP;
                this.scaling = scaling;
            }

            public Builder withReward(String resource, int amount) {
                this.reward = new Reward(new RewardItem(resource, amount));
                return parent;
            }

            public Builder withReward(Reward reward) {
                this.reward = reward;
                return parent;
            }

            public Builder withoutReward() {
                this.reward = new Reward();
                return parent;
            }

            LevelRange build() {
                Reward finalReward = reward != null ? reward : new Reward();
                return new LevelRange(startLevel, endLevel, baseXP, scaling, finalReward);
            }
        }

        public Builder addLevelEvolution(int startLevel, String icon, TextColor color, int itemCustomModelData, int blinkingItemCustomModelData) {
            LevelEvolution evolution = new LevelEvolution(startLevel, icon, color, itemCustomModelData, blinkingItemCustomModelData);
            levelModule.getLevelEvolutions().add(evolution);
            return this;
        }

        public LevelRewardBuilder addLevelReward(int... levels) {
            LevelRewardBuilder rewardBuilder = new LevelRewardBuilder(this, levels);
            pendingRewards.add(rewardBuilder);
            return rewardBuilder;
        }

        public static class LevelRewardBuilder {
            private final Builder parent;
            private final int[] levels;
            private final List<RewardItem> rewardItems = new ArrayList<>();

            private LevelRewardBuilder(Builder parent, int... levels) {
                this.parent = parent;
                this.levels = levels;
            }

            public LevelRewardBuilder withReward(String resource, int amount) {
                rewardItems.add(new RewardItem(resource, amount));
                return this;
            }

            public LevelRewardBuilder withRandomReward(String resource, int minAmount, int maxAmount) {
                rewardItems.add(RewardItem.builder(resource)
                        .setRandomAmountRange(minAmount, maxAmount)
                        .build());
                return this;
            }

            public LevelRewardBuilder withChanceReward(String resource, int amount, int chancePercent) {
                rewardItems.add(RewardItem.builder(resource)
                        .setAmount(amount)
                        .setChance(chancePercent)
                        .build());
                return this;
            }

            public Builder done() {
                return parent;
            }

            public LevelRewardBuilder addLevelReward(int... levels) {
                return parent.addLevelReward(levels);
            }

            public LevelRangeBuilder addLevelRange(int startLevel, int endLevel, int neededXP) {
                return parent.addLevelRange(startLevel, endLevel, neededXP);
            }

            public LevelModule build() {
                return parent.build();
            }

            LevelReward buildRecord() {
                Reward reward = new Reward();
                rewardItems.forEach(reward::addRewardItem);
                return new LevelReward(reward, levels);
            }
        }

        public LevelModule build() {
            for (LevelRangeBuilder rangeBuilder : pendingRanges) {
                LevelRange range = rangeBuilder.build();
                levelModule.getLevelRanges().add(range);
            }

            for (LevelRewardBuilder rewardBuilder : pendingRewards) {
                LevelReward levelReward = rewardBuilder.buildRecord();
                levelModule.getLevelRewards().add(levelReward);
            }

            levelModule.getLevelEvolutions().sort(Comparator.comparingInt(LevelEvolution::startLevel));

            return levelModule;
        }
    }
}

