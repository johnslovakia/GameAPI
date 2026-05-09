package cz.johnslovakia.gameapi.listeners;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.WinCondition;
import cz.johnslovakia.gameapi.events.PlayerEliminationEvent;
import cz.johnslovakia.gameapi.modules.game.GameInstance;
import cz.johnslovakia.gameapi.events.GamePlayerDeathEvent;
import cz.johnslovakia.gameapi.events.TeamEliminationEvent;
import cz.johnslovakia.gameapi.modules.game.GameState;
import cz.johnslovakia.gameapi.modules.game.Placement;
import cz.johnslovakia.gameapi.modules.game.map.GameMap;
import cz.johnslovakia.gameapi.modules.game.session.PlayerGameSession;
import cz.johnslovakia.gameapi.modules.game.team.GameTeam;
import cz.johnslovakia.gameapi.modules.game.team.TeamModule;
import cz.johnslovakia.gameapi.modules.ModuleManager;
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

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Getter
public class PlayerDeathListener implements Listener {

    private static final Cache<UUID, Integer> killCounter = CacheBuilder.newBuilder()
            .expireAfterWrite(6, TimeUnit.SECONDS)
            .build();

    private static final Cache<String, Boolean> blockedXKill = CacheBuilder.newBuilder()
            .expireAfterWrite(25, TimeUnit.SECONDS)
            .build();

    public String getxKillMessageKey(int count) {
        return switch (count) {
            case 2 -> "multikill.double_kill";
            case 3 -> "multikill.tripple_kill";
            case 4 -> "multikill.quadra_kill";
            case 5 -> "multikill.penta_kill";
            default -> "";
        };
    }

