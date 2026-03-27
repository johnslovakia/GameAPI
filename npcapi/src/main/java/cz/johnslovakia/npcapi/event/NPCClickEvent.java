package cz.johnslovakia.npcapi.event;

import cz.johnslovakia.npcapi.api.NPC;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a player interacts (left or right click) with an NPC.
 */
public class NPCClickEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final NPC npc;
    private final NPC.ClickType clickType;
    private boolean cancelled;

    public NPCClickEvent(@NotNull Player player, @NotNull NPC npc, @NotNull NPC.ClickType clickType) {
        this.player = player;
        this.npc = npc;
        this.clickType = clickType;
    }

    @NotNull
    public Player getPlayer() {
        return player;
    }

    @NotNull
    public NPC getNPC() {
        return npc;
    }

    @NotNull
    public NPC.ClickType getClickType() {
        return clickType;
    }

    public boolean isLeftClick() {
        return clickType == NPC.ClickType.LEFT_CLICK;
    }

    public boolean isRightClick() {
        return clickType == NPC.ClickType.RIGHT_CLICK;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    @NotNull
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
