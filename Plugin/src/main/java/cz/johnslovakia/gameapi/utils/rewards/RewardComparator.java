package cz.johnslovakia.gameapi.utils.rewards;

import cz.johnslovakia.gameapi.users.resources.Resource;

import java.util.Comparator;

public class RewardComparator implements Comparator<RewardItem> {

    public int compare(RewardItem rt1, RewardItem rt2) {

        int rank1 = rt1.getResource().getRank();
        int rank2 = rt2.getResource().getRank();

        if (rank1 > rank2){
            return +1;
        }else if (rank1 < rank2){
            return -1;
        }else{
            return 0;
        }
    }
}