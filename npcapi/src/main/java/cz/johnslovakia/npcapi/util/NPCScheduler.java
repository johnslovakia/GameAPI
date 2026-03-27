package cz.johnslovakia.npcapi.util;

import cz.johnslovakia.npcapi.api.NPC;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Helper that ties repeating Bukkit tasks to specific NPCs and cancels them
 * automatically when the NPC is removed.
 *
 * <pre>{@code
 * NPCScheduler scheduler = new NPCScheduler(plugin);
 *
 * // Make the NPC look at the nearest player every second
 * scheduler.schedule(npc, 0L, 20L, () -> {
 *     npc.getViewers().stream()
 *         .min(Comparator.comparingDouble(p -> p.getLocation().distanceSquared(npc.getLocation())))
 *         .ifPresent(npc::lookAt);
 * });
 *
 * // Cancel when done
 * scheduler.cancelAll(npc);
 * }</pre>
 */
public final class NPCScheduler {

    private final Plugin plugin;
    private final Map<String, List<BukkitTask>> tasks = new ConcurrentHashMap<>();

    public NPCScheduler(@NotNull Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Schedules a repeating task tied to the given NPC.
     *
     * @param npc Target NPC
     * @param delay Initial delay in ticks
     * @param period Repeat period in ticks
     * @param task Runnable to execute on the main thread
     */
    @NotNull
    public BukkitTask schedule(@NotNull NPC npc, long delay, long period, @NotNull Runnable task) {
        BukkitTask bt = plugin.getServer().getScheduler().runTaskTimer(plugin, task, delay, period);
        tasks.computeIfAbsent(npc.getId(), k -> Collections.synchronizedList(new ArrayList<>())).add(bt);
        return bt;
    }

    /**
     * Schedules a delayed one-shot task tied to the given NPC.
     */
    @NotNull
    public BukkitTask scheduleOnce(@NotNull NPC npc, long delay, @NotNull Runnable task) {
        BukkitTask bt = plugin.getServer().getScheduler().runTaskLater(plugin, task, delay);
        tasks.computeIfAbsent(npc.getId(), k -> Collections.synchronizedList(new ArrayList<>())).add(bt);
        return bt;
    }

    /**
     * Cancels all scheduled tasks for the given NPC.
     */
    public void cancelAll(@NotNull NPC npc) {
        cancelAll(npc.getId());
    }

    public void cancelAll(@NotNull String npcId) {
        List<BukkitTask> list = tasks.remove(npcId);
        if (list != null) list.forEach(BukkitTask::cancel);
    }

    /**
     * Cancels all tasks for all NPCs.
     */
    public void cancelAll() {
        tasks.forEach((id, list) -> list.forEach(BukkitTask::cancel));
        tasks.clear();
    }
}
