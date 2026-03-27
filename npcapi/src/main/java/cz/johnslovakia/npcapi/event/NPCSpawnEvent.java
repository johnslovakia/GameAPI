package cz.johnslovakia.npcapi.event;

import cz.johnslovakia.npcapi.api.NPC;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when an NPC is shown (spawned) to a specific player.
 */
public class NPCSpawnEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final NPC npc;
    private final Player player;

    public NPCSpawnEvent(@NotNull NPC npc, @NotNull Player player) {
        this.npc = npc;
        this.player = player;
    }

    @NotNull public NPC getNPC()       { return npc; }
    @NotNull public Player getPlayer() { return player; }

    @Override @NotNull public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList()          { return HANDLERS; }
}
