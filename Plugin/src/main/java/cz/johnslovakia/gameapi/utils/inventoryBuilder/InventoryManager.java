package cz.johnslovakia.gameapi.utils.inventoryBuilder;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.events.GameQuitEvent;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.users.PlayerManager;
import cz.johnslovakia.gameapi.utils.ItemBuilder;

import cz.johnslovakia.gameapi.utils.Logger;
import cz.johnslovakia.gameapi.utils.StringUtils;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.List;

@Getter
public class InventoryManager implements Listener {

    private final String name;
    private int holdItemSlot;

    private final List<Item> items = new ArrayList<>();
    private final List<Player> players = new ArrayList<>();
    private ItemStack fillFreeSlots;

    public InventoryManager(String name) {
        this.name = name;
        Bukkit.getPluginManager().registerEvents(this, Minigame.getInstance().getPlugin());
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

        if (!players.contains(player)) {
            return;
        }
        if (item == null){
            return;
        }
        if (item.equals(fillFreeSlots)){
            e.setCancelled(true);
            return;
        }
        if (item.getType().equals(Material.AIR) || !item.hasItemMeta()){
            return;
        }
        if (!item.getItemMeta().hasDisplayName()){
            return;
        }
        if (!(action == Action.RIGHT_CLICK_BLOCK || action == Action.RIGHT_CLICK_AIR)) {
            return;
        }

        Item jItem = getItemByString(player, item.getItemMeta().getDisplayName());
        if (jItem == null){
            return;
        }

        if (jItem.getConsumer() != null) jItem.getConsumer().accept(e);

        e.setCancelled(true);

    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent e) {
        Player player = e.getPlayer();
        org.bukkit.entity.Item item = e.getItemDrop();

        if (item.getItemStack().equals(fillFreeSlots)){
            e.setCancelled(true);
            return;
        }
        if (!item.getItemStack().hasItemMeta()){
            return;
        }
        if (!item.getItemStack().getItemMeta().hasDisplayName()){
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

    public void onItemDurabilityChange(PlayerItemDamageEvent e){
        if (!e.getItem().hasItemMeta()){
            return;
        }
        if (!e.getItem().getItemMeta().hasDisplayName()){
            return;
        }
        if (getItemByString(e.getPlayer(), e.getItem().getItemMeta().getDisplayName()) != null){
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
        give(player, true);
    }

    public void give(Player player, boolean clearInventory){
        if (!players.contains(player)) {
            players.add(player);
        }
        PlayerInventory inventory = player.getInventory();

        if (clearInventory) {
            inventory.clear();
        }

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
        PlayerManager.getGamePlayer(player).getPlayerData().setCurrentInventory(this);
    }

    public void unloadInventory(Player p) {
        if (!players.contains(p)) {
            return;
        }
        players.remove(p);
        Inventory inv = p.getInventory();
        for (Item ji : items) {
            Component component = MessageManager.get(p, ji.getTranslateKey()).getTranslated();
            if (component != null){
                for (ItemStack item : p.getInventory().getContents()){
                    if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()){
                        continue;
                    }

                    if (item.getItemMeta().displayName().equals(component)){
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
                Component component = MessageManager.get(p, ji.getTranslateKey()).getTranslated();
                if (component != null){
                    for (ItemStack item : p.getInventory().getContents()){
                        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()){
                            continue;
                        }

                        if (item.getItemMeta().displayName().equals(component)){
                            inv.remove(item);
                        }
                    }
                }
            }
            p.updateInventory();
        }
        players.clear();
    }



    public Item getItemByString(Player player, String name) {
        for (Item item : items) {
            final String s = ChatColor.stripColor(
                    LegacyComponentSerializer.legacySection().serialize(
                            MessageManager.get(player, item.getTranslateKey()).getTranslated()
                    )
            );
            if (ChatColor.stripColor(name).equalsIgnoreCase(s)) {
                return item;
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

    public boolean containsItem(String displayName){
        List<String> translatedList = new ArrayList<>();
        for (Item item : getItems()){
            for (String translated : MessageManager.getMessagesByName(item.getTranslateKey())){
                translatedList.add(translated);
            }
        }

        return translatedList.contains(displayName);
    }
}