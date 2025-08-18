package cz.johnslovakia.gameapi.game.cosmetics.defaultCosmetics;

import com.cryptomorin.xseries.XMaterial;
import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.game.cosmetics.*;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.users.KillMessage;
import cz.johnslovakia.gameapi.users.PlayerManager;
import cz.johnslovakia.gameapi.utils.Utils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class KillMessagesCategory extends CosmeticsCategory {

    public KillMessagesCategory() {
        super("Kill Messages", new ItemStack(Material.OAK_SIGN));


        FileConfiguration config = Minigame.getInstance().getPlugin().getConfig();

        int LEGENDARY_PRICE = Utils.getPrice(config, "kill_messages.legendary", 18000);
        int EPIC_PRICE = Utils.getPrice(config, "kill_messages.epic", 14000);
        int RARE_PRICE = Utils.getPrice(config, "kill_messages.rare", 8000);
        int UNCOMMON_PRICE = Utils.getPrice(config, "kill_messages.uncommon", 6000);
        int COMMON_PRICE = Utils.getPrice(config, "kill_messages.common", 4000);

        Cosmetic greekMythology = new Cosmetic("Greek Mythology Themed", new ItemStack(Material.GOLDEN_SWORD), RARE_PRICE, CosmeticRarity.RARE)
                .setSelectConsumer(gamePlayer -> {
                    KillMessage killMessage = new KillMessage(gamePlayer);
                    killMessage.addMessage(EntityDamageEvent.DamageCause.KILL, "chat.kill_message.greek_mythology_themed.melee");
                    killMessage.addMessage(EntityDamageEvent.DamageCause.FALL, "chat.kill_message.greek_mythology_themed.fall");
                    killMessage.addMessage(EntityDamageEvent.DamageCause.VOID, "chat.kill_message.greek_mythology_themed.void");
                    killMessage.addMessage(EntityDamageEvent.DamageCause.PROJECTILE, "chat.kill_message.greek_mythology_themed.ranged");

                    gamePlayer.getPlayerData().setKillMessage(killMessage);
                });

        Cosmetic dragon = new Cosmetic("Dragon", new ItemStack(GameAPI.getInstance().getVersionSupport().getCustomHead("31d6e3e41145967b32fb63576c63e3057e63beceb73cea18d20fc8547b0b0645")), EPIC_PRICE, CosmeticRarity.EPIC)
                .setSelectConsumer(gamePlayer -> {
                    KillMessage killMessage = new KillMessage(gamePlayer);
                    killMessage.addMessage(EntityDamageEvent.DamageCause.KILL, "chat.kill_message.dragon.melee");
                    killMessage.addMessage(EntityDamageEvent.DamageCause.FALL, "chat.kill_message.dragon.fall");
                    killMessage.addMessage(EntityDamageEvent.DamageCause.VOID, "chat.kill_message.dragon.void");
                    killMessage.addMessage(EntityDamageEvent.DamageCause.PROJECTILE, "chat.kill_message.dragon.ranged");

                    gamePlayer.getPlayerData().setKillMessage(killMessage);
                });

        Cosmetic toilet = new Cosmetic("Toilet", new ItemStack(Material.HOPPER), RARE_PRICE, CosmeticRarity.RARE)
                .setSelectConsumer(gamePlayer -> {
                    KillMessage killMessage = new KillMessage(gamePlayer);
                    killMessage.addMessage(EntityDamageEvent.DamageCause.KILL, "chat.kill_message.toilet.melee");
                    killMessage.addMessage(EntityDamageEvent.DamageCause.FALL, "chat.kill_message.toilet.fall");
                    killMessage.addMessage(EntityDamageEvent.DamageCause.VOID, "chat.kill_message.toilet.void");
                    killMessage.addMessage(EntityDamageEvent.DamageCause.PROJECTILE, "chat.kill_message.toilet.ranged");

                    gamePlayer.getPlayerData().setKillMessage(killMessage);
                });

        Cosmetic glorious = new Cosmetic("Glorious", new ItemStack(Material.GOLD_BLOCK), LEGENDARY_PRICE, CosmeticRarity.LEGENDARY)
                .setSelectConsumer(gamePlayer -> {
                    KillMessage killMessage = new KillMessage(gamePlayer);
                    killMessage.addMessage(EntityDamageEvent.DamageCause.KILL, "chat.kill_message.glorious.melee");
                    killMessage.addMessage(EntityDamageEvent.DamageCause.FALL, "chat.kill_message.glorious.fall");
                    killMessage.addMessage(EntityDamageEvent.DamageCause.VOID, "chat.kill_message.glorious.void");
                    killMessage.addMessage(EntityDamageEvent.DamageCause.PROJECTILE, "chat.kill_message.glorious.ranged");

                    gamePlayer.getPlayerData().setKillMessage(killMessage);
                });

        Cosmetic wizard = new Cosmetic("Wizard", new ItemStack(Material.BLAZE_ROD), RARE_PRICE, CosmeticRarity.RARE)
                .setSelectConsumer(gamePlayer -> {
                    KillMessage killMessage = new KillMessage(gamePlayer);
                    killMessage.addMessage(EntityDamageEvent.DamageCause.KILL, "chat.kill_message.wizard.melee");
                    killMessage.addMessage(EntityDamageEvent.DamageCause.FALL, "chat.kill_message.wizard.fall");
                    killMessage.addMessage(EntityDamageEvent.DamageCause.VOID, "chat.kill_message.wizard.void");
                    killMessage.addMessage(EntityDamageEvent.DamageCause.PROJECTILE, "chat.kill_message.wizard.ranged");

                    gamePlayer.getPlayerData().setKillMessage(killMessage);
                });

        Cosmetic ninja = new Cosmetic("Ninja", new ItemStack(Material.NETHER_STAR), 8000, CosmeticRarity.EPIC)
                .setSelectConsumer(gamePlayer -> {
                    KillMessage killMessage = new KillMessage(gamePlayer);
                    killMessage.addMessage(EntityDamageEvent.DamageCause.KILL, "chat.kill_message.ninja.melee");
                    killMessage.addMessage(EntityDamageEvent.DamageCause.FALL, "chat.kill_message.ninja.fall");
                    killMessage.addMessage(EntityDamageEvent.DamageCause.VOID, "chat.kill_message.ninja.void");
                    killMessage.addMessage(EntityDamageEvent.DamageCause.PROJECTILE, "chat.kill_message.ninja.ranged");

                    gamePlayer.getPlayerData().setKillMessage(killMessage);
                });

        Cosmetic genAlpha = new Cosmetic("Gen Alpha", GameAPI.getInstance().getVersionSupport().getCustomHead("760cdc9a43b1cd35bbe47ab50fd25faa7cd218bc7ec66e2227df664572748cc4"), LEGENDARY_PRICE, CosmeticRarity.LEGENDARY)
                .setSelectConsumer(gamePlayer -> {
                    KillMessage killMessage = new KillMessage(gamePlayer);
                    killMessage.addMessage(EntityDamageEvent.DamageCause.KILL, "chat.kill_message.gen_alpha.melee");
                    killMessage.addMessage(EntityDamageEvent.DamageCause.FALL, "chat.kill_message.gen_alpha.fall");
                    killMessage.addMessage(EntityDamageEvent.DamageCause.VOID, "chat.kill_message.gen_alpha.void");
                    killMessage.addMessage(EntityDamageEvent.DamageCause.PROJECTILE, "chat.kill_message.gen_alpha.ranged");

                    gamePlayer.getPlayerData().setKillMessage(killMessage);
                });

        Cosmetic enchanted = new Cosmetic("Enchanted", new ItemStack(Material.ENCHANTING_TABLE), UNCOMMON_PRICE, CosmeticRarity.UNCOMMON)
                .setAsPurchasable()
                .setSelectConsumer(gamePlayer -> {
                    KillMessage killMessage = new KillMessage(gamePlayer);
                    killMessage.addMessage(EntityDamageEvent.DamageCause.KILL, "chat.kill_message.enchanted.melee");
                    killMessage.addMessage(EntityDamageEvent.DamageCause.FALL, "chat.kill_message.enchanted.fall");
                    killMessage.addMessage(EntityDamageEvent.DamageCause.VOID, "chat.kill_message.enchanted.void");
                    killMessage.addMessage(EntityDamageEvent.DamageCause.PROJECTILE, "chat.kill_message.enchanted.ranged");

                    gamePlayer.getPlayerData().setKillMessage(killMessage);
                });

        Cosmetic dramatic = new Cosmetic("Dramatic", new ItemStack(Material.INK_SAC), COMMON_PRICE, CosmeticRarity.COMMON)
                .setAsPurchasable()
                .setSelectConsumer(gamePlayer -> {
                    KillMessage killMessage = new KillMessage(gamePlayer);
                    killMessage.addMessage(EntityDamageEvent.DamageCause.KILL, "chat.kill_message.dramatic.melee");
                    killMessage.addMessage(EntityDamageEvent.DamageCause.FALL, "chat.kill_message.dramatic.fall");
                    killMessage.addMessage(EntityDamageEvent.DamageCause.VOID, "chat.kill_message.dramatic.void");
                    killMessage.addMessage(EntityDamageEvent.DamageCause.PROJECTILE, "chat.kill_message.dramatic.ranged");

                    gamePlayer.getPlayerData().setKillMessage(killMessage);
                });

        Cosmetic swiftie = new Cosmetic("Swiftie", new ItemStack(Material.DIAMOND), LEGENDARY_PRICE, CosmeticRarity.LEGENDARY)
                .setSelectConsumer(gamePlayer -> {
                    KillMessage killMessage = new KillMessage(gamePlayer);
                    killMessage.addMessage(EntityDamageEvent.DamageCause.KILL, "chat.kill_message.swiftie.melee");
                    killMessage.addMessage(EntityDamageEvent.DamageCause.FALL, "chat.kill_message.swiftie.fall");
                    killMessage.addMessage(EntityDamageEvent.DamageCause.VOID, "chat.kill_message.swiftie.void");
                    killMessage.addMessage(EntityDamageEvent.DamageCause.PROJECTILE, "chat.kill_message.swiftie.ranged");

                    gamePlayer.getPlayerData().setKillMessage(killMessage);
                });

        greekMythology.setPreviewConsumer(gamePlayer -> sendKillMessagePreview(gamePlayer.getOnlinePlayer(), greekMythology));
        dragon.setPreviewConsumer(gamePlayer -> sendKillMessagePreview(gamePlayer.getOnlinePlayer(), dragon));
        toilet.setPreviewConsumer(gamePlayer -> sendKillMessagePreview(gamePlayer.getOnlinePlayer(), toilet));
        glorious.setPreviewConsumer(gamePlayer -> sendKillMessagePreview(gamePlayer.getOnlinePlayer(), glorious));
        wizard.setPreviewConsumer(gamePlayer -> sendKillMessagePreview(gamePlayer.getOnlinePlayer(), wizard));
        ninja.setPreviewConsumer(gamePlayer -> sendKillMessagePreview(gamePlayer.getOnlinePlayer(), ninja));
        genAlpha.setPreviewConsumer(gamePlayer -> sendKillMessagePreview(gamePlayer.getOnlinePlayer(), genAlpha));
        enchanted.setPreviewConsumer(gamePlayer -> sendKillMessagePreview(gamePlayer.getOnlinePlayer(), enchanted));
        dramatic.setPreviewConsumer(gamePlayer -> sendKillMessagePreview(gamePlayer.getOnlinePlayer(), dramatic));
        swiftie.setPreviewConsumer(gamePlayer -> sendKillMessagePreview(gamePlayer.getOnlinePlayer(), swiftie));

        addCosmetic(greekMythology, dragon, toilet, glorious, wizard, ninja, genAlpha, enchanted, dramatic, swiftie);
    }

    private static void sendKillMessagePreview(Player player, Cosmetic cosmetic){
        String messageName = cosmetic.getName().toLowerCase().replaceAll(" ", "_");

        player.sendMessage("");
        player.sendMessage("§fKill Messages §7- §a" + cosmetic.getName() + " §8(Chat Messages)");
        player.sendMessage(" §aMelee Kill: " + MessageManager.get(player, "chat.kill_message." + messageName + ".melee").replace("%player_color%", "§c").replace("%dead%", "§cPlayer").replace("%killer_color%", "§c").replace("%killer%", "§cKiller").getTranslated());
        player.sendMessage(" §aFall Damage Kill: " + MessageManager.get(player, "chat.kill_message." + messageName + ".fall").replace("%player_color%", "§c").replace("%dead%", "§cPlayer").replace("%killer_color%", "§c").replace("%killer%", "§cKiller").getTranslated());
        player.sendMessage(" §aVoid Kill: " + MessageManager.get(player, "chat.kill_message." + messageName + ".void").replace("%player_color%", "§c").replace("%dead%", "§cPlayer").replace("%killer_color%", "§c").replace("%killer%", "§cKiller").getTranslated());
        player.sendMessage(" §aRanged Kill: " + MessageManager.get(player, "chat.kill_message." + messageName + ".ranged").replace("%player_color%", "§c").replace("%dead%", "§cPlayer").replace("%killer_color%", "§c").replace("%killer%", "§cKiller").getTranslated());
        player.sendMessage("");
    }
}
