package cz.johnslovakia.gameapi.guis;

import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.inventoryBuilder.InventoryBuilder;
import cz.johnslovakia.gameapi.modules.kits.Kit;
import cz.johnslovakia.gameapi.modules.messages.MessageModule;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerManager;

import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.utils.ItemBuilder;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

public class KitInventoryEditor implements Listener {


    public static void setKitInventory(GamePlayer gamePlayer, Kit kit) {
        Inventory currentKitInventory = gamePlayer.getPlayerData().getKitInventory(kit);

        gamePlayer.getMetadata().put("set_kit_inventory.kit", kit);

        ItemStack[] inventoryItems = Arrays.copyOfRange(currentKitInventory.getContents(), 0, 35);
        if (containsArmor(kit.getContent().getContents())){
            gamePlayer.getMetadata().put("set_kit_inventory.autoArmor", !containsArmor(inventoryItems));
            //gamePlayer.getOnlinePlayer().sendMessage(containsArmor(kit.getContent().getContents()) + " " + !containsArmor(currentKitInventory.getStorageContents()));
        }else{
            gamePlayer.getMetadata().put("set_kit_inventory.autoArmor", true);
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
        Inventory gui = Bukkit.createInventory(null, 54, Component.text("§f七七七七七七七七ㆾ").font(Key.key("jsplugins", "guis")));

        for (int i = 27; i <= 35; i++){
            gui.setItem(i, new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setName(" ").setLore(ModuleManager.getModule(MessageModule.class).get(gamePlayer.getOnlinePlayer(), "inventory.set_kit_inventory.item.info").getTranslated()).toItemStack());
        }

        Inventory currentKitInventory = gamePlayer.getPlayerData().getKitInventory(kit);
        ItemStack[] hotbarItems = Arrays.copyOfRange(currentKitInventory.getContents(), 0, 9);
        ItemStack[] topInventoryItems = Arrays.copyOfRange(currentKitInventory.getContents(), 9, 36);

        for (int i = 0; i <= 8; i++) {
            gui.setItem(i + 36, hotbarItems[i]); // Hotbar
        }
        for (int i = 0; i <= 26; i++) {
            gui.setItem(i, topInventoryItems[i]); // Top inventory
        }

        ItemBuilder save = new ItemBuilder(Material.EMERALD_BLOCK);
        save.setName(ModuleManager.getModule(MessageModule.class).get(gamePlayer, "inventory.set_kit_inventory.item.save_inventory").getTranslated());
        save.setLore(ModuleManager.getModule(MessageModule.class).get(gamePlayer, "inventory.set_kit_inventory.item.save_inventory.lore").getTranslated());

        ItemBuilder reset = new ItemBuilder(Material.BARRIER);
        reset.setName(ModuleManager.getModule(MessageModule.class).get(gamePlayer, "inventory.set_kit_inventory.item.reset").getTranslated());
        reset.setLore(ModuleManager.getModule(MessageModule.class).get(gamePlayer, "inventory.set_kit_inventory.item.reset.lore").getTranslated());

        String translateKey = "kit." + kit.getName().toLowerCase().replace(" ", "_");
        ItemBuilder kitItem = new ItemBuilder(kit.getIcon());
        kitItem.hideAllFlags();
        kitItem.setName("§a" + kit.getName());
        kitItem.removeLore();
        if (ModuleManager.getModule(MessageModule.class).existMessage(translateKey)) {
            ModuleManager.getModule(MessageModule.class).get(gamePlayer, translateKey)
                    .addToItemLore(kitItem);
        }

        ItemBuilder back = new ItemBuilder(Material.ECHO_SHARD).setCustomModelData(1016).setName(ModuleManager.getModule(MessageModule.class).get(gamePlayer, "inventory.item.go_back").getTranslated());

        ItemBuilder invisibleItem = new ItemBuilder(Material.MAP).setName(" ").setCustomModelData(1010).hideAllFlags();
        for (int i = 45; i <= 53; i++){
            gui.setItem(i, invisibleItem.toItemStack());
        }

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
        armor.setName((autoArmor ? "#72f622" : "§c") + PlainTextComponentSerializer.plainText().serialize(ModuleManager.getModule(MessageModule.class).get(gamePlayer, "inventory.set_kit_inventory.item.auto_equip_armor").getTranslated()));
        armor.addLoreLine("");
        if (autoArmor) {
            ModuleManager.getModule(MessageModule.class).get(gamePlayer, "inventory.set_kit_inventory.item.auto_equip_armor.action1").addToItemLore(armor);
        }else{
            ModuleManager.getModule(MessageModule.class).get(gamePlayer, "inventory.set_kit_inventory.item.auto_equip_armor.action2").addToItemLore(armor);
        }
        return armor.toItemStack();
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        InventoryView openInventory = event.getPlayer().getOpenInventory();

        if (openInventory.getTitle().contains("ㆾ")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);
        ItemStack item = event.getCurrentItem();

        if (event.getClickedInventory() == null || !player.getOpenInventory().getTitle().contains("ㆾ")){
            return;
        }
        if (!event.getClickedInventory().equals(player.getOpenInventory().getTopInventory())){
            event.setCancelled(true);
        }
        if (item == null){
            return;
        }


        Inventory gui = event.getClickedInventory();
        Kit kit = (Kit) gamePlayer.getMetadata().get("set_kit_inventory.kit");

        if (item.getType().equals(Material.GRAY_STAINED_GLASS_PANE) || event.isShiftClick() || event.getClick().isKeyboardClick()){
            event.setCancelled(true);
        }

        int slot = event.getSlot();

        if (!((slot >= 0 && slot <= 26) || (slot >= 36 && slot <= 44))) { //<= 44, 26
            event.setCancelled(true);
        }
        if ((event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY && event.getAction() == InventoryAction.PICKUP_ALL
                || event.getAction() == InventoryAction.PICKUP_HALF || event.getAction() == InventoryAction.PICKUP_SOME)
                || event.getAction() == InventoryAction.DROP_ALL_SLOT || event.getAction() == InventoryAction.DROP_ONE_SLOT) {
            event.setCancelled(true);
        }

        if (event.getAction() == InventoryAction.PLACE_ALL && slot >= 45) {
            event.setCancelled(true);
        }

        switch (event.getSlot()) {
            case 45:
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1F, 1F);
                gamePlayer.getMetadata().put("set_kit_inventory.check_closing", false);
                KitInventory.openKitInventory(gamePlayer);
                break;
            case 46:
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1F, 1F);
                gamePlayer.getMetadata().put("set_kit_inventory.autoArmor", true);

                Inventory kitInventory = kit.getContent().getInventory();
                ItemStack[] hotbarItems = Arrays.copyOfRange(kitInventory.getContents(), 0, 9);
                ItemStack[] topInventoryItems = Arrays.copyOfRange(kitInventory.getContents(), 9, 36);

                for (int i = 0; i <= 8; i++) {
                    gui.setItem(i + 36, hotbarItems[i]); // Hotbar
                }
                for (int i = 0; i <= 26; i++) {
                    gui.setItem(i, topInventoryItems[i]); // Top inventory
                }
                break;
            case 52:
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1F, 1F);

