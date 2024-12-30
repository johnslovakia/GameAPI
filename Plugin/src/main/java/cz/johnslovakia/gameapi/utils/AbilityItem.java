package cz.johnslovakia.gameapi.utils;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.game.GameState;
import cz.johnslovakia.gameapi.messages.Language;
import cz.johnslovakia.gameapi.users.PlayerManager;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.users.GamePlayer;

import lombok.Getter;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.function.Consumer;

public class AbilityItem implements Listener {

    @Getter
    public static List<AbilityItem> abilityItems = new ArrayList<>();

    public static AbilityItem getAbilityItem(String name){
        for (AbilityItem abilityItem : abilityItems){
            if (abilityItem.getName().equalsIgnoreCase(name)){
                return abilityItem;
            }
        }
        return null;
    }


    @Getter
    private final String name;
    private final ItemStack itemStack;

    private final Map<Action, Consumer<GamePlayer>> actions;
    private final Map<Action, Cooldown> cooldowns;
    private final String loreTranslationKey;
    private final boolean consumable;


    private AbilityItem(Builder builder){
        this.name = builder.name;
        this.itemStack = builder.itemStack;

        this.actions = builder.actions;
        this.cooldowns = builder.cooldowns;
        this.loreTranslationKey = builder.loreTranslationKey;
        this.consumable = builder.consumable;

        abilityItems.add(this);

        Bukkit.getPluginManager().registerEvents(this, GameAPI.getInstance());
    }

    public ItemStack getFinalItemStack(){
        return getFinalItemStack(1);
    }

    public ItemStack getFinalItemStack(int amount){
        ItemBuilder finalItem = new ItemBuilder(itemStack, amount);
        finalItem.setName("§a" + name);
        if (loreTranslationKey != null) {
            finalItem.setLore(MessageManager.get(Language.getDefaultLanguage(), loreTranslationKey));
        }
        return finalItem.toItemStack();
    }

    public ItemStack getFinalItemStack(GamePlayer gamePlayer){
        return getFinalItemStack(gamePlayer, 1);
    }

    public ItemStack getFinalItemStack(GamePlayer gamePlayer, int amount){
        ItemBuilder finalItem = new ItemBuilder(itemStack, amount);
        finalItem.setName("§a" + name);
        if (loreTranslationKey != null) {
            finalItem.setLore(MessageManager.get(gamePlayer, loreTranslationKey).getTranslated());
        }
        return finalItem.toItemStack();
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent e) {
        if (!(e.getPlayer() instanceof Player player)){
            return;
        }
        if (!e.getInventory().getType().equals(InventoryType.CHEST) || e.getInventory().getLocation() == null){
            return;
        }
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);

