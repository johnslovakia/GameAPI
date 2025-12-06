package cz.johnslovakia.gameapi.modules.game.gameData.implementations;

import cz.johnslovakia.gameapi.modules.game.GameInstance;
import cz.johnslovakia.gameapi.modules.serverManagement.gameData.UpdatedValueInterface;

public class StartingTimeValueImple implements UpdatedValueInterface<GameInstance> {

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
        if (game.getRunningMainTask() != null) {
            if (game.getRunningMainTask().getId().equalsIgnoreCase("StartCountdown")) {
                return game.getRunningMainTask().getCounter();
            }
        }
        return 0;
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
