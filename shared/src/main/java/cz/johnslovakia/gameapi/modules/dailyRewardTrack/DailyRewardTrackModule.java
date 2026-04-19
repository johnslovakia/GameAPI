package cz.johnslovakia.gameapi.modules.dailyRewardTrack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import cz.johnslovakia.gameapi.Core;
import cz.johnslovakia.gameapi.database.JSConfigs;
import cz.johnslovakia.gameapi.database.PlayerTable;
import cz.johnslovakia.gameapi.events.DailyXPGainEvent;
import cz.johnslovakia.gameapi.modules.Module;
import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.levels.LevelModule;
import cz.johnslovakia.gameapi.modules.levels.PlayerLevelData;
import cz.johnslovakia.gameapi.rewards.Reward;
import cz.johnslovakia.gameapi.rewards.RewardItem;
import cz.johnslovakia.gameapi.rewards.unclaimed.DailyMeterUnclaimedReward;
import cz.johnslovakia.gameapi.rewards.unclaimed.UnclaimedRewardType;
import cz.johnslovakia.gameapi.rewards.unclaimed.UnclaimedRewardsModule;
import cz.johnslovakia.gameapi.users.PlayerIdentity;
import cz.johnslovakia.gameapi.utils.Logger;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import me.zort.sqllib.SQLDatabaseConnection;
import me.zort.sqllib.api.data.Row;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class DailyRewardTrackModule implements Module, Listener {

    private List<DailyRewardTier> tiers = new ArrayList<>();
    @Getter(AccessLevel.PACKAGE)
    private transient Map<String, Integer> dailyRewardsClaims = new ConcurrentHashMap<>();

    private final Map<String, Integer> pendingDailyXP = new ConcurrentHashMap<>();
    private final Set<String> processingDailyXP = ConcurrentHashMap.newKeySet();

    @Setter
    private Reward afterMaxTierReward;
    @Setter
    private int maxTier = 7;

    @Override
    public void initialize() {
    }

    @Override
    public void terminate() {
        tiers = null;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        loadPlayerData(e.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        int claims = getPlayerDailyClaims(e.getPlayer());
        String name = e.getPlayer().getName();

        CompletableFuture.runAsync(() -> {
            savePlayerDailyRewardsClaim(e.getPlayer(), claims);
            if (Bukkit.getPlayer(name) == null) dailyRewardsClaims.remove(name);
        });
    }

    @EventHandler
    public void onDailyXPGain(DailyXPGainEvent e) {
        OfflinePlayer offlinePlayer = e.getOfflinePlayer();
        String playerName = offlinePlayer.getName();
        int dailyXP = e.getNewXP();

        pendingDailyXP.put(playerName, dailyXP);
        if (!processingDailyXP.add(playerName)) return;

        processNextDailyXP(offlinePlayer, playerName);
    }

    private void processNextDailyXP(OfflinePlayer offlinePlayer, String playerName) {
        Integer dailyXP = pendingDailyXP.remove(playerName);
        if (dailyXP == null) {
            processingDailyXP.remove(playerName);
            return;
        }

        UnclaimedRewardsModule unclaimedRewardsModule = ModuleManager.getModule(UnclaimedRewardsModule.class);

        CompletableFuture<?> rewardsFuture = unclaimedRewardsModule.getOrLoadUnclaimedRewardsByType(offlinePlayer, UnclaimedRewardType.DAILYMETER);
        CompletableFuture<Integer> claimsFuture = getOrLoadPlayerClaimsAsync(offlinePlayer);

        final int capturedDailyXP = dailyXP;

        CompletableFuture.allOf(rewardsFuture, claimsFuture)
                .thenAccept(unused -> {
                    @SuppressWarnings("unchecked")
                    List<?> rawRewards = (List<?>) rewardsFuture.join();
                    int playerClaims = claimsFuture.join();

                    int accumulatedXP = 0;

                    for (DailyRewardTier tier : getTiers()) {
                        accumulatedXP += tier.neededXP();
                        if (capturedDailyXP < accumulatedXP) break;

                        boolean alreadyHasReward = rawRewards.stream()
                                .map(r -> (DailyMeterUnclaimedReward) r)
                                .filter(r -> r.getCreatedAt().toLocalDate().equals(LocalDate.now()))
                                .anyMatch(r -> r.getTier() == tier.tier());

                        boolean alreadyClaimed = tier.tier() <= playerClaims;

                        if (!alreadyHasReward && !alreadyClaimed) {
                            Reward reward = tier.reward();
                            JsonObject json = new JsonObject();
                            json.addProperty("tier", tier.tier());
                            reward.setAsClaimable(offlinePlayer, UnclaimedRewardType.DAILYMETER, json);
                        }
                    }

                    if (pendingDailyXP.containsKey(playerName)) {
                        processNextDailyXP(offlinePlayer, playerName);
                    } else {
                        processingDailyXP.remove(playerName);
                    }
                })
                .exceptionally(ex -> {
                    processingDailyXP.remove(playerName);
                    pendingDailyXP.remove(playerName);
                    Logger.log("Error processing daily XP for " + playerName + ": " + ex.getMessage(), Logger.LogType.ERROR);
                    return null;
                });
    }

    private CompletableFuture<Integer> getOrLoadPlayerClaimsAsync(OfflinePlayer player) {
        Integer cached = dailyRewardsClaims.get(player.getName());
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }

        return CompletableFuture.supplyAsync(() -> {
            if (Core.getInstance().getDatabase() == null) return 0;
            try (SQLDatabaseConnection connection = Core.getInstance().getDatabase().getConnection()) {
                if (connection == null) return 0;
                Optional<Row> result = connection.select()
                        .from(PlayerTable.TABLE_NAME)
                        .where().isEqual("Nickname", player.getName())
                        .obtainOne();
                if (result.isPresent()) {
                    int claims = result.get().getInt("DailyRewards_claims");
                    dailyRewardsClaims.putIfAbsent(player.getName(), claims);
                    return dailyRewardsClaims.get(player.getName());
                }
            } catch (Exception e) {
                Logger.log("Failed to load daily claims for " + player.getName() + ": " + e.getMessage(), Logger.LogType.ERROR);
            }
            return 0;
        });
    }

    private void loadPlayerData(OfflinePlayer player) {
        CompletableFuture.runAsync(() -> {
            if (Core.getInstance().getDatabase() == null) return;

            try (SQLDatabaseConnection connection = Core.getInstance().getDatabase().getConnection()) {
                if (connection == null) return;

                String tableName = PlayerTable.TABLE_NAME;
                LocalDate today = LocalDate.now();

                Optional<Row> result = connection.select()
                        .from(tableName)
                        .where().isEqual("Nickname", player.getName())
                        .obtainOne();

                if (result.isEmpty()) return;

                String lastReset = result.get().getString("DailyRewards_reset");

                if (lastReset == null) {
                    connection.update()
                            .table(tableName)
                            .set("DailyRewards_reset", today.toString())
                            .where().isEqual("Nickname", player.getName())
                            .execute();
                    return;
                }

                LocalDate lastResetDate = LocalDate.parse(lastReset);

                if (!lastResetDate.equals(today)) {
                    Connection conn = connection.getConnection();
                    String sql = "UPDATE `" + tableName + "` SET " +
                            "DailyRewards_reset = ?, " +
                            "DailyXP = 0, " +
                            "DailyRewards_claims = 0 " +
                            "WHERE Nickname = ?";

                    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                        stmt.setString(1, today.toString());
                        stmt.setString(2, player.getName());
                        stmt.executeUpdate();
                    }

                    if (ModuleManager.getInstance().hasModule(LevelModule.class)) {
                        PlayerLevelData levelData = ModuleManager.getModule(LevelModule.class).getPlayerData(player);
                        if (levelData != null) {
                            levelData.setDailyXP(0);
                        }
                    }

                    dailyRewardsClaims.put(player.getName(), 0);
                } else {
                    dailyRewardsClaims.put(player.getName(), result.get().getInt("DailyRewards_claims"));
                }

            } catch (SQLException e) {
                Logger.log("Error loading player data for " + player.getName() + ": " + e.getMessage(), Logger.LogType.ERROR);
                e.printStackTrace();
            }
        });
    }

    public void setPlayerDailyRewardsClaim(PlayerIdentity playerIdentity, int claims) {
        setPlayerDailyRewardsClaim(playerIdentity.getOfflinePlayer(), claims);
    }

    public void setPlayerDailyRewardsClaim(OfflinePlayer player, int claims) {
        dailyRewardsClaims.put(player.getName(), claims);
    }

    public void savePlayerDailyRewardsClaim(PlayerIdentity playerIdentity, int playerClaims) {
        savePlayerDailyRewardsClaim(playerIdentity.getOfflinePlayer(), playerClaims);
    }

    public void savePlayerDailyRewardsClaim(OfflinePlayer player, int playerClaims) {
        if (Core.getInstance().getDatabase() == null) return;

        try (SQLDatabaseConnection connection = Core.getInstance().getDatabase().getConnection()) {
            if (connection == null) return;

            connection.update()
                    .table(PlayerTable.TABLE_NAME)
                    .set("DailyRewards_claims", playerClaims)
                    .where().isEqual("Nickname", player.getName())
                    .execute();

        } catch (Exception e) {
            Logger.log("Failed to save daily rewards claims for " + player.getName() + ": " + e.getMessage(), Logger.LogType.ERROR);
            e.printStackTrace();
        }
    }

    public int getPlayerDailyClaims(PlayerIdentity playerIdentity) {
        return getPlayerDailyClaims(playerIdentity.getOfflinePlayer());
    }

    public int getPlayerDailyClaims(OfflinePlayer player) {
        return dailyRewardsClaims.getOrDefault(player.getName(), 0);
    }

    public int getXpProgressOnCurrentTier(PlayerIdentity playerIdentity) {
        return getXpProgressOnCurrentTier(playerIdentity.getOfflinePlayer());
    }

    public int getXpProgressOnCurrentTier(OfflinePlayer player) {
        PlayerLevelData data = ModuleManager.getModule(LevelModule.class).getPlayerData(player);
        if (data == null) return 0;

        int dailyXp = data.getDailyXP();
        int accumulatedXP = 0;

        for (DailyRewardTier tier : tiers) {
            if (dailyXp < accumulatedXP + tier.neededXP()) {
                return dailyXp - accumulatedXP;
            }
            accumulatedXP += tier.neededXP();
        }

        return dailyXp - accumulatedXP;
    }

    public DailyRewardTier getPlayerCurrentTier(PlayerIdentity playerIdentity) {
        return getPlayerCurrentTier(playerIdentity.getOfflinePlayer());
    }

    public DailyRewardTier getPlayerCurrentTier(OfflinePlayer player) {
        PlayerLevelData data = ModuleManager.getModule(LevelModule.class).getPlayerData(player);
        if (data == null) return null;

        int dailyXp = data.getDailyXP();
        int accumulatedXP = 0;
        DailyRewardTier currentTier = getTiers().getFirst();

        for (DailyRewardTier tier : tiers) {
            accumulatedXP += tier.neededXP();
            if (dailyXp >= accumulatedXP) {
                currentTier = tier;
            } else {
                break;
            }
        }

        return currentTier;
    }

    public DailyRewardTier getPlayerCurrentTargetTier(PlayerIdentity playerIdentity) {
        return getPlayerCurrentTargetTier(playerIdentity.getOfflinePlayer());
    }

    public DailyRewardTier getPlayerCurrentTargetTier(OfflinePlayer player) {
        PlayerLevelData data = ModuleManager.getModule(LevelModule.class).getPlayerData(player);
        if (data == null) return null;

        int dailyXp = data.getDailyXP();
        int accumulatedXP = 0;

        for (DailyRewardTier tier : tiers) {
            accumulatedXP += tier.neededXP();
            if (dailyXp < accumulatedXP) {
                return tier;
            }
        }

        return null;
    }


    public static Builder builder() {
        return new Builder();
    }

    public static DailyRewardTrackModule createDefault() {
        return Builder.createDefault().build();
    }

    public static DailyRewardTrackModule loadOrCreateDailyRewardTrackModule() {
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();

        String json = new JSConfigs().loadConfig("DailyRewardTrackModule");

        if (json != null) {
            return gson.fromJson(json, DailyRewardTrackModule.class);
        } else {
            DailyRewardTrackModule defaultManager = DailyRewardTrackModule.createDefault();
            saveDailyRewardTrackModule(defaultManager);
            return defaultManager;
        }
    }

    public static void saveDailyRewardTrackModule(DailyRewardTrackModule dailyRewardTrackModule) {
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();

        String json = gson.toJson(dailyRewardTrackModule);
        new JSConfigs().saveConfig("DailyRewardTrackModule", json);
    }

    public static class Builder {
        private final DailyRewardTrackModule module;
        private final List<TierBuilder> pendingTiers = new ArrayList<>();

        private Builder() {
            this.module = new DailyRewardTrackModule();
        }

        public static Builder createDefault() {
            return new Builder()
                    .setMaxTier(7)
                    .addTier(500)
                        .withRandomReward("Coins", 250, 500)
                    .addTier(500)
                        .withRandomReward("Coins", 250, 500)
                        .withChanceReward("CosmeticTokens", 1, 4)
                    .addTier(1000)
                        .withRandomReward("Coins", 400, 700)
                        .withChanceReward("CosmeticTokens", 1, 8)
                    .addTier(1000)
                        .withRandomReward("Coins", 400, 700)
                        .withChanceReward("CosmeticTokens", 1, 12)
                    .addTier(1500)
                        .withRandomReward("Coins", 500, 750)
                        .withChanceReward("CosmeticTokens", 1, 18)
                    .addTier(2000)
                        .withRandomReward("Coins", 700, 1000)
                        .withChanceReward("CosmeticTokens", 1, 24)
                    .addTier(3000)
                        .withRandomReward("Coins", 800, 1200)
                        .withRandomChanceReward("CosmeticTokens", 1, 2, 30)
                    .done();
        }

        public Builder setMaxTier(int maxTier) {
            module.setMaxTier(maxTier);
            return this;
        }

        public Builder setAfterMaxTierReward(Reward reward) {
            module.setAfterMaxTierReward(reward);
            return this;
        }

        public TierBuilder addTier(int neededXP) {
            TierBuilder tierBuilder = new TierBuilder(this, neededXP);
            pendingTiers.add(tierBuilder);
            return tierBuilder;
        }

        public static class TierBuilder {
            private final Builder parent;
            private final int neededXP;
            private final List<RewardItem> rewardItems = new ArrayList<>();

            private TierBuilder(Builder parent, int neededXP) {
                this.parent = parent;
                this.neededXP = neededXP;
            }

            public TierBuilder withReward(String resource, int amount) {
                rewardItems.add(new RewardItem(resource, amount));
                return this;
            }

            public TierBuilder withRandomReward(String resource, int minAmount, int maxAmount) {
                rewardItems.add(RewardItem.builder(resource)
                        .setRandomAmountRange(minAmount, maxAmount)
                        .build());
                return this;
            }

            public TierBuilder withChanceReward(String resource, int amount, int chancePercent) {
                rewardItems.add(RewardItem.builder(resource)
                        .setAmount(amount)
                        .setChance(chancePercent)
                        .build());
                return this;
            }

            public TierBuilder withRandomChanceReward(String resource, int minAmount, int maxAmount, int chancePercent) {
                rewardItems.add(RewardItem.builder(resource)
                        .setRandomAmountRange(minAmount, maxAmount)
                        .setChance(chancePercent)
                        .build());
                return this;
            }

            public Builder done() {
                return parent;
            }

            public TierBuilder addTier(int neededXP) {
                return parent.addTier(neededXP);
            }

            public DailyRewardTrackModule build() {
                return parent.build();
            }

            DailyRewardTier buildTier(int tierNumber) {
                Reward reward = new Reward();
                rewardItems.forEach(reward::addRewardItem);
                return new DailyRewardTier(tierNumber, neededXP, reward);
            }
        }

        public DailyRewardTrackModule build() {
            int tierNumber = 1;
            for (TierBuilder tierBuilder : pendingTiers) {
                if (module.tiers.size() >= module.maxTier) {
                    throw new IllegalStateException(
                            "Cannot add more tiers! Maximum is " + module.maxTier + ", current count: " + module.tiers.size()
                    );
                }
                if (tierBuilder.neededXP <= 0) {
                    throw new IllegalArgumentException("neededXP must be positive, got: " + tierBuilder.neededXP);
                }
                DailyRewardTier tier = tierBuilder.buildTier(tierNumber++);
                module.tiers.add(tier);
            }
            module.tiers.sort(Comparator.comparingInt(DailyRewardTier::tier));
            return module;
        }
    }
}