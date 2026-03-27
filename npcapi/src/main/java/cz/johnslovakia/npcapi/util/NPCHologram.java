package cz.johnslovakia.npcapi.util;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Floating text hologram backed by a Bukkit {@link TextDisplay} entity
 *
 * <pre>{@code
 * NPCHologram holo = new NPCHologram(plugin,
 *     npc.getLocation().add(0, 2.3, 0),
 *     Component.text("§6§lShop")
 * );
 * holo.showTo(player);
 * }</pre>
 */
public class NPCHologram {

    private final Plugin plugin;
    private final TextDisplay entity;
    private final Set<UUID> viewers = new HashSet<>();

    /**
     * Creates and spawns the hologram entity. Hidden from all players until {@link #showTo} is called.
     *
     * @param plugin   Owning plugin (used for show/hideEntity calls)
     * @param location Spawn location (world must not be null)
     * @param text     Text to display, or {@code null} for empty
     */
    public NPCHologram(@NotNull Plugin plugin,
                       @NotNull Location location,
                       @Nullable Component text) {
        this.plugin = plugin;

        Objects.requireNonNull(location.getWorld(), "NPCHologram location world must not be null");

        entity = location.getWorld().spawn(location, TextDisplay.class, display -> {
            display.text(text != null ? text : Component.empty());
            display.setBillboard(TextDisplay.Billboard.CENTER);
            display.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));
            display.setSeeThrough(false);
            display.setPersistent(false);
        });

        for (org.bukkit.entity.Player online : plugin.getServer().getOnlinePlayers()) {
            online.hideEntity(plugin, entity);
        }
    }

    /** Shows the hologram to {@code player}. No-op if already visible. */
    public void showTo(@NotNull org.bukkit.entity.Player player) {
        if (!viewers.add(player.getUniqueId())) return;
        player.showEntity(plugin, entity);
    }

    /** Hides the hologram from {@code player}. No-op if not visible. */
    public void hideFrom(@NotNull org.bukkit.entity.Player player) {
        if (!viewers.remove(player.getUniqueId())) return;
        player.hideEntity(plugin, entity);
    }

    /** Updates the displayed text for all current viewers. */
    public void setText(@Nullable Component text) {
        entity.text(text != null ? text : Component.empty());
    }

    /** Teleports the hologram to a new location. */
    public void moveTo(@NotNull Location location) {
        entity.teleport(location);
    }

    /**
     * Removes the hologram from all viewers and despawns the underlying entity.
     * Do not use this instance after calling this method.
     */
    public void removeAll() {
        viewers.clear();
        entity.remove();
    }

    // ──────────────────────────────────────────────────────────────────────────

    public @NotNull TextDisplay getEntity() { return entity; }
    public @Nullable Component getText() { return entity.text(); }
    public @NotNull Location getLocation() { return entity.getLocation(); }
}