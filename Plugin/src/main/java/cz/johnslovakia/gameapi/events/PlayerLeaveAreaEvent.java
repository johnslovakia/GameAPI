package cz.johnslovakia.gameapi.events;

import cz.johnslovakia.gameapi.game.map.Area;
import cz.johnslovakia.gameapi.users.GamePlayer;

public class PlayerLeaveAreaEvent extends AreaEvent {

    public PlayerLeaveAreaEvent(GamePlayer player, Area area) {
        super(player, area);
    }

}