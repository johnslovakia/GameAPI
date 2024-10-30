package cz.johnslovakia.gameapi.listeners;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.events.GamePlayerDeathEvent;
import cz.johnslovakia.gameapi.game.Game;
import cz.johnslovakia.gameapi.game.GameState;
import cz.johnslovakia.gameapi.game.cosmetics.Cosmetic;
import cz.johnslovakia.gameapi.game.cosmetics.CosmeticsCategory;
import cz.johnslovakia.gameapi.game.cosmetics.CosmeticsManager;
import cz.johnslovakia.gameapi.game.cosmetics.KillMessage;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerManager;
import cz.johnslovakia.gameapi.users.PlayerScore;
import cz.johnslovakia.gameapi.utils.Sounds;
import cz.johnslovakia.gameapi.utils.StringUtils;
import lombok.Getter;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class PlayerDeathListener implements Listener {

    private static Map<GamePlayer, Integer> spawnKillProtection = new HashMap<>();
    private static Map<GamePlayer, Integer> killCounter = new HashMap<>();
    private static Map<Integer, List<GamePlayer>> blockedxKill = new HashMap<>();


    public String getxKillMessageKey(int count){
        return switch (count){
            case 2 -> "word.double_kill";
            case 3 -> "word.tripple_kill";
            case 4 -> "word.quadra_kill";
            case 5 -> "word.penta_kill";
            default -> "";
        };
    }

    @EventHandler
    public void onGamePlayerDeath(GamePlayerDeathEvent e) {
        GamePlayer gamePlayer = e.getGamePlayer();
        Game game = e.getGame();

        boolean useTeams = game.getSettings().useTeams();


        //TODO: Nějaký nastavení?
        CosmeticsManager cosmeticsManager = GameAPI.getInstance().getCosmeticsManager();
        //CosmeticsCategory category = cosmeticsManager.getCategoryByName("Kill messages");
        //CosmeticsCategory killSoundsCategory = cosmeticsManager.getCategoryByName("Kill Sounds");
        //CosmeticsCategory killEffectCategory = cosmeticsManager.getCategoryByName("Kill Effects");

        if (e.getKiller() != null && e.getKiller() != gamePlayer){
            GamePlayer killer = e.getKiller();

            killCounter.merge(killer, 1, Integer::sum);
            new BukkitRunnable(){
                @Override
                public void run() {
                    killCounter.put(killer, killCounter.get(killer) - 1);
                }
            }.runTaskLater(GameAPI.getInstance(), 6 * 20L);

            int count = killCounter.get(killer);

            if (killCounter.get(killer) > 1){

                List<GamePlayer> list;
                if (blockedxKill.get(count) == null || blockedxKill.get(count).isEmpty()){
                    list = new ArrayList<>();
                }else{
                    list = blockedxKill.get(count);
                }
                list.add(killer);
                blockedxKill.put(count, list);

                new BukkitRunnable(){
                    @Override
                    public void run() {
                        List<GamePlayer> listNew = blockedxKill.get(count);
                        listNew.remove(killer);
                        blockedxKill.put(count, list);
                    }
                }.runTaskLater(GameAPI.getInstance(), 25 * 20L);
            }

            if (e.getDmgCause().equals(EntityDamageEvent.DamageCause.PROJECTILE)){
                //if (cosmeticsManager.getSelectedCosmetic(category, killer.getOnlinePlayer()) == null){
                    MessageManager.get(game.getParticipants(), "chat.kill")
                            .replace("%dead%", gamePlayer.getOnlinePlayer().getName())
                            .replace("%killer%", killer.getOnlinePlayer().getName())
                            .replace("%player_color%", "" + (useTeams ? gamePlayer.getPlayerData().getTeam().getChatColor() : "§a"))
                            .replace("%killer_color%", "" + (useTeams ? killer.getPlayerData().getTeam().getChatColor() : "§a"))
                            .addAndTranslate(killCounter.get(killer) > 1 && (blockedxKill.get(count) == null || !blockedxKill.get(count).contains(killer)) ? getxKillMessageKey(killCounter.get(killer)) : "")
                            .send();
                //}else{
                    //Cosmetic cosmetic = cosmeticsManager.getSelectedCosmetic(category, killer.getOnlinePlayer());
                    /*KillMessage message = Cosmetics.getKillMessage(cosmetic);
                    if (message != null) {
                        MessageManager.get(game.getParticipants(), message.getMessageKey(KillMessage.DeadCause.RANGED))
                                .replace("%dead%", gamePlayer.getOnlinePlayer().getName())
                                .replace("%killer%", killer.getOnlinePlayer().getName())
                                .replace("%player_color%", "" + (useTeams ? gamePlayer.getPlayerData().getTeam().getChatColor() : "§a"))
                                .replace("%killer_color%", "" + (useTeams ? killer.getPlayerData().getTeam().getChatColor() : "§a"))
                                .addAndTranslate(killCounter.get(killer) > 1 && (blockedxKill.get(count) == null || !blockedxKill.get(count).contains(killer)) ? getxKillMessageKey(killCounter.get(killer)) : "")
                                .send();
                    }*/
                //}
            }else if (e.getDmgCause().equals(EntityDamageEvent.DamageCause.VOID)){
                //if (cosmeticsManager.getSelectedCosmetic(category, killer.getOnlinePlayer()) == null){
                    MessageManager.get(game.getParticipants(), "chat.kill")
                            .replace("%dead%", gamePlayer.getOnlinePlayer().getName())
                            .replace("%killer%", killer.getOnlinePlayer().getName())
                            .replace("%player_color%", "" + (useTeams ? gamePlayer.getPlayerData().getTeam().getChatColor() : "§a"))
                            .replace("%killer_color%", "" + (useTeams ? killer.getPlayerData().getTeam().getChatColor() : "§a"))
                            .addAndTranslate(killCounter.get(killer) > 1 && (blockedxKill.get(count) == null || !blockedxKill.get(count).contains(killer)) ? getxKillMessageKey(killCounter.get(killer)) : "")
                            .send();
                //}else{
                    //Cosmetic cosmetic = cosmeticsManager.getSelectedCosmetic(category, killer.getOnlinePlayer());
                    /*KillMessage message = Cosmetics.getKillMessage(cosmetic);
                    if (message != null) {
                        MessageManager.get(game.getParticipants(), message.getMessageKey(KillMessage.DeadCause.VOID))
                                .replace("%dead%", gamePlayer.getOnlinePlayer().getName())
                                .replace("%killer%", killer.getOnlinePlayer().getName())
                                .replace("%player_color%", "" + (useTeams ? gamePlayer.getPlayerData().getTeam().getChatColor() : "§a"))
                                .replace("%killer_color%", "" + (useTeams ? killer.getPlayerData().getTeam().getChatColor() : "§a"))
                                .addAndTranslate(killCounter.get(killer) > 1 && (blockedxKill.get(count) == null || !blockedxKill.get(count).contains(killer)) ? getxKillMessageKey(killCounter.get(killer)) : "")
                                .send();
                    }*/
                //}
            }else if (e.getDmgCause().equals(EntityDamageEvent.DamageCause.FALL)){
                //if (cosmeticsManager.getSelectedCosmetic(category, killer.getOnlinePlayer()) == null){
                    MessageManager.get(game.getParticipants(), "chat.kill")
                            .replace("%dead%", gamePlayer.getOnlinePlayer().getName())
                            .replace("%killer%", killer.getOnlinePlayer().getName())
                            .replace("%player_color%", "" + (useTeams ? gamePlayer.getPlayerData().getTeam().getChatColor() : "§a"))
                            .replace("%killer_color%", "" + (useTeams ? killer.getPlayerData().getTeam().getChatColor() : "§a"))
                            .addAndTranslate(killCounter.get(killer) > 1 && (blockedxKill.get(count) == null || !blockedxKill.get(count).contains(killer)) ? getxKillMessageKey(killCounter.get(killer)) : "")
                            .send();
                //}else{
                //Cosmetic cosmetic = cosmeticsManager.getSelectedCosmetic(category, killer.getOnlinePlayer());
                    /*KillMessage message = Cosmetics.getKillMessage(cosmetic);
                    if (message != null) {
                        MessageManager.get(game.getParticipants(), message.getMessageKey(KillMessage.DeadCause.FALL))
                                .replace("%dead%", gamePlayer.getOnlinePlayer().getName())
                                .replace("%killer%", killer.getOnlinePlayer().getName())
                                .replace("%player_color%", "" + (useTeams ? gamePlayer.getPlayerData().getTeam().getChatColor() : "§a"))
                                .replace("%killer_color%", "" + (useTeams ? killer.getPlayerData().getTeam().getChatColor() : "§a"))
                                .addAndTranslate(killCounter.get(killer) > 1 && (blockedxKill.get(count) == null || !blockedxKill.get(count).contains(killer)) ? getxKillMessageKey(killCounter.get(killer)) : "")
                                .send();
                    }*/
                //}
            }else{
                //if (cosmeticsManager.getSelectedCosmetic(category, killer.getOnlinePlayer()) == null){
                    MessageManager.get(game.getParticipants(), "chat.kill")
                            .replace("%dead%", gamePlayer.getOnlinePlayer().getName())
                            .replace("%killer%", killer.getOnlinePlayer().getName())
                            .replace("%player_color%", "" + (useTeams ? gamePlayer.getPlayerData().getTeam().getChatColor() : "§a"))
                            .replace("%killer_color%", "" + (useTeams ? killer.getPlayerData().getTeam().getChatColor() : "§a"))
                            .addAndTranslate(killCounter.get(killer) > 1 && (blockedxKill.get(count) == null || !blockedxKill.get(count).contains(killer)) ? getxKillMessageKey(killCounter.get(killer)) : "")
                            .send();
                //}else{
                //Cosmetic cosmetic = cosmeticsManager.getSelectedCosmetic(category, killer.getOnlinePlayer());
                    /*KillMessage message = Cosmetics.getKillMessage(cosmetic);
                    if (message != null) {
                        MessageManager.get(game.getParticipants(), message.getMessageKey(KillMessage.DeadCause.MELEE))
                                .replace("%dead%", gamePlayer.getOnlinePlayer().getName())
                                .replace("%killer%", killer.getOnlinePlayer().getName())
                                .replace("%player_color%", "" + (useTeams ? gamePlayer.getPlayerData().getTeam().getChatColor() : "§a"))
                                .replace("%killer_color%", "" + (useTeams ? killer.getPlayerData().getTeam().getChatColor() : "§a"))
                                .addAndTranslate(killCounter.get(killer) > 1 && (blockedxKill.get(count) == null || !blockedxKill.get(count).contains(killer)) ? getxKillMessageKey(killCounter.get(killer)) : "")
                                .send();
                    }*/
                //}
            }


            for (GamePlayer gp : e.getGame().getPlayers()) {
                if (e.getAssists() != null && !e.getAssists().isEmpty() && e.getAssists().contains(gp)) {
                    MessageManager.get(gp, "chat.assisted")
                            .replace("%dead%", gamePlayer.getOnlinePlayer().getName())
                            .send();
                    //gp.getScoreByName("Assist").increaseScore();
                }
            }


            spawnKillProtection.merge(killer, 1, Integer::sum);

            new BukkitRunnable(){
                @Override
                public void run() {
                    spawnKillProtection.put(killer, spawnKillProtection.get(killer) - 1);
                }
            }.runTaskLater(GameAPI.getInstance(), 12 * 20);




            if (e.isFirstGameKill()){
                e.getGame().getPlayers().forEach(gp -> gp.getOnlinePlayer().sendMessage(MessageManager.get(gp, "chat.first_blood").replace("%dead%", gamePlayer.getOnlinePlayer().getName()).replace("%killer%", killer.getOnlinePlayer().getName()).getTranslated()));
                //killer.getScoreByName("FirstBlood").increaseScore();
            }


            if (killCounter.get(killer) >= 6){
                //killer.getScoreByName("Kill").increaseScore(false);
                MessageManager.get(killer, "chat.kill_fast").send(); //TODO: dořešit
            }//else{
                //killer.getScoreByName("Kill").increaseScore();
            //}

            killer.getOnlinePlayer().playSound(killer.getOnlinePlayer().getLocation(), Sounds.LEVEL_UP.bukkitSound(), 20.0F, 20.0F);

            //e.getGamePlayer().getScoreByName("Death").increaseScore();
        }else{
            if (e.getDmgCause() == EntityDamageEvent.DamageCause.VOID){
                MessageManager.get(game.getParticipants(), "chat.void")
                        .replace("%dead%", gamePlayer.getOnlinePlayer().getName())
                        .replace("%player_color%", "" + (useTeams ? gamePlayer.getPlayerData().getTeam().getChatColor() : "§a"))
                        .send();
            }else if (e.getDmgCause() == EntityDamageEvent.DamageCause.FALL){
                MessageManager.get(game.getParticipants(), "chat.fall")
                        .replace("%dead%", gamePlayer.getOnlinePlayer().getName())
                        .replace("%player_color%", "" + (useTeams ? gamePlayer.getPlayerData().getTeam().getChatColor() : "§a"))
                        .send();
            }else{
                MessageManager.get(game.getParticipants(), "chat.died")
                        .replace("%dead%", gamePlayer.getOnlinePlayer().getName())
                        .replace("%player_color%", "" + (useTeams ? gamePlayer.getPlayerData().getTeam().getChatColor() : "§a"))
                        .send();
            }


            //e.getGamePlayer().getScoreByName("Death").increaseScore();
        }



        if (!gamePlayer.isRespawning()) {
            gamePlayer.setSpectator(true);

            new BukkitRunnable(){
                @Override
                public void run() {
                    List<PlayerScore> stats = PlayerManager.getScoresByPlayer(gamePlayer).stream().filter(s -> s.getScore() != 0 && s.getStat() != null).toList();

                    if (game.getState().equals(GameState.INGAME) && !stats.isEmpty()) {
                        TextComponent message = new TextComponent(MessageManager.get(gamePlayer, "chat.view_statistic").getTranslated());

                        ComponentBuilder b = new ComponentBuilder("");
                        b.append(MessageManager.get(gamePlayer, "chat.view_statistic.survived_for")
                                .replace("%time%", StringUtils.getDurationString(game.getRunningMainTask().getStartCounter() - game.getRunningMainTask().getCounter()))
                                .getTranslated());
                        b.append("\n");
                        b.append(MessageManager.get(gamePlayer, "chat.view_statistic.outlived")
                                .replace("%outlived%", "" + ((int) game.getMetadata().get("players_at_start") - (game.getPlayers().size() + 1)))
                                .getTranslated());
                        b.append("\n");
                        for (PlayerScore score : stats) {
                            if (score.getScore() == 0) {
                                continue;
                            }
                            b.append("\n").append("§7" + score.getDisplayName(true) + ": §a" + score.getScore());
                        }

                        message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, b.create()));
                        gamePlayer.getOnlinePlayer().spigot().sendMessage(message);
                    }
                }
            }.runTaskLater(GameAPI.getInstance(), 5L);
        }


        Minigame.EndGame endGame = GameAPI.getInstance().getMinigame().getEndGameFunction();
        if (endGame != null) {
            if (endGame.validator().test(game)) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        endGame.response().accept(game);
                    }
                }.runTaskLater(GameAPI.getInstance(), 1L);
            }
        }
    }
}
