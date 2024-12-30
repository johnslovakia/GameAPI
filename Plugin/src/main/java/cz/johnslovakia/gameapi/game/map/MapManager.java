package cz.johnslovakia.gameapi.game.map;

import cz.johnslovakia.gameapi.game.Game;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter @Setter
public class MapManager {

    private Game game;
    private final List<GameMap> maps = new ArrayList<>();

    private boolean voting = true;

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

}
