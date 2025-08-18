package cz.johnslovakia.gameapi.listeners;

import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.events.GamePlayerDeathEvent;
import cz.johnslovakia.gameapi.events.PlayerDamageByPlayerEvent;
import cz.johnslovakia.gameapi.events.TeamEliminationEvent;
import cz.johnslovakia.gameapi.game.Game;
import cz.johnslovakia.gameapi.game.GameState;
import cz.johnslovakia.gameapi.game.cosmetics.CosmeticsCategory;
import cz.johnslovakia.gameapi.game.cosmetics.CosmeticsManager;
import cz.johnslovakia.gameapi.game.team.GameTeam;
import cz.johnslovakia.gameapi.game.team.TeamManager;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerScore;
import cz.johnslovakia.gameapi.utils.CharRepo;
import cz.johnslovakia.gameapi.utils.StringUtils;
import cz.johnslovakia.gameapi.utils.chatHead.ChatHeadAPI;
import lombok.Getter;

import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.List;

@Getter
public class PlayerDeathListener implements Listener {

    //private static final Map<GamePlayer, Integer> spawnKillProtection = new HashMap<>();
    private static final Map<UUID, Integer> killCounter = new HashMap<>();
    private static final Map<Integer, Set<UUID>> blockedxKill = new HashMap<>();


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
            background.setFont("jsplugins:actionbar_offset");

            Component head = ChatHeadAPI.getInstance().getHead(dead.getOfflinePlayer(), true, ChatHeadAPI.defaultSource);

            TextComponent banner = new TextComponent("\uDAFF\uDFFB \uDAFF\uDF9C\uDAFF\uDFD8Ẍ");
            banner.setColor(ChatColor.WHITE);
            banner.addExtra("\uDAFF\uDFCE\uDAFF\uDFF2" + nameSpaces);
            banner.addExtra("§f" + ChatHeadAPI.getInstance().getHeadAsString(dead.getOfflinePlayer())/*LegacyComponentSerializer.legacySection().serialize(head)*/);
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

    /*public void eliminationBanner(GamePlayer killer, GamePlayer dead){
        Player killerPlayer = killer.getOnlinePlayer();

        try {
            int nameWidth = CharRepo.getPixelWidth(dead.getOnlinePlayer().getName()) + /*head pixels + space* (8 + 5);
            int needForName = (nameWidth >= 64 ? nameWidth - 64 : 64 - nameWidth) / 2;
            String nameSpaces = StringUtils.calculateNegativeSpaces(needForName);

            Component background = Component.text("\uDAFF\uDFFB \uDAFF\uDFDDẍ");
            background.color(TextColor.fromHexString("#4e5c24"));
            background.font(Key.key("gameapi:actionbar_offset"));

            Component banner = Component.text("\uDAFF\uDFFB \uDAFF\uDF9C\uDAFF\uDFD8Ẍ"
                    + "\uDAFF\uDFCE\uDAFF\uDFF2" + nameSpaces
                    + "\uDB00\uDC02§f" + dead.getOnlinePlayer().getName().toUpperCase());
            banner.append(ChatHeadAPI.getInstance().getHead(dead.getOfflinePlayer(), true, ChatHeadAPI.defaultSource));
            banner.color(NamedTextColor.WHITE);

            background.append(banner);

            killerPlayer.sendActionBar(background);
        }catch (Exception e){
            e.printStackTrace();
        }

        // 󏾜󏿘 vycentruje banner s background logicky
        // pak vemu 64 mezery (délka textu elimination!) což zacentruje jméno na konec "elimination!",
        // pak vemu délku jména a vypočítam kolik zbývá do 64, vydělím /2 zaokrouhlím a přidám výslednou negativní mezeru před jméno, výjde:
        // /title @a actionbar {"text":"\u1E8D","color":"#4e5c24","font":"minecraft:actionbar_offset","extra":[{"text":"󏾜󏿘\u1E8C󏿗󏿵HUNZEK_","color":"white"}]}
        // /title @a actionbar {"text":"󏿝\u1E8D","color":"#4e5c24","font":"minecraft:actionbar_offset","extra":[{"text":"󏾜󏿘\u1E8C󏿎󏿲󏿸DAMIANHRAJEEEE","color":"white"}]}
    }*/

