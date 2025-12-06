package cz.johnslovakia.gameapi.rewards;

import java.util.Comparator;

public class RewardComparator implements Comparator<RewardItem> {

    public int compare(RewardItem rt1, RewardItem rt2) {
        return Integer.compare(rt1.getResource().getRank(), rt2.getResource().getRank());
    }
}