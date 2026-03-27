package cz.johnslovakia.gameapi.modules.game.runtime;

import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.modules.Module;
import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.game.GameInstance;
import cz.johnslovakia.gameapi.modules.game.GameService;
import cz.johnslovakia.gameapi.modules.game.GameState;
import cz.johnslovakia.gameapi.modules.messages.MessageModule;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.utils.GameUtils;
import cz.johnslovakia.gameapi.utils.Logger;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Getter
@Setter
public class RestartScheduler implements Module {

    /**
     * Hard deadline (ticks) after scheduling. Only applies when the scheduler
     * is told to enforce it (hard-threshold or critical situations).
     * For soft/admin restarts this is ignored — the server waits for all games.
     */
    private long hardDeadlineTicks = 20L * 60 * 10; // 10 minutes

    /** Drain watchdog interval (ticks). */
    private long drainCheckIntervalTicks = 20L * 10; // 10 s

    /** Intervals (in seconds remaining) at which players get countdown messages. */
    private final int[] countdownAlerts = {300, 180, 120, 60, 30, 15, 10, 5, 3, 2, 1};


    private final AtomicBoolean restartPending = new AtomicBoolean(false);
    private final AtomicBoolean restartInProgress = new AtomicBoolean(false);
    private boolean hardDeadlineEnabled = false;

    private RestartReason reason;
    private String reasonDetail;
    private long scheduledAtTick = -1;
    private BukkitTask drainTask;

    private final Set<Integer> sentAlerts = new HashSet<>();


    public enum RestartReason {
        /** Triggered by ServerHealthMonitor due to RAM/TPS issues */
        PERFORMANCE_DEGRADATION("Performance degradation"),
        /** Triggered by ServerHealthMonitor when RAM/TPS is critically bad */
        CRITICAL_RESOURCES("Critical resource usage"),
        /** Triggered manually by an admin */
        ADMIN_SCHEDULED("Scheduled by admin"),
        OTHER("Other");

        @Getter
        private final String displayName;

        RestartReason(String displayName) {
            this.displayName = displayName;
        }
    }


    @Override
    public void initialize() {
    }

    @Override
    public void terminate() {
        if (drainTask != null) {
            drainTask.cancel();
            drainTask = null;
        }
    }

    /**
     * Schedule a graceful restart. This is the single entry point used by both
     * {@link ServerHealthMonitor} and admin commands.
     *
     * @param reason why the restart is happening
     * @param hardDeadline if {@code true}, the server WILL restart after {@link #hardDeadlineTicks} even if games are running
     */
    public void scheduleRestart(RestartReason reason, boolean hardDeadline) {
        scheduleRestart(reason, "", hardDeadline);
    }

    /**
     * Schedule a graceful restart. This is the single entry point used by both
     * {@link ServerHealthMonitor} and admin commands.
     *
     * @param reason why the restart is happening
     * @param detail human-readable detail string (shown to admins)
     * @param hardDeadline if {@code true}, the server WILL restart after {@link #hardDeadlineTicks} even if games are running
     */
    public void scheduleRestart(RestartReason reason, String detail, boolean hardDeadline) {
        if (!restartPending.compareAndSet(false, true)) {
            Logger.log("[RestartScheduler] Restart already pending, ignoring duplicate schedule.", Logger.LogType.WARNING);

            if (hardDeadline && !this.hardDeadlineEnabled) {
                this.hardDeadlineEnabled = true;
                Logger.log("[RestartScheduler] Upgraded to hard-deadline mode.", Logger.LogType.WARNING);
                notifyAdmins("§c[!] Restart upgraded to HARD DEADLINE mode. Games will be interrupted in " + (hardDeadlineTicks / 20) + "s if needed.");
            }
            return;
        }

        this.reason = reason;
        this.reasonDetail = detail != null ? detail : "";
        this.hardDeadlineEnabled = hardDeadline;
        this.scheduledAtTick = Bukkit.getCurrentTick();
        this.sentAlerts.clear();

        Logger.log("[RestartScheduler] Restart scheduled — reason: " + reason.getDisplayName() + " | detail: " + reasonDetail + "| hard deadline: " + hardDeadline, Logger.LogType.WARNING);
        notifyAdmins(buildAdminScheduleMessage());

        GameService gameService = ModuleManager.getModule(GameService.class);
        if (gameService == null) {
            Logger.log("[RestartScheduler] No GameService found. Forcing restart.", Logger.LogType.ERROR);
            executeRestart("§cServer is restarting.");
            return;
        }

        if (reason == RestartReason.CRITICAL_RESOURCES) {
            evacuateAllArenas(gameService);
            Bukkit.getScheduler().runTaskLater(Minigame.getInstance().getPlugin(), () -> executeRestart("§c§lServer restarting — critical resource usage!"), 80L);
            return;
        }

        evacuateIdleArenas(gameService);
        startDrainWatchdog(gameService);
    }

