package cz.johnslovakia.npcapi.api;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * Represents a packet-based NPC visible to specific players.
 * NPCs are NOT real Bukkit entities — they exist only as sent packets.
 *
 * <pre>{@code
 * NPC npc = NpcAPI.get().builder("myNpc")
 *     .location(player.getLocation())
 *     .displayName(Component.text("§6Guard"))
 *     .skin(SkinData.STEVE)
 *     .onClick((clicker, type) -> clicker.sendMessage("Hi!"))
 *     .build();
 *
 * npc.show(player);
 * }</pre>
 */
public interface NPC {

    /**
     * Unique identifier of this NPC (not the entity ID).
     */
    @NotNull
    String getId();

    /**
     * Internal entity UUID used in packets.
     */
    @NotNull
    UUID getEntityUUID();

    /**
     * Internal entity ID used in packets.
     */
    int getEntityId();

    @NotNull
    Location getLocation();

    /**
     * Teleports the NPC to a new location for all viewers.
     */
    void teleport(@NotNull Location location);

    @Nullable
    String getDisplayName();

    void setDisplayName(@Nullable String name);

    @NotNull
    SkinData getSkin();

    /**
     * Changes the NPC skin. Respawns the NPC for all current viewers.
     */
    void setSkin(@NotNull SkinData skin);

    /**
     * Whether the NPC shows up in the TAB player list.
     * Note: some clients require this to render the skin correctly.
     */
    boolean isListedInTab();

    void setListedInTab(boolean listed);

    /**
     * Spawns the NPC for a specific player.
     */
    void show(@NotNull Player player);

    /**
     * Despawns the NPC for a specific player.
     */
    void hide(@NotNull Player player);

    /**
     * Despawns the NPC for all current viewers and removes it from the registry.
     */
    void remove();

    /**
     * Returns all players currently seeing this NPC.
     */
    @NotNull
    Set<Player> getViewers();

    boolean isVisible(@NotNull Player player);

    /**
     * Sets a click handler.
     *
     * @param handler BiConsumer of (Player clicker, ClickType type)
     */
    void setClickHandler(@Nullable BiConsumer<Player, ClickType> handler);

    @Nullable
    BiConsumer<Player, ClickType> getClickHandler();

    /**
     * Makes the NPC swing its arm for all viewers.
     *
     * @param offHand true = off-hand swing
     */
    void swingArm(boolean offHand);

    /**
     * Makes the NPC look at the given location for all viewers.
     */
    void lookAt(@NotNull Location target);

    /**
     * Makes the NPC look at the player for all viewers (useful in click handler).
     */
    void lookAt(@NotNull Player player);

    enum ClickType {
        LEFT_CLICK,
        RIGHT_CLICK
    }
}
