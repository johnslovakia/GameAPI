package cz.johnslovakia.gameapi.inventoryBuilder;

import cz.johnslovakia.gameapi.Shared;
import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.messages.MessageModule;
import cz.johnslovakia.gameapi.users.PlayerIdentity;
import cz.johnslovakia.gameapi.users.PlayerIdentityRegistry;
import cz.johnslovakia.gameapi.utils.ItemBuilder;

import cz.johnslovakia.gameapi.utils.Initialize;
import cz.johnslovakia.gameapi.utils.Terminate;
import cz.johnslovakia.gameapi.utils.Utils;
import lombok.Getter;

import lombok.Setter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

@Getter
public class InventoryBuilder implements Listener, Initialize, Terminate {

    public static final NamespacedKey ITEM_KEY = new NamespacedKey(Shared.getInstance().getPlugin(), "inventory_item_id");
    private static final Map<PlayerIdentity, InventoryBuilder> playerInventories = new HashMap<>();

    public static InventoryBuilder getPlayerCurrentInventory(PlayerIdentity playerIdentity) {
        return playerInventories.get(playerIdentity);
    }


    private final String name;
    @Setter
    private boolean closeInventoryListener = true;
    private int holdItemSlot;

    private List<Item> items = new ArrayList<>();
    private List<Player> players = new ArrayList<>();
    private ItemStack fillFreeSlots;

    public InventoryBuilder(String name) {
        this.name = name;
        initialize();
    }

    @Override
    public void initialize() {
        Bukkit.getPluginManager().registerEvents(this, Shared.getInstance().getPlugin());
    }

    @Override
    public void terminate() {
        HandlerList.unregisterAll(this);
        players = null;
        items = null;
    }


    public void registerItem(Item... items) {
        for (Item item : items) {
            ItemStack is = item.getItem();
            ItemMeta meta = is.getItemMeta();
            meta.getPersistentDataContainer().set(ITEM_KEY, PersistentDataType.STRING, item.getTranslationKey());
            is.setItemMeta(meta);
            this.items.add(item);
        }
    }

    public InventoryBuilder setHoldItemSlot(int slot){
        this.holdItemSlot = slot;
        return this;
    }

    public InventoryBuilder setFill(ItemStack itemStack) {
        this.fillFreeSlots = itemStack;
        return this;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        Action action = e.getAction();
        ItemStack itemStack = e.getItem();

        if (!players.contains(player) || itemStack == null || itemStack.getType().equals(Material.AIR)
                || !itemStack.hasItemMeta() || !(action == Action.RIGHT_CLICK_BLOCK || action == Action.RIGHT_CLICK_AIR)) {
            return;
        }
        if (itemStack.isSimilar(fillFreeSlots)){
            e.setCancelled(true);
            return;
        }

        getItemByItemStack(itemStack).ifPresent(item -> {
            Optional.ofNullable(item.getConsumer()).ifPresent(consumer -> consumer.accept(e));
            e.setCancelled(true);
        });
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent e) {
        Player player = e.getPlayer();
        ItemStack itemStack = e.getItemDrop().getItemStack();

        if (!players.contains(player) || !itemStack.hasItemMeta()) {
            return;
        }
        if (itemStack.isSimilar(fillFreeSlots)){
            e.setCancelled(true);
            return;
        }

        getItemByItemStack(itemStack).ifPresent(item -> {
            e.setCancelled(true);
        });
    }

    @EventHandler
    public void onItemDurabilityChange(PlayerItemDamageEvent e){
        ItemStack itemStack = e.getItem();
        Player player = e.getPlayer();

        if (!players.contains(player) || !itemStack.hasItemMeta()) {
            return;
        }
        if (itemStack.isSimilar(fillFreeSlots)){
            e.setCancelled(true);
            return;
        }

        getItemByItemStack(itemStack).ifPresent(item -> {
            e.setCancelled(true);
        });
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent e) {
        Player player = e.getPlayer();

        if (players.contains(player)){
            give(PlayerIdentityRegistry.get(player));
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!closeInventoryListener) return;
        if (e.getPlayer() instanceof Player player) {
            if (!players.contains(player)) return;

            give(PlayerIdentityRegistry.get(player));
        }
    }

    public void give(PlayerIdentity playerIdentity){
        give(playerIdentity, true);
    }

    public void give(PlayerIdentity playerIdentity, boolean clearInventory){
        Player player = playerIdentity.getOnlinePlayer();

        InventoryBuilder oldInventoryManager = getPlayerCurrentInventory(playerIdentity);
        if (oldInventoryManager != null && oldInventoryManager != this) oldInventoryManager.unloadInventory(playerIdentity);

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
            ItemBuilder translated = new ItemBuilder((item.isPlayerHead() ? Utils.getPlayerHead(player) : item.getItem()))
                    .setName(ModuleManager.getModule(MessageModule.class).get(playerIdentity, item.getTranslationKey()).getTranslated());
            if (item.getBlinking() != null &&  item.getBlinking().test(playerIdentity)){
                translated.setCustomModelData(item.getBlinkingItemCustomModelData());
            }

            inventory.setItem(item.getSlot(), translated.toItemStack());
        }
        //inventory.setHeldItemSlot(holdItemSlot);
        playerInventories.put(playerIdentity, this);
    }

    public void unloadInventory(PlayerIdentity playerIdentity) {
        Player player = playerIdentity.getOnlinePlayer();

        if (!players.contains(player)) return;
        players.remove(player);

        InventoryBuilder currentInventory = playerInventories.get(playerIdentity);
        if (currentInventory != null && currentInventory.equals(this)) playerInventories.remove(playerIdentity);

        Inventory inv = player.getInventory();
        for (Item item : items) {
            Component component = ModuleManager.getModule(MessageModule.class).get(playerIdentity, item.getTranslationKey()).getTranslated();
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
       players.forEach(player -> unloadInventory(PlayerIdentityRegistry.get(player)));
    }

    private Optional<Item> getItemByItemStack(ItemStack is) {
        if (!is.hasItemMeta()) return Optional.empty();
        String key = is.getItemMeta().getPersistentDataContainer().get(ITEM_KEY, PersistentDataType.STRING);
        return getItemByTranslationKey(key);
    }

    public Optional<Item> getItemByTranslationKey(String key){
        return items.stream().filter(item -> item.getTranslationKey().equalsIgnoreCase(key)).findAny();
    }
}