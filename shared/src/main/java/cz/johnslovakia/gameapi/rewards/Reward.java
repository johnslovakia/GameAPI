package cz.johnslovakia.gameapi.rewards;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import cz.johnslovakia.gameapi.database.UnclaimedRewardsTable;
import cz.johnslovakia.gameapi.inventoryBuilder.InventoryBuilder;
import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.messages.MessageModule;
import cz.johnslovakia.gameapi.modules.resources.Resource;
import cz.johnslovakia.gameapi.rewards.unclaimed.*;
import cz.johnslovakia.gameapi.users.PlayerIdentity;
import cz.johnslovakia.gameapi.users.PlayerIdentityRegistry;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.kyori.adventure.text.Component;

import java.time.LocalDateTime;
import java.util.*;

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

    public void setAsClaimable(PlayerIdentity playerIdentity, UnclaimedRewardType type, JsonObject data) {
        setAsClaimable(playerIdentity.getOfflinePlayer(), type, data);
    }

    public void setAsClaimable(OfflinePlayer player, UnclaimedRewardType type, JsonObject data) {
        Gson gson = new GsonBuilder()
                .create();
        String rewardJson = gson.toJson(this, Reward.class);

        UnclaimedReward unclaimedReward = switch (type) {
            case QUEST -> new QuestUnclaimedReward(player, LocalDateTime.now(), rewardJson, data, type);
            case DAILYMETER -> new DailyMeterUnclaimedReward(player, LocalDateTime.now(), rewardJson, data, type);
            case LEVELUP -> new LevelUpUnclaimedReward(player, LocalDateTime.now(), rewardJson, data, type);
            default -> throw new IllegalArgumentException("Unknown reward type: " + type);
        };

        UnclaimedRewardsTable.addUnclaimedReward(unclaimedReward, data != null ? data.toString() : "");
        ModuleManager.getModule(UnclaimedRewardsModule.class).addUnclaimedReward(player, unclaimedReward);

        if (player.isOnline()) {
            PlayerIdentity playerIdentity = PlayerIdentityRegistry.get(player);
            InventoryBuilder currentInventoryManager = InventoryBuilder.getPlayerCurrentInventory(playerIdentity);
            if (currentInventoryManager != null) currentInventoryManager.give(playerIdentity);
        }
    }

    public PlayerRewardRecord applyReward(PlayerIdentity playerIdentity) {
        return applyReward(playerIdentity.getOfflinePlayer(), true, 0);
    }

    public PlayerRewardRecord applyReward(PlayerIdentity playerIdentity, boolean sendMessage) {
        return applyReward(playerIdentity.getOfflinePlayer(), sendMessage, 0);
    }

    public PlayerRewardRecord applyReward(PlayerIdentity playerIdentity, boolean sendMessage, int bonus) {
        return applyReward(playerIdentity.getOfflinePlayer(), sendMessage, bonus);
    }

    public PlayerRewardRecord applyReward(OfflinePlayer player) {
        return applyReward(player, true, 0);
    }

    public PlayerRewardRecord applyReward(OfflinePlayer player, boolean sendMessage) {
        return applyReward(player, sendMessage, 0);
    }

    public PlayerRewardRecord applyReward(OfflinePlayer player, boolean sendMessage, int bonus) {
        Map<Resource, Integer> earned = new LinkedHashMap<>();

        boolean atleastOneApplied = false;
        for (RewardItem item : getRewardItems().stream().sorted(new RewardComparator()).toList()) {
            int amount = item.getAmount();
            if (item.getResource().isApplicableBonus()) amount += (int) (amount * bonus / 100.0);

            if (!item.shouldApply() || amount == 0) {
                continue;
            }
            earned.put(item.getResource(), amount);
            atleastOneApplied = true;
        }

        if (atleastOneApplied) {
            Player online = player instanceof Player p ? p : null;
            if (sendMessage && linkedMessageKey == null && online != null) {
                sendMessage(online, earned, bonus);
            }

            for (Map.Entry<Resource, Integer> entry : earned.entrySet()) {
                entry.getKey().getResourceInterface().deposit(player, entry.getValue());
            }
        }

        return new PlayerRewardRecord(source, earned);
    }

    public void sendMessage(Player player, Map<Resource, Integer> earned) {
        sendMessage(player, earned, 0);
    }

    public void sendMessage(Player player, Map<Resource, Integer> earned, int bonus) {
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
                        Component bonusComponent = ModuleManager.getModule(MessageModule.class).get(player, "chat.reward.bonus_applied")
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
            ModuleManager.getModule(MessageModule.class).get(player, "chat.resources.linked_to_message_reward")
                    .replace("%rewards%", text)
                    .addAndTranslate("chat.reward.bonus_applied", gp -> bonusAppliedToAll && bonus != 0)
                    .replace("%bonus%", "" + bonus)
                    .send();
        } else {
            ModuleManager.getModule(MessageModule.class).get(player, "chat.resources.reward")
                    .replace("%rewards%", text)
                    .addAndTranslate("chat.reward.bonus_applied", gp -> bonusAppliedToAll && bonus != 0)
                    .replace("%for_what%", ModuleManager.getModule(MessageModule.class).existMessage(source) ? ModuleManager.getModule(MessageModule.class).get(player, source).getTranslated() : Component.text(source))
                    .replace("%bonus%", "" + bonus)
                    .send();
        }
    }
}