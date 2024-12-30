package cz.johnslovakia.gameapi.serverManagement;

import com.google.gson.JsonObject;
import cz.johnslovakia.gameapi.game.GameState;
import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.time.LocalTime;

@Getter @Setter
public class GameData {

    private final IGame server;

    private int players = 0;
    private int maxPlayers = -1;
    private GameState gameState = GameState.LOADING;
    private JsonObject jsonObject;

    private LocalTime lastUpdate;

    public GameData(IGame server) {
        this.server = server;
    }

    public boolean shouldUpdate(){
        if (getLastUpdate() == null){
            return true;
        }
        LocalTime localTime = LocalTime.now();
        Duration dur = Duration.between(lastUpdate, localTime);
        return dur.getSeconds() >= 3; //3 seconds
    }


}
