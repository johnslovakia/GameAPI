package cz.johnslovakia.gameapi.utils.rewards;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.resources.Resource;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.HashMap;
import java.util.function.Consumer;

@Getter @Setter
public class Reward {

    private final List<RewardItem> rewardItems = new ArrayList<>();
    private Consumer<RewardItem> consumer;

    private String forWhat = null;
    private String linkedMessageKey;

    private static final Map<GamePlayer, Lock> playerLocks = new HashMap<>();

    private static Lock getPlayerLock(GamePlayer gamePlayer) {
        return playerLocks.computeIfAbsent(gamePlayer, k -> new ReentrantLock());
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

    public void applyReward(GamePlayer gamePlayer){
        applyReward(gamePlayer, true);
    }

    public void applyReward(GamePlayer gamePlayer, boolean sendMessage) {
        Lock lock = getPlayerLock(gamePlayer);

        boolean atleastOneApplied = false;
        for (RewardItem item : getRewardItems()) {
            if (!item.shouldApply() || item.getAmount() == 0) {
                continue;
            }
            if (consumer != null) {
                consumer.accept(item);
            }
            atleastOneApplied = true;
        }


        if (atleastOneApplied) {
            if (sendMessage && linkedMessageKey == null) {
                sendMessage(gamePlayer);
            }


            lock.lock();
            try {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        for (RewardItem item : getRewardItems()) {
                            item.getResource().getResourceInterface().deposit(gamePlayer, item.getAmount());
                        }
                        Bukkit.getScheduler().runTask(GameAPI.getInstance(), task -> lock.unlock());
                    }
                }.runTaskAsynchronously(GameAPI.getInstance());

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void sendMessage(GamePlayer gamePlayer){
        StringBuilder text = new StringBuilder();


        List<RewardItem> list =  new ArrayList<>(rewardItems);
        RewardComparator comparator = new RewardComparator();
        list.sort(comparator);

        int i = 0;
        for (RewardItem item : list) {
            if (!item.shouldApply()) {
                list.remove(item);
                continue;
            }
            Resource resource = item.getResource();

            if (i >= 1) {
                text.append("§7, ");
            }
            text.append(resource.getChatColor()).append(forWhat != null ? "+" : "").append(item.getAmount()).append(" ").append(resource.getName());

            i++;
        }

        if (forWhat == null) {
            MessageManager.get(gamePlayer, "chat.resources.linked_to_message_reward")
                    .replace("%rewards%", text.toString())
                    .send();
        }else{
            MessageManager.get(gamePlayer, "chat.resources.reward")
                    .replace("%rewards%", text.toString())
                    .replace("%for_what%", MessageManager.existMessage(forWhat) ? MessageManager.get(gamePlayer, forWhat).getTranslated() : forWhat)
                    .send();
        }
    }
}

