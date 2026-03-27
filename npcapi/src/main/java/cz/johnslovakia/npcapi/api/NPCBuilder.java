package cz.johnslovakia.npcapi.api;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;

/**
 * Fluent builder for creating {@link NPC} instances.
 *
 * <pre>{@code
 * NPC npc = NpcAPI.get().builder("shopkeeper")
 *     .location(loc)
 *     .displayName(Component.text("§eShop"))
 *     .skin(SkinData.ALEX)
 *     .nameVisible(true)
 *     .listedInTab(false)
 *     .onClick((p, t) -> openShop(p))
 *     .build();
 * }</pre>
 */
public interface NPCBuilder {

    @NotNull
    NPCBuilder location(@NotNull Location location);

    @NotNull
    NPCBuilder displayName(@Nullable String name);

    @NotNull
    NPCBuilder skin(@NotNull SkinData skin);

    @NotNull
    NPCBuilder listedInTab(boolean listed);

    @NotNull
    NPCBuilder onClick(@Nullable BiConsumer<Player, NPC.ClickType> handler);

    /**
     * Builds and registers the NPC without showing it to anyone yet.
     * Call {@link NPC#show(Player)} to make it visible.
     *
     * @throws IllegalStateException if location is not set
     * @throws IllegalStateException if an NPC with this ID already exists
     */
    @NotNull
    NPC build();

    /**
     * Builds, registers, and immediately shows the NPC to the given players.
     */
    @NotNull
    NPC buildAndShow(@NotNull Player... players);
}
