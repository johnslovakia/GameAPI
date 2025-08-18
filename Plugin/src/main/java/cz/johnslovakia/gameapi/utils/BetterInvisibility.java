package cz.johnslovakia.gameapi.utils;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;

import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.Pair;
import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.Minigame;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.lang.reflect.InvocationTargetException;
import java.util.*;


//NOT MY CODE

//Author: NewAmazingPVP
//https://github.com/NewAmazingPVP/BetterInvisibility/blob/main/src/main/java/newamazingpvp/betterinvisibility/BetterInvisibility.java

public class BetterInvisibility implements Listener {

    private final EnumSet<EnumWrappers.ItemSlot> hiddenSlots = EnumSet.noneOf(EnumWrappers.ItemSlot.class);
    private final boolean hidePotionParticles;
    private boolean workaround;

    public BetterInvisibility(boolean hideArmor, boolean hideMainHand, boolean hideOffHand, boolean hidePotionParticles) {
        if (hideArmor) {
            hiddenSlots.addAll(Arrays.asList(
                    EnumWrappers.ItemSlot.HEAD,
                    EnumWrappers.ItemSlot.CHEST,
                    EnumWrappers.ItemSlot.LEGS,
                    EnumWrappers.ItemSlot.FEET
            ));
        }
        if (hideMainHand) hiddenSlots.add(EnumWrappers.ItemSlot.MAINHAND);
        if (hideOffHand) hiddenSlots.add(EnumWrappers.ItemSlot.OFFHAND);
        this.hidePotionParticles = hidePotionParticles;
    }

    public BetterInvisibility registerEvents() {
        Bukkit.getPluginManager().registerEvents(this, Minigame.getInstance().getPlugin());
        return this;
    }

    public BetterInvisibility setWorkaround(boolean workaround) {
        this.workaround = workaround;
        return this;
    }

    private final Map<UUID, Long> lastHitTimestamps = new HashMap<>();

    @EventHandler
    public void onPotionEffect(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        PotionEffectType newType = event.getNewEffect() != null ? event.getNewEffect().getType() : null;
        PotionEffectType oldType = event.getOldEffect() != null ? event.getOldEffect().getType() : null;

        if (PotionEffectType.INVISIBILITY.equals(newType)) {
            if (hidePotionParticles) {
                PotionEffect original = event.getNewEffect();
                if (original != null) {
                    player.removePotionEffect(PotionEffectType.INVISIBILITY);
                    player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY,
                            original.getDuration(), original.getAmplifier(), original.isAmbient(), false));
                }
            }
            updateArmor(player, true);
        } else if (PotionEffectType.INVISIBILITY.equals(oldType)) {
            updateArmor(player, false);
        }
    }

    private void updateArmor(Player player, boolean hide) {
        ProtocolManager protocolManager = GameAPI.getInstance().getProtocolManager();
        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.ENTITY_EQUIPMENT);
        packet.getIntegers().write(0, player.getEntityId());

        List<Pair<EnumWrappers.ItemSlot, ItemStack>> slotPairs = new ArrayList<>();
        for (EnumWrappers.ItemSlot slot : hiddenSlots) {
            ItemStack item = hide ? new ItemStack(Material.AIR) : getItemFromSlot(player, slot);
            slotPairs.add(new Pair<>(slot, item));
        }

        packet.getSlotStackPairLists().write(0, slotPairs);

        for (Player viewer : player.getWorld().getPlayers()) {
            if (!viewer.equals(player)) {
                protocolManager.sendServerPacket(viewer, packet);
            }
        }
    }

    private ItemStack getItemFromSlot(Player player, EnumWrappers.ItemSlot slot) {
        switch (slot) {
            case HEAD -> { return player.getInventory().getHelmet(); }
            case CHEST -> { return player.getInventory().getChestplate(); }
            case LEGS -> { return player.getInventory().getLeggings(); }
            case FEET -> { return player.getInventory().getBoots(); }
            case MAINHAND -> { return player.getInventory().getItemInMainHand(); }
            case OFFHAND -> { return player.getInventory().getItemInOffHand(); }
            default -> { return new ItemStack(Material.AIR); }
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!workaround) return;

        if (event.getEntity() instanceof Player target && event.getDamager() instanceof Player attacker) {
            if (!target.hasPotionEffect(PotionEffectType.INVISIBILITY)) return;

            event.setCancelled(true);

            long now = System.currentTimeMillis();
            long lastHit = lastHitTimestamps.getOrDefault(target.getUniqueId(), 0L);
            if (now - lastHit < 500) return;

            lastHitTimestamps.put(target.getUniqueId(), now);
            double damage = event.getFinalDamage();
            target.setHealth(Math.max(0, target.getHealth() - damage));

            Vector knockback = target.getLocation().toVector().subtract(attacker.getLocation().toVector()).normalize()
                    .multiply(attacker.isSprinting() ? 1.3 : 0.8).add(new Vector(0, 0.35, 0));
            target.setVelocity(knockback);
        }
    }
}
