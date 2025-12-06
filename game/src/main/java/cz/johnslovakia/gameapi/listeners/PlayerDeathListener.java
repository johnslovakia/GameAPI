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
import cz.johnslovakia.gameapi.modules.game.session.PlayerGameSession;
import cz.johnslovakia.gameapi.modules.game.team.GameTeam;
import cz.johnslovakia.gameapi.modules.game.team.TeamModule;
import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.cosmetics.CosmeticsCategory;
import cz.johnslovakia.gameapi.modules.cosmetics.CosmeticsModule;
import cz.johnslovakia.gameapi.modules.killMessage.KillMessageModule;
import cz.johnslovakia.gameapi.modules.messages.MessageModule;
import cz.johnslovakia.gameapi.modules.scores.Score;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.utils.CharRepo;
import cz.johnslovakia.gameapi.utils.StringUtils;
import cz.johnslovakia.gameapi.utils.chatHead.ChatHeadAPI;
import lombok.Getter;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import org.bukkit.Bukkit;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
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
        Player player = gamePlayer.getOnlinePlayer();
        PlayerGameSession session = gamePlayer.getGameSession();
        GameInstance game = e.getGame();
        boolean useTeams = game.getSettings().isUseTeams();

        CosmeticsModule cosmeticsModule = ModuleManager.getModule(CosmeticsModule.class);
        CosmeticsCategory killMessagesCategory = cosmeticsModule.getCategoryByName("Kill messages");

        if (e.getKiller() != null && e.getKiller() != gamePlayer) {
            GamePlayer killer = e.getKiller();
            UUID killerId = killer.getOfflinePlayer().getUniqueId();

            int currentCount = killCounter.getIfPresent(killerId) != null ? killCounter.getIfPresent(killerId) + 1 : 1;
            killCounter.put(killerId, currentCount);

            if (currentCount > 1) {
                blockedXKill.put(killerId + ":" + currentCount, true);
            }

            String killKey = "";
            if (currentCount > 1 && blockedXKill.getIfPresent(killerId + ":" + currentCount) == null) {
                killKey = getxKillMessageKey(currentCount);
            }

            ModuleManager.getModule(MessageModule.class).get(game.getParticipants(), ModuleManager.getModule(KillMessageModule.class).getById(cosmeticsModule.getPlayerSelectedCosmetic(gamePlayer, killMessagesCategory).getName()).getTranslationKey(e.getDmgType()))
                    .replace("%dead%", gamePlayer.getName())
                    .replace("%killer%", killer.getName())
                    .replace("%player_color%", useTeams ? "" + gamePlayer.getGameSession().getTeam().getChatColor() : "§a")
                    .replace("%killer_color%", useTeams ? "" + killer.getGameSession().getTeam().getChatColor() : "§a")
                    .addAndTranslate(killKey)
                    .send();

            if (e.getAssists() != null && !e.getAssists().isEmpty()) {
                for (GamePlayer gp : e.getAssists()) {
                    ModuleManager.getModule(MessageModule.class).get(gp, "chat.assisted")
                            .replace("%player%", gamePlayer.getOnlinePlayer().getName())
                            .send();
                }
            }

            if (e.isFirstGameKill()) {
                ModuleManager.getModule(MessageModule.class).get(game.getParticipants(), "chat.first_blood")
                        .replace("%dead%", gamePlayer.getOnlinePlayer().getName())
                        .replace("%killer%", killer.getOnlinePlayer().getName())
                        .getTranslated();
            }

            /*spawnKillProtection.merge(killer, 1, Integer::sum);
            new BukkitRunnable() {
                @Override
                public void run() {
                    spawnKillProtection.computeIfPresent(killer, (k, v) -> v > 1 ? v - 1 : null);
                }
            }.runTaskLater(Minigame.getInstance().getPlugin(), 12 * 20L);*/

            /*if (currentCount >= 6) {
                ModuleManager.getModule(MessageModule.class).get(killer, "chat.kill_fast").send();
            }*/

            killer.getOnlinePlayer().playSound(killer.getOnlinePlayer().getLocation(), "jsplugins:good", 1F, 1F);
            eliminationBanner(killer, gamePlayer);
        } else {
            String key;
            if (e.getDmgType() == DamageType.FALL) {
                key = "chat.fall";
            } else if (e.getDmgType() == DamageType.OUT_OF_WORLD) {
                key = "chat.void";
            } else {
                key = "chat.died";
            }

            ModuleManager.getModule(MessageModule.class).get(game.getParticipants(), key)
                    .replace("%dead%", gamePlayer.getOnlinePlayer().getName())
                    .replace("%player_color%", useTeams ? "" + gamePlayer.getGameSession().getTeam().getChatColor() : "§a")
                    .send();
        }

        if (!gamePlayer.isRespawning()) {
            gamePlayer.setSpectator(true);

            int totalPlayers = game.getPlayers().size() + game.getPlacements().size();
            int placement = totalPlayers - game.getPlacements().size();
            game.getPlacements().add(new Placement<>(gamePlayer, placement));

            PlayerEliminationEvent ev = new PlayerEliminationEvent(gamePlayer, placement);
            Bukkit.getPluginManager().callEvent(ev);

            new BukkitRunnable() {
                @Override
                public void run() {
                    Map<Score, Integer> stats = session.getScores().entrySet()
                            .stream().filter(entry -> entry.getKey().getLinkedStat() != null)
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                    if (game.getState() == GameState.INGAME && !stats.isEmpty()) {
                        Component message = ModuleManager.getModule(MessageModule.class)
                                .get(gamePlayer, "chat.view_statistic")
                                .getTranslated();

                        Component hoverText = ModuleManager.getModule(MessageModule.class).get(gamePlayer, "chat.view_statistic.survived_for")
                                    .replace("%time%", StringUtils.getDurationString(
                                        game.getRunningMainTask().getStartCounter() - game.getRunningMainTask().getCounter()))
                                    .getTranslated()
                                .appendNewline();

                        hoverText = hoverText.append(ModuleManager.getModule(MessageModule.class).get(gamePlayer, "chat.view_statistic.outlived")
                                .replace("%outlived%", String.valueOf((int) game.getMetadata().get("players_at_start") - (game.getPlayers().size() + 1)))
                                .getTranslated());

                        List<Component> lines = stats.entrySet().stream()
                                .map(entry -> Component.text()
                                        .append(Component.text(entry.getKey().getDisplayName(gamePlayer) + ": ").color(NamedTextColor.GRAY))
                                        .append(Component.text(entry.getValue()).color(NamedTextColor.GREEN))
                                        .asComponent()
                                )
                                .toList();
                        Component statsComponent = Component.join(JoinConfiguration.newlines(), lines);

                        hoverText = hoverText.appendNewline().append(statsComponent);

                        player.sendMessage(message.hoverEvent(hoverText));
                        player.sendMessage(Component.empty());
                    }
                }
            }.runTaskLater(Minigame.getInstance().getPlugin(), 20L);
        }

        if (game.hasModule(TeamModule.class)) {
            GameTeam gameTeam = session.getTeam();
            if (gameTeam.getAliveMembers().isEmpty()) {
                ModuleManager.getModule(MessageModule.class).get(game.getParticipants(), "chat.team_eliminated")
                        .replace("%team%", Component.text(gameTeam.getName()).color(gameTeam.getTeamColor().getTextColor()))
                        .send();

                TeamModule teamModule = game.getModule(TeamModule.class);
                int totalActiveTeams = (int) teamModule.getTeams().values().stream()
                        .filter(team -> !team.getMembers().isEmpty())
                        .count();

                int placement = totalActiveTeams - game.getPlacements().size();
                game.getPlacements().add(new Placement<>(gameTeam, placement));

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
