package cz.johnslovakia.gameapi.game.kit;

import com.cryptomorin.xseries.XMaterial;
import cz.johnslovakia.gameapi.GUIs.KitInventory;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerManager;
import cz.johnslovakia.gameapi.utils.ItemBuilder;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class KitInventoryEditor implements Listener {


    public static void setKitInventory(GamePlayer gamePlayer, Kit kit) {
        Inventory currentKitInventory = gamePlayer.getPlayerData().getKitInventory(kit);

        gamePlayer.getMetadata().put("set_kit_inventory.kit", kit);

        if (containsArmor(kit.getContent().getContents())){
            gamePlayer.getMetadata().put("set_kit_inventory.autoArmor", !containsArmor(Arrays.stream(currentKitInventory.getStorageContents())
                    .filter(Objects::nonNull)
                    .filter(is -> !is.getType().equals(Material.AIR))
                    .filter(is -> !(is.getType().toString().toLowerCase().contains("helmet")
                            || is.getType().toString().toLowerCase().contains("chestplate")
                            || is.getType().toString().toLowerCase().contains("leggings")
                            || is.getType().toString().toLowerCase().contains("boots"))
            ).toArray(ItemStack[]::new)));
            //gamePlayer.getOnlinePlayer().sendMessage(containsArmor(kit.getContent().getContents()) + " " + !containsArmor(currentKitInventory.getStorageContents()));

        }

        openGUI(gamePlayer, kit);
    }

    private static boolean containsArmor(ItemStack[] items){
        return Arrays.stream(items)
                .filter(Objects::nonNull)
                .filter(is -> !is.getType().equals(Material.AIR))
                .anyMatch(is ->
                                is.getType().toString().toLowerCase().contains("helmet")
                                || is.getType().toString().toLowerCase().contains("chestplate")
                                || is.getType().toString().toLowerCase().contains("leggings")
                                || is.getType().toString().toLowerCase().contains("boots"));
    }


    public static void openGUI(GamePlayer gamePlayer, Kit kit) {
        Inventory gui = Bukkit.createInventory(null, 54, "Inventory Editor");

        for (int i = 27; i <= 35; i++){
            gui.setItem(i, new ItemBuilder(XMaterial.GRAY_STAINED_GLASS_PANE.parseItem()).setName("").setLore(MessageManager.get(gamePlayer.getOnlinePlayer(), "inventory.set_kit_inventory.item.info").getTranslated()).toItemStack());
        }

        Inventory currentKitInventory = gamePlayer.getPlayerData().getKitInventory(kit);
        ItemStack[] hotbarItems = Arrays.copyOfRange(currentKitInventory.getContents(), 0, 8);
        ItemStack[] topInventoryItems = Arrays.copyOfRange(currentKitInventory.getContents(), 9, 35);

        for (int i = 0; i < 8; i++) {
            gui.setItem(i + 36, hotbarItems[i]); // Hotbar
        }
        for (int i = 0; i < 26; i++) {
            gui.setItem(i, topInventoryItems[i]); // Top inventory
        }

        ItemBuilder save = new ItemBuilder(Material.EMERALD_BLOCK);
        save.setName(MessageManager.get(gamePlayer, "inventory.set_kit_inventory.item.save_inventory").getTranslated());
        save.addLoreLine("");
        save.setLore(MessageManager.get(gamePlayer, "inventory.set_kit_inventory.item.save_inventory.lore").getTranslated());

        ItemBuilder reset = new ItemBuilder(Material.BARRIER);
        reset.setName(MessageManager.get(gamePlayer, "inventory.set_kit_inventory.item.reset").getTranslated());
        reset.addLoreLine("");
        MessageManager.get(gamePlayer, "inventory.set_kit_inventory.item.reset.lore").addToItemLore(reset);

        String translateKey = "kit." + kit.getName().toLowerCase().replace(" ", "_");
        ItemBuilder kitItem = new ItemBuilder(kit.getIcon());
        kitItem.hideAllFlags();
        kitItem.setName("§a" + kit.getName());
        kitItem.removeLore();
        if (MessageManager.existMessage(translateKey)) {
            MessageManager.get(gamePlayer, translateKey)
                    .addToItemLore(kitItem);
        }

        ItemBuilder back = new ItemBuilder(Material.ARROW).setName(MessageManager.get(gamePlayer, "inventory.item.go_back").getTranslated());

        gui.setItem(45, back.toItemStack());
        gui.setItem(46, reset.toItemStack());
        gui.setItem(49, kitItem.toItemStack());
        if (containsArmor(kit.getContent().getContents())) {
            gui.setItem(52, getArmorItem(gamePlayer));
        }
        gui.setItem(53, save.toItemStack());

        gamePlayer.getOnlinePlayer().openInventory(gui);
    }

    private static ItemStack getArmorItem(GamePlayer gamePlayer){
        boolean autoArmor = (boolean) gamePlayer.getMetadata().get("set_kit_inventory.autoArmor");

        ItemBuilder armor = new ItemBuilder(Material.LEATHER_CHESTPLATE);
        armor.setName((autoArmor ? "#72f622" : "§c") + MessageManager.get(gamePlayer, "inventory.set_kit_inventory.item.auto_equip_armor").getTranslated());
        armor.addLoreLine("");
        if (autoArmor) {
            MessageManager.get(gamePlayer, "inventory.set_kit_inventory.item.auto_equip_armor.action1").addToItemLore(armor);
        }else{
            MessageManager.get(gamePlayer, "inventory.set_kit_inventory.item.auto_equip_armor.action2").addToItemLore(armor);
        }
        return armor.toItemStack();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);
        ItemStack item = event.getCurrentItem();

        if (event.getClickedInventory() == null
            || !player.getOpenInventory().getTitle().contains("Inventory Editor")
            || !event.getClickedInventory().equals(player.getOpenInventory().getTopInventory())
            || item == null){
            return;
        }

        Inventory gui = event.getClickedInventory();
        Kit kit = (Kit) gamePlayer.getMetadata().get("set_kit_inventory.kit");

        if (item.getType().equals(Material.GRAY_STAINED_GLASS_PANE) || event.isShiftClick()){
            event.setCancelled(true);
        }

        switch (event.getSlot()) {
            case 45:
                event.setCancelled(true);

                gamePlayer.getMetadata().put("set_kit_inventory.check_closing", false);
                KitInventory.openKitInventory(gamePlayer);
                break;
            case 46:
                event.setCancelled(true);
                gamePlayer.getMetadata().put("set_kit_inventory.autoArmor", true);

                Inventory kitInventory = kit.getContent().getInventory();
                ItemStack[] hotbarItems = Arrays.copyOfRange(kitInventory.getContents(), 0, 8);
                ItemStack[] topInventoryItems = Arrays.copyOfRange(kitInventory.getContents(), 9, 35);

                for (int i = 0; i < 8; i++) {
                    gui.setItem(i + 36, hotbarItems[i]); // Hotbar
                }
                for (int i = 0; i < 26; i++) {
                    gui.setItem(i, topInventoryItems[i]); // Top inventory
                }
                break;
            case 52:
                event.setCancelled(true);

                if ((boolean) gamePlayer.getMetadata().get("set_kit_inventory.autoArmor")) {
                    gamePlayer.getMetadata().put("set_kit_inventory.autoArmor", false);
                    getArmor(kit.getContent().getContents()).forEach(is -> gui.setItem(findEmptySlot(gui), is));
                }else{
                    gamePlayer.getMetadata().put("set_kit_inventory.autoArmor", true);
                    getArmor(kit.getContent().getContents()).forEach(gui::remove);
                }
                gui.setItem(52, getArmorItem(gamePlayer));

                break;
            case 53:
                event.setCancelled(true);

                gamePlayer.getMetadata().put("set_kit_inventory.check_closing", false);
                player.closeInventory();
                KitInventory.openKitInventory(gamePlayer);
                save(gamePlayer, kit, gui, (boolean) gamePlayer.getMetadata().get("set_kit_inventory.autoArmor"));
                break;
        }
    }

    private int findEmptySlot(Inventory gui) {
        for (int i = 36; i <= 44; i++) {
            if (gui.getItem(i) == null || gui.getItem(i).getType() == Material.AIR) {
                return i;
            }
        }
        for (int i = 0; i <= 26; i++) {
            if (gui.getItem(i) == null || gui.getItem(i).getType() == Material.AIR) {
                return i;
            }
        }
        return -1;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);

            if (gamePlayer.getPlayerData().getCurrentInventory() == null){
                return;
            }
            if (!event.getInventory().getType().equals(InventoryType.PLAYER)
                    && !player.getOpenInventory().getTitle().contains("Inventory Editor")){
                return;
            }
            if (gamePlayer.getMetadata().get("set_kit_inventory.check_closing") != null && !(boolean) gamePlayer.getMetadata().get("set_kit_inventory.check_closing")){
                return;
            }


            gamePlayer.getMetadata().put("set_kit_inventory.inventory", player.getInventory());

            Kit kit = (Kit) gamePlayer.getMetadata().get("set_kit_inventory.kit");
            MessageManager.get(gamePlayer, "chat.set_kit_inventory.closed_inventory")
                    .replace("%kit%", kit.getName())
                    .send();

            ComponentBuilder message = new ComponentBuilder(MessageManager.get(gamePlayer, "chat.set_kit_inventory.closed_inventory.wanna_save").getTranslated());
            message.event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/saveinventory"));
            message.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(MessageManager.get(gamePlayer, "chat.set_kit_inventory.closed_inventory.wanna_save.hover").getTranslated()).create()));
            player.spigot().sendMessage(message.create());
        }
    }

    private static List<ItemStack> getArmor(ItemStack[] items){
        return Arrays.stream(items)
                .filter(Objects::nonNull)
                .filter(is -> !is.getType().equals(Material.AIR))
                .filter(is -> is.getType().toString().toLowerCase().contains("helmet")
                        || is.getType().toString().toLowerCase().contains("chestplate")
                        || is.getType().toString().toLowerCase().contains("leggings")
                        || is.getType().toString().toLowerCase().contains("boots"))
                .toList();
    }

    private static Inventory getCopyOfInventory(Inventory inventory, Kit kit, boolean autoArmor) {
        Inventory finalInventory = Bukkit.createInventory(null, InventoryType.PLAYER);

        for (int i = 36; i < 45; i++){
            finalInventory.setItem(i - 36, inventory.getItem(i));
        }
        for (int i = 9; i < 35; i++){
            finalInventory.setItem(i, inventory.getItem(i - 9));
        }

        if (autoArmor){
            for (ItemStack item : getArmor(kit.getContent().getContents())){
                if (item.getType().toString().toLowerCase().contains("helmet")){
                    finalInventory.setItem(39, item);
                }else if (item.getType().toString().toLowerCase().contains("chestplate")){
                    finalInventory.setItem(38, item);
                }else if (item.getType().toString().toLowerCase().contains("leggings")){
                    finalInventory.setItem(37, item);
                }else if (item.getType().toString().toLowerCase().contains("boots")){
                    finalInventory.setItem(36, item);
                }
            }
        }

        return finalInventory;
    }

    private static void save(GamePlayer gamePlayer, Kit kit, Inventory inventory, boolean autoArmor) {
        gamePlayer.getPlayerData().setKitInventory(kit, getCopyOfInventory(inventory, kit, autoArmor));

        MessageManager.get(gamePlayer, "chat.set_kit_inventory.saved")
                .replace("%kit%", kit.getName())
                .send();
        gamePlayer.getMetadata().remove("set_kit_inventory.kit");
    }

    public static class SaveCommand implements CommandExecutor {

        @Override
        public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

            if (sender instanceof Player player) {
                GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);

                if (args.length == 0) {
                    if (gamePlayer.getMetadata().get("set_kit_inventory.kit") == null){
                        return false;
                    }

                    Inventory kitInventory = (Inventory) gamePlayer.getMetadata().get("set_kit_inventory.inventory");
                    Kit kit = (Kit) gamePlayer.getMetadata().get("set_kit_inventory.kit");

                    save(gamePlayer, kit, kitInventory, (boolean) gamePlayer.getMetadata().get("set_kit_inventory.autoArmor"));
                }
            }
            return false;
        }
    }
}
