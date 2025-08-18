package cz.johnslovakia.gameapi.users.quests;

import com.google.gson.JsonObject;
import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.users.resources.Resource;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerData;
import cz.johnslovakia.gameapi.utils.eTrigger.Trigger;
import cz.johnslovakia.gameapi.utils.rewards.Reward;
import cz.johnslovakia.gameapi.utils.rewards.unclaimed.UnclaimedReward;
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
    Reward getReward();
    int getCompletionGoal();
    Set<Trigger<?>> getTriggers();

    default void complete(GamePlayer gamePlayer){
        Player player = gamePlayer.getOnlinePlayer();

        gamePlayer.getPlayerData().getQuestData(this).setStatus(PlayerQuestData.Status.COMPLETED);

        new BukkitRunnable(){
            @Override
            public void run() {
                gamePlayer.getOnlinePlayer().playSound(gamePlayer.getOnlinePlayer(), "jsplugins:completed", 20.0F, 20.0F);
                player.sendMessage(MessageManager.get(player, "chat.quests.completed")
                        .replace("%type%", MessageManager.get(player, "quest_type." + getType().toString().toLowerCase()).getTranslated())
                        .replace("%quest_name%", getName())
                        .replace("%description%", MessageManager.get(player, getTranslationKey()).getTranslated())
                        .getTranslated());

                JsonObject json = new JsonObject();
                json.addProperty("questName", getName());
                json.addProperty("questType", getType().name());

                getReward().setAsClaimable(gamePlayer, UnclaimedReward.Type.QUEST, json);
            }
        }.runTaskLater(Minigame.getInstance().getPlugin(), 1L);
    }

    default void addProgress(GamePlayer gamePlayer){
        PlayerData playerData = gamePlayer.getPlayerData();
        PlayerQuestData questData = playerData.getQuestData(this);

        questData.increaseProgress();
        if (questData.getProgress() >= getCompletionGoal()){
            complete(gamePlayer);
        }
    }

    default String getTranslationKey(){
        return getType().name().toLowerCase() + "_quest." + getName().toLowerCase().replace(" ", "_");
    }
}
