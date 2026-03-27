package cz.johnslovakia.npcapi.listener;

import cz.johnslovakia.npcapi.api.NPC;
import cz.johnslovakia.npcapi.event.NPCClickEvent;
import cz.johnslovakia.npcapi.impl.NPCImpl;
import cz.johnslovakia.npcapi.impl.NPCManagerImpl;
import io.netty.channel.*;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bukkit listener + Netty pipeline injector that intercepts NPC interactions.
 *
 * <p>We inject a {@link ChannelInboundHandlerAdapter} into each player's Netty
 * pipeline to catch {@link ServerboundInteractPacket} before the server tries to
 * look up our non-existent entity and throws an error.
 *
 * <p>Click-type detection uses {@link ServerboundInteractPacket#dispatch} with
 * the public {@link ServerboundInteractPacket.Handler} interface — the concrete
 * action inner classes ({@code InteractionAction}, {@code AttackAction}, etc.)
 * are package-private and cannot be referenced directly from outside NMS.
 */
public class NPCListener implements Listener {

    private static final String HANDLER_NAME = "npc_interact_handler";

    private final NPCManagerImpl manager;
    private final Plugin plugin;
    private final Set<UUID> injected = ConcurrentHashMap.newKeySet();

    public NPCListener(@NotNull NPCManagerImpl manager, @NotNull Plugin plugin) {
        this.manager = manager;
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        inject(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        uninject(event.getPlayer());
        manager.handlePlayerQuit(event.getPlayer());
    }

    public void injectAll() {
        plugin.getServer().getOnlinePlayers().forEach(this::inject);
    }

    public void uninjectAll() {
        plugin.getServer().getOnlinePlayers().forEach(this::uninject);
    }

    private void inject(@NotNull Player player) {
        if (!injected.add(player.getUniqueId())) return;

        Channel channel = ((CraftPlayer) player).getHandle().connection.connection.channel;
        channel.pipeline().addBefore("packet_handler", HANDLER_NAME,
                new ChannelInboundHandlerAdapter() {
                    @Override
                    public void channelRead(@NotNull ChannelHandlerContext ctx,
                                            @NotNull Object msg) throws Exception {
                        if (msg instanceof ServerboundInteractPacket packet
                                && handleInteract(player, packet)) {
                            return;
                        }
                        super.channelRead(ctx, msg);
                    }
                });
    }

    private void uninject(@NotNull Player player) {
        if (!injected.remove(player.getUniqueId())) return;
        try {
            Channel ch = ((CraftPlayer) player).getHandle().connection.connection.channel;
            if (ch.pipeline().get(HANDLER_NAME) != null) {
                ch.pipeline().remove(HANDLER_NAME);
            }
        } catch (Exception ignored) {}
    }

    /**
     * @return {@code true} if the packet targeted one of our NPCs (consume it)
     */
    private boolean handleInteract(@NotNull Player player,
                                   @NotNull ServerboundInteractPacket packet) {
        int entityId = packet.getEntityId();

        Optional<NPCImpl> opt = manager.getNPCByEntityId(entityId)
                .filter(n -> n instanceof NPCImpl)
                .map(n -> (NPCImpl) n);

        if (opt.isEmpty()) return false;

        NPCImpl npc = opt.get();
        NPC.ClickType clickType = resolveClickType(packet);
        if (clickType == null) return true;

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            NPCClickEvent event = new NPCClickEvent(player, npc, clickType);
            plugin.getServer().getPluginManager().callEvent(event);

            if (!event.isCancelled() && npc.getClickHandler() != null) {
                npc.getClickHandler().accept(player, clickType);
            }
        });

        return true;
    }

    /**
     * Determines click type using {@link ServerboundInteractPacket#dispatch},
     * which accepts the public {@link ServerboundInteractPacket.Handler} interface.
     *
     * <p>The three action types are:
     * <ul>
     *   <li>{@code onInteraction(hand)} — right-click (interaction without position)</li>
     *   <li>{@code onInteraction(hand, pos)} — right-click at specific position (INTERACT_AT)</li>
     *   <li>{@code onAttack()} — left-click</li>
     * </ul>
     */
    @Nullable
    private static NPC.ClickType resolveClickType(@NotNull ServerboundInteractPacket packet) {
        NPC.ClickType[] result = {null};

        packet.dispatch(new ServerboundInteractPacket.Handler() {
            @Override
            public void onInteraction(@NotNull InteractionHand hand) {
                result[0] = NPC.ClickType.RIGHT_CLICK;
            }

            @Override
            public void onInteraction(@NotNull InteractionHand hand, @NotNull Vec3 pos) {
                result[0] = NPC.ClickType.RIGHT_CLICK;
            }

            @Override
            public void onAttack() {
                result[0] = NPC.ClickType.LEFT_CLICK;
            }
        });

        return result[0];
    }
}
