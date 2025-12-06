package cz.johnslovakia.gameapi.events;

import cz.johnslovakia.gameapi.users.PlayerIdentity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

@AllArgsConstructor
@Getter
public class DailyXPGainEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final PlayerIdentity playerIdentity;
    private final int oldXP;
    private final int newXP;
    private final int gainedXP;

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList(){
        return handlers;
    }
}