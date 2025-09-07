package cz.johnslovakia.gameapi.game.cosmetics.defaultCosmetics;

import com.cryptomorin.xseries.XMaterial;
import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.game.cosmetics.*;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.users.KillMessage;
import cz.johnslovakia.gameapi.users.PlayerManager;
import cz.johnslovakia.gameapi.users.resources.ResourcesManager;
import cz.johnslovakia.gameapi.utils.Utils;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.damage.DamageType;
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

    public KillMessagesCategory(CosmeticsManager manager) {
        super("Kill Messages", new ItemStack(Material.OAK_SIGN));


        FileConfiguration config = Minigame.getInstance().getPlugin().getConfig();

        int LEGENDARY_COINS_PRICE = Utils.getPrice(config, "kill_messages.legendary", 18000);
        int EPIC_COINS_PRICE = Utils.getPrice(config, "kill_messages.epic", 14000);
        int RARE_COINS_PRICE = Utils.getPrice(config, "kill_messages.rare", 8000);
        int UNCOMMON_COINS_PRICE = Utils.getPrice(config, "kill_messages.uncommon", 6000);
        int COMMON_COINS_PRICE = Utils.getPrice(config, "kill_messages.common", 4000);

        int LEGENDARY_TOKEN_PRICE = 7;
        int EPIC_TOKEN_PRICE = 5;
        int RARE_TOKEN_PRICE = 4;
        int UNCOMMON_TOKEN_PRICE = 3;
        int COMMON_TOKEN_PRICE = 2;

        Cosmetic greekMythology = new Cosmetic("Greek Mythology Themed", new ItemStack(Material.GOLDEN_SWORD), CosmeticRarity.RARE)
                .addCost(manager.getMainResource(), RARE_COINS_PRICE)
                .addCost(ResourcesManager.getResourceByName("CosmeticTokens"), RARE_TOKEN_PRICE)
                .setSelectConsumer(gamePlayer -> {
                    KillMessage killMessage = new KillMessage(gamePlayer);
                    killMessage.addMessage("chat.kill_message.greek_mythology_themed.melee", DamageType.PLAYER_ATTACK, DamageType.GENERIC_KILL);
                    killMessage.addMessage("chat.kill_message.greek_mythology_themed.fall", DamageType.FALL, DamageType.FALL);
                    killMessage.addMessage("chat.kill_message.greek_mythology_themed.void", DamageType.OUT_OF_WORLD, DamageType.OUT_OF_WORLD);
                    killMessage.addMessage("chat.kill_message.greek_mythology_themed.ranged", DamageType.ARROW);

                    gamePlayer.getPlayerData().setKillMessage(killMessage);
                });

        Cosmetic dragon = new Cosmetic("Dragon", new ItemStack(GameAPI.getInstance().getVersionSupport().getCustomHead("31d6e3e41145967b32fb63576c63e3057e63beceb73cea18d20fc8547b0b0645")), CosmeticRarity.EPIC)
                .addCost(manager.getMainResource(), EPIC_COINS_PRICE)
                .addCost(ResourcesManager.getResourceByName("CosmeticTokens"), EPIC_TOKEN_PRICE)
                .setSelectConsumer(gamePlayer -> {
                    KillMessage killMessage = new KillMessage(gamePlayer);
                    killMessage.addMessage("chat.kill_message.dragon.melee", DamageType.PLAYER_ATTACK, DamageType.GENERIC_KILL);
                    killMessage.addMessage("chat.kill_message.dragon.fall", DamageType.FALL);
                    killMessage.addMessage("chat.kill_message.dragon.void", DamageType.OUT_OF_WORLD);
                    killMessage.addMessage("chat.kill_message.dragon.ranged", DamageType.ARROW, DamageType.ARROW);

                    gamePlayer.getPlayerData().setKillMessage(killMessage);
                });

        Cosmetic toilet = new Cosmetic("Toilet", new ItemStack(Material.HOPPER), CosmeticRarity.RARE)
                .addCost(manager.getMainResource(), RARE_COINS_PRICE)
                .addCost(ResourcesManager.getResourceByName("CosmeticTokens"), RARE_TOKEN_PRICE)
                .setSelectConsumer(gamePlayer -> {
                    KillMessage killMessage = new KillMessage(gamePlayer);
                    killMessage.addMessage("chat.kill_message.toilet.melee", DamageType.PLAYER_ATTACK, DamageType.GENERIC_KILL);
                    killMessage.addMessage("chat.kill_message.toilet.fall", DamageType.FALL);
                    killMessage.addMessage("chat.kill_message.toilet.void", DamageType.OUT_OF_WORLD);
                    killMessage.addMessage("chat.kill_message.toilet.ranged", DamageType.ARROW);

                    gamePlayer.getPlayerData().setKillMessage(killMessage);
                });

        Cosmetic glorious = new Cosmetic("Glorious", new ItemStack(Material.GOLD_BLOCK), CosmeticRarity.LEGENDARY)
                .addCost(manager.getMainResource(), LEGENDARY_COINS_PRICE)
                .addCost(ResourcesManager.getResourceByName("CosmeticTokens"), LEGENDARY_TOKEN_PRICE)
                .setSelectConsumer(gamePlayer -> {
                    KillMessage killMessage = new KillMessage(gamePlayer);
                    killMessage.addMessage("chat.kill_message.glorious.melee", DamageType.PLAYER_ATTACK, DamageType.GENERIC_KILL);
                    killMessage.addMessage("chat.kill_message.glorious.fall", DamageType.FALL);
                    killMessage.addMessage("chat.kill_message.glorious.void", DamageType.OUT_OF_WORLD);
                    killMessage.addMessage("chat.kill_message.glorious.ranged", DamageType.ARROW);

                    gamePlayer.getPlayerData().setKillMessage(killMessage);
                });

        Cosmetic wizard = new Cosmetic("Wizard", new ItemStack(Material.BLAZE_ROD), CosmeticRarity.RARE)
                .addCost(manager.getMainResource(), RARE_COINS_PRICE)
                .addCost(ResourcesManager.getResourceByName("CosmeticTokens"), RARE_TOKEN_PRICE)
                .setSelectConsumer(gamePlayer -> {
                    KillMessage killMessage = new KillMessage(gamePlayer);
                    killMessage.addMessage("chat.kill_message.wizard.melee", DamageType.PLAYER_ATTACK, DamageType.GENERIC_KILL);
                    killMessage.addMessage("chat.kill_message.wizard.fall", DamageType.FALL);
                    killMessage.addMessage("chat.kill_message.wizard.void", DamageType.OUT_OF_WORLD);
                    killMessage.addMessage("chat.kill_message.wizard.ranged", DamageType.ARROW);

                    gamePlayer.getPlayerData().setKillMessage(killMessage);
                });

        Cosmetic ninja = new Cosmetic("Ninja", new ItemStack(Material.NETHER_STAR), CosmeticRarity.EPIC)
                .addCost(manager.getMainResource(), EPIC_COINS_PRICE)
                .addCost(ResourcesManager.getResourceByName("CosmeticTokens"), EPIC_TOKEN_PRICE)
                .setSelectConsumer(gamePlayer -> {
                    KillMessage killMessage = new KillMessage(gamePlayer);
                    killMessage.addMessage("chat.kill_message.ninja.melee", DamageType.PLAYER_ATTACK, DamageType.GENERIC_KILL);
                    killMessage.addMessage("chat.kill_message.ninja.fall", DamageType.FALL);
                    killMessage.addMessage("chat.kill_message.ninja.void", DamageType.OUT_OF_WORLD);
                    killMessage.addMessage("chat.kill_message.ninja.ranged", DamageType.ARROW);

                    gamePlayer.getPlayerData().setKillMessage(killMessage);
                });

        Cosmetic genAlpha = new Cosmetic("Gen Alpha", GameAPI.getInstance().getVersionSupport().getCustomHead("760cdc9a43b1cd35bbe47ab50fd25faa7cd218bc7ec66e2227df664572748cc4"), CosmeticRarity.LEGENDARY)
                .addCost(manager.getMainResource(), LEGENDARY_COINS_PRICE)
                .addCost(ResourcesManager.getResourceByName("CosmeticTokens"), LEGENDARY_TOKEN_PRICE)
                .setSelectConsumer(gamePlayer -> {
                    KillMessage killMessage = new KillMessage(gamePlayer);
                    killMessage.addMessage("chat.kill_message.gen_alpha.melee", DamageType.PLAYER_ATTACK, DamageType.GENERIC_KILL);
                    killMessage.addMessage("chat.kill_message.gen_alpha.fall", DamageType.FALL);
                    killMessage.addMessage("chat.kill_message.gen_alpha.void", DamageType.OUT_OF_WORLD);
                    killMessage.addMessage("chat.kill_message.gen_alpha.ranged", DamageType.ARROW);

                    gamePlayer.getPlayerData().setKillMessage(killMessage);
                });

        Cosmetic enchanted = new Cosmetic("Enchanted", new ItemStack(Material.ENCHANTING_TABLE), CosmeticRarity.UNCOMMON)
                .addCost(manager.getMainResource(), UNCOMMON_COINS_PRICE)
                .addCost(ResourcesManager.getResourceByName("CosmeticTokens"), UNCOMMON_TOKEN_PRICE)
                .setAsPurchasable()
                .setSelectConsumer(gamePlayer -> {
                    KillMessage killMessage = new KillMessage(gamePlayer);
                    killMessage.addMessage("chat.kill_message.enchanted.melee", DamageType.PLAYER_ATTACK, DamageType.GENERIC_KILL);
                    killMessage.addMessage("chat.kill_message.enchanted.fall", DamageType.FALL);
                    killMessage.addMessage("chat.kill_message.enchanted.void", DamageType.OUT_OF_WORLD);
                    killMessage.addMessage("chat.kill_message.enchanted.ranged", DamageType.ARROW);

                    gamePlayer.getPlayerData().setKillMessage(killMessage);
                });

        Cosmetic dramatic = new Cosmetic("Dramatic", new ItemStack(Material.INK_SAC), CosmeticRarity.COMMON)
                .addCost(manager.getMainResource(), COMMON_COINS_PRICE)
                .addCost(ResourcesManager.getResourceByName("CosmeticTokens"), COMMON_TOKEN_PRICE)
                .setAsPurchasable()
                .setSelectConsumer(gamePlayer -> {
                    KillMessage killMessage = new KillMessage(gamePlayer);
                    killMessage.addMessage("chat.kill_message.dramatic.melee", DamageType.PLAYER_ATTACK, DamageType.GENERIC_KILL);
                    killMessage.addMessage("chat.kill_message.dramatic.fall", DamageType.FALL);
                    killMessage.addMessage("chat.kill_message.dramatic.void", DamageType.OUT_OF_WORLD);
                    killMessage.addMessage("chat.kill_message.dramatic.ranged", DamageType.ARROW);

                    gamePlayer.getPlayerData().setKillMessage(killMessage);
                });

        Cosmetic swiftie = new Cosmetic("Swiftie", new ItemStack(Material.DIAMOND), CosmeticRarity.LEGENDARY)
                .addCost(manager.getMainResource(), LEGENDARY_COINS_PRICE)
                .addCost(ResourcesManager.getResourceByName("CosmeticTokens"), LEGENDARY_TOKEN_PRICE)
                .setSelectConsumer(gamePlayer -> {
                    KillMessage killMessage = new KillMessage(gamePlayer);
                    killMessage.addMessage("chat.kill_message.swiftie.melee", DamageType.PLAYER_ATTACK, DamageType.GENERIC_KILL);
                    killMessage.addMessage("chat.kill_message.swiftie.fall", DamageType.FALL);
                    killMessage.addMessage("chat.kill_message.swiftie.void", DamageType.OUT_OF_WORLD);
                    killMessage.addMessage("chat.kill_message.swiftie.ranged", DamageType.ARROW);

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

        player.sendMessage(Component.text(" §aMelee Kill:").append(MessageManager.get(player, "chat.kill_message." + messageName + ".melee").replace("%player_color%", "§c").replace("%dead%", "§cPlayer").replace("%killer_color%", "§c").replace("%killer%", "§cKiller").getTranslated()));
        player.sendMessage(Component.text(" §aFall Damage Kill: ").append(MessageManager.get(player, "chat.kill_message." + messageName + ".fall").replace("%player_color%", "§c").replace("%dead%", "§cPlayer").replace("%killer_color%", "§c").replace("%killer%", "§cKiller").getTranslated()));
        player.sendMessage(Component.text(" §aVoid Kill: ").append(MessageManager.get(player, "chat.kill_message." + messageName + ".void").replace("%player_color%", "§c").replace("%dead%", "§cPlayer").replace("%killer_color%", "§c").replace("%killer%", "§cKiller").getTranslated()));
        player.sendMessage(Component.text(" §aRanged Kill: ").append(MessageManager.get(player, "chat.kill_message." + messageName + ".ranged").replace("%player_color%", "§c").replace("%dead%", "§cPlayer").replace("%killer_color%", "§c").replace("%killer%", "§cKiller").getTranslated()));
        player.sendMessage("");
    }
}
