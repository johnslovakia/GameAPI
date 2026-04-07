package cz.johnslovakia.gameapi.listeners;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.events.PlayerEliminationEvent;
import cz.johnslovakia.gameapi.modules.game.GameInstance;
import cz.johnslovakia.gameapi.events.GamePlayerDeathEvent;
import cz.johnslovakia.gameapi.events.TeamEliminationEvent;
import cz.johnslovakia.gameapi.modules.game.GameState;
import cz.johnslovakia.gameapi.modules.game.Placement;
import cz.johnslovakia.gameapi.modules.game.lobby.LobbyModule;
import cz.johnslovakia.gameapi.modules.game.session.PlayerGameSession;
import cz.johnslovakia.gameapi.modules.game.team.GameTeam;
import cz.johnslovakia.gameapi.modules.game.team.TeamModule;
import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.cosmetics.CosmeticsCategory;
import cz.johnslovakia.gameapi.modules.cosmetics.CosmeticsModule;
import cz.johnslovakia.gameapi.modules.killMessage.KillMessageModule;
import cz.johnslovakia.gameapi.modules.messages.MessageModule;
import cz.johnslovakia.gameapi.modules.resources.Resource;
import cz.johnslovakia.gameapi.modules.scores.Score;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.GamePlayerState;
import cz.johnslovakia.gameapi.users.PlayerManager;
import cz.johnslovakia.gameapi.utils.CharRepo;
import cz.johnslovakia.gameapi.utils.GameUtils;
import cz.johnslovakia.gameapi.utils.StringUtils;
import cz.johnslovakia.gameapi.utils.chatHead.ChatHeadAPI;
import lombok.Getter;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import org.bukkit.Bukkit;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Getter
public class PlayerDeathListener implements Listener {



    //private static final Map<GamePlayer, Integer> spawnKillProtection = new HashMap<>();
    private static final Cache<UUID, Integer> killCounter = CacheBuilder.newBuilder()
            .expireAfterWrite(6, TimeUnit.SECONDS)
            .build();

    private static final Cache<String, Boolean> blockedXKill = CacheBuilder.newBuilder()
            .expireAfterWrite(25, TimeUnit.SECONDS)
            .build();


    public String getxKillMessageKey(int count){
        return switch (count){
            case 2 -> "multikill.double_kill";
            case 3 -> "multikill.tripple_kill";
            case 4 -> "multikill.quadra_kill";
            case 5 -> "multikill.penta_kill";
            default -> "";
        };
    }

    /*public void eliminationBanner(GamePlayer killer, GamePlayer dead){
        Player killerPlayer = killer.getOnlinePlayer();
        try {
            int nameWidth = CharRepo.getPixelWidth(dead.getOnlinePlayer().getName()) + /*head pixels + space* (8 + 5);
            int needForName = (nameWidth >= 64 ? nameWidth - 64 : 64 - nameWidth) / 2;
            String nameSpaces = StringUtils.calculateNegativeSpaces(needForName);

            TextComponent background = new TextComponent("\uDAFF\uDFFB \uDAFF\uDFDDẍ");
            background.setColor(ChatColor.of("#4e5c24"));
            background.setFont("jsplugins:actionbar_offset");

            Component head = ChatHeadAPI.getInstance().getHeadAsComponent(dead.getOfflinePlayer().getUniqueId(), true, ChatHeadAPI.defaultSource);

            TextComponent banner = new TextComponent("\uDAFF\uDFFB \uDAFF\uDF9C\uDAFF\uDFD8Ẍ");
            banner.setColor(ChatColor.WHITE);
            banner.addExtra("\uDAFF\uDFCE\uDAFF\uDFF2" + nameSpaces);
            banner.addExtra("§f" + ChatHeadAPI.getInstance().getHeadAsString(dead.getOfflinePlayer())/*LegacyComponentSerializer.legacySection().serialize(head)*);
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
    }*/

    public void eliminationBanner(GamePlayer killer, GamePlayer dead){
        Player killerPlayer = killer.getOnlinePlayer();

        try {
            int nameWidth = CharRepo.getPixelWidth(dead.getOnlinePlayer().getName()) + /*head pixels + space*/ (8 + 5);
            int needForName = (nameWidth >= 64 ? nameWidth - 64 : 64 - nameWidth) / 2;
            String nameSpaces = StringUtils.calculateNegativeSpaces(needForName);

            Component background = Component.text("\uDAFF\uDFFB \uDAFF\uDFDDẍ");
            background = background.color(TextColor.fromHexString("#4e5c24"));
            background = background.font(Key.key("gameapi:actionbar_offset"));

            Component banner = Component.text("\uDAFF\uDFFB \uDAFF\uDF9C\uDAFF\uDFD8Ẍ"
                    + "\uDAFF\uDFCE\uDAFF\uDFF2" + nameSpaces
                    + "\uDB00\uDC02§f" + dead.getOnlinePlayer().getName().toUpperCase());
            banner = banner.append(ChatHeadAPI.getInstance().getHeadAsComponent(dead.getOfflinePlayer().getUniqueId(), true, ChatHeadAPI.defaultSource));
            banner = banner.color(NamedTextColor.WHITE);

            background = background.append(banner);

            killerPlayer.sendActionBar(background);
        }catch (Exception e){
            e.printStackTrace();
        }

        // 󏾜󏿘 vycentruje banner s background logicky
        // pak vemu 64 mezery (délka textu elimination!) což zacentruje jméno na konec "elimination!",
        // pak vemu délku jména a vypočítam kolik zbývá do 64, vydělím /2 zaokrouhlím a přidám výslednou negativní mezeru před jméno, výjde:
        // /title @a actionbar {"text":"\u1E8D","color":"#4e5c24","font":"minecraft:actionbar_offset","extra":[{"text":"󏾜󏿘\u1E8C󏿗󏿵HUNZEK_","color":"white"}]}
        // /title @a actionbar {"text":"󏿝\u1E8D","color":"#4e5c24","font":"minecraft:actionbar_offset","extra":[{"text":"󏾜󏿘\u1E8C󏿎󏿲󏿸DAMIANHRAJEEEE","color":"white"}]}
    }

