package cz.johnslovakia.npcapi.impl;

import cz.johnslovakia.npcapi.api.NPC;
import cz.johnslovakia.npcapi.api.SkinData;
import cz.johnslovakia.npcapi.event.NPCDespawnEvent;
import cz.johnslovakia.npcapi.event.NPCSpawnEvent;
import cz.johnslovakia.npcapi.util.NPCHologram;
import cz.johnslovakia.npcapi.util.PacketHelper;
import net.kyori.adventure.text.Component;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NPCImpl implements NPC {

    private final String id;
    private final UUID entityUUID;
    private final NPCManagerImpl manager;
    private int entityId;

    private Location location;
    private String displayName;
    private SkinData skin;
    private boolean listedInTab = false;
    private BiConsumer<Player, ClickType> clickHandler;

    private final Set<UUID> viewers = ConcurrentHashMap.newKeySet();

    private ServerPlayer fakePlayer;
    private NPCHologram hologram;
    private final Map<UUID, BukkitTask> pendingTabRemovals = new ConcurrentHashMap<>();

    public NPCImpl(@NotNull String id,
                   @NotNull UUID entityUUID,
                   @NotNull Location location,
                   @Nullable String displayName,
                   @NotNull SkinData skin,
                   boolean listedInTab,
                   @Nullable BiConsumer<Player, ClickType> clickHandler,
                   @NotNull NPCManagerImpl manager) {
        this.id = id;
        this.entityUUID = entityUUID;
        this.location = location.clone();
        this.displayName = displayName;
        this.skin = skin;
        this.listedInTab = listedInTab;
        this.clickHandler = clickHandler;
        this.manager = manager;

        this.fakePlayer = PacketHelper.createFakePlayer(entityUUID, buildTabName(), skin, location);
        this.entityId = fakePlayer.getId();

        if (displayName != null) {
            this.hologram = new NPCHologram(manager.getPlugin(), holoLocation(location), Component.text(colorizer(displayName)));
        }
    }

    @Override public @NotNull String getId() { return id; }
    @Override public @NotNull UUID getEntityUUID() { return entityUUID; }
    @Override public int getEntityId() { return entityId; }
    @Override public @NotNull Location getLocation() { return location.clone(); }

    @Override
    public void teleport(@NotNull Location location) {
        ServerPlayer oldFakePlayer = fakePlayer;
        boolean changedWorld = !Objects.equals(this.location.getWorld(), location.getWorld());
        this.location = location.clone();

        if (changedWorld) {
            recreateFakePlayer();
            respawnAll(oldFakePlayer);
            if (hologram != null) {
                hologram.moveTo(holoLocation(location));
            }
            return;
        }

        fakePlayer.setPos(location.getX(), location.getY(), location.getZ());
        fakePlayer.setYRot(location.getYaw());
        fakePlayer.setXRot(location.getPitch());
        fakePlayer.setYHeadRot(location.getYaw());

        for (Player viewer : getViewers()) {
            if (canRenderTo(viewer)) {
                PacketHelper.sendTeleportPacket(viewer, fakePlayer, location);
            } else {
                sendDespawnPackets(viewer, fakePlayer);
            }
        }
        if (hologram != null) {
            hologram.moveTo(holoLocation(location));
        }
    }

    @Override public @Nullable String getDisplayName() { return displayName; }

    @Override
    public void setDisplayName(@Nullable String name) {
        this.displayName = name;
        rebuildHologram();
    }

    @Override public @NotNull SkinData getSkin() { return skin; }

    @Override
    public void setSkin(@NotNull SkinData skin) {
        this.skin = skin;
        ServerPlayer oldFakePlayer = fakePlayer;
        recreateFakePlayer();
        respawnAll(oldFakePlayer);
    }

    @Override public boolean isListedInTab() { return listedInTab; }

    @Override
    public void setListedInTab(boolean listed) {
        this.listedInTab = listed;
        if (!listed) {
            for (Player viewer : getViewers()) {
                cancelTabRemoval(viewer);
                PacketHelper.sendTabRemovePacket(viewer, fakePlayer);
            }
        } else {
            for (Player viewer : getViewers()) {
                cancelTabRemoval(viewer);
                PacketHelper.sendTabAddPacket(viewer, fakePlayer);
            }
        }
    }

    @Override
    public void show(@NotNull Player player) {
        boolean added = viewers.add(player.getUniqueId());
        if (!canRenderTo(player)) {
            return;
        }

        if (added) {
            sendSpawnPackets(player);
            if (hologram != null) hologram.showTo(player);
            new NPCSpawnEvent(this, player).callEvent();
        } else {
            refresh(player);
        }
    }

    @Override
    public void refresh(@NotNull Player player) {
        if (!viewers.contains(player.getUniqueId())) return;

        if (!canRenderTo(player)) {
            cancelTabRemoval(player);
            sendDespawnPackets(player, fakePlayer);
            if (hologram != null) hologram.hideFrom(player);
            return;
        }

        sendDespawnPackets(player, fakePlayer);
        sendSpawnPackets(player);
        if (hologram != null) {
            hologram.refreshTo(player);
        }
    }

    @Override
    public void hide(@NotNull Player player) {
        if (!viewers.remove(player.getUniqueId())) return;
        cancelTabRemoval(player);
        sendDespawnPackets(player, fakePlayer);
        if (hologram != null) hologram.hideFrom(player);
        new NPCDespawnEvent(this, player, NPCDespawnEvent.DespawnReason.HIDDEN).callEvent();
    }

    @Override
    public void remove() {
        for (Player viewer : getViewers()) {
            cancelTabRemoval(viewer);
            sendDespawnPackets(viewer, fakePlayer);
            new NPCDespawnEvent(this, viewer, NPCDespawnEvent.DespawnReason.REMOVED).callEvent();
        }
        pendingTabRemovals.values().forEach(BukkitTask::cancel);
        pendingTabRemovals.clear();
        viewers.clear();
        if (hologram != null) {
            hologram.removeAll();
            hologram = null;
        }
        manager.unregister(id);
    }

    @Override
    public @NotNull Set<Player> getViewers() {
        Set<Player> result = new HashSet<>();
        for (UUID uuid : viewers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) result.add(p);
        }
        return Collections.unmodifiableSet(result);
    }

    @Override
    public boolean isVisible(@NotNull Player player) {
        return viewers.contains(player.getUniqueId());
    }

    @Override
    public void setClickHandler(@Nullable BiConsumer<Player, ClickType> handler) {
        this.clickHandler = handler;
    }

    @Override
    public @Nullable BiConsumer<Player, ClickType> getClickHandler() {
        return clickHandler;
    }

    @Override
    public void swingArm(boolean offHand) {
        for (Player viewer : getViewers()) {
            if (canRenderTo(viewer)) PacketHelper.sendSwingArmPacket(viewer, fakePlayer, offHand);
        }
    }

    @Override
    public void lookAt(@NotNull Location target) {
        float[] angles = PacketHelper.getAngles(location, target);
        for (Player viewer : getViewers()) {
            if (canRenderTo(viewer)) PacketHelper.sendLookPacket(viewer, fakePlayer, angles[0], angles[1]);
        }
    }

    @Override
    public void lookAt(@NotNull Player player) {
        lookAt(player.getEyeLocation());
    }

    public void onViewerQuit(@NotNull Player player) {
        if (viewers.remove(player.getUniqueId())) {
            cancelTabRemoval(player);
            if (hologram != null) hologram.hideFrom(player);
            new NPCDespawnEvent(this, player, NPCDespawnEvent.DespawnReason.PLAYER_QUIT).callEvent();
        }
    }

    private void respawnAll(@NotNull ServerPlayer oldFakePlayer) {
        Set<Player> current = new HashSet<>(getViewers());
        for (Player viewer : current) {
            cancelTabRemoval(viewer);
            sendDespawnPackets(viewer, oldFakePlayer);
        }
        for (Player viewer : current) {
            if (canRenderTo(viewer)) {
                sendSpawnPackets(viewer);
                if (hologram != null) hologram.refreshTo(viewer);
            } else if (hologram != null) {
                hologram.hideFrom(viewer);
            }
        }
    }

    private void rebuildHologram() {
        Set<Player> current = new HashSet<>(getViewers());
        if (hologram != null) {
            hologram.removeAll();
            hologram = null;
        }
        if (displayName != null) {
            hologram = new NPCHologram(manager.getPlugin(), holoLocation(location), Component.text(colorizer(displayName)));
            for (Player viewer : current) hologram.showTo(viewer);
        }
    }

    private Location holoLocation(Location base) {
        return base.clone().add(0, 2.1, 0);
    }

    private boolean canRenderTo(@NotNull Player player) {
        return player.isOnline()
                && player.getWorld().equals(location.getWorld());
    }

    private void recreateFakePlayer() {
        int oldEntityId = entityId;
        this.fakePlayer = PacketHelper.createFakePlayer(entityUUID, buildTabName(), skin, location);
        this.entityId = fakePlayer.getId();
        manager.updateEntityId(this, oldEntityId);
    }

    private void sendSpawnPackets(@NotNull Player player) {
        PacketHelper.sendSpawnPackets(player, fakePlayer);
        if (!listedInTab) {
            scheduleTabRemoval(player);
        }
    }

    private void sendDespawnPackets(@NotNull Player player, @NotNull ServerPlayer target) {
        PacketHelper.sendDespawnPackets(player, target);
    }

    private void scheduleTabRemoval(@NotNull Player player) {
        cancelTabRemoval(player);
        UUID viewerId = player.getUniqueId();
        BukkitTask task = Bukkit.getScheduler().runTaskLater(manager.getPlugin(), () -> {
            pendingTabRemovals.remove(viewerId);
            if (player.isOnline() && isVisible(player) && canRenderTo(player) && !listedInTab) {
                PacketHelper.sendTabRemovePacket(player, fakePlayer);
            }
        }, 60L);
        pendingTabRemovals.put(viewerId, task);
    }

    private void cancelTabRemoval(@NotNull Player player) {
        BukkitTask task = pendingTabRemovals.remove(player.getUniqueId());
        if (task != null) task.cancel();
    }

    public static String colorizer(String message) {
        Pattern pattern = Pattern.compile("#[a-fA-F0-9]{6}");
        Matcher matcher = pattern.matcher(message);
        while (matcher.find()) {
            String hexCode = message.substring(matcher.start(), matcher.end());
            String replaceSharp = hexCode.replace('#', 'x');
            char[] ch = replaceSharp.toCharArray();
            StringBuilder builder = new StringBuilder("");
            for (char c : ch) builder.append("&" + c);
            message = message.replace(hexCode, builder.toString());
            matcher = pattern.matcher(message);
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private String buildTabName() {
        return id.length() > 14 ? id.substring(0, 14) : id;
    }

    public @NotNull ServerPlayer getFakePlayer() {
        return fakePlayer;
    }
}
