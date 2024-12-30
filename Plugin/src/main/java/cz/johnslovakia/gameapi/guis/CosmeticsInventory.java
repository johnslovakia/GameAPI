package cz.johnslovakia.gameapi.guis;

import com.cryptomorin.xseries.XEnchantment;
import com.cryptomorin.xseries.XMaterial;
import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.game.cosmetics.Cosmetic;
import cz.johnslovakia.gameapi.game.cosmetics.CosmeticsManager;
import cz.johnslovakia.gameapi.game.kit.KitManager;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerData;
import cz.johnslovakia.gameapi.users.PlayerManager;
import cz.johnslovakia.gameapi.users.quests.PlayerQuestData;
import cz.johnslovakia.gameapi.users.quests.Quest;
import cz.johnslovakia.gameapi.users.quests.QuestType;
import cz.johnslovakia.gameapi.users.resources.Resource;
import cz.johnslovakia.gameapi.utils.ItemBuilder;
import cz.johnslovakia.gameapi.utils.Sounds;
import cz.johnslovakia.gameapi.utils.Utils;
import cz.johnslovakia.gameapi.utils.rewards.RewardItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;

import java.util.Arrays;

public class CosmeticsInventory implements Listener {

    private static ItemStack getEditedItem(GamePlayer gamePlayer, Cosmetic cosmetic){
        PlayerData data = gamePlayer.getPlayerData();
        CosmeticsManager manager = cosmetic.getCategory().getManager();
        int balance = data.getBalance(manager.getEconomy());

        ItemBuilder item = new ItemBuilder(cosmetic.hasPlayer(gamePlayer) ? cosmetic.getIcon() : new ItemStack(XMaterial.INK_SAC.parseMaterial()));
        if (!cosmetic.hasPlayer(gamePlayer)){
            item.setName((balance >= cosmetic.getPrice() ? "§a" + cosmetic.getName() : "§c" + cosmetic.getName()));
        }else{
            item.setName("§a§l" + cosmetic.getName());
        }
        item.hideAllFlags();
        item.removeLore();
        item.removeEnchantment(XEnchantment.DAMAGE_ALL.getEnchant());

        item.addLoreLine("§8" + cosmetic.getCategory().getName());
        item.addLoreLine("");
        MessageManager.get(gamePlayer, "inventory.cosmetics.rarity")
                .replace("%rarity%", cosmetic.getRarity().getColor() + MessageManager.get(gamePlayer, cosmetic.getRarity().getTranslateKey()).getTranslated())
                .addToItemLore(item);
        MessageManager.get(gamePlayer, "inventory.cosmetics.price")
                .replace("%price%", String.valueOf(cosmetic.getPrice()))
                .replace("%economy_name%", manager.getEconomy().getName())
                .addToItemLore(item);
        item.addLoreLine("");

        /*if (cosmetic.getLoreKey() != null && MessageManager.existMessage(cosmetic.getLoreKey())) {
            MessageManager.get(gamePlayer, cosmetic.getLoreKey())
                    .addToItemLore(item);
            item.addLoreLine("");
        }*/

        if (manager.getSelectedCosmetic(cosmetic.getCategory(), gamePlayer) != null &&
                manager.getSelectedCosmetic(cosmetic.getCategory(), gamePlayer).equals(cosmetic)) {
            item.addEnchant(XEnchantment.DAMAGE_ALL.getEnchant(), 1);
            MessageManager.get(gamePlayer, "inventory.cosmetics.selected")
                    .addToItemLore(item);
        } else if (cosmetic.hasPlayer(gamePlayer)) {
            MessageManager.get(gamePlayer, "inventory.cosmetics.select")
                    .addToItemLore(item);
        } else if (balance <= cosmetic.getPrice()) {
            MessageManager.get(gamePlayer, "inventory.cosmetics.dont_have_enough")
                    .replace("%economy_name%", manager.getEconomy().getName())
                    .addToItemLore(item);
        } else {
            MessageManager.get(gamePlayer, "inventory.cosmetics.purchase")
                    .addToItemLore(item);
        }

        return item.toItemStack();
    }

