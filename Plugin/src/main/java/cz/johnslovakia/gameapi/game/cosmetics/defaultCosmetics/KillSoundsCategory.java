package cz.johnslovakia.gameapi.game.cosmetics.defaultCosmetics;

import com.cryptomorin.xseries.XMaterial;
import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.events.GamePlayerDeathEvent;
import cz.johnslovakia.gameapi.game.cosmetics.*;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.utils.Sounds;
import cz.johnslovakia.gameapi.utils.Utils;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

public class KillSoundsCategory extends CosmeticsCategory implements Listener {

    public KillSoundsCategory() {
        super("Kill Sounds", new ItemStack(Material.MUSIC_DISC_5));

        FileConfiguration config = GameAPI.getInstance().getMinigame().getPlugin().getConfig();
        
        int LEGENDARY_PRICE = Utils.getPrice(config, "kill_sounds.legendary", 18000);
        int EPIC_PRICE = Utils.getPrice(config, "kill_sounds.epic", 14000);
        int RARE_PRICE = Utils.getPrice(config, "kill_sounds.rare", 8000);
        int UNCOMMON_PRICE = Utils.getPrice(config, "kill_sounds.uncommon", 6000);
        int COMMON_PRICE = Utils.getPrice(config, "kill_sounds.common", 4000);

        Cosmetic explodeSoundCosmetic = new Cosmetic("Explode Sound", new ItemStack(Material.TNT), RARE_PRICE, CosmeticRarity.RARE)
                .setPreviewConsumer(gamePlayer -> Sounds.EXPLODE.playSound(gamePlayer.getOnlinePlayer()));
        Cosmetic anvilLandSoundCosmetic = new Cosmetic("Anvil Land Sound", new ItemStack(Material.ANVIL), COMMON_PRICE, CosmeticRarity.COMMON)
                .setPreviewConsumer(gamePlayer -> Sounds.ANVIL_LAND.playSound(gamePlayer.getOnlinePlayer()));
        Cosmetic glassSoundCosmetic = new Cosmetic("Glass Sound", new ItemStack(Material.GLASS), COMMON_PRICE, CosmeticRarity.COMMON)
                .setPreviewConsumer(gamePlayer -> Sounds.GLASS.playSound(gamePlayer.getOnlinePlayer()));
        Cosmetic eggPopSoundCosmetic = new Cosmetic("Egg Pop Sound", new ItemStack(Material.EGG), COMMON_PRICE, CosmeticRarity.COMMON)
                .setPreviewConsumer(gamePlayer -> Sounds.CHICKEN_EGG_POP.playSound(gamePlayer.getOnlinePlayer()));
        Cosmetic woofSoundCosmetic = new Cosmetic("Wolf Howl Sound", new ItemStack(XMaterial.WOLF_SPAWN_EGG.parseMaterial()), EPIC_PRICE, CosmeticRarity.EPIC)
                .setPreviewConsumer(gamePlayer -> Sounds.WOLF_HOWL.playSound(gamePlayer.getOnlinePlayer()));
        Cosmetic swimSoundCosmetic = new Cosmetic("Swim Sound", new ItemStack(Material.WATER_BUCKET), COMMON_PRICE, CosmeticRarity.COMMON)
                .setPreviewConsumer(gamePlayer -> Sounds.SWIM.playSound(gamePlayer.getOnlinePlayer()));
        Cosmetic burpSoundCosmetic = new Cosmetic("Burp Sound", new ItemStack(Material.BREAD), UNCOMMON_PRICE, CosmeticRarity.UNCOMMON)
                .setPreviewConsumer(gamePlayer -> Sounds.BURP.playSound(gamePlayer.getOnlinePlayer()));
        Cosmetic levelUpSoundCosmetic = new Cosmetic("Level Up Sound", new ItemStack(XMaterial.EXPERIENCE_BOTTLE.parseMaterial()), COMMON_PRICE, CosmeticRarity.COMMON)
                .setPreviewConsumer(gamePlayer -> Sounds.LEVEL_UP.playSound(gamePlayer.getOnlinePlayer()));
        Cosmetic dragonHitSoundCosmetic = new Cosmetic("Dragon Hit Sound", new ItemStack(GameAPI.getInstance().getVersionSupport().getCustomHead("31d6e3e41145967b32fb63576c63e3057e63beceb73cea18d20fc8547b0b0645")), RARE_PRICE, CosmeticRarity.RARE)
                .setPreviewConsumer(gamePlayer -> Sounds.ENDERDRAGON_HIT.playSound(gamePlayer.getOnlinePlayer()));
        Cosmetic blazeDeathSoundCosmetic = new Cosmetic("Blaze Death Sound", new ItemStack(XMaterial.GOLDEN_SWORD.parseMaterial()), UNCOMMON_PRICE, CosmeticRarity.UNCOMMON)
                .setPreviewConsumer(gamePlayer -> Sounds.BLAZE_DEATH.playSound(gamePlayer.getOnlinePlayer()));
        Cosmetic villagerSoundCosmetic = new Cosmetic("Villager Sound", new ItemStack(XMaterial.VILLAGER_SPAWN_EGG.parseMaterial()), COMMON_PRICE, CosmeticRarity.COMMON)
                .setPreviewConsumer(gamePlayer -> Sounds.VILLAGER_IDLE.playSound(gamePlayer.getOnlinePlayer()));
        Cosmetic meowSoundCosmetic = new Cosmetic("Meow Sound", new ItemStack(GameAPI.getInstance().getVersionSupport().getCustomHead("13df83a5cdab5a6143d1127e26369636881a194dd3199df5aa9123a4d33ad58a")), COMMON_PRICE, CosmeticRarity.COMMON)
                .setPreviewConsumer(gamePlayer -> Sounds.CAT_MEOW.playSound(gamePlayer.getOnlinePlayer()));
        Cosmetic ghastMoanSoundCosmetic = new Cosmetic("Ghast Moan Sound", new ItemStack(XMaterial.GHAST_SPAWN_EGG.parseMaterial()), COMMON_PRICE, CosmeticRarity.COMMON)
                .setPreviewConsumer(gamePlayer -> Sounds.GHAST_MOAN.playSound(gamePlayer.getOnlinePlayer()));
        Cosmetic witherSoundCosmetic = new Cosmetic("Wither Sound", new ItemStack(XMaterial.WITHER_SKELETON_SKULL.parseMaterial()), EPIC_PRICE, CosmeticRarity.EPIC)
                .setPreviewConsumer(gamePlayer -> Sounds.WITHER_DEATH.playSound(gamePlayer.getOnlinePlayer()));

        addCosmetic(explodeSoundCosmetic, anvilLandSoundCosmetic, glassSoundCosmetic, eggPopSoundCosmetic, woofSoundCosmetic, swimSoundCosmetic, burpSoundCosmetic, levelUpSoundCosmetic,
                dragonHitSoundCosmetic, blazeDeathSoundCosmetic, villagerSoundCosmetic, meowSoundCosmetic, ghastMoanSoundCosmetic, witherSoundCosmetic);


        Bukkit.getPluginManager().registerEvents(this, GameAPI.getInstance());
    }


    @EventHandler
    public void onGamePlayerDeath(GamePlayerDeathEvent e) {
        GamePlayer killer = e.getKiller();
        if (killer != null && getSelectedCosmetic(killer) != null){
            getSelectedCosmetic(killer).getPreviewConsumer().accept(e.getGamePlayer());
            getSelectedCosmetic(killer).getPreviewConsumer().accept(killer);
        }
    }
}
