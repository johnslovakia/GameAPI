package cz.johnslovakia.gameapi.game.kit;

import com.cryptomorin.xseries.XEnchantment;
import cz.johnslovakia.gameapi.GUIs.KitInventory;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerManager;
import cz.johnslovakia.gameapi.utils.ItemBuilder;
import cz.johnslovakia.gameapi.utils.inventoryBuilder.InventoryManager;
import cz.johnslovakia.gameapi.utils.inventoryBuilder.Item;
import me.zort.containr.Component;
import me.zort.containr.GUI;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class KitInventoryEditor implements Listener {

    /*@EventHandler
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

            gamePlayer.getMetadata().put("set_kit_inventory.inventory", player.getInventory());
            InventoryManager oldInventory = (InventoryManager) gamePlayer.getMetadata().get("set_kit_inventory.oldInventory");
            oldInventory.give(player);

            Kit kit = (Kit) gamePlayer.getMetadata().get("set_kit_inventory.kit");
            MessageManager.get(gamePlayer, "chat.set_kit_inventory.closed_inventory")
                    .replace("%kit%", kit.getName())
                    .send();

            ComponentBuilder message = new ComponentBuilder(MessageManager.get(gamePlayer, "chat.set_kit_inventory.closed_inventory.wanna_save").getTranslated());
            message.event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/saveinventory"));
            message.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(MessageManager.get(gamePlayer, "chat.set_kit_inventory.closed_inventory.wanna_save.hover").getTranslated()).create()));
            player.spigot().sendMessage(message.create());

            /*Bukkit.getScheduler().runTaskLater(GameAPI.getInstance(), () -> {
                player.openInventory(event.getInventory());
            }, 1L);*
        }
    }*/

    private static Inventory getCopyOfInventory(Inventory playerInventory, Kit kit, boolean autoArmor){
        Inventory finalInventory = Bukkit.createInventory(null, InventoryType.PLAYER);
        //finalInventory.setContents(inventory.getContents());
        for (int i = 0; i < playerInventory.getSize(); i++) {
            ItemStack item = playerInventory.getItem(i);
            if (item != null) {
                finalInventory.setItem(i, item);
            }
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

    private static void save(GamePlayer gamePlayer, Kit kit, Inventory playerInventory, boolean autoArmor){
        Player player = gamePlayer.getOnlinePlayer();
        //player.closeInventory();

        InventoryManager oldInventory = (InventoryManager) gamePlayer.getMetadata().get("set_kit_inventory.oldInventory");


        gamePlayer.getPlayerData().setKitInventory(kit, getCopyOfInventory(playerInventory, kit, autoArmor));

        player.getInventory().clear();
        oldInventory.give(player);

        MessageManager.get(gamePlayer, "chat.set_kit_inventory.saved")
                .replace("%kit%", kit.getName())
                .send();
        gamePlayer.getMetadata().remove("set_kit_inventory.kit");
    }

    public static void setKitInventory(GamePlayer gamePlayer, Kit kit) {
        Player player = gamePlayer.getOnlinePlayer();
        Inventory inventory = player.getInventory();
        InventoryManager oldInventory = gamePlayer.getPlayerData().getCurrentInventory();
        Inventory currentKitInventory = gamePlayer.getPlayerData().getKitInventory(kit);


        gamePlayer.getMetadata().put("set_kit_inventory.kit", kit);
        gamePlayer.getMetadata().put("set_kit_inventory.oldInventory", oldInventory);

        /*inventory.setContents(Arrays.stream(currentKitInventory.getContents())
                .filter(Objects::nonNull)
                .filter(is -> !is.getType().equals(Material.AIR))
                .filter(is -> !(is.getType().toString().toLowerCase().contains("helmet")
                        || is.getType().toString().toLowerCase().contains("chestplate")
                        || is.getType().toString().toLowerCase().contains("leggings")
                        || is.getType().toString().toLowerCase().contains("boots")))
                .toArray(ItemStack[]::new));*/ //bere to vždy armor i když není auto
        inventory.setContents(currentKitInventory.getContents());

        openGUI(gamePlayer, kit);
    }

    public static void openGUI(GamePlayer gamePlayer, Kit kit){
        Inventory inventory = gamePlayer.getOnlinePlayer().getInventory();

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


        GUI finalGUI = Component.gui()
                .title("Inventory Editor")
                .rows(1)
                .prepare((gui, guiPlayer) -> {
                    AutoArmor autoArmorItem = getAutoArmorItem(gamePlayer, kit);
                    gamePlayer.getMetadata().put("set_kit_inventory.autoArmor", autoArmorItem.autoArmor);

                    gui.setElement(0, Component.element(new ItemBuilder(Material.ARROW).setName(MessageManager.get(gamePlayer, "inventory.item.go_back").getTranslated()).toItemStack()).addClick(i -> {
                        gamePlayer.getOnlinePlayer().getInventory().clear();
                        InventoryManager oldInventory = (InventoryManager) gamePlayer.getMetadata().get("set_kit_inventory.oldInventory");
                        oldInventory.give(guiPlayer);

                        gui.close(guiPlayer);
                        KitInventory.openKitInventory(gamePlayer);
                    }).build());
                    gui.setElement(1, Component.element(reset.toItemStack()).addClick(i -> {
                        inventory.setContents(kit.getContent().getContents());
                        openGUI(gamePlayer, kit);
                    }).build());


                    gui.setElement(4, Component.element(kitItem.toItemStack()).build());


                    if (containsArmor(kit.getContent().getContents())) {
                        gui.setElement(7, Component.element(autoArmorItem.item).addClick(i -> {
                            if (autoArmorItem.autoArmor) {
                                getArmor(kit.getContent().getContents()).forEach(inventory::addItem);
                                openGUI(gamePlayer, kit);
                            }else{
                                getArmor(kit.getContent().getContents()).forEach(inventory::removeItem);
                                openGUI(gamePlayer, kit);
                            }
                        }).build());
                    }
                    gui.setElement(8, Component.element(save.toItemStack()).addClick(i -> {
                        gui.close(guiPlayer);
                        KitInventory.openKitInventory(gamePlayer);
                        save(gamePlayer, kit, inventory, autoArmorItem.autoArmor);
                    }).build());



                    gui.onClose(GUI.CloseReason.BY_PLAYER, player -> {
                        gamePlayer.getMetadata().put("set_kit_inventory.inventory", getCopyOfInventory(inventory, kit, autoArmorItem.autoArmor));
                        InventoryManager oldInventory = (InventoryManager) gamePlayer.getMetadata().get("set_kit_inventory.oldInventory");
                        oldInventory.give(player);

                        MessageManager.get(gamePlayer, "chat.set_kit_inventory.closed_inventory")
                                .replace("%kit%", kit.getName())
                                .send();

                        ComponentBuilder message = new ComponentBuilder(MessageManager.get(gamePlayer, "chat.set_kit_inventory.closed_inventory.wanna_save").getTranslated());
                        message.event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/saveinventory"));
                        message.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(MessageManager.get(gamePlayer, "chat.set_kit_inventory.closed_inventory.wanna_save.hover").getTranslated()).create()));
                        player.spigot().sendMessage(message.create());
                    });
                })
                .build();


                finalGUI.open(gamePlayer.getOnlinePlayer());
    }

    private static AutoArmor getAutoArmorItem(GamePlayer gamePlayer, Kit kit){
        Inventory currentKitInventory = gamePlayer.getOnlinePlayer().getInventory();

        boolean cArmor = containsArmor(kit.getContent().getContents());
        boolean autoArmor = cArmor && !containsArmor(currentKitInventory.getStorageContents());

        ItemBuilder armor = new ItemBuilder(Material.LEATHER_CHESTPLATE);
        armor.setName((autoArmor ? "#72f622" : "§c") + MessageManager.get(gamePlayer, "inventory.set_kit_inventory.item.auto_equip_armor").getTranslated());
        armor.addLoreLine("");
        if (autoArmor) {
            MessageManager.get(gamePlayer, "inventory.set_kit_inventory.item.auto_equip_armor.action1").addToItemLore(armor);
        }else{
            MessageManager.get(gamePlayer, "inventory.set_kit_inventory.item.auto_equip_armor.action2").addToItemLore(armor);
        }

        return new AutoArmor(autoArmor, armor.toItemStack());
    }

    public record AutoArmor(boolean autoArmor, ItemStack item){}

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
