package cz.johnslovakia.gameapi.modules.resources;

import cz.johnslovakia.gameapi.modules.resources.storage.BatchedStorage;
import cz.johnslovakia.gameapi.modules.resources.storage.ResourceStorage;
import cz.johnslovakia.gameapi.modules.resources.storage.VaultStorage;

import lombok.Getter;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;

@Getter
public class Resource {

    private final String name;
    private final String displayName;
    private final ChatColor color;
    private final int rank;

    private final int firstDailyWinReward;
    private final String imgChar;

    private ResourceStorage resourceInterface;
    private boolean applicableBonus;

    private Resource(Builder builder) {
        this.name = builder.name;
        this.displayName = builder.displayName;
        this.color = builder.color;
        this.rank = builder.rank;
        this.firstDailyWinReward = builder.firstDailyWinReward;
        this.applicableBonus = builder.applicableBonus;
        this.imgChar = builder.imgChar;
        this.resourceInterface = builder.resourceStorage;
    }

    public String getDisplayName(){
        if (displayName != null)
            return displayName;
        return name;
    }

    public String formattedName() {
        String caps = name.toLowerCase();
        return caps.substring(0, 1).toUpperCase() + caps.substring(1);
    }

    public ObservableResource observe(@NotNull ResourceChangeListener listener) {
        ResourceStorage currentInterface = this.resourceInterface;
        if (currentInterface == null) return null;
        ObservableResource observable;

        if (!(currentInterface instanceof ObservableResource)) {
            observable = new ObservableResource(currentInterface);
            this.resourceInterface = observable;
        } else {
            observable = (ObservableResource) currentInterface;
        }

        observable.addListener(listener);
        return observable;
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    @Getter
    public static class Builder {
        private final String name;
        private String displayName;
        private ChatColor color = ChatColor.GOLD;
        private int rank = 0;
        private int firstDailyWinReward = 0;
        private boolean applicableBonus = false;
        private String imgChar;
        private ResourceStorage resourceStorage;

        private Builder(String name) {
            this.name = name;
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder color(ChatColor color) {
            this.color = color;
            return this;
        }

        public Builder rank(int rank) {
            this.rank = rank;
            return this;
        }

        public Builder firstDailyWinReward(int reward) {
            this.firstDailyWinReward = reward;
            return this;
        }

        public Builder applicableBonus(boolean applicable) {
            this.applicableBonus = applicable;
            return this;
        }

        public Builder imgChar(String imgChar) {
            this.imgChar = imgChar;
            return this;
        }

        public Builder customInterface(ResourceStorage resourceStorage) {
            this.resourceStorage = resourceStorage;
            return this;
        }

        public Builder batched(String tableName) {
            this.resourceStorage = new BatchedStorage(getName(), tableName);
            return this;
        }

        public Builder vault(Economy vaultEconomy) {
            this.resourceStorage = new VaultStorage(vaultEconomy);
            return this;
        }

        public Resource build() {
            if (resourceStorage == null) {
                throw new IllegalStateException(
                        "Storage is not set! Use .batched() or .customInterface()."
                );
            }
            return new Resource(this);
        }
    }
}