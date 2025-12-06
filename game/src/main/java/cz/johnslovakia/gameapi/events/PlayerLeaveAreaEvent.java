package cz.johnslovakia.gameapi.events;

import cz.johnslovakia.gameapi.modules.game.map.Area;
import cz.johnslovakia.gameapi.users.GamePlayer;
import org.bukkit.event.player.PlayerMoveEvent;

public class PlayerLeaveAreaEvent extends AreaEvent {

    public PlayerLeaveAreaEvent(GamePlayer player, Area area, PlayerMoveEvent moveEvent) {
        super(player, area, moveEvent);
    }

}