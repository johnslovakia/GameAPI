package cz.johnslovakia.gameapi.utils.inventoryBuilder;

import com.comphenix.protocol.PacketType;
import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.events.GameQuitEvent;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
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
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;

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
        //TODO: ...
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

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (e.getPlayer() instanceof Player player) {
            if (!players.contains(player)) {
                return;
            }
            GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);

            //TODO: asi nefunguje
            /*if (gamePlayer.getMetadata().containsKey("inventory_manager_blinking_task")){
                ((BukkitRunnable) gamePlayer.getMetadata().get("inventory_manager_blinking_task")).cancel();
                gamePlayer.getMetadata().remove("inventory_manager_blinking_task");
            }*/
            give(player);
        }
    }

    public void give(Player player){
        give(player, true);
    }

    public void give(Player player, boolean clearInventory){
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);

        InventoryManager oldInventoryManager = gamePlayer.getPlayerData().getCurrentInventory();
        if (oldInventoryManager != null && oldInventoryManager != this) oldInventoryManager.unloadInventory(player);

        PlayerInventory inventory = player.getInventory();
        if (!players.contains(player)) {
            inventory.setHeldItemSlot(holdItemSlot);
            players.add(player);
        }

        if (clearInventory) {
            for (int i = 0; i < 36; i++) {
                inventory.clear(i);
            }
        }

        if (fillFreeSlots != null){
            for (int i = 0; i < inventory.getSize(); i++) {
                inventory.setItem(i, fillFreeSlots);
            }
        }

        for (Item item : items){
            ItemBuilder translated = new ItemBuilder((item.isPlayerHead() ? GameAPI.getInstance().getVersionSupport().getPlayerHead(player) : item.getItem()))
                    .setName(MessageManager.get(player, item.getTranslateKey()).getTranslated());
            if (item.getBlinking() != null &&  item.getBlinking().test(gamePlayer)){
                translated.setCustomModelData(item.getBlinkingItemCustomModelData());
            }

            inventory.setItem(item.getSlot(), translated.toItemStack());
        }
        //inventory.setHeldItemSlot(holdItemSlot);
        PlayerManager.getGamePlayer(player).getPlayerData().setCurrentInventory(this);
    }

    public void unloadInventory(Player player) {
        if (!players.contains(player)) return;
        players.remove(player);

        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);

        InventoryManager currentInventory = gamePlayer.getPlayerData().getCurrentInventory();
        if (currentInventory != null && currentInventory.equals(this)) gamePlayer.getPlayerData().setCurrentInventory(null);

        Inventory inv = player.getInventory();
        for (Item item : items) {
            Component component = MessageManager.get(player, item.getTranslateKey()).getTranslated();
            if (component != null){
                for (ItemStack itemStack : player.getInventory().getContents()){
                    if (itemStack == null || !itemStack.hasItemMeta() || !itemStack.getItemMeta().hasDisplayName()){
                        continue;
                    }

                    if (itemStack.getItemMeta().displayName().equals(component)){
                        inv.remove(itemStack);
                    }
                }
            }
        }
    }

    public void unloadAllInventories() {
       players.forEach(this::unloadInventory);
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