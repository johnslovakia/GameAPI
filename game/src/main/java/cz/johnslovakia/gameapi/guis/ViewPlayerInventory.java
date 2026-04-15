package cz.johnslovakia.gameapi.guis;

import cz.johnslovakia.gameapi.modules.kits.KitManager;
import cz.johnslovakia.gameapi.modules.messages.MessageModule;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerManager;

import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.utils.ItemBuilder;
import cz.johnslovakia.gameapi.utils.Utils;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;

import java.util.Arrays;

public class ViewPlayerInventory implements Listener {

    public static void openGUI(GamePlayer gamePlayer, GamePlayer targetGamePlayer){
        MessageModule messageModule = ModuleManager.getModule(MessageModule.class);
        Player player = gamePlayer.getOnlinePlayer();
        Player target = targetGamePlayer.getOnlinePlayer();

        PlayerInventory pInv = target.getInventory();
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("§f七七七七七七七七ㆹ").font(Key.key("jsplugins", "guis")));

        ItemStack gray = new ItemBuilder(Material.GRAY_STAINED_GLASS).setName(" ").hideAllFlags().toItemStack();

        for (int i = 27; i <= 35; i++){
            inv.setItem(i, new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setName(" ").setLore(messageModule.getMessage(gamePlayer.getOnlinePlayer(), "inventory.set_kit_inventory.item.info").toComponent()).toItemStack());
        }

        ItemStack[] hotbarItems = Arrays.copyOfRange(pInv.getContents(), 0, 8);
        ItemStack[] topInventoryItems = Arrays.copyOfRange(pInv.getContents(), 9, 35);

        for (int i = 0; i < 8; i++) {
            inv.setItem(i + 36, hotbarItems[i]);
        }
        for (int i = 0; i < 26; i++) {
            inv.setItem(i, topInventoryItems[i]);
        }


        inv.setItem(45, new ItemBuilder(Material.ECHO_SHARD).setCustomModelData(1016).setName(messageModule.getMessage(gamePlayer, "inventory.item.go_back").toComponent()).toItemStack());


        inv.setItem(47, (pInv.getHelmet() != null ? pInv.getHelmet() : new ItemBuilder(Material.BARRIER).setName(messageModule.getMessage(gamePlayer, "inventory.view_player_inventory.no_helmet").toComponent()).toItemStack()));
        inv.setItem(48, (pInv.getChestplate() != null ? pInv.getChestplate() : new ItemBuilder(Material.BARRIER).setName(messageModule.getMessage(gamePlayer, "inventory.view_player_inventory.no_chestplate").toComponent()).toItemStack()));
        inv.setItem(49, (pInv.getLeggings() != null ? pInv.getLeggings() : new ItemBuilder(Material.BARRIER).setName(messageModule.getMessage(gamePlayer, "inventory.view_player_inventory.no_leggings").toComponent()).toItemStack()));
        inv.setItem(50, (pInv.getBoots() != null ? pInv.getBoots() : new ItemBuilder(Material.BARRIER).setName(messageModule.getMessage(gamePlayer, "inventory.view_player_inventory.no_boots").toComponent()).toItemStack()));

        ItemBuilder inf = new ItemBuilder(Utils.getPlayerHead(target));
        inf.setName((PlayerManager.getGamePlayer(target).getGameSession().getTeam() != null ? PlayerManager.getGamePlayer(target).getGameSession().getTeam().getChatColor() : "§r§b") + target.getName());
        inf.setLore(messageModule.getMessage(player, "inventory.player_inventory.health")
                .replace("%health%", "" + (int) target.getHealth())
                .replace("%max_health%", "" + (int) target.getAttribute(Attribute.MAX_HEALTH).getValue()).toComponent());
        messageModule.getMessage(player, "inventory.player_inventory.food")
                .replace("%food%", "" + target.getFoodLevel())
                .addToItemLore(inf);
        messageModule.getMessage(player, "inventory.player_inventory.experience")
                .replace("%experience%", "" + target.getLevel())
                .addToItemLore(inf);
        if (KitManager.getKitManager(gamePlayer.getGame()) != null) {
            messageModule.getMessage(player, "inventory.teleporter.kit")
                    .replace("%kit%", (PlayerManager.getGamePlayer(target).getGameSession().getSelectedKit()) != null ? net.kyori.adventure.text.Component.text(PlayerManager.getGamePlayer(target).getGameSession().getSelectedKit().getName()) : messageModule.getMessage(player, "word.none_kit").toComponent())
                    .addToItemLore(inf);
        }

        inf.addLoreLine("");
        messageModule.getMessage(player, "inventory.player_inventory.effects")
                .addToItemLore(inf);
        for(PotionEffect effect : target.getPlayer().getActivePotionEffects()){
            inf.addLoreLine(" §7" + effect.getType().getName().toLowerCase() + " " + (effect.getAmplifier() + 1) + " (" + Utils.getDurationString(effect.getDuration() / 20) + "§7)");
        }

        inv.setItem(53, inf.toItemStack());

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);
        ItemStack item = event.getCurrentItem();

        if (event.getClickedInventory() == null || !player.getOpenInventory().getTitle().contains("ㆹ")){
            return;
        }
        if (item == null){
            return;
        }
        event.setCancelled(true);

        if (event.getSlot() == 45){
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1F, 1F);
            TeleporterInventory.openGUI(gamePlayer);
        }
    }
}
