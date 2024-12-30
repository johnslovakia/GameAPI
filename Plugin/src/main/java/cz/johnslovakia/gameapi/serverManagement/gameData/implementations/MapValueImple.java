package cz.johnslovakia.gameapi.serverManagement.gameData.implementations;

import cz.johnslovakia.gameapi.game.Game;
import cz.johnslovakia.gameapi.serverManagement.gameData.UpdatedValueInterface;

public class MapValueImple implements UpdatedValueInterface {

    @Override
    public String getWhat() {
        return "String";
    }

    @Override
    public String getStringValue(Game game) {
        if (game.getCurrentMap() == null){
            return "Voting...";
        }
        return game.getCurrentMap().getName();
    }

    @Override
    public Integer getIntegerValue(Game game) {
        return 0;
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
