package cz.johnslovakia.gameapi.listeners;

import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.modules.game.GameInstance;
import cz.johnslovakia.gameapi.modules.game.GameState;
import cz.johnslovakia.gameapi.events.GameQuitEvent;
import cz.johnslovakia.gameapi.events.GameStartEvent;
import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.messages.MessageModule;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerManager;
import cz.johnslovakia.gameapi.utils.GameUtils;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AntiAFK implements Listener {

    private static final long AFK_DELAY_INGAME = 150 * 20L;
    private static final int KICK_COUNTDOWN_SECONDS = 15;

    private final Map<GamePlayer, BukkitTask> afkTasks = new ConcurrentHashMap<>();
    private final Map<GamePlayer, Boolean> isAfk = new ConcurrentHashMap<>();
    private final Map<GamePlayer, Boolean> afkByJoin = new ConcurrentHashMap<>();

    private boolean isExempt(Player player) {
        return player.hasPermission("*") || player.isOp();
    }

    private boolean hasPlayerMoved(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();

        return from.distanceSquared(to) > 0.0025
                || Math.abs(from.getYaw()   - to.getYaw())   > 0.5f
                || Math.abs(from.getPitch() - to.getPitch()) > 0.5f;
    }

    private void cancelAfkTask(GamePlayer gamePlayer) {
        BukkitTask task = afkTasks.remove(gamePlayer);
        if (task != null) task.cancel();
    }

    private void scheduleAfkTask(Player player, GamePlayer gamePlayer) {
        cancelAfkTask(gamePlayer);

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (PlayerManager.getGamePlayer(player) != gamePlayer) return;

                isAfk.put(gamePlayer, true);
                kickPlayerIfAfk(player, gamePlayer);
            }
        }.runTaskLaterAsynchronously(Minigame.getInstance().getPlugin(), AFK_DELAY_INGAME);

        afkTasks.put(gamePlayer, task);
    }

    private void cleanupPlayer(GamePlayer gamePlayer) {
        cancelAfkTask(gamePlayer);
        isAfk.remove(gamePlayer);
        afkByJoin.remove(gamePlayer);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (isExempt(player)) return;

        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);
        if (gamePlayer.getPlayerData() == null) return;

        GameInstance game = gamePlayer.getGame();
        if (game == null) return;
        if (!game.getSettings().isEnabledAntiAFKSystem()) return;
        if (game.getState() != GameState.INGAME) return;

        if (hasPlayerMoved(event)) {
            cancelAfkTask(gamePlayer);
            if (isAfk.getOrDefault(gamePlayer, false)) {
                afkByJoin.remove(gamePlayer);
                isAfk.put(gamePlayer, false);
            }
        } else {
            if (!afkTasks.containsKey(gamePlayer)) {
                scheduleAfkTask(player, gamePlayer);
            }
        }
    }

    @EventHandler
    public void onGameStart(GameStartEvent event) {
        for (GamePlayer gamePlayer : event.getGame().getParticipants()) {
            Player player = gamePlayer.getOnlinePlayer();
            if (player == null || isExempt(player)) continue;
            if (gamePlayer.getPlayerData() == null) continue;

            GameInstance game = gamePlayer.getGame();
            if (game == null) continue;
            if (!game.getSettings().isEnabledAntiAFKSystem()) continue;

            afkByJoin.put(gamePlayer, true);
            scheduleAfkTask(player, gamePlayer);
        }
    }

    @EventHandler
    public void onGameQuit(GameQuitEvent event) {
        cleanupPlayer(event.getGamePlayer());
    }

    private void kickPlayerIfAfk(Player player, GamePlayer gamePlayer) {
        if (isExempt(player)) return;
        if (gamePlayer.getPlayerData() == null || gamePlayer.getGame() == null) return;

        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_BREAK, 1.0f, 1.0f);

        new BukkitRunnable() {
            int countdown = KICK_COUNTDOWN_SECONDS;

            @Override
            public void run() {
                if (PlayerManager.getGamePlayer(player) != gamePlayer
                        || !isAfk.getOrDefault(gamePlayer, false)) {
                    this.cancel();
                    return;
                }

                if (countdown <= 0) {
                    if (player.isOnline() && !player.isDead()) {
                        GameUtils.sendToLobby(player);
                        cleanupPlayer(gamePlayer);
                    }
                    this.cancel();
                    return;
                }

                if (countdown == KICK_COUNTDOWN_SECONDS || countdown == 10 || countdown <= 5) {
                    ModuleManager.getModule(MessageModule.class)
                            .get(player, "chat.afk_kick_countdown")
                            .replace("%seconds%", String.valueOf(countdown))
                            .send();
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f);
                }

                countdown--;
            }
        }.runTaskTimer(Minigame.getInstance().getPlugin(), 0L, 20L);
    }
}