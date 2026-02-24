package cz.johnslovakia.gameapi.listeners;

import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.modules.game.GameInstance;
import cz.johnslovakia.gameapi.modules.game.GameState;
import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.messages.MessageModule;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerManager;
import cz.johnslovakia.gameapi.utils.AbilityItem;
import cz.johnslovakia.gameapi.utils.Cooldown;
import cz.johnslovakia.gameapi.utils.ItemBuilder;
import cz.johnslovakia.gameapi.utils.StagedCooldown;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class AbilityItemListener implements Listener {

    @EventHandler
    public void onInventoryPickupItem(EntityPickupItemEvent e) {
        if (!(e.getEntity() instanceof Player player)) return;
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);

        GameInstance game = gamePlayer.getGame();
        if (game == null) return;
        if (game.getState() != GameState.INGAME || game.isPreparation()) return;

        if (e.getItem().getItemStack().getItemMeta() == null) return;
        Item item = e.getItem();

        if (!AbilityItem.isAbilityItem(e.getItem().getItemStack())) return;

        ItemMeta meta = item.getItemStack().getItemMeta();
        if (meta == null) return;

        Optional<AbilityItem> abilityItemOptional = AbilityItem.getAbilityItem(item.getItemStack());
        abilityItemOptional.ifPresent(abilityItem -> {
            if (abilityItem.getLoreTranslationKey() != null) {
                ItemBuilder itemBuilder = new ItemBuilder(item.getItemStack());
                itemBuilder.setLore(ModuleManager.getModule(MessageModule.class).get(gamePlayer, abilityItem.getLoreTranslationKey()).getTranslated());
                item.setItemStack(itemBuilder.toItemStack());
            }
        });
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);

        GameInstance game = gamePlayer.getGame();
        if (game == null) return;
        if (game.getState() != GameState.INGAME || game.isPreparation()) return;

        ItemStack itemStack = e.getCurrentItem();
        if (itemStack == null) return;
        if (!AbilityItem.isAbilityItem(itemStack)) return;

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return;

        Optional<AbilityItem> abilityItemOptional = AbilityItem.getAbilityItem(itemStack);
        abilityItemOptional.ifPresent(abilityItem -> {
            if (abilityItem.getLoreTranslationKey() != null) {
                ItemBuilder item = new ItemBuilder(itemStack);
                item.setLore(ModuleManager.getModule(MessageModule.class).get(gamePlayer, abilityItem.getLoreTranslationKey()).getTranslated());
                e.setCurrentItem(item.toItemStack());
            }

            Collection<Cooldown> cooldowns = abilityItem.getCooldowns().values();
            for (Cooldown cooldown : cooldowns) {
                if (cooldown.hasItemStackCooldown(player.getUniqueId())) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            cooldown.forceUpdateItems(player);
                        }
                    }.runTaskLater(Minigame.getInstance().getPlugin(), 1L);
                    break;
                }
            }

            for (StagedCooldown sc : abilityItem.getStagedCooldowns().values()) {
                if (sc.getShortCooldown().hasItemStackCooldown(player.getUniqueId())) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            sc.getShortCooldown().forceUpdateItems(player);
                        }
                    }.runTaskLater(Minigame.getInstance().getPlugin(), 1L);
                    break;
                }
                if (sc.getLongCooldown().hasItemStackCooldown(player.getUniqueId())) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            sc.getLongCooldown().forceUpdateItems(player);
                        }
                    }.runTaskLater(Minigame.getInstance().getPlugin(), 1L);
                    break;
                }
            }
        });
    }

    @EventHandler
    public void onInventoryClose(org.bukkit.event.inventory.InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player player)) return;

        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);
        GameInstance game = gamePlayer.getGame();
        if (game == null) return;
        if (game.getState() != GameState.INGAME || game.isPreparation()) return;

        for (Cooldown cooldown : Cooldown.getList()) {
            if (cooldown.hasItemStackCooldown(player.getUniqueId())) {
                cooldown.forceUpdateItems(player);
            }
        }

        for (AbilityItem abilityItem : AbilityItem.getAbilityItems().values()) {
            for (StagedCooldown sc : abilityItem.getStagedCooldowns().values()) {
                if (sc.getShortCooldown().hasItemStackCooldown(player.getUniqueId())) {
                    sc.getShortCooldown().forceUpdateItems(player);
                }
                if (sc.getLongCooldown().hasItemStackCooldown(player.getUniqueId())) {
                    sc.getLongCooldown().forceUpdateItems(player);
                }
            }
        }
    }

    @EventHandler
    private void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player player)) return;

        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);

        GameInstance game = gamePlayer.getGame();
        if (game == null) return;
        if (game.getState() != GameState.INGAME || game.isPreparation()) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir() || !item.hasItemMeta()) return;
        if (!AbilityItem.isAbilityItem(item)) return;

        Optional<AbilityItem> abilityItemOptional = AbilityItem.getAbilityItem(item);

        abilityItemOptional.ifPresent(abilityItem -> {
            if (gamePlayer.getGame().getState() != GameState.INGAME) {
                e.setCancelled(true);
                return;
            }
            if (e.getEntity() instanceof Player damaged && gamePlayer.getGameSession().getTeam().equals(PlayerManager.getGamePlayer(damaged).getGameSession().getTeam())) {
                return;
            }

            for (AbilityItem.Action action : abilityItem.getActions().keySet()) {
                if (!(action.equals(AbilityItem.Action.DEFAULT) || action.equals(AbilityItem.Action.ENTITY_DAMAGE))) {
                    continue;
                }
                if (!action.equals(AbilityItem.Action.DEFAULT)) {
                    e.setCancelled(true);
                }

                if (handleCooldownBlock(abilityItem, action, gamePlayer, player)) {
                    e.setCancelled(true);
                    return;
                }

                if (!runValidators(abilityItem.getValidators(), gamePlayer) || !runValidators(abilityItem.getValidators(), e)) {
                    return;
                }

                abilityItem.getActions().get(action).accept(new AbilityItem.ActionContext(gamePlayer, abilityItem, null, null, e));

                if (abilityItem.isConsumable()) {
                    abilityItem.consume(player, item);
                }
                startCooldown(abilityItem, action, gamePlayer, item);
            }
        });
    }

    @EventHandler
    private void onPlayerInteractEntity(PlayerInteractEntityEvent e) {
        Player player = e.getPlayer();
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);

        GameInstance game = gamePlayer.getGame();
        if (game == null) return;
        if (game.getState() != GameState.INGAME || game.isPreparation()) return;

        ItemStack item = player.getInventory().getItem(e.getHand());
        if (item.getType().isAir() || !item.hasItemMeta()) return;

        if (!AbilityItem.isAbilityItem(item)) return;

        Optional<AbilityItem> abilityItemOptional = AbilityItem.getAbilityItem(item);

        abilityItemOptional.ifPresent(abilityItem -> {
            if (gamePlayer.getGame().getState() != GameState.INGAME) {
                e.setCancelled(true);
                return;
            }
            if (e.getRightClicked() instanceof Player clicked && gamePlayer.getGameSession().getTeam().equals(PlayerManager.getGamePlayer(clicked).getGameSession().getTeam())) {
                return;
            }

            for (AbilityItem.Action action : abilityItem.getActions().keySet()) {
                if (!(action.equals(AbilityItem.Action.DEFAULT) || action.equals(AbilityItem.Action.RIGHT_CLICK_ENTITY))) {
                    continue;
                }
                if (!action.equals(AbilityItem.Action.DEFAULT)) {
                    e.setCancelled(true);
                }

                if (handleCooldownBlock(abilityItem, action, gamePlayer, player)) {
                    e.setCancelled(true);
                    return;
                }

                if (!runValidators(abilityItem.getValidators(), gamePlayer) || !runValidators(abilityItem.getValidators(), e)) {
                    return;
                }

                abilityItem.getActions().get(action).accept(new AbilityItem.ActionContext(gamePlayer, abilityItem, null, e, null));

                if (abilityItem.isConsumable()) {
                    abilityItem.consume(player, item);
                }
                startCooldown(abilityItem, action, gamePlayer, item);
            }
        });
    }

    @EventHandler
    private void onPlayerInteract(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);

        GameInstance game = gamePlayer.getGame();
        if (game == null) return;
        if (game.getState() != GameState.INGAME || game.isPreparation()) return;

        ItemStack item = e.getItem();
        if (item == null || item.getItemMeta() == null) return;
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

                if (handleCooldownBlock(abilityItem, action, gamePlayer, player)) {
                    e.setCancelled(true);
                    return;
                }

                if (!runValidators(abilityItem.getValidators(), gamePlayer) || !runValidators(abilityItem.getValidators(), e)) {
                    return;
                }

                abilityItem.getActions().get(action).accept(new AbilityItem.ActionContext(gamePlayer, abilityItem, e, null, null));

                if (abilityItem.isConsumable() && e.getHand() != null) {
                    abilityItem.consume(player, item);
                }
                startCooldown(abilityItem, action, gamePlayer, item);
            }
        });
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;

        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);
        GameInstance game = gamePlayer.getGame();
        if (game == null) return;
        if (game.getState() != GameState.INGAME || game.isPreparation()) return;

        ItemStack item = e.getOldCursor();
        if (item.getType().isAir() || !item.hasItemMeta()) return;
        if (!AbilityItem.isAbilityItem(item)) return;

        Optional<AbilityItem> abilityItemOptional = AbilityItem.getAbilityItem(item);
        abilityItemOptional.ifPresent(abilityItem -> {
            Collection<Cooldown> cooldowns = abilityItem.getCooldowns().values();

            boolean hasCooldown = !cooldowns.isEmpty() && cooldowns.stream().anyMatch(c -> c.contains(player));
            boolean hasStagedCooldown = abilityItem.getStagedCooldowns().values().stream()
                    .anyMatch(sc -> sc.isOnCooldown(player));

            if (hasCooldown || hasStagedCooldown) {
                e.setCancelled(true);
                e.setResult(Event.Result.DENY);

                for (Cooldown cooldown : cooldowns) {
                    if (cooldown.contains(player)) {
                        setCorrectCursorAmount(player, item, cooldown.getCountdown(player));
                        return;
                    }
                }
                for (StagedCooldown sc : abilityItem.getStagedCooldowns().values()) {
                    if (sc.isOnCooldown(player)) {
                        setCorrectCursorAmount(player, item, sc.getCountdown(player));
                        return;
                    }
                }
            }
        });
    }

    private void setCorrectCursorAmount(Player player, ItemStack item, double countdown) {
        int amount = Math.max(1, Math.min(64, (int) Math.ceil(countdown)));
        ItemStack correctedItem = item.clone();
        correctedItem.setAmount(amount);
        player.setItemOnCursor(correctedItem);
    }

    private String formatCountdown(double countdown) {
        double value = Math.max(0.1, countdown);
        if (value >= 1.0) {
            return String.valueOf((int) Math.ceil(value));
        }
        return String.valueOf(Math.round(value * 10.0) / 10.0);
    }

    private boolean handleCooldownBlock(AbilityItem abilityItem, AbilityItem.Action action,
                                         GamePlayer gamePlayer, Player player) {
        Cooldown cooldown = abilityItem.getCooldown(action);
        if (cooldown != null && cooldown.contains(gamePlayer)) {
            boolean itemStackCooldown = !abilityItem.isConsumable() && cooldown.getCooldown() <= 64;
            if (!itemStackCooldown) {
                ModuleManager.getModule(MessageModule.class)
                        .get(player, "chat.delay")
                        .replace("%countdown%", formatCountdown(cooldown.getCountdown(gamePlayer)))
                        .send();
            }
            return true;
        }


        StagedCooldown stagedCooldown = abilityItem.getStagedCooldown(action);
        if (stagedCooldown != null && stagedCooldown.isOnCooldown(gamePlayer)) {
            double activeCooldownDuration = stagedCooldown.getShortCooldown().contains(gamePlayer)
                    ? stagedCooldown.getShortCooldown().getCooldown()
                    : stagedCooldown.getLongCooldown().getCooldown();
            boolean itemStackCooldown = !abilityItem.isConsumable() && activeCooldownDuration <= 64;
            if (!itemStackCooldown) {
                ModuleManager.getModule(MessageModule.class)
                        .get(player, "chat.delay")
                        .replace("%countdown%", formatCountdown(stagedCooldown.getCountdown(player)))
                        .send();
            }
            return true;
        }

        return false;
    }

    private void startCooldown(AbilityItem abilityItem, AbilityItem.Action action,
                                GamePlayer gamePlayer, ItemStack item) {
        Cooldown cooldown = abilityItem.getCooldown(action);
        if (cooldown != null) {
            boolean itemStackCooldown = !abilityItem.isConsumable() && cooldown.getCooldown() <= 64;
            if (itemStackCooldown) {
                cooldown.startItemStackCooldown(gamePlayer, item);
            } else {
                cooldown.startCooldown(gamePlayer);
            }
        }

        StagedCooldown stagedCooldown = abilityItem.getStagedCooldown(action);
        if (stagedCooldown != null) {
            stagedCooldown.use(gamePlayer, item);
        }
    }

    public <T> boolean runValidators(List<AbilityItem.Validator<?>> validators, T object) {
        for (AbilityItem.Validator<?> validator : validators) {
            if (!validator.type().isInstance(object)) continue;

            @SuppressWarnings("unchecked")
            AbilityItem.Validator<T> v = (AbilityItem.Validator<T>) validator;

            boolean valid = v.validator().test(object);
            if (!valid) {
                Consumer<T> consumer = v.consumer();
                if (consumer != null) consumer.accept(object);
                return false;
            }
        }
        return true;
    }
}