package cz.johnslovakia.gameapi.modules.resources;

import java.util.Comparator;

public class ResourceComparator implements Comparator<Resource> {

    public int compare(Resource rt1, Resource rt2) {
        return Integer.compare(rt1.getRank(), rt2.getRank());
    }
}