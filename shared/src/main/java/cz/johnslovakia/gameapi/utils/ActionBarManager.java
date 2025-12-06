package cz.johnslovakia.gameapi.utils;

import cz.johnslovakia.gameapi.Shared;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class ActionBarManager {

    private static final Map<UUID, PriorityQueue<Message>> messageQueues = new HashMap<>();
    private static final Set<UUID> currentlyShowing = new HashSet<>();

    private static final int DEFAULT_PRIORITY = 0;

    public static void sendActionBar(Player player, String message) {
        sendActionBar(player, message, DEFAULT_PRIORITY);
    }

    public static void sendActionBar(Player player, String message, int priority) {
        sendActionBar(player, LegacyComponentSerializer.legacyAmpersand().deserialize(StringUtils.colorizer(message)), priority);
    }

    public static void sendActionBar(Player player, Component message) {
        sendActionBar(player, message, DEFAULT_PRIORITY);
    }

    public static void sendActionBar(Player player, Component message, int priority) {
        UUID uuid = player.getUniqueId();
        messageQueues.putIfAbsent(uuid, new PriorityQueue<>());
        messageQueues.get(uuid).add(new Message(message, 60, priority));

        if (!currentlyShowing.contains(uuid)) {
            processQueue(player);
        }
    }

    private static void processQueue(Player player) {
        UUID uuid = player.getUniqueId();
        PriorityQueue<Message> queue = messageQueues.get(uuid);

        if (queue == null || queue.isEmpty()) {
            currentlyShowing.remove(uuid);
            return;
        }

        currentlyShowing.add(uuid);
        Message msg = queue.poll();
        if (msg == null) return;

        new BukkitRunnable() {
            int ticksLeft = msg.durationTicks;

            @Override
            public void run() {
                if (!player.isOnline() || ticksLeft <= 0) {
                    cancel();
                    processQueue(player);
                    return;
                }

                player.sendActionBar(msg.component);
                ticksLeft--;
            }
        }.runTaskTimer(Shared.getInstance().getPlugin(), 0L, 1L);
    }

    private record Message(Component component, int durationTicks, int priority) implements Comparable<Message> {
        @Override
        public int compareTo(Message other) {
            return Integer.compare(other.priority, this.priority);
        }
    }
}