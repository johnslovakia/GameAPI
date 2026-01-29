package cz.johnslovakia.gameapi.utils;

import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.messages.MessageModule;
import cz.johnslovakia.gameapi.modules.messages.MessageType;
import cz.johnslovakia.gameapi.users.PlayerIdentity;
import cz.johnslovakia.gameapi.users.PlayerIdentityRegistry;

import lombok.Getter;
import lombok.Setter;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;

@Getter
public class Cooldown {

    @Getter
    private static List<Cooldown> list = new ArrayList<>();

    private final String name;
    @Setter
    private double cooldown;
    @Setter
    private Consumer<Cooldown> endConsumer;
    private final Map<UUID, Double> players = new HashMap<>();
    private final Map<UUID, BukkitTask> activeTasks = new HashMap<>();
    private final Map<UUID, String> itemStackCooldownAbilityNames = new HashMap<>();
    private final Map<UUID, Integer> originalAmounts = new HashMap<>();

    public Cooldown(String name, double cooldown) {
        this.name = name;
        this.cooldown = cooldown;
        list.add(this);
    }

    public void startCooldown(PlayerIdentity playerIdentity) {
        startCooldown(playerIdentity, false);
    }

    public void startCooldown(PlayerIdentity playerIdentity, boolean actionbar) {
        UUID uuid = playerIdentity.getUniqueId();

        cancelTask(uuid);
        
        players.put(uuid, cooldown);

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!playerIdentity.getOfflinePlayer().isOnline()) {
                    cleanup(uuid);
                    this.cancel();
                    return;
                }

                players.computeIfPresent(uuid, (k, v) -> v - 0.1D);

                double countdown = getCountdown(playerIdentity);
                if (countdown <= 0) {
                    if (endConsumer != null)
                        endConsumer.accept(Cooldown.this);
                    cleanup(uuid);
                    if (actionbar) {
                        ModuleManager.getModule(MessageModule.class).get(playerIdentity, "chat.countdown_is_over").send(MessageType.ACTIONBAR);
                    }
                    this.cancel();
                } else {
                    if (actionbar) {
                        Utils.countdownTimerBar(playerIdentity.getOnlinePlayer(), cooldown, countdown);
                    }
                }
            }
        }.runTaskTimer(Minigame.getInstance().getPlugin(), 0L, 2L);
        
        activeTasks.put(uuid, task);
    }

    public void startItemStackCooldown(PlayerIdentity playerIdentity, @NotNull ItemStack itemStack) {
        UUID uuid = playerIdentity.getUniqueId();

        cancelTask(uuid);
        
        players.put(uuid, cooldown);

        Optional<AbilityItem> abilityOpt = AbilityItem.getAbilityItem(itemStack);
        if (abilityOpt.isEmpty()) return;
        
        String abilityName = abilityOpt.get().getName();
        itemStackCooldownAbilityNames.put(uuid, abilityName);
        originalAmounts.put(uuid, itemStack.getAmount());

        Player player = playerIdentity.getOnlinePlayer();
        player.setCooldown(itemStack.getType(), (int) (cooldown * 20));

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                Player p = playerIdentity.getOnlinePlayer();
                
                if (!playerIdentity.getOfflinePlayer().isOnline()) {
                    cleanup(uuid);
                    this.cancel();
                    return;
                }

                double countdown = getCountdown(playerIdentity);
                
                if (countdown <= 0) {
                    restoreAllItemAmounts(p, abilityName, uuid);
                    cleanup(uuid);
                    this.cancel();
                    return;
                }

                players.computeIfPresent(uuid, (k, v) -> v - 1D);

                updateAllItemAmounts(p, abilityName, countdown);
            }
        }.runTaskTimer(Minigame.getInstance().getPlugin(), 0L, 20L);
        
        activeTasks.put(uuid, task);
    }

    private void updateAllItemAmounts(Player player, String abilityName, double countdown) {
        if (player == null || !player.isOnline()) return;
        
        int targetAmount = Math.max(1, Math.min(64, (int) Math.ceil(countdown)));
        
        for (ItemStack item : player.getInventory().getContents()) {
            if (!AbilityItem.isAbilityItem(item)) continue;
            
            Optional<AbilityItem> abilityOpt = AbilityItem.getAbilityItem(item);
            if (abilityOpt.isPresent() && abilityOpt.get().getName().equals(abilityName)) {
                item.setAmount(targetAmount);
            }
        }

        ItemStack cursor = player.getItemOnCursor();
        if (!cursor.getType().isAir() && AbilityItem.isAbilityItem(cursor)) {
            Optional<AbilityItem> abilityOpt = AbilityItem.getAbilityItem(cursor);
            if (abilityOpt.isPresent() && abilityOpt.get().getName().equals(abilityName)) {
                cursor.setAmount(targetAmount);
            }
        }
    }

    private void restoreAllItemAmounts(Player player, String abilityName, UUID uuid) {
        if (player == null || !player.isOnline()) return;
        
        int originalAmount = originalAmounts.getOrDefault(uuid, 1);
        
        for (ItemStack item : player.getInventory().getContents()) {
            if (!AbilityItem.isAbilityItem(item)) continue;
            
            Optional<AbilityItem> abilityOpt = AbilityItem.getAbilityItem(item);
            if (abilityOpt.isPresent() && abilityOpt.get().getName().equals(abilityName)) {
                item.setAmount(originalAmount);
            }
        }

        ItemStack cursor = player.getItemOnCursor();
        if (!cursor.getType().isAir() && AbilityItem.isAbilityItem(cursor)) {
            Optional<AbilityItem> abilityOpt = AbilityItem.getAbilityItem(cursor);
            if (abilityOpt.isPresent() && abilityOpt.get().getName().equals(abilityName)) {
                cursor.setAmount(originalAmount);
            }
        }
    }

    public void forceUpdateItems(Player player) {
        UUID uuid = player.getUniqueId();
        if (!players.containsKey(uuid)) return;
        
        String abilityName = itemStackCooldownAbilityNames.get(uuid);
        if (abilityName == null) return;
        
        double countdown = getCountdown(player);
        if (countdown > 0) {
            updateAllItemAmounts(player, abilityName, countdown);
        } else {
            restoreAllItemAmounts(player, abilityName, uuid);
        }
    }

    public void cancelCooldown(UUID uuid) {
        cancelTask(uuid);
        cleanup(uuid);
    }

    private void cancelTask(UUID uuid) {
        BukkitTask task = activeTasks.remove(uuid);
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
    }
    
    private void cleanup(UUID uuid) {
        players.remove(uuid);
        itemStackCooldownAbilityNames.remove(uuid);
        originalAmounts.remove(uuid);
        activeTasks.remove(uuid);
    }

    public double getCountdown(PlayerIdentity playerIdentity) {
        UUID uuid = playerIdentity.getUniqueId();
        return players.getOrDefault(uuid, 0.0);
    }

    public double getCountdown(Player player) {
        return getCountdown(PlayerIdentityRegistry.get(player));
    }

    public boolean contains(PlayerIdentity playerIdentity) {
        return players.containsKey(playerIdentity.getUniqueId());
    }

    public boolean contains(Player player) {
        return contains(PlayerIdentityRegistry.get(player));
    }
    
    public boolean hasItemStackCooldown(UUID uuid) {
        return itemStackCooldownAbilityNames.containsKey(uuid);
    }

    public static Cooldown getCooldown(String name) {
        for (Cooldown cd : getList()) {
            if (cd.getName().equalsIgnoreCase(name)) {
                return cd;
            }
        }
        return null;
    }
}