package cz.johnslovakia.gameapi.utils.rewards;

import cz.johnslovakia.gameapi.users.resources.Resource;
import cz.johnslovakia.gameapi.utils.RandomUtils;
import lombok.Getter;

import java.util.Random;

@Getter
public class RewardItem {

    private final Resource resource;
    private final int amount;
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

    public boolean randomAmount(){
        return randomMinRange != 0 && randomMaxRange > 0;
    }

    public int getAmount() {
        if (randomMinRange != 0 && randomMaxRange > 0) {
            return RandomUtils.randomInteger(getRandomMinRange(), getRandomMaxRange());
        }
        return amount;
    }

    public RewardItem(Resource resource, int amount) {
        this.resource = resource;
        this.amount = amount;
    }

    public RewardItem(String resourceName, int amount) {
        this.resource = Resource.getResourceByName(resourceName);
        this.amount = amount;
    }

    public boolean shouldApply() {
        //TODO: opravit
        /*if (chance == 100) return true;
        return new Random().nextInt(100) < chance;*/
        return true;
    }


    public static RewardItem.Builder builder(Resource resource) { return new RewardItem.Builder(resource);}
    public static RewardItem.Builder builder(String resourceName) { return new RewardItem.Builder(Resource.getResourceByName(resourceName));}

    public static class Builder {

        private final Resource resource;
        private int amount = 1;
        private int chance = 100;
        private int randomMinRange = 0;
        private int randomMaxRange = 0;

        public Builder(Resource resource) {
            this.resource = resource;
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