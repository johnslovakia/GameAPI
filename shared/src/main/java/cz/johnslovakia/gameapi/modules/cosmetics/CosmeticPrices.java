package cz.johnslovakia.gameapi.modules.cosmetics;

import lombok.Getter;
import lombok.Setter;

@Getter
public class CosmeticPrices {

    private PriceSet killSounds = new PriceSet(25000, 20000, 15000, 12000, 9000);
    private PriceSet killEffects = new PriceSet(40000, 30000, 25000, 17000, 12000);
    private PriceSet killMessages = new PriceSet(40000, 30000, 25000, 20000, 15000);
    private PriceSet projectileTrails = new PriceSet(30000, 25000, 20000, 15000, 10000);
    private PriceSet hats = new PriceSet(40000, 30000, 25000, 20000, 15000);

    public PriceSet getByKey(String key) {
        return switch (key) {
            case "kill_sounds" -> killSounds;
            case "kill_effects" -> killEffects;
            case "kill_messages" -> killMessages;
            case "projectile_trails" -> projectileTrails;
            case "hats" -> hats;
            default -> null;
        };
    }

    @Getter
    @Setter
    public static class PriceSet {

        private int legendary;
        private int epic;
        private int rare;
        private int uncommon;
        private int common;

        public PriceSet() {}

        public PriceSet(int legendary, int epic, int rare, int uncommon, int common) {
            this.legendary = legendary;
            this.epic = epic;
            this.rare = rare;
            this.uncommon = uncommon;
            this.common = common;
        }

        public int getByRarity(String rarity) {
            return switch (rarity.toLowerCase()) {
                case "legendary" -> legendary;
                case "epic" -> epic;
                case "rare" -> rare;
                case "uncommon" -> uncommon;
                case "common" -> common;
                default -> 0;
            };
        }

        public void setByRarity(String rarity, int value) {
            switch (rarity.toLowerCase()) {
                case "legendary" -> legendary = value;
                case "epic" -> epic = value;
                case "rare" -> rare = value;
                case "uncommon" -> uncommon = value;
                case "common" -> common = value;
            }
        }
    }
}