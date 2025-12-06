package cz.johnslovakia.gameapi.modules.game.gameData.implementations;

import cz.johnslovakia.gameapi.modules.game.GameInstance;
import cz.johnslovakia.gameapi.modules.serverManagement.gameData.UpdatedValueInterface;

public class MaxPlayersValueImple implements UpdatedValueInterface<GameInstance> {

    @Override
    public String getWhat() {
        return "Integer";
    }

    @Override
    public String getStringValue(GameInstance game) {
        return null;
    }

    @Override
    public Integer getIntegerValue(GameInstance game) {
        return game.getSettings().getMaxPlayers();
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
