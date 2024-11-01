package cz.johnslovakia.gameapi.users.quests;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.economy.Economy;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerData;
import cz.johnslovakia.gameapi.utils.eTrigger.Trigger;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

public interface Quest {

    QuestType getType();
    String getName();
    default String getDisplayName(){
        return getName();
    }
    Map<Economy, Integer> getRewards();
    int getCompletionGoal();
    Set<Trigger<?>> getTriggers();

    default void complete(GamePlayer gamePlayer){
        Player player = gamePlayer.getOnlinePlayer();

        new BukkitRunnable(){
            @Override
            public void run() {
                gamePlayer.getOnlinePlayer().playSound(gamePlayer.getOnlinePlayer(), "custom:completed", 20.0F, 20.0F);
                player.sendMessage("");
                player.sendMessage(MessageManager.get(player, "chat.quests.completed")
                        .replace("%type%", MessageManager.get(player, "quest_type." + getType().toString().toLowerCase()).getTranslated())
                        .replace("%quest_name%", getName())
                        .replace("%description%", MessageManager.get(player, getTranslationKey()).getTranslated())
                        .getTranslated());
                for (Economy economy : getRewards().keySet()){
                    player.sendMessage(" " + economy.getChatColor() + "+" + getRewards().get(economy) + " §7" + economy.getName());
                }
                player.sendMessage("");

                for (Economy economy : getRewards().keySet()) {
                    economy.getEconomyInterface().deposit(gamePlayer, getRewards().get(economy));
                }
            }
        }.runTaskLater(GameAPI.getInstance(), 15L);

        gamePlayer.getPlayerData().getQuestData(this).setStatus(PlayerQuestData.Status.COMPLETED);
        gamePlayer.getPlayerData().getQuestData(this).setCompletionDate(LocalDate.now());
    }

    default void addProgress(GamePlayer gamePlayer){
        PlayerData playerData = gamePlayer.getPlayerData();
        if (playerData.getQuestData(this).isCompleted()){
            complete(gamePlayer);
        }else{
            playerData.addQuestProgress(this);
        }
    }

    default String getTranslationKey(){
        return getType().name().toLowerCase() + "_quest." + getName().toLowerCase().replace(" ", "_");
    }
}