    public void eliminationBanner(GamePlayer killer, GamePlayer dead) {
        Player killerPlayer = killer.getOnlinePlayer();
        try {
            int nameWidth = CharRepo.getPixelWidth(dead.getOnlinePlayer().getName()) + (8 + 5);
            int needForName = (nameWidth >= 64 ? nameWidth - 64 : 64 - nameWidth) / 2;
            String nameSpaces = StringUtils.calculateNegativeSpaces(needForName);

            Component background = Component.text("\uDAFF\uDFFB \uDAFF\uDFDDẍ")
                    .color(TextColor.fromHexString("#4e5c24"))
                    .font(Key.key("gameapi:actionbar_offset"));

            Component banner = Component.text("\uDAFF\uDFFB \uDAFF\uDF9C\uDAFF\uDFD8Ẍ"
                    + "\uDAFF\uDFCE\uDAFF\uDFF2" + nameSpaces
                    + "\uDB00\uDC02§f" + dead.getOnlinePlayer().getName().toUpperCase())
                    .append(ChatHeadAPI.getInstance().getHeadAsComponent(
                            dead.getOfflinePlayer().getUniqueId(), true, ChatHeadAPI.defaultSource))
                    .color(NamedTextColor.WHITE);

            killerPlayer.sendActionBar(background.append(banner));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onGamePlayerDeath(GamePlayerDeathEvent e) {
        GamePlayer gamePlayer = e.getGamePlayer();
        GameInstance game = e.getGame();
        Player player = gamePlayer.getOnlinePlayer();
        PlayerGameSession session = gamePlayer.getGameSession();
        MessageModule messageModule = ModuleManager.getModule(MessageModule.class);
        boolean useTeams = game.getSettings().isUseTeams();

        if (e.getKiller() != null && e.getKiller() != gamePlayer) {
            GamePlayer killer = e.getKiller();
            UUID killerId = killer.getOfflinePlayer().getUniqueId();

            int currentCount = killCounter.getIfPresent(killerId) != null
                    ? killCounter.getIfPresent(killerId) + 1 : 1;
            killCounter.put(killerId, currentCount);

            String multiKillKey = "";
            if (currentCount > 1 && blockedXKill.getIfPresent(killerId + ":" + currentCount) == null) {
                multiKillKey = getxKillMessageKey(currentCount);
                blockedXKill.put(killerId + ":" + currentCount, true);
            }

            messageModule.getMessage(game.getParticipants(),
                    ModuleManager.getModule(KillMessageModule.class).getForPlayer(killer).getTranslationKey(e.getDmgType()))
                    .replace("%dead%", gamePlayer.getName())
                    .replace("%killer%", killer.getName())
                    .replace("%dead_color%", useTeams ? "" + gamePlayer.getGameSession().getTeam().getChatColor() : "§a")
                    .replace("%player_color%", useTeams ? "" + gamePlayer.getGameSession().getTeam().getChatColor() : "§a")
                    .replace("%killer_color%", useTeams ? "" + killer.getGameSession().getTeam().getChatColor() : "§a")
                    .addAndTranslate(multiKillKey)
                    .addAndTranslate(e.isFinalKill() && game.getSettings().isEnabledRespawning() ? "word.elimination" : "")
                    .send();

            if (e.getAssists() != null && !e.getAssists().isEmpty()) {
                for (GamePlayer gp : e.getAssists()) {
                    messageModule.getMessage(gp, "chat.assisted")
                            .replace("%player%", gamePlayer.getOnlinePlayer().getName())
                            .send();
                }
            }

            if (e.isFirstGameKill()) {
                messageModule.getMessage(game.getParticipants(), "chat.first_blood")
                        .replace("%dead%", gamePlayer.getOnlinePlayer().getName())
                        .replace("%killer%", killer.getOnlinePlayer().getName())
                        .replace("%killer_color%", useTeams ? "" + killer.getGameSession().getTeam().getChatColor() : "§a")
                        .send();
            }
        } else {
            String key;
            if (e.getDmgType() == DamageType.FALL) {
                key = "chat.fall";
            } else if (e.getDmgType() == DamageType.OUT_OF_WORLD) {
                key = "chat.void";
            } else {
                key = "chat.died";
            }
            messageModule.getMessage(game.getParticipants(), key)
                    .replace("%dead%", gamePlayer.getOnlinePlayer().getName())
                    .replace("%player_color%", useTeams && gamePlayer.getGameSession().getTeam() != null
                            ? "" + gamePlayer.getGameSession().getTeam().getChatColor() : "§a")
                    .replace("%dead_color%", useTeams && gamePlayer.getGameSession().getTeam() != null
                            ? "" + gamePlayer.getGameSession().getTeam().getChatColor() : "§a")
                    .send();
        }

        boolean willBeSpectator = !gamePlayer.isRespawning()
                || (session.getState().equals(GamePlayerState.DISCONNECTED)
                        && !game.getSettings().isEnabledReJoin());

        if (willBeSpectator) {
            int placement = game.getPlayers().size();
            game.getPlacements().add(new Placement<>(gamePlayer, placement));

            if (gamePlayer.isOnline()) {
                messageModule.getMessage(gamePlayer, "title.spectator").send();
                gamePlayer.setSpectator(true);
                player.teleport(GameUtils.getNonRespawnLocation(game));
            }

            PlayerEliminationEvent ev = new PlayerEliminationEvent(gamePlayer, e.getKiller(), e.getAssists(), placement);
            Bukkit.getPluginManager().callEvent(ev);

            if (gamePlayer.isOnline()) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!game.getState().equals(GameState.INGAME) || !gamePlayer.isOnline()) return;

                        player.sendMessage(Component.empty());

                        Component message = messageModule.getMessage(gamePlayer, "chat.view_summary").toComponent();

                        Component hoverText = messageModule.getMessage(gamePlayer, "chat.view_statistic.survived_for")
                                .replace("%time%", StringUtils.getDurationString(
                                        game.getRunningMainTask().getStartCounter() - game.getRunningMainTask().getCounter()))
                                .toComponent().appendNewline();
                        hoverText = hoverText.append(messageModule.getMessage(gamePlayer, "chat.view_statistic.outlived")
                                .replace("%outlived%", String.valueOf(
                                        (int) game.getMetadata().get("players_at_start") - (game.getPlayers().size() + 1)))
                                .toComponent());

                        Map<Score, Integer> stats = session.getScores().entrySet().stream()
                                .filter(entry -> entry.getKey().getLinkedStat() != null)
                                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                        if (!stats.isEmpty()) {
                            hoverText = hoverText.appendNewline();
                            List<Component> lines = stats.entrySet().stream()
                                    .map(entry -> Component.text()
                                            .append(Component.text(entry.getKey().getDisplayName(gamePlayer) + ": ").color(NamedTextColor.GRAY))
                                            .append(Component.text(entry.getValue()).color(NamedTextColor.GREEN))
                                            .asComponent())
                                    .toList();
                            hoverText = hoverText.appendNewline().append(Component.join(JoinConfiguration.newlines(), lines));
                        }

                        if (session.hasEarnedSomething()) {
                            hoverText = hoverText.appendNewline().appendNewline()
                                    .append(messageModule.getMessage(gamePlayer, "chat.view_statistic.rewards_earned").toComponent());
                            for (Map.Entry<Resource, Integer> entry : session.getEarnedRewards().entrySet()) {
                                Resource resource = entry.getKey();
                                String imgChar = resource.getImgChar() != null ? resource.getImgChar() + " " : "";
                                hoverText = hoverText.appendNewline()
                                        .append(Component.text(" §7• " + imgChar + resource.getColor()
                                                + entry.getValue() + " §7" + resource.getDisplayName()));
                            }
                        }

                        player.sendMessage(message.hoverEvent(hoverText));
                        player.sendMessage(Component.empty());
                    }
                }.runTaskLater(Minigame.getInstance().getPlugin(), 30L);
            }

        } else {
            handleIngameRespawn(gamePlayer, player, game);
        }

        if (game.hasModule(TeamModule.class)) {
            GameTeam gameTeam = session.getTeam();
            if (gameTeam != null) {
                if (gameTeam.getAliveMembers().isEmpty()
                        && (gameTeam.isDead() || !game.getSettings().isEnabledJoiningAfterStart())) {

                    messageModule.getMessage(game.getParticipants(), "chat.team_eliminated")
                            .replace("%team%", Component.text(gameTeam.getName())
                                    .color(gameTeam.getTeamColor().getTextColor()))
                            .send();

                    TeamModule teamModule = game.getModule(TeamModule.class);
                    int totalActiveTeams = (int) teamModule.getTeams().values().stream()
                            .filter(team -> !team.getAliveMembers().isEmpty()).count();

                    int placement = totalActiveTeams - game.getPlacements().size();
                    game.getPlacements().add(new Placement<>(gameTeam, placement));

                    TeamEliminationEvent ev = new TeamEliminationEvent(gameTeam, e.getKiller(), e.getAssists(), placement);
                    Bukkit.getPluginManager().callEvent(ev);
                }
            }
        }

        WinCondition winCondition = Minigame.getInstance().getWinCondition();
        if (winCondition != null && winCondition.shouldEnd(game)) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!game.getState().equals(GameState.INGAME)) return;
                    winCondition.resolve(game);
                }
            }.runTaskLater(Minigame.getInstance().getPlugin(), 2L);
        }
    }

    private void handleIngameRespawn(GamePlayer gamePlayer, Player player, GameInstance game) {
        GameMap playingMap = game.getCurrentMap();

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;
                player.setFireTicks(0);
                player.setArrowsInBody(0);
            }
        }.runTaskLater(Minigame.getInstance().getPlugin(), 5L);

        int cooldown = game.getSettings().getRespawnCooldown();

        if (cooldown == -1) {
            player.teleport(getActiveRespawnLocation(gamePlayer, playingMap, game));
        } else {
            player.teleport(GameUtils.getNonRespawnLocation(game));

            new BukkitRunnable() {
                int second = cooldown;

                @Override
                public void run() {
                    if (!player.isOnline() || gamePlayer.getGame() == null) {
                        this.cancel();
                        return;
                    }
                    if (second == 0) {
                        player.teleport(getActiveRespawnLocation(gamePlayer, playingMap, game));
                        this.cancel();
                    } else {
                        ModuleManager.getModule(MessageModule.class)
                                .getMessage(gamePlayer, "title.respawn")
                                .replace("%time%", "" + second)
                                .send();
                    }
                    second--;
                }
            }.runTaskTimer(Minigame.getInstance().getPlugin(), 0L, 20L);
        }
    }

    private Location getActiveRespawnLocation(GamePlayer gamePlayer, GameMap playingMap, GameInstance game) {
        Location location = playingMap.getPlayerToLocation(gamePlayer);
        if (location != null) return location;

        if (gamePlayer.getGameSession() != null && gamePlayer.getGameSession().getTeam() != null) {
            location = gamePlayer.getGameSession().getTeam().getSpawn();
            if (location != null) return location;
        }

        return GameUtils.getNonRespawnLocation(game);
    }
}