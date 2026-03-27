package cz.johnslovakia.npcapi.util;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import cz.johnslovakia.npcapi.api.SkinData;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class PacketHelper {

    public static final byte SKIN_ALL_PARTS = 0x7F;
    private static final Logger LOG = Logger.getLogger("NpcAPI/PacketHelper");

    @SuppressWarnings("rawtypes")
    private static net.minecraft.network.syncher.EntityDataAccessor SKIN_PARTS_ACCESSOR;

    private static Constructor<?> ENTRY_CTOR;
    private static Constructor<?> PACKET_LIST_CTOR;

    private static int ENTRY_LISTED_INDEX = 2;

    static {
        net.minecraft.network.syncher.EntityDataAccessor<?> found = null;
        try {
            for (Field f : net.minecraft.world.entity.player.Player.class.getDeclaredFields()) {
                if (!Modifier.isStatic(f.getModifiers())) continue;
                if (f.getType() != net.minecraft.network.syncher.EntityDataAccessor.class) continue;
                f.setAccessible(true);
                var acc = (net.minecraft.network.syncher.EntityDataAccessor<?>) f.get(null);
                if (acc != null && acc.serializer().getClass().getName().contains("Byte")) found = acc;
            }
        } catch (Exception ignored) {}
        SKIN_PARTS_ACCESSOR = found;

        try {
            PACKET_LIST_CTOR = ClientboundPlayerInfoUpdatePacket.class
                    .getDeclaredConstructor(EnumSet.class, List.class);
            PACKET_LIST_CTOR.setAccessible(true);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "NpcAPI: PACKET_LIST_CTOR not found", e);
        }

        for (Class<?> inner : ClientboundPlayerInfoUpdatePacket.class.getDeclaredClasses()) {
            for (Constructor<?> c : inner.getDeclaredConstructors()) {
                Class<?>[] params = c.getParameterTypes();
                if (params.length >= 1 && params[0] == UUID.class) {
                    c.setAccessible(true);
                    ENTRY_CTOR = c;
                    int boolCount = 0;
                    for (int i = 0; i < params.length; i++) {
                        if (params[i] == boolean.class) {
                            if (boolCount == 0) { ENTRY_LISTED_INDEX = i; }
                            boolCount++;
                        }
                    }
                    LOG.info("NpcAPI: Entry ctor found — " + params.length + " params, listed@" + ENTRY_LISTED_INDEX);
                    break;
                }
            }
            if (ENTRY_CTOR != null) break;
        }
        if (ENTRY_CTOR == null) LOG.severe("NpcAPI: Entry ctor NOT found");
    }

    private PacketHelper() {}

    @NotNull
    public static ServerPlayer createFakePlayer(@NotNull UUID uuid, @NotNull String name, @NotNull SkinData skin, @NotNull Location location) {
        CraftServer craftServer = (CraftServer) org.bukkit.Bukkit.getServer();
        ServerLevel level = ((CraftWorld) Objects.requireNonNull(location.getWorld())).getHandle();

        com.destroystokyo.paper.profile.CraftPlayerProfile paperProfile =
                new com.destroystokyo.paper.profile.CraftPlayerProfile(uuid, name);
        paperProfile.setProperty(skin.isSigned()
                ? new com.destroystokyo.paper.profile.ProfileProperty("textures", skin.getValue(), skin.getSignature())
                : new com.destroystokyo.paper.profile.ProfileProperty("textures", skin.getValue()));
        GameProfile profile = paperProfile.getGameProfile();

        ServerPlayer fakePlayer = new ServerPlayer(craftServer.getServer(), level, profile, buildClientInformation());
        fakePlayer.setPos(location.getX(), location.getY(), location.getZ());
        fakePlayer.setYRot(location.getYaw());
        fakePlayer.setXRot(location.getPitch());
        fakePlayer.setYHeadRot(location.getYaw());

        applySkinParts(fakePlayer);
        return fakePlayer;
    }

    public static void sendSpawnPackets(@NotNull Player viewer, @NotNull ServerPlayer fakePlayer) {
        ClientboundPlayerInfoUpdatePacket addPacket = buildEntryPacket(fakePlayer, true);
        if (addPacket == null) {
            LOG.severe("NpcAPI: buildEntryPacket returned null for " + viewer.getName());
            return;
        }
        sendPacket(viewer, addPacket);

        sendPacket(viewer, new ClientboundAddEntityPacket(
                fakePlayer.getId(), fakePlayer.getUUID(),
                fakePlayer.getX(), fakePlayer.getY(), fakePlayer.getZ(),
                fakePlayer.getXRot(), fakePlayer.getYRot(),
                fakePlayer.getType(), 0, Vec3.ZERO, fakePlayer.getYHeadRot()
        ));

        List<SynchedEntityData.DataValue<?>> allData = fakePlayer.getEntityData().getNonDefaultValues();
        if (allData != null && !allData.isEmpty()) {
            sendPacket(viewer, new ClientboundSetEntityDataPacket(fakePlayer.getId(), allData));
        }

        sendPacket(viewer, new ClientboundRotateHeadPacket(
                fakePlayer, (byte) Math.round(fakePlayer.getYHeadRot() * 256f / 360f)));

        sendPacket(viewer, buildHideNameTagPacket(fakePlayer));
    }

    public static void sendDespawnPackets(@NotNull Player viewer, @NotNull ServerPlayer fakePlayer) {
        sendPacket(viewer, new ClientboundRemoveEntitiesPacket(fakePlayer.getId()));
        sendPacket(viewer, new ClientboundPlayerInfoRemovePacket(List.of(fakePlayer.getUUID())));
        sendPacket(viewer, buildRemoveTeamPacket(npcTeamName(fakePlayer)));
    }

    public static void sendTabRemovePacket(@NotNull Player viewer, @NotNull ServerPlayer fakePlayer) {
        sendPacket(viewer, new ClientboundPlayerInfoRemovePacket(List.of(fakePlayer.getUUID())));
    }

    public static void sendTabAddPacket(@NotNull Player viewer, @NotNull ServerPlayer fakePlayer) {
        ClientboundPlayerInfoUpdatePacket pkt = buildEntryPacket(fakePlayer, true);
        if (pkt != null) sendPacket(viewer, pkt);
    }

    public static void sendTeleportPacket(@NotNull Player viewer, @NotNull ServerPlayer fakePlayer, @NotNull Location location) {
        fakePlayer.setPos(location.getX(), location.getY(), location.getZ());
        fakePlayer.setYRot(location.getYaw());
        fakePlayer.setXRot(location.getPitch());
        fakePlayer.setYHeadRot(location.getYaw());

        sendPacket(viewer, new ClientboundTeleportEntityPacket(
                fakePlayer.getId(), PositionMoveRotation.of(fakePlayer), Set.of(), false));
        sendPacket(viewer, new ClientboundRotateHeadPacket(
                fakePlayer, (byte) Math.round(location.getYaw() * 256f / 360f)));
    }

    public static void sendLookPacket(@NotNull Player viewer, @NotNull ServerPlayer fakePlayer, float yaw, float pitch) {
        fakePlayer.setYRot(yaw);
        fakePlayer.setXRot(pitch);
        fakePlayer.setYHeadRot(yaw);

        sendPacket(viewer, new ClientboundMoveEntityPacket.Rot(
                fakePlayer.getId(),
                (byte) Math.round(yaw * 256f / 360f),
                (byte) Math.round(pitch * 256f / 360f),
                false
        ));
        sendPacket(viewer, new ClientboundRotateHeadPacket(
                fakePlayer, (byte) Math.round(yaw * 256f / 360f)));
    }

    public static void sendSwingArmPacket(@NotNull Player viewer, @NotNull ServerPlayer fakePlayer, boolean offHand) {
        sendPacket(viewer, new ClientboundAnimatePacket(fakePlayer, offHand ? 3 : 0));
    }

    public static void sendPacket(@NotNull Player player, @NotNull net.minecraft.network.protocol.Packet<?> packet) {
        ((CraftPlayer) player).getHandle().connection.send(packet);
    }

    public static float[] getAngles(@NotNull Location from, @NotNull Location to) {
        double dx = to.getX() - from.getX();
        double dy = (to.getY() + 1.62) - (from.getY() + 1.62);
        double dz = to.getZ() - from.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        return new float[]{
                (float) (-Math.toDegrees(Math.atan2(dx, dz))),
                (float) (-Math.toDegrees(Math.atan2(dy, dist)))
        };
    }

    @Nullable
    private static ClientboundPlayerInfoUpdatePacket buildEntryPacket(@NotNull ServerPlayer fakePlayer, boolean listed) {
        if (ENTRY_CTOR == null || PACKET_LIST_CTOR == null) return null;
        try {
            Class<?>[] types = ENTRY_CTOR.getParameterTypes();
            Object[] args = new Object[types.length];
            int boolSeen = 0;
            for (int i = 0; i < types.length; i++) {
                Class<?> t = types[i];
                if (t == UUID.class) { args[i] = fakePlayer.getUUID(); }
                else if (t == GameProfile.class) { args[i] = fakePlayer.getGameProfile(); }
                else if (t == GameType.class) { args[i] = GameType.SURVIVAL; }
                else if (t == int.class) { args[i] = 0; }
                else if (t == boolean.class) {
                    args[i] = (boolSeen == 0) ? listed : false;
                    boolSeen++;
                }
                else { args[i] = null; }
            }
            Object entry = ENTRY_CTOR.newInstance(args);
            return (ClientboundPlayerInfoUpdatePacket) PACKET_LIST_CTOR.newInstance(
                    EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER),
                    List.of(entry)
            );
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "NpcAPI: buildEntryPacket failed", e);
            return null;
        }
    }

    private static String npcTeamName(@NotNull ServerPlayer fakePlayer) {
        String hex = fakePlayer.getUUID().toString().replace("-", "");
        return hex.substring(hex.length() - 16);
    }

    @NotNull
    private static ClientboundSetPlayerTeamPacket buildHideNameTagPacket(@NotNull ServerPlayer fakePlayer) {
        Scoreboard sb = new Scoreboard();
        PlayerTeam team = new PlayerTeam(sb, npcTeamName(fakePlayer));
        team.setNameTagVisibility(Team.Visibility.NEVER);
        team.getPlayers().add(fakePlayer.getGameProfile().name());
        return ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(team, true);
    }

    @NotNull
    private static ClientboundSetPlayerTeamPacket buildRemoveTeamPacket(@NotNull String teamName) {
        Scoreboard sb = new Scoreboard();
        PlayerTeam team = new PlayerTeam(sb, teamName);
        return ClientboundSetPlayerTeamPacket.createRemovePacket(team);
    }

    @NotNull
    private static ClientInformation buildClientInformation() {
        try {
            var m = ClientInformation.class.getDeclaredMethod("createDefault");
            m.setAccessible(true);
            return (ClientInformation) m.invoke(null);
        } catch (Exception ignored) {}

        try {
            var ctor = ClientInformation.class.getDeclaredConstructors()[0];
            ctor.setAccessible(true);
            var types = ctor.getParameterTypes();
            var args = new Object[types.length];
            for (int i = 0; i < types.length; i++) {
                Class<?> t = types[i];
                if (t == String.class) args[i] = "en_us";
                else if (t == int.class) args[i] = i == 1 ? 8 : (int) SKIN_ALL_PARTS;
                else if (t == boolean.class) args[i] = true;
                else if (t == ChatVisiblity.class) args[i] = ChatVisiblity.FULL;
                else if (t == HumanoidArm.class) args[i] = HumanoidArm.RIGHT;
                else if (t == ParticleStatus.class) args[i] = ParticleStatus.DECREASED;
                else if (t.isEnum()) args[i] = t.getEnumConstants()[0];
                else args[i] = null;
            }
            return (ClientInformation) ctor.newInstance(args);
        } catch (Exception ignored) {}

        return new ClientInformation("en_us", 8, ChatVisiblity.FULL, true,
                (int) SKIN_ALL_PARTS, HumanoidArm.RIGHT, false, false, ParticleStatus.DECREASED);
    }

    @SuppressWarnings("unchecked")
    private static void applySkinParts(@NotNull ServerPlayer fakePlayer) {
        if (SKIN_PARTS_ACCESSOR == null) return;
        try { fakePlayer.getEntityData().set(SKIN_PARTS_ACCESSOR, SKIN_ALL_PARTS); }
        catch (Exception ignored) {}
    }
}