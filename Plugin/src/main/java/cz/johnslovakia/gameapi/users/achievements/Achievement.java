package cz.johnslovakia.gameapi.users.achievements;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerData;
import cz.johnslovakia.gameapi.utils.eTrigger.Trigger;
import cz.johnslovakia.gameapi.utils.rewards.Reward;

import cz.johnslovakia.gameapi.utils.rewards.RewardItem;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.LocalDate;
import java.util.Set;

public interface Achievement {

    String getName();
    default String getDisplayName(){
        return getName();
    }
    int getPoints();
    //Map<int> getStages(); //je i tam dole zakomentovany complete
    int getCompletionGoal();
    Set<Trigger<?>> getTriggers();

    default void complete(GamePlayer gamePlayer){
        Player player = gamePlayer.getOnlinePlayer();
        PlayerData playerData = gamePlayer.getPlayerData();

        if (playerData.getAchievementData(this).isEmpty())
            return;

        PlayerAchievementData data = playerData.getAchievementData(this).get();

        data.setStatus(PlayerAchievementData.Status.UNLOCKED);
        data.setCompletionDate(LocalDate.now());

        new BukkitRunnable(){
            @Override
            public void run() {
                gamePlayer.getOnlinePlayer().playSound(gamePlayer.getOnlinePlayer(), "custom:completed", 20.0F, 20.0F);
                player.sendMessage(MessageManager.get(player, "chat.achievements.unlocked")
                        .replace("%name%", getName())
                        .replace("%description%", MessageManager.get(player, getTranslationKey()).getTranslated())
                        .getTranslated());

                new Reward(new RewardItem("Points", getPoints())).applyReward(gamePlayer);
            }
        }.runTaskLater(GameAPI.getInstance(), 1L);
    }

    default void addProgress(GamePlayer gamePlayer){
        PlayerData playerData = gamePlayer.getPlayerData();

        if (playerData.getAchievementData(this).isEmpty())
            return;

        PlayerAchievementData achievementData = playerData.getAchievementData(this).get();

        achievementData.increaseProgress();
        //if (achievementData.getProgress() >= getCompletionGoal())
            complete(gamePlayer);
    }

    default String getTranslationKey(){
        return "achievement." + getName().toLowerCase().replace(" ", "_");
    }
}
