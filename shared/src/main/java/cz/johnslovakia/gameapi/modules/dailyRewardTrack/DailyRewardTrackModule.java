package cz.johnslovakia.gameapi.modules.dailyRewardTrack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import cz.johnslovakia.gameapi.Shared;
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
import cz.johnslovakia.gameapi.users.PlayerIdentityRegistry;
import cz.johnslovakia.gameapi.utils.Logger;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import me.zort.sqllib.SQLDatabaseConnection;
import me.zort.sqllib.api.data.Row;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
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
    private Map<PlayerIdentity, Integer> dailyRewardsClaims = new ConcurrentHashMap<>();

    @Setter
    private Reward afterMaxTierReward;
    @Setter
    private int maxTier = 7;

    @Override
    public void initialize() {

    }

    @Override
    public void terminate() {
        tiers = null;;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        PlayerIdentity playerIdentity = PlayerIdentityRegistry.get(player);

        loadPlayerData(playerIdentity);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        dailyRewardsClaims.remove(PlayerIdentityRegistry.get(e.getPlayer()));
    }

    private void loadPlayerData(PlayerIdentity playerIdentity) {
        CompletableFuture.runAsync(() -> {
            if (Shared.getInstance().getDatabase() == null) {
                return;
            }

            try (SQLDatabaseConnection connection = Shared.getInstance().getDatabase().getConnection()) {
                if (connection == null) {
                    return;
                }

                String tableName = PlayerTable.TABLE_NAME;
                LocalDate today = LocalDate.now();

                Optional<Row> result = connection.select()
                        .from(tableName)
                        .where().isEqual("Nickname", playerIdentity.getName())
                        .obtainOne();

                if (result.isPresent()) {
                    String lastReset = result.get().getString("DailyRewards_reset");

                    if (lastReset == null) {
                        connection.update()
                                .table(tableName)
                                .set("DailyRewards_reset", today.toString())
                                .where().isEqual("Nickname", playerIdentity.getName())
                                .execute();
                    } else {
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
                                stmt.setString(2, playerIdentity.getName());
                                stmt.executeUpdate();
                            }

                            if (ModuleManager.getInstance().hasModule(LevelModule.class)) {
                                ModuleManager.getModule(LevelModule.class).getPlayerData(playerIdentity).setDailyXP(0);
                            }
                            dailyRewardsClaims.put(playerIdentity, 0);
                        } else {
                            dailyRewardsClaims.computeIfAbsent(playerIdentity,
                                    pi -> result.get().getInt("DailyRewards_claims"));
                        }
                    }
                }

            } catch (SQLException e) {
                Logger.log("Error loading player data for " + playerIdentity.getName() + ": " + e.getMessage(), Logger.LogType.ERROR);
                e.printStackTrace();
            }
        });
    }

    public void setPlayerDailyRewardsClaim(PlayerIdentity playerIdentity, int claims){
        dailyRewardsClaims.put(playerIdentity, claims);
    }

    public void savePlayerDailyRewardsClaim(PlayerIdentity playerIdentity) {
        Bukkit.getScheduler().runTaskAsynchronously(Shared.getInstance().getPlugin(), task -> {
            if (Shared.getInstance().getDatabase() == null) return;

            try (SQLDatabaseConnection connection = Shared.getInstance().getDatabase().getConnection()) {
                if (connection == null) return;

                String tableName = PlayerTable.TABLE_NAME;

                connection.update()
                        .table(tableName)
                        .set("DailyRewards_claims", getPlayerDailyClaims(playerIdentity))
                        .where().isEqual("Nickname", playerIdentity.getName())
                        .execute();

            } catch (Exception e) {
                Logger.log("Failed to save daily rewards claims for " + playerIdentity.getName() + ": " + e.getMessage(), Logger.LogType.ERROR);
                e.printStackTrace();
            }
        });
    }

    public int getPlayerDailyClaims(PlayerIdentity playerIdentity){
        return dailyRewardsClaims.getOrDefault(playerIdentity, 0);
    }

    /*@EventHandler
    public void onDailyXPGain(DailyXPGainEvent e) {
        PlayerIdentity playerIdentity = e.getPlayerIdentity();
        UnclaimedRewardsModule unclaimedRewardsModule = ModuleManager.getModule(UnclaimedRewardsModule.class);
        int newBalance = e.getNewXP();

        /*List<DailyRewardTier> tiers = new ArrayList<>(getTiers()
                .stream()
                .filter(t ->
                        t.tier() > getPlayerDailyClaims(playerIdentity) &&
                                unclaimedRewardsModule.getPlayerUnclaimedRewardsByType(playerIdentity, UnclaimedRewardType.DAILYMETER)
                                        .stream()
                                        .map(r -> (DailyMeterUnclaimedReward) r)
                                        .noneMatch(r -> r.getTier() == t.tier())
                )
                .toList());*


        int xp = newBalance;
        for (DailyRewardTier tier : getTiers()) {

            boolean isClaimedOrUnclaimed = tier.tier() <= getPlayerDailyClaims(playerIdentity) ||
                    unclaimedRewardsModule.getPlayerUnclaimedRewardsByType(playerIdentity, UnclaimedRewardType.DAILYMETER)
                            .stream()
                            .map(r -> (DailyMeterUnclaimedReward) r)
                            .anyMatch(r -> r.getTier() == tier.tier());

            if (isClaimedOrUnclaimed) {
                xp -= tier.neededXP();
            } else if (xp >= tier.neededXP()) {
                Reward reward = tier.reward();

                JsonObject json = new JsonObject();
                json.addProperty("tier", tier.tier());
                reward.setAsClaimable(playerIdentity, UnclaimedRewardType.DAILYMETER, json);

                xp -= tier.neededXP();
            } else {
                break;
            }
        }
    }*/

    @EventHandler
    public void onDailyXPGain(DailyXPGainEvent e) {
        PlayerIdentity playerIdentity = e.getPlayerIdentity();
        UnclaimedRewardsModule unclaimedRewardsModule = ModuleManager.getModule(UnclaimedRewardsModule.class);
        int dailyXP = e.getNewXP();

        int accumulatedXP = 0;

        for (DailyRewardTier tier : getTiers()) {
            accumulatedXP += tier.neededXP();

            if (dailyXP >= accumulatedXP) {
                boolean alreadyHasReward = unclaimedRewardsModule
                        .getPlayerUnclaimedRewardsByType(playerIdentity, UnclaimedRewardType.DAILYMETER)
                        .stream()
                        .map(r -> (DailyMeterUnclaimedReward) r)
                        .filter(r -> r.getCreatedAt().toLocalDate().equals(LocalDate.now()))
                        .anyMatch(r -> r.getTier() == tier.tier());

                boolean alreadyClaimed = tier.tier() <= getPlayerDailyClaims(playerIdentity);

                if (!alreadyHasReward && !alreadyClaimed) {
                    Reward reward = tier.reward();
                    JsonObject json = new JsonObject();
                    json.addProperty("tier", tier.tier());
                    reward.setAsClaimable(playerIdentity, UnclaimedRewardType.DAILYMETER, json);
                }
            }
        }
    }

    public int getXpProgressOnCurrentTier(PlayerIdentity playerIdentity) {
        PlayerLevelData data = ModuleManager.getModule(LevelModule.class).getPlayerData(playerIdentity);
        if (data == null) return 0;

        int dailyXp = data.getDailyXP();
        int xpSpent = 0;

        for (DailyRewardTier tier : tiers) {
            if (dailyXp < xpSpent + tier.neededXP()) {
                return dailyXp - xpSpent;
            }
            xpSpent += tier.neededXP();
        }

        return tiers.isEmpty() ? 0 : tiers.getLast().neededXP();
    }

    public DailyRewardTier getPlayerCurrentTier(PlayerIdentity playerIdentity) {
        PlayerLevelData data = ModuleManager.getModule(LevelModule.class).getPlayerData(playerIdentity);
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
        PlayerLevelData data = ModuleManager.getModule(LevelModule.class).getPlayerData(playerIdentity);
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
                    .addTier(750)
                        .withRandomReward("Coins", 400, 600)
                        .withChanceReward("CosmeticTokens", 1, 4)
                    .addTier(750)
                        .withRandomReward("Coins", 500, 700)
                        .withChanceReward("CosmeticTokens", 1, 8)
                    .addTier(1000)
                        .withRandomReward("Coins", 600, 700)
                        .withChanceReward("CosmeticTokens", 1, 12)
                    .addTier(1500)
                        .withRandomReward("Coins", 700, 900)
                        .withChanceReward("CosmeticTokens", 1, 18)
                    .addTier(2000)
                        .withRandomReward("Coins", 700, 900)
                        .withChanceReward("CosmeticTokens", 1, 24)
                    .addTier(2500)
                        .withRandomReward("Coins", 800, 1000)
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
                            "Cannot add more tiers! Maximum is " + module.maxTier +
                                    ", current count: " + module.tiers.size()
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
