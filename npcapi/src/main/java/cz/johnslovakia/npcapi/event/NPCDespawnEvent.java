package cz.johnslovakia.npcapi.event;

import cz.johnslovakia.npcapi.api.NPC;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when an NPC is hidden (despawned) from a specific player.
 */
public class NPCDespawnEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final NPC npc;
    private final Player player;
    private final DespawnReason reason;

    public NPCDespawnEvent(@NotNull NPC npc, @NotNull Player player, @NotNull DespawnReason reason) {
        this.npc = npc;
        this.player = player;
        this.reason = reason;
    }

    @NotNull public NPC getNPC()              { return npc; }
    @NotNull public Player getPlayer()        { return player; }
    @NotNull public DespawnReason getReason() { return reason; }

    @Override @NotNull public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList()          { return HANDLERS; }

    public enum DespawnReason {
        /** {@link NPC#hide(Player)} was called */
        HIDDEN,
        /** {@link NPC#remove()} was called */
        REMOVED,
        /** Player disconnected */
        PLAYER_QUIT
    }
}
