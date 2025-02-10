package cz.johnslovakia.gameapi.listeners;

import com.cryptomorin.xseries.particles.ParticleDisplay;
import com.cryptomorin.xseries.particles.XParticle;
import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.events.GamePlayerDeathEvent;
import cz.johnslovakia.gameapi.game.Game;
import cz.johnslovakia.gameapi.game.GameState;
import cz.johnslovakia.gameapi.game.cosmetics.Cosmetic;
import cz.johnslovakia.gameapi.game.cosmetics.CosmeticsCategory;
import cz.johnslovakia.gameapi.game.cosmetics.CosmeticsManager;
import cz.johnslovakia.gameapi.users.KillMessage;
import cz.johnslovakia.gameapi.game.team.GameTeam;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerManager;
import cz.johnslovakia.gameapi.users.PlayerScore;
import cz.johnslovakia.gameapi.utils.CharRepo;
import cz.johnslovakia.gameapi.utils.Sounds;
import cz.johnslovakia.gameapi.utils.StringUtils;
import cz.johnslovakia.gameapi.utils.chatHead.ChatHeadAPI;
import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.awt.*;
import java.util.*;
import java.util.List;

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

    public void eliminationBanner(GamePlayer killer, GamePlayer dead){
        Player killerPlayer = killer.getOnlinePlayer();
        try {
            int nameWidth = CharRepo.getPixelWidth(dead.getOnlinePlayer().getName()) + /*head pixels + space*/ (8 + 5);
            int needForName = (nameWidth >= 64 ? nameWidth - 64 : 64 - nameWidth) / 2;
            String nameSpaces = StringUtils.calculateNegativeSpaces(needForName);

            TextComponent background = new TextComponent("\uDAFF\uDFFB \uDAFF\uDFDDẍ");
            background.setColor(ChatColor.of("#4e5c24"));
            background.setFont("gameapi:actionbar_offset");

            BaseComponent[] head = ChatHeadAPI.getInstance().getHead(dead.getOfflinePlayer(), true, ChatHeadAPI.defaultSource);

            TextComponent banner = new TextComponent("\uDAFF\uDFFB \uDAFF\uDF9C\uDAFF\uDFD8Ẍ");
            banner.setColor(ChatColor.WHITE);
            banner.addExtra("\uDAFF\uDFCE\uDAFF\uDFF2" + nameSpaces);
            Arrays.stream(head).toList().forEach(banner::addExtra);
            banner.addExtra("\uDB00\uDC02§f" + dead.getOnlinePlayer().getName().toUpperCase());

            background.addExtra(banner);
            killerPlayer.spigot().sendMessage(ChatMessageType.ACTION_BAR, background);
        }catch (Exception e){
            e.printStackTrace();
        }

        //󏾜󏿘 vycentruje banner s background logicky
        //pak vemu 64 mezery (délka textu elimination!) což zacentruje jméno na konec "elimination!",
        // pak vemu délku jména a vypočítam kolik zbývá do 64, vydělím /2 zaokrouhlím a přidám výslednou negativní mezeru před jméno, výjde:
        // /title @a actionbar {"text":"\u1E8D","color":"#4e5c24","font":"minecraft:actionbar_offset","extra":[{"text":"󏾜󏿘\u1E8C󏿗󏿵HUNZEK_","color":"white"}]}
        ///title @a actionbar {"text":"󏿝\u1E8D","color":"#4e5c24","font":"minecraft:actionbar_offset","extra":[{"text":"󏾜󏿘\u1E8C󏿎󏿲󏿸DAMIANHRAJEEEE","color":"white"}]}
    }

    @EventHandler
    public void onGamePlayerDeath(GamePlayerDeathEvent e) {
        GamePlayer gamePlayer = e.getGamePlayer();
        Game game = e.getGame();

        boolean useTeams = game.getSettings().useTeams();


        //TODO: Nějaký nastavení?
        CosmeticsManager cosmeticsManager = GameAPI.getInstance().getCosmeticsManager();
        CosmeticsCategory killMessagesCategory = cosmeticsManager.getCategoryByName("Kill messages");
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

            if (cosmeticsManager.getSelectedCosmetic(killer, killMessagesCategory) == null || killer.getPlayerData().getKillMessage() == null){
                MessageManager.get(game.getParticipants(), "chat.kill")
                        .replace("%dead%", gamePlayer.getOnlinePlayer().getName())
                        .replace("%killer%", killer.getOnlinePlayer().getName())
                        .replace("%player_color%", "" + (useTeams ? gamePlayer.getPlayerData().getTeam().getChatColor() : "§a"))
                        .replace("%killer_color%", "" + (useTeams ? killer.getPlayerData().getTeam().getChatColor() : "§a"))
                        .addAndTranslate(killCounter.get(killer) > 1 && (blockedxKill.get(count) == null || !blockedxKill.get(count).contains(killer)) ? getxKillMessageKey(killCounter.get(killer)) : "")
                        .send();
            }else{
                MessageManager.get(game.getParticipants(), killer.getPlayerData().getKillMessage().getMessageKey(e.getDmgCause()))
                        .replace("%dead%", gamePlayer.getOnlinePlayer().getName())
                        .replace("%killer%", killer.getOnlinePlayer().getName())
                        .replace("%player_color%", "" + (useTeams ? gamePlayer.getPlayerData().getTeam().getChatColor() : "§a"))
                        .replace("%killer_color%", "" + (useTeams ? killer.getPlayerData().getTeam().getChatColor() : "§a"))
                        .addAndTranslate(killCounter.get(killer) > 1 && (blockedxKill.get(count) == null || !blockedxKill.get(count).contains(killer)) ? getxKillMessageKey(killCounter.get(killer)) : "")
                        .send();
            }


            for (GamePlayer gp : e.getGame().getPlayers()) {
                if (e.getAssists() != null && !e.getAssists().isEmpty() && e.getAssists().contains(gp)) {
                    MessageManager.get(gp, "chat.assisted")
                            .replace("%dead%", gamePlayer.getOnlinePlayer().getName())
                            .send();
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
                e.getGame().getPlayers().forEach(gp -> gp.getOnlinePlayer().sendMessage(MessageManager.get(gp, "chat.first_blood")
                        .replace("%dead%", gamePlayer.getOnlinePlayer().getName())
                        .replace("%killer%", killer.getOnlinePlayer().getName())
                        .getTranslated()));
            }


            if (killCounter.get(killer) >= 6){
                MessageManager.get(killer, "chat.kill_fast").send(); //TODO: dořešit
            }

            killer.getOnlinePlayer().playSound(killer.getOnlinePlayer().getLocation(), "custom:good", 1F, 1F);
            eliminationBanner(killer, gamePlayer);
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
        }



        if (useTeams){
            GameTeam gameTeam = gamePlayer.getPlayerData().getTeam();
            if (gameTeam.getAliveMembers().isEmpty()) {
                MessageManager.get(game.getParticipants(), "chat.team_eliminated")
                        .replace("%team%", gameTeam.getChatColor() + gameTeam.getName())
                        .send();
            }
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
                            b.append("\n").append("§7" + score.getPluralName() + ": §a" + score.getScore());
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
                }.runTaskLater(GameAPI.getInstance(), 2L);
            }
        }
    }
}