                if (event.getCurrentItem() != null && event.getCurrentItem().getType().equals(Material.LEATHER_CHESTPLATE)) {
                    if ((boolean) gamePlayer.getMetadata().get("set_kit_inventory.autoArmor")) {
                        gamePlayer.getMetadata().put("set_kit_inventory.autoArmor", false);
                        getArmor(kit.getContent().getContents()).forEach(is -> gui.setItem(findEmptySlot(gui), is));
                    } else {
                        gamePlayer.getMetadata().put("set_kit_inventory.autoArmor", true);
                        getArmor(kit.getContent().getContents()).forEach(gui::remove);
                    }
                    gui.setItem(52, getArmorItem(gamePlayer));
                }

                break;
            case 53:
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1F, 1F);
                gamePlayer.getMetadata().put("set_kit_inventory.check_closing", false);
                player.closeInventory();
                KitInventory.openKitInventory(gamePlayer);
                save(gamePlayer, kit, gui, (boolean) gamePlayer.getMetadata().get("set_kit_inventory.autoArmor"));
                break;
        }
    }

    private int findEmptySlot(Inventory gui) {
        for (int i = 36; i <= 44; i++) { //<= 44
            if (gui.getItem(i) == null || gui.getItem(i).getType() == Material.AIR) {
                return i;
            }
        }
        for (int i = 0; i <= 26; i++) { //<= 26
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
            InventoryBuilder currentInventory = InventoryBuilder.getPlayerCurrentInventory(gamePlayer);

            if (currentInventory == null){
                return;
            }
            if (!event.getInventory().getType().equals(InventoryType.PLAYER)
                    && !player.getOpenInventory().getTitle().contains("ㆾ")){
                return;
            }

            Bukkit.getScheduler().runTaskLater(Minigame.getInstance().getPlugin(),
                    () -> currentInventory.give(gamePlayer, true), 2L);

            if (gamePlayer.getMetadata().get("set_kit_inventory.check_closing") != null && !(boolean) gamePlayer.getMetadata().get("set_kit_inventory.check_closing")){
                return;
            }



            Kit kit = (Kit) gamePlayer.getMetadata().get("set_kit_inventory.kit");


            Inventory oldLayout = gamePlayer.getPlayerData().getKitInventory(kit);
            Inventory notSavedLayout = getCopyOfInventory(event.getInventory(), kit, (gamePlayer.getMetadata().get("set_kit_inventory.autoArmor") == null || (boolean) gamePlayer.getMetadata().get("set_kit_inventory.autoArmor")));
            if (oldLayout.getSize() == notSavedLayout.getSize() && IntStream.range(0, oldLayout.getSize()).allMatch(i -> Objects.equals(oldLayout.getItem(i), notSavedLayout.getItem(i)))){
                return;
            }

            gamePlayer.getMetadata().put("set_kit_inventory.inventory", event.getInventory());


            ModuleManager.getModule(MessageModule.class).get(gamePlayer, "chat.set_kit_inventory.closed_inventory")
                    .replace("%kit%", kit.getName())
                    .send();

            Component message = ModuleManager.getModule(MessageModule.class).get(gamePlayer, "chat.set_kit_inventory.closed_inventory.wanna_save").getTranslated()
                    .hoverEvent(ModuleManager.getModule(MessageModule.class).get(gamePlayer, "chat.set_kit_inventory.closed_inventory.wanna_save.hover").getTranslated())
                    .clickEvent(ClickEvent.runCommand("/saveinventory"));

            player.sendMessage(message);
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

        for (int i = 36; i <= 45; i++){
            finalInventory.setItem(i - 36, inventory.getItem(i));
        }
        for (int i = 9; i <= 35; i++){
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

    public static void save(GamePlayer gamePlayer, Kit kit, Inventory inventory, boolean autoArmor) {
        gamePlayer.getPlayerData().setKitInventory(kit, getCopyOfInventory(inventory, kit, autoArmor));

        ModuleManager.getModule(MessageModule.class).get(gamePlayer, "chat.set_kit_inventory.saved")
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