    @EventHandler
    public void onGamePlayerDeath(GamePlayerDeathEvent e) {
        GamePlayer gamePlayer = e.getGamePlayer();
        Game game = e.getGame();
        boolean useTeams = game.getSettings().useTeams();

        CosmeticsManager cosmeticsManager = Minigame.getInstance().getCosmeticsManager();
        CosmeticsCategory killMessagesCategory = cosmeticsManager.getCategoryByName("Kill messages");

        if (e.getKiller() != null && e.getKiller() != gamePlayer) {
            GamePlayer killer = e.getKiller();
            UUID killerId = killer.getOfflinePlayer().getUniqueId();

            killCounter.merge(killerId, 1, Integer::sum);
            int currentCount = killCounter.get(killerId);

            new BukkitRunnable() {
                @Override
                public void run() {
                    killCounter.computeIfPresent(killerId, (k, v) -> v > 1 ? v - 1 : null);
                }
            }.runTaskLater(Minigame.getInstance().getPlugin(), 6 * 20L);

            if (currentCount > 1) {
                blockedxKill.computeIfAbsent(currentCount, k -> new HashSet<>()).add(killerId);

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        Set<UUID> set = blockedxKill.get(currentCount);
                        if (set != null) {
                            set.remove(killerId);
                            if (set.isEmpty()) blockedxKill.remove(currentCount);
                        }
                    }
                }.runTaskLater(Minigame.getInstance().getPlugin(), 25 * 20L);
            }

            String killKey = "";
            Set<UUID> blockedSet = blockedxKill.get(currentCount);
            if (currentCount > 1 && (blockedSet == null || !blockedSet.contains(killerId))) {
                killKey = getxKillMessageKey(currentCount);
            }

            if (cosmeticsManager.getSelectedCosmetic(killer, killMessagesCategory) == null || killer.getPlayerData().getKillMessage() == null) {
                MessageManager.get(game.getParticipants(), "chat.kill")
                        .replace("%dead%", gamePlayer.getOnlinePlayer().getName())
                        .replace("%killer%", killer.getOnlinePlayer().getName())
                        .replace("%player_color%", useTeams ? "" + gamePlayer.getTeam().getChatColor() : "§a")
                        .replace("%killer_color%", useTeams ? "" + killer.getTeam().getChatColor() : "§a")
                        .addAndTranslate(killKey)
                        .send();
            } else {
                MessageManager.get(game.getParticipants(), killer.getPlayerData().getKillMessage().getMessageKey(e.getDmgCause()))
                        .replace("%dead%", gamePlayer.getOnlinePlayer().getName())
                        .replace("%killer%", killer.getOnlinePlayer().getName())
                        .replace("%player_color%", useTeams ? "" + gamePlayer.getTeam().getChatColor() : "§a")
                        .replace("%killer_color%", useTeams ? "" + killer.getTeam().getChatColor() : "§a")
                        .addAndTranslate(killKey)
                        .send();
            }

            if (e.getAssists() != null && !e.getAssists().isEmpty()) {
                for (GamePlayer gp : e.getAssists()) {
                    MessageManager.get(gp, "chat.assisted")
                            .replace("%player%", gamePlayer.getOnlinePlayer().getName())
                            .send();
                }
            }

            if (e.isFirstGameKill()) {
                game.getPlayers().forEach(gp -> gp.getOnlinePlayer().sendMessage(
                        MessageManager.get(gp, "chat.first_blood")
                                .replace("%dead%", gamePlayer.getOnlinePlayer().getName())
                                .replace("%killer%", killer.getOnlinePlayer().getName())
                                .getTranslated()
                ));
            }

