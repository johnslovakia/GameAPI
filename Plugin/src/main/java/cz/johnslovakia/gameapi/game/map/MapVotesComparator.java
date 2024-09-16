package cz.johnslovakia.gameapi.game.map;

import java.util.Comparator;

public class MapVotesComparator implements Comparator<GameMap> {

    @Override
    public int compare(GameMap map1, GameMap map2) {
        return Integer.compare(map2.getVotes(), map1.getVotes());
    }
}