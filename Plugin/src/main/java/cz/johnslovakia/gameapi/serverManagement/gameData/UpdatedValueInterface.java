package cz.johnslovakia.gameapi.serverManagement.gameData;

import cz.johnslovakia.gameapi.game.Game;

public interface UpdatedValueInterface {

    String getWhat();
    String getStringValue(Game game);
    Integer getIntegerValue(Game game);
    Double getDoubleValue(Game game);
    Boolean getBooleanValue(Game game);
}
