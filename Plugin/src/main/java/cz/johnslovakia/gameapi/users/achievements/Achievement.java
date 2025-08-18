package cz.johnslovakia.gameapi.users.achievements;

import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerData;
import cz.johnslovakia.gameapi.utils.eTrigger.Trigger;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public interface Achievement {

    String getName();
    default String getDisplayName(){
        return getName();
    }
    List<AchievementStage> getStages();
    Set<Trigger<?>> getTriggers();

    default void complete(GamePlayer gamePlayer, AchievementStage stage){
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
                gamePlayer.getOnlinePlayer().playSound(gamePlayer.getOnlinePlayer(), "jsplugins:completed", 20.0F, 20.0F);
                /*MessageManager.get(player, "chat.achievements.unlocked")
                        .replace("%name%", getName())
                        .replace("%description%", MessageManager.get(player, getTranslationKey()).getTranslated())
                        .send();*/

                Component textComponent = MessageManager.get(player, "chat.achievements.unlocked")
                            .replace("%name%", getName())
                            .replace("%description%", MessageManager.get(player, getTranslationKey()).getTranslated())
                            .getTranslated()
                        .hoverEvent(HoverEvent.showText(MessageManager.get(player, getTranslationKey()).getTranslated()));
                player.sendMessage(textComponent);

                //
                // new Reward(new RewardItem("Achievement Points", getPoints())).applyReward(gamePlayer);
            }
        }.runTaskLater(Minigame.getInstance().getPlugin(), 1L);

        if (data.getStage().stage() < getStages().size()){ //otestovat
            data.setStage(getStages().get(data.getStage().stage()));
            data.setProgress(0);
        }
    }

    default void addProgress(GamePlayer gamePlayer){
        PlayerData playerData = gamePlayer.getPlayerData();

        if (playerData.getAchievementData(this).isEmpty())
            return;

        PlayerAchievementData achievementData = playerData.getAchievementData(this).get();

        achievementData.increaseProgress();
        if (achievementData.getProgress() >= achievementData.getStage().goal())
            complete(gamePlayer, achievementData.getStage());
    }

    default String getTranslationKey(){
        return "achievement." + getName().toLowerCase().replace(" ", "_");
    }
}
