package cz.johnslovakia.gameapi.users.resources;

import java.util.Comparator;

public class ResourceComparator implements Comparator<Resource> {

    public int compare(Resource rt1, Resource rt2) {

        int rank1 = rt1.getRank();
        int rank2 = rt2.getRank();

        if (rank1 > rank2){
            return +1;
        }else if (rank1 < rank2){
            return -1;
        }else{
            return 0;
        }
    }
}