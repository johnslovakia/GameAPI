package cz.johnslovakia.gameapi.game.map;

import cz.johnslovakia.gameapi.game.Game;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class MapManager {

    private Game game;
    private List<GameMap> maps = new ArrayList<>();

    private boolean voting;

    public MapManager(Game game) {
        this.game = game;
        game.setMapManager(this);
    }

    public void addMap(GameMap map){
        if (maps.contains(map)){
            return;
        }
        maps.add(map);
    }

    public boolean isEnabledVoting() {
        return voting;
    }

    public void setVoting(boolean voting) {
        this.voting = voting;
    }
}
