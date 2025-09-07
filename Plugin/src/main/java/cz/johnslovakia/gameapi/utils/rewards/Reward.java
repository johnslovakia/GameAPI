package cz.johnslovakia.gameapi.utils.rewards;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.datastorage.UnclaimedRewardsTable;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.resources.Resource;
import cz.johnslovakia.gameapi.utils.inventoryBuilder.InventoryManager;
import cz.johnslovakia.gameapi.utils.rewards.unclaimed.DailyMeterUnclaimedReward;
import cz.johnslovakia.gameapi.utils.rewards.unclaimed.LevelUpUnclaimedReward;
import cz.johnslovakia.gameapi.utils.rewards.unclaimed.QuestUnclaimedReward;
import cz.johnslovakia.gameapi.utils.rewards.unclaimed.UnclaimedReward;

import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Getter @Setter
public class Reward {

    private final List<RewardItem> rewardItems = new ArrayList<>();

    private String forWhat = null;
    private String linkedMessageKey;

    //@Expose(serialize = false, deserialize = false)
    private transient Map<GamePlayer, Lock> playerLocks = new HashMap<>();

    private Lock getPlayerLock(GamePlayer gamePlayer) {
        return playerLocks.computeIfAbsent(gamePlayer, k -> new ReentrantLock());
    }

    public Reward(String forWhat) {
        this.forWhat = forWhat;
    }

    public Reward() {
    }

    public Reward(RewardItem... rewardItems) {
        if (rewardItems.length != 0)
            this.rewardItems.addAll(List.of(rewardItems));
    }

    public Reward(String forWhat, RewardItem... rewardItems) {
        this.forWhat = forWhat;
        if (rewardItems.length != 0)
            this.rewardItems.addAll(List.of(rewardItems));
    }

    public void addRewardItem(RewardItem... rewardItems){
        for (RewardItem item : rewardItems){
            if (!this.rewardItems.contains(item)){
                this.rewardItems.add(item);
            }
        }
    }


    public void setAsClaimable(GamePlayer gamePlayer, UnclaimedReward.Type type){
        setAsClaimable(gamePlayer, type, null);
    }

    public void setAsClaimable(GamePlayer gamePlayer, UnclaimedReward.Type type, JsonObject data){
        Gson gson = new GsonBuilder()
                //.excludeFieldsWithoutExposeAnnotation()
                .create();
        String rewardJson = gson.toJson(this, Reward.class);

        UnclaimedReward unclaimedReward;
        switch (type) {
            case QUEST -> unclaimedReward = new QuestUnclaimedReward(gamePlayer, LocalDateTime.now(), rewardJson, data, type);
            case DAILYMETER -> unclaimedReward = new DailyMeterUnclaimedReward(gamePlayer, LocalDateTime.now(), rewardJson, data, type);
            case LEVELUP -> unclaimedReward = new LevelUpUnclaimedReward(gamePlayer, LocalDateTime.now(), rewardJson, data, type);
            default -> throw new IllegalArgumentException("Unknown reward type: " + type);
        };

        UnclaimedRewardsTable.addUnclaimedReward(unclaimedReward, data != null ? data.toString() : "");
        gamePlayer.getPlayerData().addUnclaimedReward(unclaimedReward);

        InventoryManager currentInventoryManager = gamePlayer.getPlayerData().getCurrentInventory();
        if (currentInventoryManager != null && gamePlayer.getGame().getPlayers().contains(gamePlayer)) currentInventoryManager.give(gamePlayer.getOnlinePlayer());
    }

    public PlayerRewardRecord applyReward(GamePlayer gamePlayer){
        return applyReward(gamePlayer, true, 0);
    }

    public PlayerRewardRecord applyReward(GamePlayer gamePlayer, boolean sendMessage){
        return applyReward(gamePlayer, sendMessage, 0);
    }

    public PlayerRewardRecord applyReward(GamePlayer gamePlayer, boolean sendMessage, int bonus) {
        Lock lock = getPlayerLock(gamePlayer);
        Map<Resource, Integer> earned = new HashMap<>();

        boolean atleastOneApplied = false;
        for (RewardItem item : getRewardItems()) {
            int amount = item.getAmount();
            if (item.getResource().isApplicableBonus()) amount += (int) (amount * bonus / 100.0);

            if (!item.shouldApply() || amount == 0) {
                continue;
            }
            earned.put(item.getResource(), amount);
            atleastOneApplied = true;
        }


        if (atleastOneApplied) {
            if (sendMessage && linkedMessageKey == null) {
                sendMessage(gamePlayer, earned, bonus);
            }

            lock.lock();
            try {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        for (Resource resource : earned.keySet()) {
                            resource.getResourceInterface().deposit(gamePlayer, earned.get(resource));
                        }

                        Bukkit.getScheduler().runTask(Minigame.getInstance().getPlugin(), task -> lock.unlock());
                    }
                }.runTaskAsynchronously(Minigame.getInstance().getPlugin());

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return new PlayerRewardRecord(earned);
    }

