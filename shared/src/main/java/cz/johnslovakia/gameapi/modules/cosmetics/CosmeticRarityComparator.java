package cz.johnslovakia.gameapi.modules.cosmetics;

import java.util.Comparator;

public class CosmeticRarityComparator implements Comparator<Cosmetic> {

    public int compare(Cosmetic cosmetic1, Cosmetic cosmetic2) {
        CosmeticRarity cosmeticRarity1 = cosmetic1.getRarity();
        CosmeticRarity cosmeticRarity2 = cosmetic2.getRarity();

        int rank1 = cosmeticRarity1.getRank();
        int rank2 = cosmeticRarity2.getRank();

        return Integer.compare(rank1, rank2);
    }

}