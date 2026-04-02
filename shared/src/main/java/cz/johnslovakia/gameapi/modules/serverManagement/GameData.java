package cz.johnslovakia.gameapi.modules.serverManagement;

import com.google.gson.JsonObject;
import cz.johnslovakia.gameapi.modules.game.GameState;
import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.time.Instant;

@Getter @Setter
public class GameData {

    private final IGame server;

    private int players = 0;
    private int maxPlayers = -1;
    private GameState gameState = GameState.LOADING;
    private JsonObject jsonObject;

    private Instant lastUpdate;

    public GameData(IGame server) {
        this.server = server;
    }

    public boolean shouldUpdate(){
        if (lastUpdate == null) return true;
        return Duration.between(lastUpdate, Instant.now()).getSeconds() >= 3;
    }
}