package cz.johnslovakia.gameapi.rewards;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import cz.johnslovakia.gameapi.Shared;
import cz.johnslovakia.gameapi.database.UnclaimedRewardsTable;
import cz.johnslovakia.gameapi.inventoryBuilder.InventoryBuilder;
import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.messages.MessageModule;
import cz.johnslovakia.gameapi.modules.resources.Resource;
import cz.johnslovakia.gameapi.rewards.unclaimed.*;
import cz.johnslovakia.gameapi.users.PlayerIdentity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Getter @Setter @NoArgsConstructor
public class Reward {

    private final List<RewardItem> rewardItems = new ArrayList<>();

    private String source = null;
    private String linkedMessageKey;

    public Reward(String source) {
        this.source = source;
    }

    public Reward(RewardItem... rewardItems) {
        if (rewardItems.length != 0)
            this.rewardItems.addAll(List.of(rewardItems));
    }

    public Reward(String source, RewardItem... rewardItems) {
        this.source = source;
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


    public void setAsClaimable(PlayerIdentity playerIdentity, UnclaimedRewardType type, JsonObject data){
        Gson gson = new GsonBuilder()
                //.excludeFieldsWithoutExposeAnnotation()
                .create();
        String rewardJson = gson.toJson(this, Reward.class);

        UnclaimedReward unclaimedReward;
        switch (type) {
            case QUEST -> unclaimedReward = new QuestUnclaimedReward(playerIdentity, LocalDateTime.now(), rewardJson, data, type);
            case DAILYMETER -> unclaimedReward = new DailyMeterUnclaimedReward(playerIdentity, LocalDateTime.now(), rewardJson, data, type);
            case LEVELUP -> unclaimedReward = new LevelUpUnclaimedReward(playerIdentity, LocalDateTime.now(), rewardJson, data, type);
            default -> throw new IllegalArgumentException("Unknown reward type: " + type);
        };

        UnclaimedRewardsTable.addUnclaimedReward(unclaimedReward, data != null ? data.toString() : "");
        ModuleManager.getModule(UnclaimedRewardsModule.class).addUnclaimedReward(playerIdentity, unclaimedReward);

        InventoryBuilder currentInventoryManager = InventoryBuilder.getPlayerCurrentInventory(playerIdentity);
        if (currentInventoryManager != null) currentInventoryManager.give(playerIdentity);
    }

    public PlayerRewardRecord applyReward(PlayerIdentity playerIdentity){
        return applyReward(playerIdentity, true, 0);
    }

    public PlayerRewardRecord applyReward(PlayerIdentity playerIdentity, boolean sendMessage){
        return applyReward(playerIdentity, sendMessage, 0);
    }

    public PlayerRewardRecord applyReward(PlayerIdentity playerIdentity, boolean sendMessage, int bonus) {
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
                sendMessage(playerIdentity, earned, bonus);
            }

            for (Resource resource : earned.keySet()) {
                resource.getResourceInterface().deposit(playerIdentity, earned.get(resource));
            }
        }

        return new PlayerRewardRecord(source, earned);
    }

    public void sendMessage(PlayerIdentity playerIdentity, Map<Resource, Integer> earned){
        sendMessage(playerIdentity, earned, 0);
    }

    public void sendMessage(PlayerIdentity playerIdentity, Map<Resource, Integer> earned, int bonus){
        Set<Map.Entry<Resource, Integer>> entrySet = earned.entrySet();
        boolean bonusAppliedToAll = entrySet.stream()
                .allMatch(entry -> entry.getKey().isApplicableBonus());

        List<Component> components = entrySet.stream()
                .map(entry -> {
                    Resource resource = entry.getKey();
                    int amount = entry.getValue();

                    Component base = Component.text()
                            .append(Component.text(resource.getColor() + (source != null ? "+" : "") + amount + " "))
                            .append(Component.text(resource.getColor() + resource.getDisplayName()))
                            .build();

                    if (!bonusAppliedToAll && resource.isApplicableBonus()) {
                        Component bonusComponent = ModuleManager.getModule(MessageModule.class).get(playerIdentity, "chat.reward.bonus_applied")
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

        if (source == null) {
            ModuleManager.getModule(MessageModule.class).get(playerIdentity, "chat.resources.linked_to_message_reward")
                    .replace("%rewards%", text)
                    .addAndTranslate("chat.reward.bonus_applied", gp -> bonusAppliedToAll && bonus != 0)
                    .replace("%bonus%", "" + bonus)
                    .send();
        }else{
            ModuleManager.getModule(MessageModule.class).get(playerIdentity, "chat.resources.reward")
                    .replace("%rewards%", text)
                    .addAndTranslate("chat.reward.bonus_applied", gp -> bonusAppliedToAll && bonus != 0)
                    .replace("%for_what%", ModuleManager.getModule(MessageModule.class).existMessage(source) ? ModuleManager.getModule(MessageModule.class).get(playerIdentity, source).getTranslated() : Component.text(source))
                    .replace("%bonus%", "" + bonus)
                    .send();
        }
    }
}