        Arrays.stream(e.getInventory().getContents()).toList().forEach(itemStack -> {
            if (itemStack == null){
                return;
            }
            if (itemStack.getType().equals(Material.AIR)){
                return;
            }
            if (itemStack.equals(getFinalItemStack())){
                itemStack.setItemMeta(getFinalItemStack(gamePlayer).getItemMeta());
            }
        });
    }

    @EventHandler
    public void onInventoryPickupItem(EntityPickupItemEvent e) {
        if (!(e.getEntity() instanceof Player player)){
            return;
        }
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);
        if (e.getItem().getItemStack().getItemMeta() == null){
            return;
        }
        ItemMeta meta = e.getItem().getItemStack().getItemMeta();

        if (meta == null){
            return;
        }

        if (meta.getDisplayName().contains(name)){
            if (loreTranslationKey != null) {
                meta.setLore(Collections.singletonList(MessageManager.get(gamePlayer, loreTranslationKey).getTranslated()));
                ItemBuilder item = new ItemBuilder(e.getItem().getItemStack());
                item.setLore(MessageManager.get(gamePlayer, loreTranslationKey).getTranslated());
                e.getItem().setItemStack(item.toItemStack());
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)){
            return;
        }
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);
        if (e.getCurrentItem() == null){
            return;
        }
        if (e.getCurrentItem().getItemMeta() == null){
            return;
        }
        ItemMeta meta = e.getCurrentItem().getItemMeta();

        if (meta == null){
            return;
        }

        if (meta.getDisplayName().contains(name)){
            if (loreTranslationKey != null) {
                meta.setLore(Collections.singletonList(MessageManager.get(gamePlayer, loreTranslationKey).getTranslated()));
                ItemBuilder item = new ItemBuilder(e.getCurrentItem());
                item.setLore(MessageManager.get(gamePlayer, loreTranslationKey).getTranslated());
                e.setCurrentItem(item.toItemStack());
            }
        }
    }

    @EventHandler
    private void onInventoryInteract(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);
        ItemStack item = e.getItem();

        if (item == null || item.getItemMeta() == null){
            return;
        }

        if (!item.getItemMeta().getDisplayName().equals(getFinalItemStack(gamePlayer).getItemMeta().getDisplayName())
        || !item.getItemMeta().getLore().equals(getFinalItemStack(gamePlayer).getItemMeta().getLore())){
            return;
        }
        if (gamePlayer.getPlayerData().getGame().getState() != GameState.INGAME){
            e.setCancelled(true);
            return;
        }


        for (Action action : actions.keySet()){

            if (!action.equals(Action.DEFAULT)) {
                if (action.equals(Action.LEFT_CLICK) && !(e.getAction().equals(org.bukkit.event.block.Action.LEFT_CLICK_BLOCK) || e.getAction().equals(org.bukkit.event.block.Action.LEFT_CLICK_AIR))) {
                    continue;
                } else if (action.equals(Action.RIGHT_CLICK) && !(e.getAction().equals(org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) || e.getAction().equals(org.bukkit.event.block.Action.RIGHT_CLICK_AIR))) {
                    continue;
                }
            }

            /*if (!e.getAction().equals(action)){
                continue;
            }*/

            Block block = e.getClickedBlock();
            if (block != null){
                List<Material> materials = Arrays.asList(Material.CHEST, Material.CRAFTING_TABLE, Material.ENCHANTING_TABLE, Material.ANVIL, Material.CHIPPED_ANVIL, Material.DAMAGED_ANVIL);
                if (materials.contains(block.getType())){
                    return;
                }
            }

            if (!action.equals(Action.DEFAULT)) {
                e.setCancelled(true);
            }


            Cooldown cooldown = null;
            if (!cooldowns.isEmpty() && cooldowns.get(action) != null){
                cooldown = cooldowns.get(action);
            }
            if (cooldown != null && cooldown.contains(gamePlayer)){
                String roundedDouble = String.valueOf(Math.round(cooldown.getCountdown(gamePlayer) * 100.0) / 100.0);
                MessageManager.get(player, "chat.delay").replace("%countdown%", roundedDouble).send();
                e.setCancelled(true);
                return;
            }


            actions.get(action).accept(gamePlayer);
            if (consumable && e.getHand() != null){
                if (item.getAmount() > 1) {
                    item.setAmount(item.getAmount() - 1);
                } else {
                    player.getInventory().remove(item);
                }

            }
            if (cooldown != null) {
                cooldown.startCooldown(gamePlayer);
            }
        }
    }

    public enum Action{
        LEFT_CLICK, RIGHT_CLICK, DEFAULT;
    }


    public static class Builder {

        private final String name;
        private final ItemStack itemStack;

        private final Map<Action, Consumer<GamePlayer>> actions = new HashMap<>();
        private final Map<Action, Cooldown> cooldowns = new HashMap<>();
        private String loreTranslationKey;
        private boolean consumable = false;

        public Builder(String name, ItemStack itemStack) {
            this.name = name;
            this.itemStack = itemStack;
        }

        public Builder addAction(Action action, Consumer<GamePlayer> consumer) {
            actions.put(action, consumer);
            return this;
        }

        public Builder addCooldown(Action action, Cooldown cooldown) {
            cooldowns.put(action, cooldown);
            return this;
        }

        public Builder setLoreTranslationKey(String loreTranslationKey) {
            this.loreTranslationKey = loreTranslationKey;
            return this;
        }

        public Builder setConsumable(boolean consumable) {
            this.consumable = consumable;
            return this;
        }

        public AbilityItem build() {
            AbilityItem abilityItem = getAbilityItem(name);
            if (abilityItem != null){
                return abilityItem;
            }

            return new AbilityItem(this);
        }
    }
}
