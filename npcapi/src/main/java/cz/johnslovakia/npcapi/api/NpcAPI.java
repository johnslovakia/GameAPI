package cz.johnslovakia.npcapi.api;

import cz.johnslovakia.npcapi.impl.NPCManagerImpl;
import cz.johnslovakia.npcapi.listener.NPCListener;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Main entry point for the NPC API.
 *
 * <h2>Usage after init</h2>
 * <pre>{@code
 * NPC guard = NpcAPI.get()
 *     .builder("guard-1")
 *     .location(spawnPoint)
 *     .displayName(Component.text("§cGuard"))
 *     .skin(SkinData.STEVE)
 *     .onClick((player, type) -> player.sendMessage("Halt!"))
 *     .build();
 *
 * guard.show(player);
 * }</pre>
 */
public final class NpcAPI {

    private static NPCManagerImpl manager;
    private static NPCListener    listener;

    private NpcAPI() {}

    /**
     * Initialises the NPC API and binds it to the given plugin.
     *
     * <p>Call this from your plugin's {@code onEnable()}. Calling it more than
     * once (without {@link #shutdown()} in between) is a no-op — the first
     * caller "owns" the instance.
     *
     * @param plugin The owning plugin (used for listener registration, scheduling, logging)
     */
    public static synchronized void init(@NotNull Plugin plugin) {
        if (manager != null) {
            plugin.getLogger().warning(
                    "NpcAPI.init() called again by " + plugin.getName()
                    + " — already initialised, ignoring.");
            return;
        }

        manager  = new NPCManagerImpl(plugin);
        listener = new NPCListener(manager, plugin);

        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
        listener.injectAll();

        plugin.getLogger().info("NpcAPI initialised by " + plugin.getName() + ".");
    }

    /**
     * Shuts down the NPC API: despawns all NPCs and removes Netty injections.
     *
     * <p>Call this from your plugin's {@code onDisable()}.
     */
    public static synchronized void shutdown() {
        if (manager == null) return;

        manager.removeAll();
        manager = null;

        if (listener != null) {
            listener.uninjectAll();
            listener = null;
        }
    }

    /**
     * Returns the active {@link NPCManager}.
     *
     * @throws IllegalStateException if {@link #init(Plugin)} has not been called yet
     */
    @NotNull
    public static NPCManager get() {
        if (manager == null) {
            throw new IllegalStateException(
                    "NpcAPI has not been initialised. Call NpcAPI.init(plugin) in your onEnable().");
        }
        return manager;
    }

    /** Returns {@code true} if {@link #init(Plugin)} has been called and {@link #shutdown()} has not. */
    public static boolean isInitialised() {
        return manager != null;
    }
}
