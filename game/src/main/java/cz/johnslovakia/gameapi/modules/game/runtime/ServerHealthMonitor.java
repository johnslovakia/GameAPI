package cz.johnslovakia.gameapi.modules.game.runtime;

import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.modules.Module;
import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.utils.Logger;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.atomic.AtomicBoolean;

@Getter
@Setter
public class ServerHealthMonitor implements Module {

    /** RAM % that triggers breach counting (soft threshold). */
    private double ramSoftThresholdPercent = 85.0;

    /** RAM % that triggers hard-deadline mode (arenas WILL be interrupted). */
    private double ramHardThresholdPercent = 92.0;

    /** RAM % that triggers an immediate forced restart. */
    private double ramCriticalPercent = 96.0;

    /** TPS (1-min avg) below which a breach is counted. */
    private double tpsSoftThreshold = 12.0;

    /** TPS (1-min avg) below which hard-deadline mode activates. */
    private double tpsHardThreshold = 6.0;



    /** Interval between health checks (ticks). */
    private long checkIntervalTicks = 20L * 45; // 45 s

    /** Consecutive soft breaches needed before entering restart-pending. */
    private int requiredConsecutiveBreaches = 4; // ~2 min at 30 s


    private BukkitTask healthCheckTask;
    private int consecutiveBreaches = 0;
    private final AtomicBoolean monitoringActive = new AtomicBoolean(false);


    @Override
    public void initialize() {
        startHealthCheck();
        Logger.log("[HealthMonitor] Initialized — RAM soft=" + ramSoftThresholdPercent +
                   "%, hard=" + ramHardThresholdPercent +
                   "%, critical=" + ramCriticalPercent +
                   "% | TPS soft=" + tpsSoftThreshold +
                   ", hard=" + tpsHardThreshold +
                   " | breaches required=" + requiredConsecutiveBreaches,
                   Logger.LogType.INFO);
    }

    @Override
    public void terminate() {
        monitoringActive.set(false);
        if (healthCheckTask != null) {
            healthCheckTask.cancel();
            healthCheckTask = null;
        }
    }

    private void startHealthCheck() {
        monitoringActive.set(true);
        healthCheckTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!monitoringActive.get()) {
                    cancel();
                    return;
                }
                performHealthCheck();
            }
        }.runTaskTimer(Minigame.getInstance().getPlugin(), checkIntervalTicks, checkIntervalTicks);
    }

    private void performHealthCheck() {
        RestartScheduler scheduler = ModuleManager.getModule(RestartScheduler.class);
        if (scheduler == null) return;
        if (scheduler.isRestartPending() || scheduler.isRestartInProgress()) return;
        HealthSnapshot snapshot = takeSnapshot();

        /*Logger.log(String.format(
                "[HealthMonitor] RAM: %.1f%% (%dMB/%dMB) | TPS(1m/5m/15m): %.1f/%.1f/%.1f | Breaches: %d/%d",
                snapshot.ramPercent(), snapshot.usedMemoryMB(), snapshot.maxMemoryMB(),
                snapshot.tps1m(), snapshot.tps5m(), snapshot.tps15m(),
                consecutiveBreaches, requiredConsecutiveBreaches),
                Logger.LogType.INFO);*/

        if (snapshot.ramPercent() >= ramCriticalPercent) {
            Logger.log("[HealthMonitor] CRITICAL RAM! Forcing immediate restart.", Logger.LogType.ERROR);
            scheduler.scheduleRestart(RestartScheduler.RestartReason.CRITICAL_RESOURCES, buildReasonDetail(snapshot), true);
            return;
        }

        boolean hardBreach = snapshot.ramPercent() >= ramHardThresholdPercent || snapshot.tps1m() <= tpsHardThreshold;
        if (hardBreach) {
            Logger.log("[HealthMonitor] Hard threshold breached. Scheduling restart with deadline.", Logger.LogType.WARNING);
            scheduler.scheduleRestart(RestartScheduler.RestartReason.PERFORMANCE_DEGRADATION, buildReasonDetail(snapshot), true);
            return;
        }

        boolean softBreach = snapshot.ramPercent() >= ramSoftThresholdPercent || snapshot.tps1m() <= tpsSoftThreshold;

        if (softBreach) {
            consecutiveBreaches++;
        } else {
            consecutiveBreaches = Math.max(0, consecutiveBreaches - 1);
        }

        if (consecutiveBreaches >= requiredConsecutiveBreaches) {
            Logger.log("[HealthMonitor] Soft threshold sustained. Scheduling graceful restart (no hard deadline).",
                       Logger.LogType.WARNING);
            scheduler.scheduleRestart(RestartScheduler.RestartReason.PERFORMANCE_DEGRADATION, buildReasonDetail(snapshot), false);
            consecutiveBreaches = 0;
        }
    }


    public HealthSnapshot takeSnapshot() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        double ramPercent = (double) usedMemory / maxMemory * 100.0;
        double[] tps = Bukkit.getServer().getTPS();

        return new HealthSnapshot(ramPercent, usedMemory / (1024 * 1024), maxMemory / (1024 * 1024), tps[0], tps[1], tps[2]);
    }

    private String buildReasonDetail(HealthSnapshot s) {
        return String.format("RAM: %.1f%% (%dMB/%dMB) | TPS: %.1f", s.ramPercent(), s.usedMemoryMB(), s.maxMemoryMB(), s.tps1m());
    }


    public record HealthSnapshot(double ramPercent, long usedMemoryMB, long maxMemoryMB, double tps1m, double tps5m, double tps15m) {}
}