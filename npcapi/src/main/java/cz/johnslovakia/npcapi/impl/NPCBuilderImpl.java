package cz.johnslovakia.npcapi.impl;

import cz.johnslovakia.npcapi.api.NPC;
import cz.johnslovakia.npcapi.api.NPCBuilder;
import cz.johnslovakia.npcapi.api.SkinData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.BiConsumer;

public class NPCBuilderImpl implements NPCBuilder {

    private final String id;
    private final NPCManagerImpl manager;

    private Location location;
    private String displayName;
    private SkinData skin = SkinData.STEVE;
    private boolean nameVisible = true;
    private boolean listedInTab = false;
    private BiConsumer<Player, NPC.ClickType> clickHandler;

    public NPCBuilderImpl(@NotNull String id, @NotNull NPCManagerImpl manager) {
        this.id = id;
        this.manager = manager;
    }

    @Override
    public @NotNull NPCBuilder location(@NotNull Location location) {
        this.location = location.clone();
        return this;
    }

    @Override
    public @NotNull NPCBuilder displayName(@Nullable String name) {
        this.displayName = name;
        return this;
    }

    @Override
    public @NotNull NPCBuilder skin(@NotNull SkinData skin) {
        this.skin = skin;
        return this;
    }

    @Override
    public @NotNull NPCBuilder listedInTab(boolean listed) {
        this.listedInTab = listed;
        return this;
    }

    @Override
    public @NotNull NPCBuilder onClick(@Nullable BiConsumer<Player, NPC.ClickType> handler) {
        this.clickHandler = handler;
        return this;
    }

    @Override
    public @NotNull NPC build() {
        validate();
        NPCImpl npc = new NPCImpl(
                id,
                UUID.randomUUID(),
                location,
                displayName,
                skin,
                listedInTab,
                clickHandler,
                manager
        );
        manager.register(npc);
        return npc;
    }

    @Override
    public @NotNull NPC buildAndShow(@NotNull Player... players) {
        NPC npc = build();
        for (Player p : players) {
            npc.show(p);
        }
        return npc;
    }

    private void validate() {
        if (location == null) {
            throw new IllegalStateException("NPC location must be set before building (id=" + id + ")");
        }
        if (location.getWorld() == null) {
            throw new IllegalStateException("NPC location world must not be null (id=" + id + ")");
        }
        if (manager.getNPC(id).isPresent()) {
            throw new IllegalStateException("An NPC with id '" + id + "' already exists.");
        }
    }
}
