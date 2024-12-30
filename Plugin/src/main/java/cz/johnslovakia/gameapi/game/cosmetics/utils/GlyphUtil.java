package cz.johnslovakia.gameapi.game.cosmetics.utils;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.utils.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.logging.Level;

public class GlyphUtil
{
    
    public static void sendGlyphs(File file, Location loc) {
        BufferedImage image = null;
        try {
            image = ImageIO.read(file);
        } catch (final IOException e) {
            Logger.log("Unable to read glyph file!", Logger.LogType.ERROR);
        }
        ImageParticles particles = new ImageParticles(image, 1);
        particles.setAnchor(50, 10);
        particles.setDisplayRatio(0.1);
        Map<Location, Color> particle = particles.getParticles(loc, loc.getPitch(), 180.0f);
        for (Location spot : particle.keySet()) {
            //HCore.syncScheduler().run(() -> sendRedstoneParticle(spot, particle.get(spot)));
            new BukkitRunnable(){
                @Override
                public void run() {
                    sendRedstoneParticle(spot, particle.get(spot));
                }
            }.runTaskAsynchronously(GameAPI.getInstance());
        }
    }

    public static void sendRedstoneParticle(Location location, Color color){
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.WORLD_PARTICLES);
        packet.getParticles().write(0, EnumWrappers.Particle.REDSTONE);
        packet.getFloat().write(0, (float) location.getX());
        packet.getFloat().write(1, (float) location.getY());
        packet.getFloat().write(2, (float) location.getZ());
        packet.getFloat().write(3, (float) color.getRed() / 255);
        packet.getFloat().write(4, (float) color.getGreen() / 255);
        packet.getFloat().write(5, (float) color.getBlue() / 255);
        packet.getFloat().write(6, 1.0f);

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            ProtocolLibrary.getProtocolManager().sendServerPacket(onlinePlayer, packet);
        }
    }

    public static void sendRedstoneParticle(Player player, Location location, Color color){
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.WORLD_PARTICLES);
        packet.getParticles().write(0, EnumWrappers.Particle.REDSTONE);
        packet.getFloat().write(0, (float) location.getX());
        packet.getFloat().write(1, (float) location.getY());
        packet.getFloat().write(2, (float) location.getZ());
        packet.getFloat().write(3, (float) color.getRed() / 255);
        packet.getFloat().write(4, (float) color.getGreen() / 255);
        packet.getFloat().write(5, (float) color.getBlue() / 255);
        packet.getFloat().write(6, 1.0f);

        ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
    }
}