    public boolean cancelRestart() {
        if (restartInProgress.get()) return false;
        if (!restartPending.compareAndSet(true, false)) return false;

        if (drainTask != null) {
            drainTask.cancel();
            drainTask = null;
        }

        reason = null;
        reasonDetail = null;
        hardDeadlineEnabled = false;
        scheduledAtTick = -1;
        sentAlerts.clear();

        Logger.log("[RestartScheduler] Restart cancelled by admin.", Logger.LogType.INFO);
        notifyPlayers("§cPlanned server restart has been cancelled.");
        return true;
    }

    public boolean isRestartPending() {
        return restartPending.get();
    }

    public boolean isRestartInProgress() {
        return restartInProgress.get();
    }

    private void startDrainWatchdog(GameService gameService) {
        drainTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (restartInProgress.get()) {
                    cancel();
                    return;
                }
                tickDrain(gameService);
            }
        }.runTaskTimer(Minigame.getInstance().getPlugin(), drainCheckIntervalTicks, drainCheckIntervalTicks);
    }

    private void tickDrain(GameService gameService) {
        long elapsed = Bukkit.getCurrentTick() - scheduledAtTick;
        long remainingTicks = hardDeadlineEnabled ? (hardDeadlineTicks - elapsed) : -1;
        int remainingSec = hardDeadlineEnabled ? (int) (remainingTicks / 20) : -1;

        int ingameArenas = 0;
        int waitingWithPlayers = 0;
        int totalPlayers = 0;

        for (GameInstance game : gameService.getGames().values()) {
            int players = game.getPlayers().size();
            totalPlayers += players;

            if (game.getState() == GameState.INGAME) {
                ingameArenas++;
            } else if ((game.getState() == GameState.WAITING || game.getState() == GameState.STARTING) && players > 0) {
                waitingWithPlayers++;
            }
        }

        int online = Bukkit.getOnlinePlayers().size();
        String deadlineInfo = hardDeadlineEnabled
                ? "hard deadline in " + formatTime(remainingSec)
                : "no hard deadline (waiting for all games)";

        Logger.log(String.format("[RestartScheduler] Drain — INGAME: %d, WAITING(with players): %d, arena players: %d, online: %d | %s", ingameArenas, waitingWithPlayers, totalPlayers, online, deadlineInfo), Logger.LogType.INFO);


        if (hardDeadlineEnabled && remainingSec > 0) {
            sendCountdownAlerts(remainingSec);
        }

        if (ingameArenas == 0 && waitingWithPlayers == 0) {
            Logger.log("[RestartScheduler] All arenas drained. Restarting now.", Logger.LogType.INFO);
            notifyAdmins("§e[RestartScheduler] All arenas empty. Restarting server.");
            executeRestart("§eServer is restarting.");
            return;
        }

        if (ingameArenas == 0 && waitingWithPlayers > 0) {
            Logger.log("[RestartScheduler] Only waiting arenas left. Evacuating and restarting.", Logger.LogType.INFO);
            notifyAdmins("§e[RestartScheduler] Only waiting arenas remain (" + waitingWithPlayers + "). Evacuating and restarting.");

            evacuateIdleArenas(gameService);
            Bukkit.getScheduler().runTaskLater(Minigame.getInstance().getPlugin(), () ->
                    executeRestart("§eServer is restarting."), 80L);
            return;
        }

        if (hardDeadlineEnabled && elapsed >= hardDeadlineTicks) {
            Logger.log("[RestartScheduler] Hard deadline reached with " + ingameArenas + " INGAME arenas still running.", Logger.LogType.WARNING);
            notifyAdmins("§c[!] Hard deadline reached. Evacuating ALL arenas and restarting.");

            evacuateAllArenas(gameService);
            Bukkit.getScheduler().runTaskLater(Minigame.getInstance().getPlugin(), () ->
                    executeRestart("§c§lServer is restarting."), 80L);
            return;
        }

        evacuateIdleArenas(gameService);

        if (!hardDeadlineEnabled && elapsed % (20L * 60) < drainCheckIntervalTicks) {
            notifyAdmins("§e[RestartScheduler] Waiting for " + ingameArenas +
                         " running game(s) to finish. No deadline — games will finish naturally.");
        }
    }

    private void evacuateIdleArenas(GameService gameService) {
        for (GameInstance game : gameService.getGames().values()) {
            if (game.getState() != GameState.WAITING && game.getState() != GameState.STARTING) continue;
            if (game.getPlayers().isEmpty()) continue;

            Logger.log("[RestartScheduler] Evacuating idle arena " + game.getID() +
                       " (" + game.getPlayers().size() + " players).", Logger.LogType.INFO);

            for (Player player : new ArrayList<>(getOnlinePlayers(game))) {
                if (getReason().equals(RestartReason.ADMIN_SCHEDULED)) {
                    player.sendMessage("§c⚠ §cThe server will restart shortly. Sending you to lobby.");
                    Bukkit.getScheduler().runTaskLater(Minigame.getInstance().getPlugin(), task -> {
                        GameUtils.sendToLobby(player, false);
                    }, 20L);
                }else {
                    player.sendMessage("§c⚠ §cThe server will restart shortly. Sending you to another server.");
                    Bukkit.getScheduler().runTaskLater(Minigame.getInstance().getPlugin(), task -> {
                        gameService.newArena(player, true, true);
                    }, 20L);
                }
            }
        }
    }

    private void evacuateAllArenas(GameService gameService) {
        for (GameInstance game : gameService.getGames().values()) {
            if (game.getPlayers().isEmpty()) continue;

            Logger.log("[RestartScheduler] Force-evacuating arena " + game.getID() +
                       " (state=" + game.getState() + ", " + game.getPlayers().size() + " players).",
                       Logger.LogType.WARNING);

            for (Player player : new ArrayList<>(getOnlinePlayers(game))) {
                if (getReason().equals(RestartReason.ADMIN_SCHEDULED)) {
                    player.sendMessage("§c⚠ §cThe server is restarting. Sending you to lobby.");
                    Bukkit.getScheduler().runTaskLater(Minigame.getInstance().getPlugin(), task -> {
                        GameUtils.sendToLobby(player, false);
                    }, 20L);
                }else{
                    player.sendMessage("§c⚠ §cThe server is restarting. Sending you to another server.");
                    Bukkit.getScheduler().runTaskLater(Minigame.getInstance().getPlugin(), task -> {
                        gameService.newArena(player, true, true);
                    }, 20L);
                }
            }
        }
    }

    private List<Player> getOnlinePlayers(GameInstance game) {
        List<Player> result = new ArrayList<>();
        for (Object gp : game.getPlayers()) {
            if (gp instanceof Player p) {
                result.add(p);
            } else if (gp instanceof GamePlayer gamePlayer) {
                Player p = gamePlayer.getOnlinePlayer();
                if (p != null && p.isOnline()) {
                    result.add(p);
                }
            }
        }
        return result;
    }

    private void notifyAdmins(String message) {
        String permission = Minigame.getInstance().getName().toLowerCase() + ".admin";
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.isOp() || player.hasPermission(permission)) {
                player.sendMessage(message);
            }
        }

        Logger.log("[RestartScheduler] ADMIN: " + message, Logger.LogType.INFO);
    }

    private void notifyPlayers(String message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(message);
        }
    }

    private void sendCountdownAlerts(int remainingSec) {
        for (int threshold : countdownAlerts) {
            if (remainingSec <= threshold && remainingSec > threshold - (drainCheckIntervalTicks / 20) && !sentAlerts.contains(threshold)) {
                sentAlerts.add(threshold);

                String timeStr = formatTime(threshold);
                notifyPlayers("§c⚠ §cThe server will restart in #df1c1c" + timeStr + "§c!");
                notifyAdmins("§c[RestartScheduler] " + timeStr + " until hard deadline. " + "Reason: " + reason.getDisplayName() + (reasonDetail.isEmpty() ? "" : " (" + reasonDetail + ")"));
                break;
            }
        }
    }


    private String buildAdminScheduleMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n§c§l  ⚠ SERVER RESTART SCHEDULED\n\n");
        sb.append("§7  Reason: §f").append(reason.getDisplayName()).append("\n");
        if (!reasonDetail.isEmpty()) {
            sb.append("§7  Detail: §f").append(reasonDetail).append("\n");
        }
        if (hardDeadlineEnabled) {
            sb.append("§7  Hard deadline: §c").append(formatTime((int) (hardDeadlineTicks / 20))).append("\n");
            sb.append("§7  Mode: §cGames may be interrupted after deadline.\n");
        } else {
            sb.append("§7  Mode: §aGraceful — all running games will finish naturally.\n");
        }

        ServerHealthMonitor monitor = ModuleManager.getModule(ServerHealthMonitor.class);
        if (monitor != null) {
            ServerHealthMonitor.HealthSnapshot snap = monitor.takeSnapshot();
            sb.append("§7  RAM: §f").append(String.format("%.1f%%", snap.ramPercent()))
              .append(" (").append(snap.usedMemoryMB()).append("MB/").append(snap.maxMemoryMB()).append("MB)\n");
            sb.append("§7  TPS: §f").append(String.format("%.1f / %.1f / %.1f", snap.tps1m(), snap.tps5m(), snap.tps15m())).append("\n");
        }

        GameService gs = ModuleManager.getModule(GameService.class);
        if (gs != null) {
            long ingame = gs.getGames().values().stream().filter(g -> g.getState() == GameState.INGAME).count();
            long waiting = gs.getGames().values().stream()
                    .filter(g -> g.getState() == GameState.WAITING || g.getState() == GameState.STARTING).count();
            sb.append("§7  Arenas: §f").append(ingame).append(" in-game, ").append(waiting).append(" waiting\n");
        }

        sb.append("\n");
        return sb.toString();
    }

    private void executeRestart(String broadcastMessage) {
        if (!restartInProgress.compareAndSet(false, true)) return;
        terminate();

        Logger.log("[RestartScheduler] Executing server restart. Reason: " + (reason != null ? reason.getDisplayName() : "N/A"), Logger.LogType.WARNING);
        notifyAdmins("§c[!] Server restart executing now. Reason: " + (reason != null ? reason.getDisplayName() : "Unknown") + (reasonDetail != null && !reasonDetail.isEmpty() ? " — " + reasonDetail : ""));

        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.sendMessage(broadcastMessage);
                }
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "restart");
            }
        }.runTaskLater(Minigame.getInstance().getPlugin(), 40L);
    }

    private String formatTime(int totalSeconds) {
        if (totalSeconds <= 0) return "0s";
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        if (minutes > 0 && seconds > 0) return minutes + "m " + seconds + "s";
        if (minutes > 0) return minutes + "m";
        return seconds + "s";
    }
}