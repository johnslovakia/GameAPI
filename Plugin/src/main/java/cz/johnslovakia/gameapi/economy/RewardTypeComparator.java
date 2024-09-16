package cz.johnslovakia.gameapi.economy;

import java.util.Comparator;

public class RewardTypeComparator implements Comparator<Economy> {

    public int compare(Economy rt1, Economy rt2) {

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