package cz.johnslovakia.gameapi.modules.settings;

import cz.johnslovakia.gameapi.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class SettingsEditSession {

    private static final int MAX_UNDO_ACTIONS = 5;
    private static final Map<UUID, SettingsEditSession> SESSIONS = new ConcurrentHashMap<>();
    private static final Map<String, TrackedSource> SOURCES = new ConcurrentHashMap<>();
    private static final ThreadLocal<SettingsEditSession> CURRENT = new ThreadLocal<>();

    private final UUID playerId;
    private final Deque<UndoAction> undoActions = new ArrayDeque<>();
    private final Set<String> pendingSaves = new LinkedHashSet<>();
    private ActionCapture currentAction;
    private int actionDepth;

    private SettingsEditSession(UUID playerId) {
        this.playerId = playerId;
    }

    public static SettingsEditSession get(Player player) {
        return SESSIONS.computeIfAbsent(player.getUniqueId(), SettingsEditSession::new);
    }

    public static void registerSource(String sourceId,
                                      String displayName,
                                      Supplier<String> snapshotSupplier,
                                      Consumer<String> restoreConsumer,
                                      Runnable saveRunnable) {
        SOURCES.put(sourceId, new TrackedSource(sourceId, displayName, snapshotSupplier, restoreConsumer, saveRunnable));
    }

    public static void runAction(Player player, Runnable action) {
        runAction(player, null, action);
    }

    public static void runAction(Player player, String undoGroup, Runnable action) {
        SettingsEditSession session = get(player);
        session.beginAction(undoGroup);
        SettingsEditSession previous = CURRENT.get();
        CURRENT.set(session);
        try {
            action.run();
        } finally {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
            session.endAction();
        }
    }

    public static boolean deferSave(String sourceId) {
        SettingsEditSession session = CURRENT.get();
        if (session == null) {
            return false;
        }
        session.markDirty(sourceId);
        return true;
    }

    public static void afterCurrentAction(Runnable action) {
        SettingsEditSession session = CURRENT.get();
        if (session == null || session.currentAction == null) {
            action.run();
            return;
        }
        session.currentAction.afterActions.add(action);
    }

    public static void finish(Player player) {
        SettingsEditSession session = SESSIONS.remove(player.getUniqueId());
        if (session == null) {
            return;
        }
        int saved = session.flush(true);
        if (saved > 0) {
            player.sendMessage("§aSettings saved.");
        }
    }

    public static void flushAll() {
        for (SettingsEditSession session : SESSIONS.values()) {
            session.flush(true);
        }
        SESSIONS.clear();
    }

    public static void saveCheckpoint(Player player) {
        SettingsEditSession session = SESSIONS.get(player.getUniqueId());
        if (session != null) {
            session.flush(false);
        }
    }

    public boolean hasUndo() {
        return !undoActions.isEmpty();
    }

    public int undoCount() {
        return undoActions.size();
    }

    public boolean undoLast(Player player) {
        UndoAction action = undoActions.pollFirst();
        if (action == null) {
            player.sendMessage("§cThere is nothing to undo.");
            player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 0.7F, 0.8F);
            return false;
        }

        for (Map.Entry<String, String> entry : action.snapshots().entrySet()) {
            TrackedSource source = SOURCES.get(entry.getKey());
            if (source == null) {
                continue;
            }
            source.restore(entry.getValue());
            pendingSaves.add(entry.getKey());
        }

        flush(false);
        player.sendMessage("§aUndid last settings change.");
        player.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 0.8F, 1.35F);
        return true;
    }

    public ItemStack undoItem() {
        boolean hasUndo = hasUndo();
        ItemBuilder builder = new ItemBuilder(hasUndo ? Material.REDSTONE : Material.GRAY_DYE)
                .setName("§aUndo Last Change")
                .removeLore();

        if (hasUndo) {
            builder.addLoreLine("§7Available undo actions: §f" + undoCount() + "§7/§f" + MAX_UNDO_ACTIONS);
            builder.addLoreLine("");
            builder.addLoreLine("§a► Click to undo the last settings change");
        } else {
            builder.addLoreLine("§8No changes to undo.");
        }
        return builder.toItemStack();
    }

    private void beginAction(String undoGroup) {
        if (actionDepth++ > 0) {
            return;
        }

        currentAction = new ActionCapture(undoGroup);
        for (TrackedSource source : SOURCES.values()) {
            String snapshot = source.snapshot();
            if (snapshot != null) {
                currentAction.beforeSnapshots.put(source.id(), snapshot);
            }
        }
    }

    private void markDirty(String sourceId) {
        if (currentAction == null) {
            pendingSaves.add(sourceId);
            return;
        }
        currentAction.dirtySources.add(sourceId);
    }

    private void endAction() {
        if (--actionDepth > 0) {
            return;
        }

        ActionCapture finished = currentAction;
        currentAction = null;
        if (finished == null) {
            return;
        }

        try {
            if (finished.dirtySources.isEmpty()) {
                return;
            }

            Map<String, String> changedSnapshots = new LinkedHashMap<>();
            for (String sourceId : finished.dirtySources) {
                TrackedSource source = SOURCES.get(sourceId);
                if (source == null) {
                    continue;
                }

                String before = finished.beforeSnapshots.get(sourceId);
                String after = source.snapshot();
                if (before == null || !Objects.equals(before, after)) {
                    if (before != null) {
                        changedSnapshots.put(sourceId, before);
                    }
                    pendingSaves.add(sourceId);
                }
            }

            if (changedSnapshots.isEmpty()) {
                return;
            }

            UndoAction previous = undoActions.peekFirst();
            if (finished.undoGroup != null
                    && previous != null
                    && finished.undoGroup.equals(previous.undoGroup())
                    && previous.snapshots().keySet().containsAll(changedSnapshots.keySet())) {
                Map<String, String> mergedSnapshots = new LinkedHashMap<>(previous.snapshots());
                changedSnapshots.forEach(mergedSnapshots::putIfAbsent);

                Map<String, String> stillChanged = new LinkedHashMap<>();
                for (Map.Entry<String, String> entry : mergedSnapshots.entrySet()) {
                    TrackedSource source = SOURCES.get(entry.getKey());
                    if (source == null) {
                        continue;
                    }
                    if (!Objects.equals(entry.getValue(), source.snapshot())) {
                        stillChanged.put(entry.getKey(), entry.getValue());
                    }
                }

                undoActions.removeFirst();
                if (!stillChanged.isEmpty()) {
                    undoActions.addFirst(new UndoAction(finished.undoGroup, stillChanged));
                }
                return;
            }

            undoActions.addFirst(new UndoAction(finished.undoGroup, changedSnapshots));
            while (undoActions.size() > MAX_UNDO_ACTIONS) {
                undoActions.removeLast();
            }
        } finally {
            if (!finished.afterActions.isEmpty()) {
                flush(false);
            }
            for (Runnable afterAction : finished.afterActions) {
                afterAction.run();
            }
        }
    }

    private int flush(boolean clearUndo) {
        int saved = 0;
        for (String sourceId : new LinkedHashSet<>(pendingSaves)) {
            TrackedSource source = SOURCES.get(sourceId);
            if (source == null) {
                continue;
            }
            source.save();
            saved++;
        }
        pendingSaves.clear();
        if (clearUndo) {
            undoActions.clear();
        }
        return saved;
    }

    private record TrackedSource(String id,
                                 String displayName,
                                 Supplier<String> snapshotSupplier,
                                 Consumer<String> restoreConsumer,
                                 Runnable saveRunnable) {
        String snapshot() {
            try {
                return snapshotSupplier.get();
            } catch (Exception ignored) {
                return null;
            }
        }

        void restore(String snapshot) {
            try {
                restoreConsumer.accept(snapshot);
            } catch (Exception ignored) {}
        }

        void save() {
            try {
                saveRunnable.run();
            } catch (Exception ignored) {}
        }
    }

    private static final class ActionCapture {
        private final String undoGroup;
        private final Map<String, String> beforeSnapshots = new LinkedHashMap<>();
        private final Set<String> dirtySources = new LinkedHashSet<>();
        private final List<Runnable> afterActions = new ArrayList<>();

        private ActionCapture(String undoGroup) {
            this.undoGroup = undoGroup;
        }
    }

    private record UndoAction(String undoGroup, Map<String, String> snapshots) {}
}
