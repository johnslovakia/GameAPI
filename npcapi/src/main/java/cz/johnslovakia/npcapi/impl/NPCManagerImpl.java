package cz.johnslovakia.npcapi.impl;

import cz.johnslovakia.npcapi.api.NPC;
import cz.johnslovakia.npcapi.api.NPCBuilder;
import cz.johnslovakia.npcapi.api.NPCManager;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class NPCManagerImpl implements NPCManager {

    private final Plugin plugin;
    private final Map<String, NPCImpl> npcById = new ConcurrentHashMap<>();
    private final Map<Integer, NPCImpl> npcByEntityId = new ConcurrentHashMap<>();

    public NPCManagerImpl(@NotNull Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull NPCBuilder builder(@NotNull String id) {
        return new NPCBuilderImpl(id, this);
    }

    @Override
    public @NotNull Optional<NPC> getNPC(@NotNull String id) {
        return Optional.ofNullable(npcById.get(id));
    }

    @Override
    public @NotNull Optional<NPC> getNPCByEntityId(int entityId) {
        return Optional.ofNullable(npcByEntityId.get(entityId));
    }

    @Override
    public @NotNull Collection<NPC> getAllNPCs() {
        return Collections.unmodifiableCollection(new ArrayList<>(npcById.values()));
    }

    @Override
    public @NotNull Collection<NPC> getVisibleNPCs(@NotNull Player player) {
        return npcById.values().stream()
                .filter(npc -> npc.isVisible(player))
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public boolean removeNPC(@NotNull String id) {
        NPCImpl npc = npcById.get(id);
        if (npc == null) return false;
        npc.remove();
        return true;
    }

    @Override
    public void removeAll() {
        new ArrayList<>(npcById.values()).forEach(NPCImpl::remove);
        npcById.clear();
        npcByEntityId.clear();
    }

    public void register(@NotNull NPCImpl npc) {
        npcById.put(npc.getId(), npc);
        npcByEntityId.put(npc.getEntityId(), npc);
    }

    public void unregister(@NotNull String id) {
        NPCImpl removed = npcById.remove(id);
        if (removed != null) {
            npcByEntityId.remove(removed.getEntityId());
        }
    }

    /** Notify all NPCs that a player has left — cleans up viewer sets. */
    public void handlePlayerQuit(@NotNull Player player) {
        for (NPCImpl npc : npcById.values()) {
            npc.onViewerQuit(player);
        }
    }

    @NotNull
    public Plugin getPlugin() {
        return plugin;
    }
}
