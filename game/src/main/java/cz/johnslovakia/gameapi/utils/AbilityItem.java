package cz.johnslovakia.gameapi.utils;

import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.messages.Language;
import cz.johnslovakia.gameapi.modules.messages.MessageModule;
import cz.johnslovakia.gameapi.users.GamePlayer;

import lombok.Getter;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Getter
public class AbilityItem implements Listener{

    private static NamespacedKey ABILITY_ITEM_ID;// = new NamespacedKey(Minigame.getInstance().getPlugin(), "ability_id");

    public static boolean isAbilityItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(ABILITY_ITEM_ID, PersistentDataType.STRING);
    }

    public static void initialize(JavaPlugin javaPlugin) {
        ABILITY_ITEM_ID = new NamespacedKey(javaPlugin, "ability_id");
    }


    @Getter
    public static Map<String, AbilityItem> abilityItems = new ConcurrentHashMap<>();

    public static AbilityItem getAbilityItem(String name){
        return abilityItems.get(name);
    }

    public static Optional<AbilityItem> getAbilityItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return Optional.empty();
        }

        ItemMeta meta = item.getItemMeta();
        String id = meta.getPersistentDataContainer().get(ABILITY_ITEM_ID, PersistentDataType.STRING);

        if (id == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(abilityItems.get(id));
    }

    /*public static Optional<AbilityItem> getAbilityItem(ItemStack item){
        return abilityItems.stream().filter(abilityItem -> abilityItem.getFinalItemStack().getItemMeta().getDisplayName().equals(item.getItemMeta().getDisplayName())).findAny();
    }*/


    private final String name;
    private final ItemStack itemStack;

    private final List<Validator<?>> validators;
    private final Map<Action, Consumer<ActionContext>> actions;
    private final Map<Action, Cooldown> cooldowns;
    private final String loreTranslationKey;
    private final boolean consumable;


    private AbilityItem(Builder builder){
        this.name = builder.name;
        this.itemStack = builder.itemStack;

        this.validators = builder.validators;
        this.actions = builder.actions;
        this.cooldowns = builder.cooldowns;
        this.loreTranslationKey = builder.loreTranslationKey;
        this.consumable = builder.consumable;

        abilityItems.put(builder.name, this);
    }

    public Cooldown getCooldown(Action action){
        return cooldowns.get(action);
    }

    public void consume(Player player, ItemStack itemStack){
        if (itemStack.getAmount() > 1) {
            itemStack.setAmount(itemStack.getAmount() - 1);
        } else {
            player.getInventory().remove(itemStack);
        }
    }

    public ItemStack getFinalItemStack(){
        return getFinalItemStack(1);
    }

    public ItemStack getFinalItemStack(int amount){
        ItemBuilder finalItem = new ItemBuilder(itemStack, amount);
        finalItem.setName("§a" + name);
        if (loreTranslationKey != null) {
            finalItem.setLore(ModuleManager.getModule(MessageModule.class).get(Language.getDefaultLanguage(), loreTranslationKey));
        }

        ItemStack itemStack = finalItem.toItemStack();
        ItemMeta meta = itemStack.getItemMeta();
        meta.getPersistentDataContainer().set(ABILITY_ITEM_ID, PersistentDataType.STRING, getName());
        itemStack.setItemMeta(meta);

        return itemStack;
    }

    public ItemStack getFinalItemStack(GamePlayer gamePlayer){
        return getFinalItemStack(gamePlayer, 1);
    }

    public ItemStack getFinalItemStack(GamePlayer gamePlayer, int amount){
        ItemBuilder finalItem = new ItemBuilder(itemStack, amount);
        finalItem.setName("§a" + name);
        if (loreTranslationKey != null) {
            finalItem.setLore(ModuleManager.getModule(MessageModule.class).get(gamePlayer, loreTranslationKey).getTranslated());
        }

        ItemStack itemStack = finalItem.toItemStack();
        ItemMeta meta = itemStack.getItemMeta();
        meta.getPersistentDataContainer().set(ABILITY_ITEM_ID, PersistentDataType.STRING, getName());
        itemStack.setItemMeta(meta);

        return itemStack;
    }

    public enum Action{
        LEFT_CLICK, RIGHT_CLICK, RIGHT_CLICK_ENTITY, ENTITY_DAMAGE, DEFAULT;
    }

    public record ActionContext(GamePlayer gamePlayer, AbilityItem abilityItem, PlayerInteractEvent playerInteractEvent, PlayerInteractEntityEvent playerInteractEntityEvent, EntityDamageByEntityEvent entityDamageByEntityEvent){}
    public record Validator<T>(Class<T> type, Predicate<T> validator, Consumer<T> consumer) {}

    public static class Builder {

        private final String name;
        private final ItemStack itemStack;

        private final List<Validator<?>> validators = new ArrayList<>();
        private final Map<Action, Consumer<ActionContext>> actions = new HashMap<>();
        private final Map<Action, Cooldown> cooldowns = new HashMap<>();
        private String loreTranslationKey;
        private boolean consumable = false;

        public Builder(String name, ItemStack itemStack) {
            this.name = name;
            this.itemStack = itemStack;
        }

        public <T> Builder addValidator(Class<T> type, Predicate<T> predicate, Consumer<T> consumer) {
            validators.add(new Validator<>(type, predicate, consumer));
            return this;
        }

        public Builder addAction(Action action, Consumer<ActionContext> consumer) {
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
