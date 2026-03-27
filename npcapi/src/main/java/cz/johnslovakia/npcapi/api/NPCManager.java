package cz.johnslovakia.npcapi.api;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;

/**
 * Registry for all active {@link NPC} instances.
 * Obtain via {@link NpcAPI#get()}.
 */
public interface NPCManager {

    /**
     * Creates a new builder for an NPC with the given unique ID.
     *
     * @param id Unique string identifier (used internally and for lookups)
     */
    @NotNull
    NPCBuilder builder(@NotNull String id);

    /**
     * Returns an NPC by its string ID, or empty if not found.
     */
    @NotNull
    Optional<NPC> getNPC(@NotNull String id);

    /**
     * Returns an NPC by its internal packet entity ID.
     */
    @NotNull
    Optional<NPC> getNPCByEntityId(int entityId);

    /**
     * Immutable snapshot of all registered NPCs.
     */
    @NotNull
    Collection<NPC> getAllNPCs();

    /**
     * Returns all NPCs currently visible to the given player.
     */
    @NotNull
    Collection<NPC> getVisibleNPCs(@NotNull Player player);

    /**
     * Removes and despawns an NPC by ID.
     *
     * @return true if an NPC with this ID existed and was removed
     */
    boolean removeNPC(@NotNull String id);

    /**
     * Removes and despawns all registered NPCs.
     */
    void removeAll();
}
