package cz.johnslovakia.gameapi.rewards;

import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.resources.Resource;
import cz.johnslovakia.gameapi.modules.resources.ResourcesModule;
import cz.johnslovakia.gameapi.utils.RandomUtils;
import lombok.Getter;
import lombok.Setter;

import java.util.Random;

@Getter
public class RewardItem {

    private final int amount;
    @Setter
    private String resource;
    private int chance = 100;
    private int randomMinRange = 0;
    private int randomMaxRange = 0;

    public RewardItem(Builder builder) {
        if (builder.chance < 0 || builder.chance > 100) {
            throw new IllegalArgumentException("Chance must be between 0 and 100!");
        }
        this.resource = builder.resource;
        this.chance = builder.chance;
        this.amount = builder.amount;
        this.randomMinRange = builder.randomMinRange;
        this.randomMaxRange = builder.randomMaxRange;
    }

    public Resource getResource(){
        return ModuleManager.getModule(ResourcesModule.class).getResourceByName(resource);
    }

    public RewardItem(Resource resource, int amount) {
        this.resource = resource.getName();
        this.amount = amount;
    }

    public RewardItem(String resourceName, int amount) {
        this.resource = resourceName;
        this.amount = amount;
    }

    public boolean randomAmount(){
        return randomMinRange != 0 && randomMaxRange > 0;
    }

    public int getAmount() {
        if (randomMinRange != 0 && randomMaxRange > 0) {
            return RandomUtils.randomInteger(getRandomMinRange(), getRandomMaxRange());
        }
        return amount;
    }

    public boolean shouldApply() {
        if (chance >= 100) return true;
        if (chance <= 0) return false;
        return new Random().nextInt(100) < chance;
    }


    public static RewardItem.Builder builder(Resource resource) { return new RewardItem.Builder(resource.getName());}
    public static RewardItem.Builder builder(String resourceName) { return new RewardItem.Builder(resourceName);}

    public static class Builder {

        private final String resource;
        private int amount = 1;
        private int chance = 100;
        private int randomMinRange = 0;
        private int randomMaxRange = 0;

        public Builder(String resourceName) {
            this.resource = resourceName;
        }

        public Builder setAmount(int amount) {
            this.amount = amount;
            return this;
        }

        public Builder setChance(int chance) {
            this.chance = chance;
            return this;
        }

        public Builder setRandomAmountRange(int minRange, int maxRange) {
            this.randomMinRange = minRange;
            this.randomMaxRange = maxRange;
            return this;
        }

        public RewardItem build() {
            return new RewardItem(this);
        }
    }
}