    public static void openGUI(GamePlayer gamePlayer, GamePlayer targetGamePlayer){
        Player player = gamePlayer.getOnlinePlayer();
        Player target = targetGamePlayer.getOnlinePlayer();

        PlayerInventory pInv = target.getInventory();
        Inventory inv = Bukkit.createInventory(null, 54, "§f七七七七七七七七ㆹ");

        ItemStack gray = new ItemBuilder(XMaterial.GRAY_STAINED_GLASS.parseMaterial()).setName(" ").hideAllFlags().toItemStack();

        for (int i = 27; i <= 35; i++){
            inv.setItem(i, new ItemBuilder(XMaterial.GRAY_STAINED_GLASS_PANE.parseItem()).setName(" ").setLore(MessageManager.get(gamePlayer.getOnlinePlayer(), "inventory.set_kit_inventory.item.info").getTranslated()).toItemStack());
        }

        ItemStack[] hotbarItems = Arrays.copyOfRange(pInv.getContents(), 0, 8);
        ItemStack[] topInventoryItems = Arrays.copyOfRange(pInv.getContents(), 9, 35);

        for (int i = 0; i < 8; i++) {
            inv.setItem(i + 36, hotbarItems[i]);
        }
        for (int i = 0; i < 26; i++) {
            inv.setItem(i, topInventoryItems[i]);
        }


        inv.setItem(45, new ItemBuilder(Material.ECHO_SHARD).setCustomModelData(1016).setName(MessageManager.get(gamePlayer, "inventory.item.go_back").getTranslated()).toItemStack());


        inv.setItem(47, (pInv.getHelmet() != null ? pInv.getHelmet() : new ItemBuilder(Material.BARRIER).setName(MessageManager.get(gamePlayer, "inventory.view_player_inventory.no_helmet").getTranslated()).toItemStack()));
        inv.setItem(48, (pInv.getChestplate() != null ? pInv.getChestplate() : new ItemBuilder(Material.BARRIER).setName(MessageManager.get(gamePlayer, "inventory.view_player_inventory.no_chestplate").getTranslated()).toItemStack()));
        inv.setItem(49, (pInv.getLeggings() != null ? pInv.getLeggings() : new ItemBuilder(Material.BARRIER).setName(MessageManager.get(gamePlayer, "inventory.view_player_inventory.no_leggings").getTranslated()).toItemStack()));
        inv.setItem(50, (pInv.getBoots() != null ? pInv.getBoots() : new ItemBuilder(Material.BARRIER).setName(MessageManager.get(gamePlayer, "inventory.view_player_inventory.no_boots").getTranslated()).toItemStack()));

        ItemBuilder inf = new ItemBuilder(GameAPI.getInstance().getVersionSupport().getPlayerHead(target));
        inf.setName((PlayerManager.getGamePlayer(target).getPlayerData().getTeam() != null ? PlayerManager.getGamePlayer(target).getPlayerData().getTeam().getChatColor() : "§r§b") + target.getName());
        inf.setLore(MessageManager.get(player, "inventory.player_inventory.health")
                .replace("%health%", "" + (int) target.getHealth())
                .replace("%max_health%", "" + (int) GameAPI.getInstance().getVersionSupport().getMaxPlayerHealth(target)).getTranslated());
        MessageManager.get(player, "inventory.player_inventory.food")
                .replace("%food%", "" + target.getFoodLevel())
                .addToItemLore(inf);
        MessageManager.get(player, "inventory.player_inventory.experience")
                .replace("%experience%", "" + target.getLevel())
                .addToItemLore(inf);
        if (KitManager.getKitManager(gamePlayer.getPlayerData().getGame()) != null) {
            MessageManager.get(player, "inventory.teleporter.kit")
                    .replace("%kit%", (PlayerManager.getGamePlayer(target).getPlayerData().getKit()) != null ? PlayerManager.getGamePlayer(target).getPlayerData().getKit().getName() : MessageManager.get(player, "word.none_kit").getTranslated())
                    .addToItemLore(inf);
        }

        inf.addLoreLine("");
        MessageManager.get(player, "inventory.player_inventory.effects")
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
            player.playSound(player.getLocation(), Sounds.CLICK.bukkitSound(), 1F, 1F);
            TeleporterInventory.openGUI(gamePlayer);
        }
    }
}