    /*public PlayerRewardRecord applyReward(GamePlayer gamePlayer, boolean sendMessage, int bonus) {
        Map<Resource, Integer> earned = new HashMap<>();

        boolean atleastOneApplied = false;
        for (RewardItem item : getRewardItems()) {
            int amount = item.getAmount();
            if (!item.shouldApply() || amount == 0) {
                continue;
            }
            amount = amount + (int) (((double) amount * (double) bonus) / (double) 100);
            earned.put(item.getResource(), amount);
            atleastOneApplied = true;
        }

        if (!atleastOneApplied) {
            return new PlayerRewardRecord(new HashMap<>());
        }

        if (sendMessage && linkedMessageKey == null) {
            sendMessage(gamePlayer, bonus);
        }

        try {
            new BukkitRunnable() {
                @Override
                public void run() {
                    Lock taskLock = getPlayerLock(gamePlayer);
                    taskLock.lock();
                    try {
                        for (Map.Entry<Resource, Integer> entry : earned.entrySet()) {
                            Resource resource = entry.getKey();
                            int finalAmount = entry.getValue();

                            Logger.log("Applying reward: " + finalAmount + " " + resource.getName() + " for player " + gamePlayer.getOnlinePlayer().getName(), Logger.LogType.INFO);
                            resource.getResourceInterface().deposit(gamePlayer, finalAmount);
                        }
                    } catch (Exception e) {
                        Logger.log("Error applying reward asynchronously for player " + gamePlayer.getOnlinePlayer().getName() + ": " + e.getMessage(), Logger.LogType.ERROR);
                        e.printStackTrace();
                    } finally {
                        taskLock.unlock();
                    }
                }
            }.runTaskAsynchronously(Minigame.getInstance().getPlugin());

        } catch (Exception e) {
            Logger.log("Failed to schedule asynchronous reward application task for player " + gamePlayer.getOnlinePlayer().getName() + ": " + e.getMessage(), Logger.LogType.ERROR);
            e.printStackTrace();
        }

        return new PlayerRewardRecord(earned);
    }*/

    public void sendMessage(GamePlayer gamePlayer, Map<Resource, Integer> earned){
        sendMessage(gamePlayer, earned, 0);
    }

    public void sendMessage(GamePlayer gamePlayer, Map<Resource, Integer> earned, int bonus){
        Set<Map.Entry<Resource, Integer>> entrySet = earned.entrySet();
        boolean bonusAppliedToAll = entrySet.stream()
                .allMatch(entry -> entry.getKey().isApplicableBonus());

        List<Component> components = entrySet.stream()
                .map(entry -> {
                    Resource resource = entry.getKey();
                    int amount = entry.getValue();

                    Component base = Component.text()
                            .append(Component.text(resource.getColor() + (forWhat != null ? "+" : "") + amount + " "))
                            .append(Component.text(resource.getColor() + resource.getDisplayName()))
                            .build();

                    if (!bonusAppliedToAll && resource.isApplicableBonus()) {
                        Component bonusComponent = MessageManager.get(gamePlayer, "chat.reward.bonus_applied")
                                .replace("%bonus%", String.valueOf(bonus))
                                .getTranslated();

                        base = base.append(Component.space()).append(bonusComponent);
                    }

                    return base;
                })
                .toList();

        Component text = Component.empty();
        for (int i = 0; i < components.size(); i++) {
            if (i > 0) text = text.append(Component.text("§7, "));
            text = text.append(components.get(i));
        }

        if (forWhat == null) {
            MessageManager.get(gamePlayer, "chat.resources.linked_to_message_reward")
                    .replace("%rewards%", text)
                    .addAndTranslate("chat.reward.bonus_applied", gp -> bonusAppliedToAll && bonus != 0)
                    .replace("%bonus%", "" + bonus)
                    .send();
        }else{
            MessageManager.get(gamePlayer, "chat.resources.reward")
                    .replace("%rewards%", text)
                    .addAndTranslate("chat.reward.bonus_applied", gp -> bonusAppliedToAll && bonus != 0)
                    .replace("%for_what%", MessageManager.existMessage(forWhat) ? MessageManager.get(gamePlayer, forWhat).getTranslated() : Component.text(forWhat))
                    .replace("%bonus%", "" + bonus)
                    .send();
        }
    }
}

