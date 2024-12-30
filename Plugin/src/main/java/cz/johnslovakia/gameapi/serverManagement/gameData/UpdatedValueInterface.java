package cz.johnslovakia.gameapi.serverManagement.gameData;

import cz.johnslovakia.gameapi.game.Game;

public interface UpdatedValueInterface {

    public String getWhat();
    public String getStringValue(Game game);
    public Integer getIntegerValue(Game game);
    public Double getDoubleValue(Game game);
    public Boolean getBooleanValue(Game game);
}