            /*spawnKillProtection.merge(killer, 1, Integer::sum);
            new BukkitRunnable() {
                @Override
                public void run() {
                    spawnKillProtection.computeIfPresent(killer, (k, v) -> v > 1 ? v - 1 : null);
                }
            }.runTaskLater(Minigame.getInstance().getPlugin(), 12 * 20L);*/

            /*if (currentCount >= 6) {
                MessageManager.get(killer, "chat.kill_fast").send();
            }*/

            killer.getOnlinePlayer().playSound(killer.getOnlinePlayer().getLocation(), "jsplugins:good", 1F, 1F);
            eliminationBanner(killer, gamePlayer);
        } else {
            String key = switch (e.getDmgCause()) {
                case VOID -> "chat.void";
                case FALL -> "chat.fall";
                default -> "chat.died";
            };

            MessageManager.get(game.getParticipants(), key)
                    .replace("%dead%", gamePlayer.getOnlinePlayer().getName())
                    .replace("%player_color%", useTeams ? "" + gamePlayer.getTeam().getChatColor() : "§a")
                    .send();
        }

        if (!gamePlayer.isRespawning()) {
            gamePlayer.setSpectator(true);

            new BukkitRunnable() {
                @Override
                public void run() {
                    List<PlayerScore> stats = gamePlayer.getPlayerData().getScores().stream()
                            .filter(s -> s.getScore() != 0 && s.getStat() != null)
                            .toList();

                    if (game.getState() == GameState.INGAME && !stats.isEmpty()) {
                        Component message = MessageManager.get(gamePlayer, "chat.view_statistic").getTranslated();


                        Component hoverText = Component.empty();
                        hoverText = hoverText.append(MessageManager.get(gamePlayer, "chat.view_statistic.survived_for")
                                .replace("%time%", StringUtils.getDurationString(
                                        game.getRunningMainTask().getStartCounter() - game.getRunningMainTask().getCounter()))
                                .getTranslated())
                                .appendNewline();

                        hoverText = hoverText.append(MessageManager.get(gamePlayer, "chat.view_statistic.outlived")
                                .replace("%outlived%", String.valueOf((int) game.getMetadata().get("players_at_start") - (game.getPlayers().size() + 1)))
                                .getTranslated());

                        for (PlayerScore score : stats) {
                            hoverText = hoverText
                                    .appendNewline()
                                    .append(Component.text("§7" + score.getPluralName() + ": §a" + score.getScore()));
                        }


                        gamePlayer.getOnlinePlayer().sendMessage(message.hoverEvent(hoverText));
                    }
                }
            }.runTaskLater(Minigame.getInstance().getPlugin(), 5L);
        }

        if (useTeams) {
            GameTeam gameTeam = gamePlayer.getTeam();
            if (gameTeam.getAliveMembers().isEmpty()) {
                MessageManager.get(game.getParticipants(), "chat.team_eliminated")
                        .replace("%team%", Component.text(gameTeam.getName()).color(gameTeam.getTeamColor().getTextColor()))
                        .send();

                TeamManager teamManager = game.getTeamManager();
                Map<Integer, GameTeam> placements = teamManager.getTeamPlacement();

                int placement = teamManager.getTeams().stream().filter(team -> !team.getMembers().isEmpty()).toList().size() - placements.size();
                placements.put(placement, gameTeam);

                TeamEliminationEvent ev = new TeamEliminationEvent(gameTeam, placement);
                Bukkit.getPluginManager().callEvent(ev);
            }
        }

        Minigame.EndGame endGame = Minigame.getInstance().getEndGameFunction();
        if (endGame != null && endGame.validator().test(game)) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    endGame.response().accept(game);
                }
            }.runTaskLater(Minigame.getInstance().getPlugin(), 2L);
        }
    }
}
