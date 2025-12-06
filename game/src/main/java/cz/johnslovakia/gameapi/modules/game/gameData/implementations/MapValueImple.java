package cz.johnslovakia.gameapi.modules.game.gameData.implementations;

import cz.johnslovakia.gameapi.modules.game.GameInstance;
import cz.johnslovakia.gameapi.modules.serverManagement.gameData.UpdatedValueInterface;

public class MapValueImple implements UpdatedValueInterface<GameInstance> {

    @Override
    public String getWhat() {
        return "String";
    }

    @Override
    public String getStringValue(GameInstance game) {
        if (game.getCurrentMap() == null){
            return "Voting...";
        }
        return game.getCurrentMap().getName();
    }

    @Override
    public Integer getIntegerValue(GameInstance game) {
        return null;
    }

    @Override
    public Double getDoubleValue(GameInstance game) {
        return null;
    }

    @Override
    public Boolean getBooleanValue(GameInstance game) {
        return null;
    }
}
