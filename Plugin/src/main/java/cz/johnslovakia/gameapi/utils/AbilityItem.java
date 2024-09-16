package cz.johnslovakia.gameapi.utils;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.users.PlayerManager;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class AbilityItem implements Listener {

    private final String name;
    private final ItemStack itemStack;

    private Map<Action, Consumer<GamePlayer>> actions = new HashMap<>();
    private Map<Action, Cooldown> cooldowns = new HashMap<>();
    private final String loreTranslationKey;


    private AbilityItem(Builder builder){
        this.name = builder.name;
        this.itemStack = builder.itemStack;

        this.actions = builder.actions;
        this.cooldowns = builder.cooldowns;
        this.loreTranslationKey = builder.loreTranslationKey;

        Bukkit.getPluginManager().registerEvents(this, GameAPI.getInstance());
    }

    private ItemStack getFinalItemStack(GamePlayer gamePlayer){
        ItemBuilder finalItem = new ItemBuilder(itemStack);
        finalItem.setName("§a" + name);
        if (loreTranslationKey != null) {
            finalItem.setLore("");
            finalItem.addLoreLine(MessageManager.get(gamePlayer, loreTranslationKey).getTranslated());
        }
        return finalItem.toItemStack();
    }


    @EventHandler
    public void onInventoryInteract(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);


        if (e.getItem() == null || !e.getItem().equals(getFinalItemStack(gamePlayer))){
            return;
        }

        for (Action action : actions.keySet()){
            if (!e.getAction().equals(action)){
                continue;
            }
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
        }

    }


    public class Builder{

        private final String name;
        private final ItemStack itemStack;

        private Map<Action, Consumer<GamePlayer>> actions = new HashMap<>();
        private Map<Action, Cooldown> cooldowns = new HashMap<>();
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
    }
}
