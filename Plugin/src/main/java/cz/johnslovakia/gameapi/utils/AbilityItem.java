package cz.johnslovakia.gameapi.utils;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.users.PlayerManager;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class AbilityItem implements Listener {

    private final String name;
    private final ItemStack itemStack;

    private final Map<Action, Consumer<GamePlayer>> actions;
    private final Map<Action, Cooldown> cooldowns;
    private final String loreTranslationKey;


    private AbilityItem(Builder builder){
        this.name = builder.name;
        this.itemStack = builder.itemStack;

        this.actions = builder.actions;
        this.cooldowns = builder.cooldowns;
        this.loreTranslationKey = builder.loreTranslationKey;

        Bukkit.getPluginManager().registerEvents(this, GameAPI.getInstance());
    }

    public ItemStack getFinalItemStack(GamePlayer gamePlayer){
        ItemBuilder finalItem = new ItemBuilder(itemStack);
        finalItem.setName("§a" + name);
        if (loreTranslationKey != null) {
            finalItem.setLore("");
            MessageManager.get(gamePlayer, loreTranslationKey).addToItemLore(finalItem);
        }
        return finalItem.toItemStack();
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
    public void onInventoryPickupItem(InventoryClickEvent e) {
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


        if (e.getItem() == null || !e.getItem().equals(getFinalItemStack(gamePlayer))){
            return;
        }


        for (Action action : actions.keySet()){

            if (action.equals(Action.LEFT_CLICK) && !(e.getAction().equals(org.bukkit.event.block.Action.LEFT_CLICK_BLOCK) || e.getAction().equals(org.bukkit.event.block.Action.LEFT_CLICK_AIR))){
                continue;
            }else if (action.equals(Action.RIGHT_CLICK) && !(e.getAction().equals(org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) || e.getAction().equals(org.bukkit.event.block.Action.RIGHT_CLICK_AIR))){
                continue;
            }

            /*if (!e.getAction().equals(action)){
                continue;
            }*/
            Cooldown cooldown = null;
            if (!cooldowns.isEmpty() && cooldowns.get(action) != null){
                cooldown = cooldowns.get(action);
            }
            if (cooldown != null && cooldown.contains(gamePlayer)){
                String roundedDouble = String.valueOf(Math.round(cooldown.getCountdown(gamePlayer) * 100.0) / 100.0);
                MessageManager.get(player, "chat.delay").replace("%countdown%", roundedDouble).send();
                return;
            }
            actions.get(action).accept(gamePlayer);
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

        public AbilityItem build() {
            return new AbilityItem(this);
        }
    }
}
