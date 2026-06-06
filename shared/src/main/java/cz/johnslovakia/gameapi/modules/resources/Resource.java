package cz.johnslovakia.gameapi.modules.resources;

import cz.johnslovakia.gameapi.modules.resources.storage.BatchedStorage;
import cz.johnslovakia.gameapi.modules.resources.storage.DeferredVaultStorage;
import cz.johnslovakia.gameapi.modules.resources.storage.JsonResourceStorage;
import cz.johnslovakia.gameapi.modules.resources.storage.ResourceStorage;
import cz.johnslovakia.gameapi.modules.resources.storage.VaultStorage;

import lombok.Getter;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;
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
    private final ResourceDefinition definition;

    private Resource(Builder builder) {
        this.name = builder.name;
        this.displayName = builder.displayName;
        this.color = builder.color;
        this.rank = builder.rank;
        this.firstDailyWinReward = builder.firstDailyWinReward;
        this.applicableBonus = builder.applicableBonus;
        this.imgChar = builder.imgChar;
        this.resourceInterface = builder.resourceStorage;
        this.definition = builder.createDefinition();
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
        private boolean publishedDefinition = false;
        private String definitionStorage;
        private String definitionTableName;
        private final java.util.List<String> migrateFromBatchedTables = new java.util.ArrayList<>();

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
            this.publishedDefinition = false;
            this.definitionStorage = null;
            this.definitionTableName = null;
            return this;
        }

        public Builder batched(String tableName) {
            this.resourceStorage = new BatchedStorage(getName(), tableName);
            this.publishedDefinition = false;
            this.definitionStorage = null;
            this.definitionTableName = null;
            return this;
        }

        public Builder global() {
            this.resourceStorage = new JsonResourceStorage(getName(), migrateFromBatchedTables);
            this.publishedDefinition = true;
            this.definitionStorage = ResourceDefinition.STORAGE_PLAYER_TABLE_JSON;
            this.definitionTableName = null;
            return this;
        }

        public Builder migrateFromBatched(String... tableNames) {
            if (tableNames == null) return this;

            for (String tableName : tableNames) {
                if (tableName != null && !tableName.isBlank() && !migrateFromBatchedTables.contains(tableName)) {
                    migrateFromBatchedTables.add(tableName);
                }
            }

            if (resourceStorage instanceof JsonResourceStorage jsonResourceStorage) {
                jsonResourceStorage.addLegacyBatchedTables(migrateFromBatchedTables);
            }
            return this;
        }

        public Builder vault(Economy vaultEconomy) {
            this.resourceStorage = new VaultStorage(vaultEconomy);
            this.publishedDefinition = false;
            this.definitionStorage = null;
            this.definitionTableName = null;
            return this;
        }

        public Builder deferredVault(String tableName, JavaPlugin plugin) {
            this.resourceStorage = new DeferredVaultStorage(getName(), tableName, plugin);
            this.publishedDefinition = true;
            this.definitionStorage = ResourceDefinition.STORAGE_DEFERRED_VAULT;
            this.definitionTableName = tableName;
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

        private ResourceDefinition createDefinition() {
            if (!publishedDefinition) return null;

            return new ResourceDefinition(
                    name,
                    displayName,
                    color,
                    rank,
                    firstDailyWinReward,
                    applicableBonus,
                    imgChar,
                    definitionStorage,
                    definitionTableName,
                    migrateFromBatchedTables
            );
        }
    }
}
