package cz.johnslovakia.gameapi.serverManagement.gameData.implementations;

import cz.johnslovakia.gameapi.game.Game;
import cz.johnslovakia.gameapi.serverManagement.gameData.UpdatedValueInterface;

public class StartingTimeValueImple implements UpdatedValueInterface {

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
        if (game.getRunningMainTask() != null) {
            if (game.getRunningMainTask().getId().equalsIgnoreCase("StartCountdown")) {
                return game.getRunningMainTask().getCounter();
            }
        }
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
