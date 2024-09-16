package cz.johnslovakia.gameapi.events;

import cz.johnslovakia.gameapi.game.map.Area;
import cz.johnslovakia.gameapi.users.GamePlayer;

public class PlayerEnterAreaEvent extends AreaEvent {

    public PlayerEnterAreaEvent(GamePlayer player, Area area) {
        super(player, area);
    }
}