package cz.johnslovakia.gameapi.modules.cosmetics.defaultCosmetics;

import cz.johnslovakia.gameapi.Shared;
import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.cosmetics.Cosmetic;
import cz.johnslovakia.gameapi.modules.cosmetics.CosmeticRarity;
import cz.johnslovakia.gameapi.modules.cosmetics.CosmeticsCategory;
import cz.johnslovakia.gameapi.modules.cosmetics.CosmeticsModule;
import cz.johnslovakia.gameapi.modules.resources.ResourcesModule;
import cz.johnslovakia.gameapi.utils.ItemBuilder;
import cz.johnslovakia.gameapi.utils.Utils;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Listener;

public class HatsCategory extends CosmeticsCategory implements Listener {

    public HatsCategory(CosmeticsModule manager) {
        super("Hats", new ItemBuilder(Material.CARVED_PUMPKIN)
                .setCustomModelData(1).toItemStack());

        FileConfiguration config = Shared.getInstance().getPlugin().getConfig();
        
        int LEGENDARY_COINS_PRICE = Utils.getPrice(config, "hats.legendary", 18000);
        int EPIC_COINS_PRICE = Utils.getPrice(config, "hats.epic", 14000);
        int RARE_COINS_PRICE = Utils.getPrice(config, "hats.rare", 10000);
        int UNCOMMON_COINS_PRICE = Utils.getPrice(config, "hats.uncommon", 8000);
        int COMMON_COINS_PRICE = Utils.getPrice(config, "hats.common", 6000);

        int LEGENDARY_TOKEN_PRICE = 7;
        int EPIC_TOKEN_PRICE = 5;
        int RARE_TOKEN_PRICE = 4;
        int UNCOMMON_TOKEN_PRICE = 3;
        int COMMON_TOKEN_PRICE = 2;

        Cosmetic classic = new Cosmetic("Classic", new ItemBuilder(Material.CARVED_PUMPKIN)
                .setCustomModelData(1).toItemStack(), CosmeticRarity.COMMON)
                .addCost(manager.getMainResource(), COMMON_COINS_PRICE)
                .addCost(ModuleManager.getModule(ResourcesModule.class).getResourceByName("CosmeticTokens"), COMMON_TOKEN_PRICE)
                .setAsPurchasable()
                .setSelectConsumer(gamePlayer -> {
                    ItemBuilder item = new ItemBuilder(Material.CARVED_PUMPKIN)
                            .setName("§eClassic")
                            .setCustomModelData(1);

                    gamePlayer.getOnlinePlayer().getInventory().setHelmet(item.toItemStack());
                });
        Cosmetic headphones = new Cosmetic("Headphones", new ItemBuilder(Material.CARVED_PUMPKIN)
                .setCustomModelData(2).toItemStack(), CosmeticRarity.COMMON)
                .addCost(manager.getMainResource(), COMMON_COINS_PRICE)
                .addCost(ModuleManager.getModule(ResourcesModule.class).getResourceByName("CosmeticTokens"), COMMON_TOKEN_PRICE)
                .setAsPurchasable()
                .setSelectConsumer(gamePlayer -> {
                    ItemBuilder item = new ItemBuilder(Material.CARVED_PUMPKIN)
                            .setName("§eHeadphones")
                            .setCustomModelData(2);

                    gamePlayer.getOnlinePlayer().getInventory().setHelmet(item.toItemStack());
                });
        Cosmetic straw = new Cosmetic("Straw Hat", new ItemBuilder(Material.CARVED_PUMPKIN)
                .setCustomModelData(3).toItemStack(), CosmeticRarity.UNCOMMON)
                .addCost(manager.getMainResource(), UNCOMMON_COINS_PRICE)
                .addCost(ModuleManager.getModule(ResourcesModule.class).getResourceByName("CosmeticTokens"), UNCOMMON_TOKEN_PRICE)
                .setSelectConsumer(gamePlayer -> {
                    ItemBuilder item = new ItemBuilder(Material.CARVED_PUMPKIN)
                            .setName("§eStraw Hat")
                            .setCustomModelData(3);

                    gamePlayer.getOnlinePlayer().getInventory().setHelmet(item.toItemStack());
                });
        Cosmetic miner = new Cosmetic("Miner's Helmet", new ItemBuilder(Material.CARVED_PUMPKIN)
                .setCustomModelData(4).toItemStack(), CosmeticRarity.UNCOMMON)
                .addCost(manager.getMainResource(), UNCOMMON_COINS_PRICE)
                .addCost(ModuleManager.getModule(ResourcesModule.class).getResourceByName("CosmeticTokens"), UNCOMMON_TOKEN_PRICE)
                .setAsPurchasable()
                .setSelectConsumer(gamePlayer -> {
                    ItemBuilder item = new ItemBuilder(Material.CARVED_PUMPKIN)
                            .setName("§eMiner's Helmet")
                            .setCustomModelData(4);

                    gamePlayer.getOnlinePlayer().getInventory().setHelmet(item.toItemStack());
                });
        Cosmetic aureole = new Cosmetic("Aureole", new ItemBuilder(Material.CARVED_PUMPKIN)
                .setCustomModelData(5).toItemStack(), CosmeticRarity.RARE)
                .addCost(manager.getMainResource(), RARE_COINS_PRICE)
                .addCost(ModuleManager.getModule(ResourcesModule.class).getResourceByName("CosmeticTokens"), RARE_TOKEN_PRICE)
                .setSelectConsumer(gamePlayer -> {
                    ItemBuilder item = new ItemBuilder(Material.CARVED_PUMPKIN)
                            .setName("§eAureole")
                            .setCustomModelData(5);

                    gamePlayer.getOnlinePlayer().getInventory().setHelmet(item.toItemStack());
                });
        Cosmetic devilhorns = new Cosmetic("Devil Horns", new ItemBuilder(Material.CARVED_PUMPKIN)
                .setCustomModelData(6).toItemStack(), CosmeticRarity.EPIC)
                .addCost(manager.getMainResource(), EPIC_COINS_PRICE)
                .addCost(ModuleManager.getModule(ResourcesModule.class).getResourceByName("CosmeticTokens"), EPIC_TOKEN_PRICE)
                .setSelectConsumer(gamePlayer -> {
                    ItemBuilder item = new ItemBuilder(Material.CARVED_PUMPKIN)
                            .setName("§eDevil Horns")
                            .setCustomModelData(6);

                    gamePlayer.getOnlinePlayer().getInventory().setHelmet(item.toItemStack());;
                });
        Cosmetic nosignal = new Cosmetic("No Signal TV", new ItemBuilder(Material.CARVED_PUMPKIN)
                .setCustomModelData(7).toItemStack(), CosmeticRarity.LEGENDARY)
                .addCost(manager.getMainResource(), LEGENDARY_COINS_PRICE)
                .addCost(ModuleManager.getModule(ResourcesModule.class).getResourceByName("CosmeticTokens"), LEGENDARY_TOKEN_PRICE)
                .setSelectConsumer(gamePlayer -> {
                    ItemBuilder item = new ItemBuilder(Material.CARVED_PUMPKIN)
                            .setName("§eNo Signal TV")
                            .setCustomModelData(7);

                    gamePlayer.getOnlinePlayer().getInventory().setHelmet(item.toItemStack());
                });
        Cosmetic viking = new Cosmetic("Viking's Helmet", new ItemBuilder(Material.CARVED_PUMPKIN)
                .setCustomModelData(8).toItemStack(), CosmeticRarity.RARE)
                .addCost(manager.getMainResource(), RARE_COINS_PRICE)
                .addCost(ModuleManager.getModule(ResourcesModule.class).getResourceByName("CosmeticTokens"), RARE_TOKEN_PRICE)
                .setSelectConsumer(gamePlayer -> {
                    ItemBuilder item = new ItemBuilder(Material.CARVED_PUMPKIN)
                            .setName("§eViking's Helmet")
                            .setCustomModelData(8);

                    gamePlayer.getOnlinePlayer().getInventory().setHelmet(item.toItemStack());
                });
        Cosmetic clown = new Cosmetic("Clown Hat", new ItemBuilder(Material.CARVED_PUMPKIN)
                .setCustomModelData(9).toItemStack(), CosmeticRarity.RARE)
                .addCost(manager.getMainResource(), RARE_COINS_PRICE)
                .addCost(ModuleManager.getModule(ResourcesModule.class).getResourceByName("CosmeticTokens"), RARE_TOKEN_PRICE)
                .setSelectConsumer(gamePlayer -> {
                    ItemBuilder item = new ItemBuilder(Material.CARVED_PUMPKIN)
                            .setName("§eClown Hat")
                            .setCustomModelData(9);

                    gamePlayer.getOnlinePlayer().getInventory().setHelmet(item.toItemStack());
                });
        Cosmetic santa = new Cosmetic("Santa Hat", new ItemBuilder(Material.CARVED_PUMPKIN)
                .setCustomModelData(10).toItemStack(), CosmeticRarity.RARE)
                .addCost(manager.getMainResource(), RARE_COINS_PRICE)
                .addCost(ModuleManager.getModule(ResourcesModule.class).getResourceByName("CosmeticTokens"), RARE_TOKEN_PRICE)
                .setSelectConsumer(gamePlayer -> {
                    ItemBuilder item = new ItemBuilder(Material.CARVED_PUMPKIN)
                            .setName("§eSanta Hat")
                            .setCustomModelData(10);

                    gamePlayer.getOnlinePlayer().getInventory().setHelmet(item.toItemStack());
                });
        Cosmetic witch = new Cosmetic("Witch Hat", new ItemBuilder(Material.CARVED_PUMPKIN)
                .setCustomModelData(11).toItemStack(), CosmeticRarity.EPIC)
                .addCost(manager.getMainResource(), EPIC_COINS_PRICE)
                .addCost(ModuleManager.getModule(ResourcesModule.class).getResourceByName("CosmeticTokens"), EPIC_TOKEN_PRICE)
                .setSelectConsumer(gamePlayer -> {
                    ItemBuilder item = new ItemBuilder(Material.CARVED_PUMPKIN)
                            .setName("§eWitch Hat")
                            .setCustomModelData(11);

                    gamePlayer.getOnlinePlayer().getInventory().setHelmet(item.toItemStack());
                });
        Cosmetic pirate = new Cosmetic("Pirate Hat", new ItemBuilder(Material.CARVED_PUMPKIN)
                .setCustomModelData(12).toItemStack(), CosmeticRarity.EPIC)
                .addCost(manager.getMainResource(), EPIC_COINS_PRICE)
                .addCost(ModuleManager.getModule(ResourcesModule.class).getResourceByName("CosmeticTokens"), EPIC_TOKEN_PRICE)
                .setSelectConsumer(gamePlayer -> {
                    ItemBuilder item = new ItemBuilder(Material.CARVED_PUMPKIN)
                            .setName("§ePirate Hat")
                            .setCustomModelData(12);

                    gamePlayer.getOnlinePlayer().getInventory().setHelmet(item.toItemStack());
                });

        addCosmetic(classic, headphones, straw, miner, aureole, devilhorns, nosignal, viking, clown, santa, witch, pirate);
    }
}
