package cz.johnslovakia.gameapi.utils.inventoryBuilder;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.events.GameQuitEvent;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.List;

public class InventoryManager implements Listener {

    private static List<InventoryManager> managers = new ArrayList<>();

    private String name;
    private int holdItemSlot;

    private List<Item> items = new ArrayList<>();
    private List<Player> players = new ArrayList<>();
    private ItemStack fillFreeSlots;

    public InventoryManager(String name) {
        this.name = name;
        Bukkit.getPluginManager().registerEvents(this, GameAPI.getInstance());
        managers.add(this);
    }

    public void registerItem(Item... item){
        for (Item i : item) {
            if (!items.contains(i)) {
                items.add(i);
            }
        }
    }

    public InventoryManager setHoldItemSlot(int slot){
        this.holdItemSlot = slot;
        return this;
    }

    public InventoryManager setFill(ItemStack itemStack) {
        this.fillFreeSlots = itemStack;
        return this;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        Action action = e.getAction();
        ItemStack item = e.getItem();

        if (item == null){
            return;
        }
        if (!players.contains(player)) {
            return;
        }
        if (!(action == Action.RIGHT_CLICK_BLOCK || action == Action.RIGHT_CLICK_AIR)) {
            return;
        }

        if (item.getType().equals(Material.AIR)){
            return;
        }

        if (item.getItemMeta() == null){
            return;
        }
        if (item.getItemMeta().getDisplayName() == null){
            return;
        }

        Item jItem = getItemByString(player, item.getItemMeta().getDisplayName());
        if (jItem == null){
            return;
        }
        jItem.run(e);
        e.setCancelled(true);

    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent e) {
        Player player = e.getPlayer();
        org.bukkit.entity.Item item = e.getItemDrop();

        if (item.getItemStack().getItemMeta() == null){
            return;
        }
        if (item.getItemStack().getItemMeta().getDisplayName() == null){
            return;
        }
        if (!players.contains(player)){
            return;
        }

        Item jItem = getItemByString(player, item.getItemStack().getItemMeta().getDisplayName());
        if (jItem != null){
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        Player player = (Player) e.getWhoClicked();
        Inventory inv = e.getInventory();
        ItemStack item = e.getCurrentItem();

        if (!players.contains(player)) {
            return;
        }
        if (item == null) {
            return;
        }
        if (item.getType() == Material.AIR) {
            return;
        }
        if (!item.hasItemMeta()){
            return;
        }
        if (item.getItemMeta().getDisplayName() == null){
            return;
        }
        if (getItemByString(player, item.getItemMeta().getDisplayName()) == null) {
            return;
        }
        e.setCancelled(true);
    }

    public void onItemDurabilityChange(PlayerItemDamageEvent e){
        if (getItemByString(e.getItem().getItemMeta().getDisplayName()) != null){
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent e) {
        Player player = e.getPlayer();

        if (players.contains(player)){
            give(player);
        }
    }

    @EventHandler
    public void onPlayerQuit(GameQuitEvent e) {
        Player player = e.getGamePlayer().getOnlinePlayer();

        if (players.contains(player)){
            unloadInventory(player);
        }
    }

    public void give(Player player){
        if (!players.contains(player)) {
            players.add(player);
        }
        PlayerInventory inventory = player.getInventory();

        inventory.clear();

        if (fillFreeSlots != null){
            for (int i = 0; i < inventory.getSize(); i++) {
                inventory.setItem(i, fillFreeSlots);
            }
        }

        for (Item item : items){
            ItemStack translated = new ItemBuilder((item.isPlayerHead() ? GameAPI.getInstance().getVersionSupport().getPlayerHead(player) : item.getItem()))
                    .setName(MessageManager.get(player, item.getTranslateKey()).getTranslated()).toItemStack();

            inventory.setItem(item.getSlot(), translated);
        }
        inventory.setHeldItemSlot(holdItemSlot);
        //player.updateInventory();

    }

    public void unloadInventory(Player p) {
        if (!players.contains(p)) {
            return;
        }
        players.remove(p);
        Inventory inv = p.getInventory();
        for (Item ji : items) {
            final String s = MessageManager.get(p, ji.getTranslateKey()).getTranslated();
            if (s != null){
                for (ItemStack item : p.getInventory().getContents()){
                    if (item == null){
                        continue;
                    }
                    if (!item.hasItemMeta()){
                        continue;
                    }
                    if (item.getItemMeta().getDisplayName() == null){
                        continue;
                    }

                    if (item.getItemMeta().getDisplayName().equalsIgnoreCase(s)){
                        //inv.remove(item);
                        inv.remove(item);
                    }
                }
            }
        }
    }

    public void unloadAllInventories() {
        for (Player p : players) {
            if (!players.contains(p)) {
                return;
            }
            Inventory inv = p.getInventory();
            for (Item ji : items) {
                final String s = MessageManager.get(p, ji.getTranslateKey()).getTranslated();
                if (s != null){
                    for (ItemStack item : p.getInventory().getContents()){
                        if (item == null){
                            continue;
                        }
                        if (!item.hasItemMeta()){
                            continue;
                        }
                        if (item.getItemMeta().getDisplayName() == null){
                            continue;
                        }

                        if (item.getItemMeta().getDisplayName().equalsIgnoreCase(s)){
                            inv.remove(item);
                        }
                    }
                }
            }
            p.updateInventory();
        }
        players.clear();
    }



    public Item getItemByString(Player player, String name){
        for (Item item : items){
            final String s = MessageManager.get(player, item.getTranslateKey()).getTranslated();
            if (s != null) {
                if (name.equals(s)) {
                    return item;
                }
            }
        }
        return null;
    }

    public Item getItemByTranslateKey(String key){
        for (Item item : items){
            if (item.getTranslateKey().equals(key)){
                return item;
            }
        }
        return null;
    }

    public Item getItemByString(String name){
        for (Item item : items){
            for (String translated : MessageManager.getMessagesByName(item.getTranslateKey())){
                if (MessageManager.getMessagesByMSG(name).contains(translated)){
                    return item;
                }
            }
        }
        return null;
    }

    public boolean containsItem(String displayName){
        List<String> translatedList = new ArrayList<>();
        for (Item item : getItems()){
            for (String translated : MessageManager.getMessagesByName(item.getTranslateKey())){
                translatedList.add(translated);
            }
        }

        return translatedList.contains(displayName);
    }

    public String getName() {
        return name;
    }

    public List<Item> getItems() {
        return items;
    }

    public List<Player> getPlayers() {
        return players;
    }




    public static InventoryManager getInventoryByName(String name){
        for (InventoryManager manager : getManagers()){
            if (manager.getName().equalsIgnoreCase(name)){
                return manager;
            }
        }
        return null;
    }

    public static List<InventoryManager> getManagers() {
        return managers;
    }
}