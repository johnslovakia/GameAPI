package cz.johnslovakia.gameapi.modules.cosmetics.defaultCosmetics;

import com.cryptomorin.xseries.XMaterial;
import cz.johnslovakia.gameapi.Shared;
import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.cosmetics.Cosmetic;
import cz.johnslovakia.gameapi.modules.cosmetics.CosmeticRarity;
import cz.johnslovakia.gameapi.modules.cosmetics.CosmeticsCategory;
import cz.johnslovakia.gameapi.modules.cosmetics.CosmeticsModule;
import cz.johnslovakia.gameapi.modules.resources.ResourcesModule;
import cz.johnslovakia.gameapi.utils.Utils;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

public class KillSoundsCategory extends CosmeticsCategory implements Listener {

    public KillSoundsCategory(CosmeticsModule manager) {
        super("Kill Sounds", new ItemStack(Material.MUSIC_DISC_5));

        FileConfiguration config = Shared.getInstance().getPlugin().getConfig();

        int LEGENDARY_COINS_PRICE = Utils.getPrice(config, "kill_sounds.legendary", 18000);
        int EPIC_COINS_PRICE = Utils.getPrice(config, "kill_sounds.epic", 14000);
        int RARE_COINS_PRICE = Utils.getPrice(config, "kill_sounds.rare", 8000);
        int UNCOMMON_COINS_PRICE = Utils.getPrice(config, "kill_sounds.uncommon", 6000);
        int COMMON_COINS_PRICE = Utils.getPrice(config, "kill_sounds.common", 4000);

        int LEGENDARY_TOKEN_PRICE = 7;
        int EPIC_TOKEN_PRICE = 5;
        int RARE_TOKEN_PRICE = 4;
        int UNCOMMON_TOKEN_PRICE = 3;
        int COMMON_TOKEN_PRICE = 2;

        Cosmetic explodeSoundCosmetic = new Cosmetic("Explode Sound", new ItemStack(Material.TNT), CosmeticRarity.RARE)
                .addCost(manager.getMainResource(), RARE_COINS_PRICE)
                .addCost(ModuleManager.getModule(ResourcesModule.class).getResourceByName("CosmeticTokens"), RARE_TOKEN_PRICE)
                .setPreviewConsumer(gamePlayer -> gamePlayer.getOnlinePlayer().playSound(gamePlayer.getOnlinePlayer(), Sound.ENTITY_GENERIC_EXPLODE, 1F, 1F));
        Cosmetic anvilLandSoundCosmetic = new Cosmetic("Anvil Land Sound", new ItemStack(Material.ANVIL), CosmeticRarity.COMMON)
                .addCost(manager.getMainResource(), COMMON_COINS_PRICE)
                .addCost(ModuleManager.getModule(ResourcesModule.class).getResourceByName("CosmeticTokens"), COMMON_TOKEN_PRICE)
                .setPreviewConsumer(gamePlayer -> gamePlayer.getOnlinePlayer().playSound(gamePlayer.getOnlinePlayer(), Sound.BLOCK_ANVIL_LAND, 1F, 1F));
        Cosmetic glassSoundCosmetic = new Cosmetic("Glass Sound", new ItemStack(Material.GLASS), CosmeticRarity.COMMON)
                .addCost(manager.getMainResource(), COMMON_COINS_PRICE)
                .addCost(ModuleManager.getModule(ResourcesModule.class).getResourceByName("CosmeticTokens"), COMMON_TOKEN_PRICE)
                .setAsPurchasable()
                .setPreviewConsumer(gamePlayer -> gamePlayer.getOnlinePlayer().playSound(gamePlayer.getOnlinePlayer(), Sound.BLOCK_GLASS_BREAK, 1F, 1F));
        Cosmetic eggPopSoundCosmetic = new Cosmetic("Egg Pop Sound", new ItemStack(Material.EGG), CosmeticRarity.COMMON)
                .addCost(manager.getMainResource(), COMMON_COINS_PRICE)
                .addCost(ModuleManager.getModule(ResourcesModule.class).getResourceByName("CosmeticTokens"), COMMON_TOKEN_PRICE)
                .setAsPurchasable()
                .setPreviewConsumer(gamePlayer -> gamePlayer.getOnlinePlayer().playSound(gamePlayer.getOnlinePlayer(), Sound.BLOCK_SNIFFER_EGG_PLOP, 1F, 1F));
        Cosmetic woofSoundCosmetic = new Cosmetic("Wolf Howl Sound", new ItemStack(XMaterial.WOLF_SPAWN_EGG.parseMaterial()), CosmeticRarity.EPIC)
                .addCost(manager.getMainResource(), EPIC_COINS_PRICE)
                .addCost(ModuleManager.getModule(ResourcesModule.class).getResourceByName("CosmeticTokens"), EPIC_TOKEN_PRICE)
                .setPreviewConsumer(gamePlayer -> gamePlayer.getOnlinePlayer().playSound(gamePlayer.getOnlinePlayer(), Sound.ENTITY_WOLF_AMBIENT, 1F, 1F));
        Cosmetic swimSoundCosmetic = new Cosmetic("Swim Sound", new ItemStack(Material.WATER_BUCKET), CosmeticRarity.COMMON)
                .addCost(manager.getMainResource(), COMMON_COINS_PRICE)
                .addCost(ModuleManager.getModule(ResourcesModule.class).getResourceByName("CosmeticTokens"), COMMON_TOKEN_PRICE)
                .setPreviewConsumer(gamePlayer -> gamePlayer.getOnlinePlayer().playSound(gamePlayer.getOnlinePlayer(), Sound.ENTITY_PLAYER_SWIM, 1F, 1F));
        Cosmetic burpSoundCosmetic = new Cosmetic("Burp Sound", new ItemStack(Material.BREAD), CosmeticRarity.UNCOMMON)
                .addCost(manager.getMainResource(), UNCOMMON_COINS_PRICE)
                .addCost(ModuleManager.getModule(ResourcesModule.class).getResourceByName("CosmeticTokens"), UNCOMMON_TOKEN_PRICE)
                .setPreviewConsumer(gamePlayer -> gamePlayer.getOnlinePlayer().playSound(gamePlayer.getOnlinePlayer(), Sound.ENTITY_PLAYER_BURP, 1F, 1F));
        Cosmetic levelUpSoundCosmetic = new Cosmetic("Level Up Sound", new ItemStack(XMaterial.EXPERIENCE_BOTTLE.parseMaterial()), CosmeticRarity.COMMON)
                .addCost(manager.getMainResource(), COMMON_COINS_PRICE)
                .addCost(ModuleManager.getModule(ResourcesModule.class).getResourceByName("CosmeticTokens"), COMMON_TOKEN_PRICE)
                .setAsPurchasable()
                .setPreviewConsumer(gamePlayer -> gamePlayer.getOnlinePlayer().playSound(gamePlayer.getOnlinePlayer(), Sound.ENTITY_PLAYER_LEVELUP, 1F, 1F));
        Cosmetic dragonHitSoundCosmetic = new Cosmetic("Dragon Hit Sound", new ItemStack(Utils.getCustomHead("31d6e3e41145967b32fb63576c63e3057e63beceb73cea18d20fc8547b0b0645")), CosmeticRarity.RARE)
                .addCost(manager.getMainResource(), RARE_COINS_PRICE)
                .addCost(ModuleManager.getModule(ResourcesModule.class).getResourceByName("CosmeticTokens"), RARE_TOKEN_PRICE)
                .setPreviewConsumer(gamePlayer -> gamePlayer.getOnlinePlayer().playSound(gamePlayer.getOnlinePlayer(), Sound.ENTITY_ENDER_DRAGON_HURT, 1F, 1F));
        Cosmetic blazeDeathSoundCosmetic = new Cosmetic("Blaze Death Sound", new ItemStack(XMaterial.GOLDEN_SWORD.parseMaterial()), CosmeticRarity.UNCOMMON)
                .addCost(manager.getMainResource(), UNCOMMON_COINS_PRICE)
                .addCost(ModuleManager.getModule(ResourcesModule.class).getResourceByName("CosmeticTokens"), UNCOMMON_TOKEN_PRICE)
                .setPreviewConsumer(gamePlayer -> gamePlayer.getOnlinePlayer().playSound(gamePlayer.getOnlinePlayer(), Sound.ENTITY_BLAZE_DEATH, 1F, 1F));
        Cosmetic villagerSoundCosmetic = new Cosmetic("Villager Sound", new ItemStack(XMaterial.VILLAGER_SPAWN_EGG.parseMaterial()), CosmeticRarity.COMMON)
                .addCost(manager.getMainResource(), COMMON_COINS_PRICE)
                .addCost(ModuleManager.getModule(ResourcesModule.class).getResourceByName("CosmeticTokens"), COMMON_TOKEN_PRICE)
                .setPreviewConsumer(gamePlayer -> gamePlayer.getOnlinePlayer().playSound(gamePlayer.getOnlinePlayer(), Sound.ENTITY_VILLAGER_AMBIENT, 1F, 1F));
        Cosmetic meowSoundCosmetic = new Cosmetic("Meow Sound", new ItemStack(Utils.getCustomHead("13df83a5cdab5a6143d1127e26369636881a194dd3199df5aa9123a4d33ad58a")), CosmeticRarity.COMMON)
                .addCost(manager.getMainResource(), COMMON_COINS_PRICE)
                .addCost(ModuleManager.getModule(ResourcesModule.class).getResourceByName("CosmeticTokens"), COMMON_TOKEN_PRICE)
                .setAsPurchasable()
                .setPreviewConsumer(gamePlayer -> gamePlayer.getOnlinePlayer().playSound(gamePlayer.getOnlinePlayer(), Sound.ENTITY_CAT_AMBIENT, 1F, 1F));
        Cosmetic ghastMoanSoundCosmetic = new Cosmetic("Ghast Moan Sound", new ItemStack(XMaterial.GHAST_SPAWN_EGG.parseMaterial()), CosmeticRarity.COMMON)
                .addCost(manager.getMainResource(), COMMON_COINS_PRICE)
                .addCost(ModuleManager.getModule(ResourcesModule.class).getResourceByName("CosmeticTokens"), COMMON_TOKEN_PRICE)
                .setPreviewConsumer(gamePlayer -> gamePlayer.getOnlinePlayer().playSound(gamePlayer.getOnlinePlayer(), Sound.ENTITY_GHAST_SCREAM, 1F, 1F));
        Cosmetic witherSoundCosmetic = new Cosmetic("Wither Sound", new ItemStack(XMaterial.WITHER_SKELETON_SKULL.parseMaterial()), CosmeticRarity.EPIC)
                .addCost(manager.getMainResource(), EPIC_COINS_PRICE)
                .addCost(ModuleManager.getModule(ResourcesModule.class).getResourceByName("CosmeticTokens"), EPIC_TOKEN_PRICE)
                .setPreviewConsumer(gamePlayer -> gamePlayer.getOnlinePlayer().playSound(gamePlayer.getOnlinePlayer(), Sound.ENTITY_WITHER_DEATH, 1F, 1F));

        addCosmetic(explodeSoundCosmetic, anvilLandSoundCosmetic, glassSoundCosmetic, eggPopSoundCosmetic, woofSoundCosmetic, swimSoundCosmetic, burpSoundCosmetic, levelUpSoundCosmetic,
                dragonHitSoundCosmetic, blazeDeathSoundCosmetic, villagerSoundCosmetic, meowSoundCosmetic, ghastMoanSoundCosmetic, witherSoundCosmetic);
    }
}