    @EventHandler (priority = EventPriority.LOW)
    public void onGamePlayerDeath(GamePlayerDeathEvent e) {
        GamePlayer gamePlayer = e.getGamePlayer();
        GameInstance game = e.getGame();
        Player player = gamePlayer.getOnlinePlayer();
        PlayerGameSession session = gamePlayer.getGameSession();
        MessageModule messageModule = ModuleManager.getModule(MessageModule.class);
        boolean useTeams = game.getSettings().isUseTeams();

        if (!game.getState().equals(GameState.INGAME)) {
            e.setCancelled(true);
            if (game.getState().equals(GameState.WAITING) || game.getState().equals(GameState.STARTING)) {
                LobbyModule lobbyModule = game.getModule(LobbyModule.class);
                if (lobbyModule != null && lobbyModule.getLobbyLocation() != null) {
                    player.teleport(lobbyModule.getLobbyLocation().getLocation());
                }
            } else{
                player.teleport(GameUtils.getNonRespawnLocation(game));
            }
            return;
        }

        if (e.getKiller() != null && e.getKiller() != gamePlayer) {
            GamePlayer killer = e.getKiller();
            UUID killerId = killer.getOfflinePlayer().getUniqueId();

            int currentCount = killCounter.getIfPresent(killerId) != null ? killCounter.getIfPresent(killerId) + 1 : 1;
            killCounter.put(killerId, currentCount);

            String multiKillKey = "";
            if (currentCount > 1 && blockedXKill.getIfPresent(killerId + ":" + currentCount) == null) {
                multiKillKey = getxKillMessageKey(currentCount);
                blockedXKill.put(killerId + ":" + currentCount, true);
            }

            messageModule.get(game.getParticipants(), ModuleManager.getModule(KillMessageModule.class).getForPlayer(killer).getTranslationKey(e.getDmgType()))
                    .replace("%dead%", gamePlayer.getName())
                    .replace("%killer%", killer.getName())
                    .replace("%dead_color%", useTeams ? "" + gamePlayer.getGameSession().getTeam().getChatColor() : "§a")
                    .replace("%player_color%", useTeams ? "" + gamePlayer.getGameSession().getTeam().getChatColor() : "§a")
                    .replace("%killer_color%", useTeams ? "" + killer.getGameSession().getTeam().getChatColor() : "§a")
                    .addAndTranslate(multiKillKey)
                    .send();

            if (e.getAssists() != null && !e.getAssists().isEmpty()) {
                for (GamePlayer gp : e.getAssists()) {
                    messageModule.get(gp, "chat.assisted")
                            .replace("%player%", gamePlayer.getOnlinePlayer().getName())
                            .send();
                }
            }

            if (e.isFirstGameKill()) {
                messageModule.get(game.getParticipants(), "chat.first_blood")
                        .replace("%dead%", gamePlayer.getOnlinePlayer().getName())
                        .replace("%killer%", killer.getOnlinePlayer().getName())
                        .replace("%killer_color%", useTeams ? "" + killer.getGameSession().getTeam().getChatColor() : "§a")
                        .send();
            }

            /*spawnKillProtection.merge(killer, 1, Integer::sum);
            new BukkitRunnable() {
                @Override
                public void run() {
                    spawnKillProtection.computeIfPresent(killer, (k, v) -> v > 1 ? v - 1 : null);
                }
            }.runTaskLater(Minigame.getInstance().getPlugin(), 12 * 20L);*/

            /*if (currentCount >= 6) {
                messageModule.get(killer, "chat.kill_fast").send();
            }*/

            //killer.getOnlinePlayer().playSound(killer.getOnlinePlayer().getLocation(), "jsplugins:good", 1F, 1F);
            //eliminationBanner(killer, gamePlayer);
        } else {
            String key;
            if (e.getDmgType() == DamageType.FALL) {
                key = "chat.fall";
            } else if (e.getDmgType() == DamageType.OUT_OF_WORLD) {
                key = "chat.void";
            } else {
                key = "chat.died";
            }

            messageModule.get(game.getParticipants(), key)
                    .replace("%dead%", gamePlayer.getOnlinePlayer().getName())
                    .replace("%player_color%", useTeams && gamePlayer.getGameSession().getTeam() != null ? "" + gamePlayer.getGameSession().getTeam().getChatColor() : "§a")
                    .replace("%dead_color%", useTeams && gamePlayer.getGameSession().getTeam() != null ? "" + gamePlayer.getGameSession().getTeam().getChatColor() : "§a")
                    .send();
        }

        if (!gamePlayer.isRespawning() || (gamePlayer.getGameSession().getState().equals(GamePlayerState.DISCONNECTED) && !game.getSettings().isEnabledReJoin())) {
            int placement = game.getPlayers().size();
            game.getPlacements().add(new Placement<>(gamePlayer, placement));

            if (gamePlayer.isOnline()){
                messageModule.get(gamePlayer, "title.spectator")
                        .send();
                gamePlayer.setSpectator(true);
            }

            PlayerEliminationEvent ev = new PlayerEliminationEvent(gamePlayer, e.getKiller(), e.getAssists(), placement);
            Bukkit.getPluginManager().callEvent(ev);


            if (gamePlayer.isOnline()) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!game.getState().equals(GameState.INGAME) || !gamePlayer.isOnline()) return;

                        player.sendMessage(Component.empty());

                        Component message = messageModule
                                .get(gamePlayer, "chat.view_summary")
                                .getTranslated();

                        Component hoverText = messageModule.get(gamePlayer, "chat.view_statistic.survived_for")
                                .replace("%time%", StringUtils.getDurationString(
                                        game.getRunningMainTask().getStartCounter() - game.getRunningMainTask().getCounter()))
                                .getTranslated()
                                .appendNewline();
                        hoverText = hoverText.append(messageModule.get(gamePlayer, "chat.view_statistic.outlived")
                                .replace("%outlived%", String.valueOf((int) game.getMetadata().get("players_at_start") - (game.getPlayers().size() + 1)))
                                .getTranslated());

                        Map<Score, Integer> stats = session.getScores().entrySet()
                                .stream().filter(entry -> entry.getKey().getLinkedStat() != null)
                                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                        if (!stats.isEmpty()) {
                            hoverText = hoverText.appendNewline();

                            List<Component> lines = stats.entrySet().stream()
                                    .map(entry -> Component.text()
                                            .append(Component.text(entry.getKey().getDisplayName(gamePlayer) + ": ").color(NamedTextColor.GRAY))
                                            .append(Component.text(entry.getValue()).color(NamedTextColor.GREEN))
                                            .asComponent()
                                    )
                                    .toList();
                            Component statsComponent = Component.join(JoinConfiguration.newlines(), lines);

                            hoverText = hoverText.appendNewline().append(statsComponent);
                        }

                        if (session.earnedSomething()) {
                            hoverText = hoverText.appendNewline().appendNewline()
                                    .append(messageModule.get(gamePlayer, "chat.view_statistic.rewards_earned").getTranslated());

                            for (Map.Entry<Resource, Integer> entry : session.getTotalEarned().entrySet()) {
                                Resource resource = entry.getKey();
                                String imgChar = resource.getImgChar() != null ? resource.getImgChar() + " " : "";

                                hoverText = hoverText.appendNewline()
                                        .append(Component.text(
                                                " §7• " + imgChar + resource.getColor()
                                                        + entry.getValue() + " §7" + resource.getDisplayName()
                                        ));
                            }
                        }

                        player.sendMessage(message.hoverEvent(hoverText));
                        player.sendMessage(Component.empty());
                    }
                }.runTaskLater(Minigame.getInstance().getPlugin(), 30L);
            }
        }

        if (game.hasModule(TeamModule.class)) {
            GameTeam gameTeam = session.getTeam();
            if (gameTeam != null) {
                if (gameTeam.getAliveMembers().isEmpty()) {
                    messageModule.get(game.getParticipants(), "chat.team_eliminated")
                            .replace("%team%", Component.text(gameTeam.getName()).color(gameTeam.getTeamColor().getTextColor()))
                            .send();

                    TeamModule teamModule = game.getModule(TeamModule.class);
                    int totalActiveTeams = (int) teamModule.getTeams().values().stream()
                            .filter(team -> !team.getAliveMembers().isEmpty())
                            .count();

                    int placement = totalActiveTeams - game.getPlacements().size();
                    game.getPlacements().add(new Placement<>(gameTeam, placement));

                    TeamEliminationEvent ev = new TeamEliminationEvent(gameTeam, e.getKiller(), e.getAssists(), placement);
                    Bukkit.getPluginManager().callEvent(ev);
                }
            }
        }

        Minigame.EndGame endGame = Minigame.getInstance().getEndGameFunction();
        if (endGame != null && endGame.validator().test(game)) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!game.getState().equals(GameState.INGAME)) return;
                    endGame.response().accept(game);
                }
            }.runTaskLater(Minigame.getInstance().getPlugin(), 2L);
        }
    }
}