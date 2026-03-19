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

    private static final long AFK_DELAY_TICKS = 150 * 20L;
    private static final int KICK_COUNTDOWN_SECONDS = 15;
    private static final long RESCHEDULE_DEBOUNCE_MS = 1_000L;

    private final Map<GamePlayer, BukkitTask> afkTasks = new ConcurrentHashMap<>();
    private final Map<GamePlayer, BukkitTask> countdownTasks = new ConcurrentHashMap<>();
    private final Map<GamePlayer, Boolean> isAfk = new ConcurrentHashMap<>();
    private final Map<GamePlayer, Long> lastReschedule = new ConcurrentHashMap<>();


    private boolean isExempt(Player player) {
        return player.isOp() || player.hasPermission("*");
    }

    private boolean hasPlayerMoved(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return false;

        return from.distanceSquared(to) > 0.0025
                || Math.abs(from.getYaw() - to.getYaw())   > 0.5f
                || Math.abs(from.getPitch() - to.getPitch()) > 0.5f;
    }

    private void cancelAfkTask(GamePlayer gamePlayer) {
        BukkitTask afk = afkTasks.remove(gamePlayer);
        if (afk != null) afk.cancel();

        BukkitTask countdown = countdownTasks.remove(gamePlayer);
        if (countdown != null) countdown.cancel();
    }

    private void scheduleAfkTask(Player player, GamePlayer gamePlayer) {
        long now  = System.currentTimeMillis();
        Long last = lastReschedule.get(gamePlayer);

        if (last != null && afkTasks.containsKey(gamePlayer) && (now - last) < RESCHEDULE_DEBOUNCE_MS) {
            return;
        }

        cancelAfkTask(gamePlayer);
        lastReschedule.put(gamePlayer, now);

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                isAfk.put(gamePlayer, true);
                kickPlayerIfAfk(player, gamePlayer);
            }
        }.runTaskLater(Minigame.getInstance().getPlugin(), AFK_DELAY_TICKS);

        afkTasks.put(gamePlayer, task);
    }


    private void cleanupPlayer(GamePlayer gamePlayer) {
        cancelAfkTask(gamePlayer);
        isAfk.remove(gamePlayer);
        lastReschedule.remove(gamePlayer);
    }


    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (isExempt(player)) return;

        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);
        if (gamePlayer == null || gamePlayer.getPlayerData() == null) return;

        GameInstance game = gamePlayer.getGame();
        if (game == null) return;
        if (!game.getSettings().isEnabledAntiAFKSystem()) return;
        if (game.getState() != GameState.INGAME) return;

        if (!hasPlayerMoved(event)) return;

        if (isAfk.getOrDefault(gamePlayer, false)) {
            isAfk.put(gamePlayer, false);
        }

        scheduleAfkTask(player, gamePlayer);
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

            scheduleAfkTask(player, gamePlayer);
        }
    }

    @EventHandler
    public void onGameQuit(GameQuitEvent event) {
        cleanupPlayer(event.getGamePlayer());
    }


    private void kickPlayerIfAfk(Player player, GamePlayer gamePlayer) {
        if (!player.isOnline()) return;
        if (isExempt(player)) return;
        if (gamePlayer.getPlayerData() == null || gamePlayer.getGame() == null) return;
        if (!isAfk.getOrDefault(gamePlayer, false)) return;

        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_BREAK, 1.0f, 1.0f);

        BukkitTask task = new BukkitRunnable() {
            int countdown = KICK_COUNTDOWN_SECONDS;

            @Override
            public void run() {
                if (!isAfk.getOrDefault(gamePlayer, false)
                        || PlayerManager.getGamePlayer(player) != gamePlayer) {
                    countdownTasks.remove(gamePlayer);
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

        countdownTasks.put(gamePlayer, task);
    }
}