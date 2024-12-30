package cz.johnslovakia.gameapi.serverManagement.gameData.implementations;

import cz.johnslovakia.gameapi.game.Game;
import cz.johnslovakia.gameapi.serverManagement.gameData.UpdatedValueInterface;

public class PlayersValueImple implements UpdatedValueInterface {

    @Override
    public String getWhat() {
        return "Integer";
    }

    @Override
    public String getStringValue(Game game) {
        return null;
    }

    @Override
    public Integer getIntegerValue(Game game) {
        return game.getPlayers().size();
    }

    @Override
    public Double getDoubleValue(Game game) {
        return 0.0;
    }

    @Override
    public Boolean getBooleanValue(Game game) {
        return null;
    }
}
