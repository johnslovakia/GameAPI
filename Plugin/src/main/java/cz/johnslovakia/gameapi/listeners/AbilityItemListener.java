package cz.johnslovakia.gameapi.listeners;

import cz.johnslovakia.gameapi.game.Game;
import cz.johnslovakia.gameapi.game.GameState;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerManager;
import cz.johnslovakia.gameapi.utils.AbilityItem;
import cz.johnslovakia.gameapi.utils.Cooldown;
import cz.johnslovakia.gameapi.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class AbilityItemListener implements Listener {


    /*@EventHandler
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
    }*/

    @EventHandler
    public void onInventoryPickupItem(EntityPickupItemEvent e) {
        if (!(e.getEntity() instanceof Player player)){
            return;
        }
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);

        Game game = gamePlayer.getGame();
        if (game == null) return;
        if (game.getState() != GameState.INGAME || game.isPreparation()) return;

        if (e.getItem().getItemStack().getItemMeta() == null){
            return;
        }
        Item item = e.getItem();

        if (!AbilityItem.isAbilityItem(e.getItem().getItemStack())) return;

        ItemMeta meta = item.getItemStack().getItemMeta();
        if (meta == null) return;


        Optional<AbilityItem> abilityItemOptional = AbilityItem.getAbilityItem(item.getItemStack());

        abilityItemOptional.ifPresent(abilityItem -> {
            if (abilityItem.getLoreTranslationKey() != null) {
                //meta.setLore(Collections.singletonList(MessageManager.get(gamePlayer, abilityItem.getLoreTranslationKey()).getTranslated()));
                ItemBuilder itemBuilder = new ItemBuilder(item.getItemStack());
                itemBuilder.setLore(MessageManager.get(gamePlayer, abilityItem.getLoreTranslationKey()).getTranslated());
                item.setItemStack(itemBuilder.toItemStack());
            }
        });
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)){
            return;
        }
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);

        Game game = gamePlayer.getGame();
        if (game == null) return;
        if (game.getState() != GameState.INGAME || game.isPreparation()) return;

        ItemStack itemStack = e.getCurrentItem();
        if (itemStack == null){
            return;
        }
        if (!AbilityItem.isAbilityItem(itemStack)) return;

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return;

        Optional<AbilityItem> abilityItemOptional = AbilityItem.getAbilityItem(itemStack);

        abilityItemOptional.ifPresent(abilityItem -> {
            if (abilityItem.getLoreTranslationKey() != null) {
                //meta.setLore(Collections.singletonList(MessageManager.get(gamePlayer, abilityItem.getLoreTranslationKey()).getTranslated()));
                ItemBuilder item = new ItemBuilder(itemStack);
                item.setLore(MessageManager.get(gamePlayer, abilityItem.getLoreTranslationKey()).getTranslated());
                e.setCurrentItem(item.toItemStack());
            }
        });
    }


    @EventHandler
    private void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player player) {
            GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);

            Game game = gamePlayer.getGame();
            if (game == null) return;
            if (game.getState() != GameState.INGAME || game.isPreparation()) return;

            ItemStack item = player.getInventory().getItemInMainHand();
            if (item.getType().isAir() || !item.hasItemMeta()) {
                return;
            }
            if (!AbilityItem.isAbilityItem(item)) return;

            Optional<AbilityItem> abilityItemOptional = AbilityItem.getAbilityItem(item);

            abilityItemOptional.ifPresent(abilityItem -> {
                if (gamePlayer.getGame().getState() != GameState.INGAME) {
                    e.setCancelled(true);
                    return;
                }

                for (AbilityItem.Action action : abilityItem.getActions().keySet()) {
                    if (!(action.equals(AbilityItem.Action.DEFAULT) || action.equals(AbilityItem.Action.ENTITY_DAMAGE))) {
                        continue;
                    }
                    if (!runValidators(abilityItem.getValidators(), gamePlayer) && !runValidators(abilityItem.getValidators(), e)) {
                        return;
                    }

                    if (!action.equals(AbilityItem.Action.DEFAULT)) {
                        e.setCancelled(true);
                    }


                    Cooldown cooldown = null;
                    if (!abilityItem.getPlayerCooldowns().isEmpty() && abilityItem.getPlayerCooldowns().get(gamePlayer) != null) {
                        cooldown = abilityItem.getPlayerCooldowns().get(gamePlayer);
                    } else if (!abilityItem.getCooldowns().isEmpty() && abilityItem.getCooldowns().get(action) != null) {
                        cooldown = abilityItem.getCooldowns().get(action);
                    }
                    if (cooldown != null && cooldown.contains(gamePlayer)) {
                        String roundedDouble = String.valueOf(Math.round(cooldown.getCountdown(gamePlayer) * 100.0) / 100.0);
                        MessageManager.get(player, "chat.delay").replace("%countdown%", roundedDouble).send();
                        e.setCancelled(true);
                        return;
                    }


                    abilityItem.getActions().get(action).accept(new AbilityItem.ActionContext(gamePlayer, abilityItem, null, null, e));
                    if (abilityItem.isConsumable()) {
                        abilityItem.consume(player, item);

                    }
                    if (cooldown != null) {
                        cooldown.startCooldown(gamePlayer);
                    }
                }
            });
        }
    }

    @EventHandler
    private void onPlayerInteractEntity(PlayerInteractEntityEvent e) {
        Player player = e.getPlayer();
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);

        Game game = gamePlayer.getGame();
        if (game == null) return;
        if (game.getState() != GameState.INGAME || game.isPreparation()) return;

        ItemStack item = player.getInventory().getItem(e.getHand());
        if (item.getType().isAir() || !item.hasItemMeta()) {
            return;
        }

        if (!AbilityItem.isAbilityItem(item)) return;

        Optional<AbilityItem> abilityItemOptional = AbilityItem.getAbilityItem(item);

        abilityItemOptional.ifPresent(abilityItem -> {
            if (gamePlayer.getGame().getState() != GameState.INGAME) {
                e.setCancelled(true);
                return;
            }

            for (AbilityItem.Action action : abilityItem.getActions().keySet()) {
                if (!(action.equals(AbilityItem.Action.DEFAULT) || action.equals(AbilityItem.Action.RIGHT_CLICK_ENTITY))) {
                    continue;
                }
                if (!runValidators(abilityItem.getValidators(), gamePlayer) && !runValidators(abilityItem.getValidators(), e)) {
                    return;
                }

                if (!action.equals(AbilityItem.Action.DEFAULT)) {
                    e.setCancelled(true);
                }


                Cooldown cooldown = null;
                if (!abilityItem.getPlayerCooldowns().isEmpty() && abilityItem.getPlayerCooldowns().get(gamePlayer) != null) {
                    cooldown = abilityItem.getPlayerCooldowns().get(gamePlayer);
                } else if (!abilityItem.getCooldowns().isEmpty() && abilityItem.getCooldowns().get(action) != null) {
                    cooldown = abilityItem.getCooldowns().get(action);
                }
                if (cooldown != null && cooldown.contains(gamePlayer)) {
                    String roundedDouble = String.valueOf(Math.round(cooldown.getCountdown(gamePlayer) * 100.0) / 100.0);
                    MessageManager.get(player, "chat.delay").replace("%countdown%", roundedDouble).send();
                    e.setCancelled(true);
                    return;
                }


                abilityItem.getActions().get(action).accept(new AbilityItem.ActionContext(gamePlayer, abilityItem, null, e, null));
                if (abilityItem.isConsumable()) {
                    abilityItem.consume(player, item);
                }
                if (cooldown != null) {
                    cooldown.startCooldown(gamePlayer);
                }
            }
        });
    }

    @EventHandler
    private void onPlayerInteract(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);

        Game game = gamePlayer.getGame();
        if (game == null) return;
        if (game.getState() != GameState.INGAME || game.isPreparation()) return;

        ItemStack item = e.getItem();

        if (item == null || item.getItemMeta() == null){
            return;
        }

        if (!AbilityItem.isAbilityItem(item)) return;

        Optional<AbilityItem> abilityItemOptional = AbilityItem.getAbilityItem(item);

        abilityItemOptional.ifPresent(abilityItem -> {
            if (gamePlayer.getGame().getState() != GameState.INGAME) {
                e.setCancelled(true);
                return;
            }


            for (AbilityItem.Action action : abilityItem.getActions().keySet()) {

                if (!action.equals(AbilityItem.Action.DEFAULT)) {
                    if (action.equals(AbilityItem.Action.LEFT_CLICK) && !(e.getAction().equals(org.bukkit.event.block.Action.LEFT_CLICK_BLOCK) || e.getAction().equals(org.bukkit.event.block.Action.LEFT_CLICK_AIR))) {
                        continue;
                    } else if (action.equals(AbilityItem.Action.RIGHT_CLICK) && !(e.getAction().equals(org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) || e.getAction().equals(org.bukkit.event.block.Action.RIGHT_CLICK_AIR))) {
                        continue;
                    } else if (action.equals(AbilityItem.Action.RIGHT_CLICK_ENTITY)) {
                        continue;
                    }
                }
                if (!runValidators(abilityItem.getValidators(), gamePlayer) && !runValidators(abilityItem.getValidators(), e)) {
                    return;
                }

            /*if (!e.getAction().equals(action)){
                continue;
            }*/

                Block block = e.getClickedBlock();
                if (block != null) {
                    List<Material> materials = Arrays.asList(Material.CHEST, Material.CRAFTING_TABLE, Material.ENCHANTING_TABLE, Material.ANVIL, Material.CHIPPED_ANVIL, Material.DAMAGED_ANVIL);
                    if (materials.contains(block.getType())) {
                        return;
                    }
                }


                if (!action.equals(AbilityItem.Action.DEFAULT)) {
                    e.setCancelled(true);
                }


                Cooldown cooldown = null;
                if (!abilityItem.getPlayerCooldowns().isEmpty() && abilityItem.getPlayerCooldowns().get(gamePlayer) != null) {
                    cooldown = abilityItem.getPlayerCooldowns().get(gamePlayer);
                } else if (!abilityItem.getCooldowns().isEmpty() && abilityItem.getCooldowns().get(action) != null) {
                    cooldown = abilityItem.getCooldowns().get(action);
                }
                if (cooldown != null && cooldown.contains(gamePlayer)) {
                    String roundedDouble = String.valueOf(Math.round(cooldown.getCountdown(gamePlayer) * 100.0) / 100.0);
                    MessageManager.get(player, "chat.delay").replace("%countdown%", roundedDouble).send();
                    e.setCancelled(true);
                    return;
                }


                abilityItem.getActions().get(action).accept(new AbilityItem.ActionContext(gamePlayer, abilityItem, e, null, null));
                if (abilityItem.isConsumable() && e.getHand() != null) {
                    abilityItem.consume(player, item);
                }
                if (cooldown != null) {
                    cooldown.startCooldown(gamePlayer);
                }
            }
        });
    }

    public <T> boolean runValidators(List<AbilityItem.Validator<?>> validators, T object) {
        for (AbilityItem.Validator<?> validator : validators) {
            if (validator.type().isInstance(object)) {
                @SuppressWarnings("unchecked")
                AbilityItem.Validator<T> v = (AbilityItem.Validator<T>) validator;
                if (!v.validator().test(object)) {
                    if (v.consumer() != null) v.consumer().accept(object);
                    return false;
                }
            }
        }
        return true;
    }
}
