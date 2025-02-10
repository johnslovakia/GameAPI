package cz.johnslovakia.gameapi.game.cosmetics.defaultCosmetics;

import com.cryptomorin.xseries.particles.ParticleDisplay;
import com.cryptomorin.xseries.particles.XParticle;
import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.events.GamePlayerDeathEvent;
import cz.johnslovakia.gameapi.game.cosmetics.Cosmetic;
import cz.johnslovakia.gameapi.game.cosmetics.CosmeticRarity;
import cz.johnslovakia.gameapi.game.cosmetics.CosmeticsCategory;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.utils.*;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class KillEffectsCategory extends CosmeticsCategory implements Listener {

    public KillEffectsCategory() {
        super("Kill Effects", new ItemStack(Material.IRON_SWORD));

        FileConfiguration config = GameAPI.getInstance().getMinigame().getPlugin().getConfig();
        
        int LEGENDARY_PRICE = Utils.getPrice(config, "kill_effects.legendary", 18000);
        int EPIC_PRICE = Utils.getPrice(config, "kill_effects.epic", 14000);
        int RARE_PRICE = Utils.getPrice(config, "kill_effects.rare", 8000);
        int UNCOMMON_PRICE = Utils.getPrice(config, "kill_effects.uncommon", 6000);
        int COMMON_PRICE = Utils.getPrice(config, "kill_effects.common", 4000);

        Cosmetic hearth = new Cosmetic("Hearth", new ItemStack(Material.REDSTONE), COMMON_PRICE, CosmeticRarity.COMMON)
                .setLocationConsumer(location -> {
                    //Location finalLocation = location.add(0.5, 0.5, 0.5);
                    for (double height = 0.0; height < 1.0; height += 0.2) {
                        location.getWorld().spawnParticle(Particle.HEART, location.clone().add((double) MathUtils.randomRange(-1.0f, 1.0f), height, (double)MathUtils.randomRange(-1.0f, 1.0f)), 1);
                    }
                });
        Cosmetic squid = new Cosmetic("Squid", new ItemStack(Material.SQUID_SPAWN_EGG), EPIC_PRICE, CosmeticRarity.EPIC)
                .setLocationConsumer(location -> {
                    Location finalLocation = location.add(0, -0.3, 0);

                    ArmorStand armor = (ArmorStand)finalLocation.getWorld().spawnEntity(finalLocation.add(0, -1, 0), EntityType.ARMOR_STAND);
                    armor.setVisible(false);
                    armor.setGravity(false);
                    Entity e = finalLocation.getWorld().spawnEntity(finalLocation, EntityType.SQUID);
                    armor.addPassenger(e);
                    e.setInvulnerable(true);

                    new BukkitRunnable() {
                        int i = 0;
                        @Override
                        public void run() {
                            i++;
                            if (armor.getPassengers().get(0) == null){
                                cancel();
                            }
                            Entity passenger = armor.getPassengers().get(0);
                            armor.eject();
                            armor.teleport(armor.getLocation().add(0,0.5,0));
                            armor.addPassenger(passenger);
                            armor.getLocation().getWorld().spawnParticle(Particle.FLAME, armor.getLocation().add(0.0, -0.2, 0.0), 1);
                            if(i == 20) {
                                armor.remove();
                                e.remove();
                                armor.getLocation().getWorld().spawnParticle(Particle.EXPLOSION, armor.getLocation().add(0.0, 0.5, 0.0), 1);
                                i = 0;
                                cancel();
                            }
                        }
                    }.runTaskTimer(GameAPI.getInstance(), 1, 0);
                });
        Cosmetic ball = new Cosmetic("Ball", new ItemStack(Material.SNOWBALL), UNCOMMON_PRICE, CosmeticRarity.UNCOMMON)
                .setLocationConsumer(location -> {
                    XParticle.sphere(1.5, 12, ParticleDisplay.of(Particle.DUST).withColor(Color.RED, 0.75f).withLocation(location.add(0, 0.4, 0)));
                });
        Cosmetic line = new Cosmetic("Line into the sky", new ItemStack(Material.BLAZE_ROD), RARE_PRICE, CosmeticRarity.RARE)
                .setLocationConsumer(location -> {
                    XParticle.line(location.subtract(0, 1, 0),  location.add(0, 60, 0), 12, ParticleDisplay.of(Particle.DUST).withColor(Color.RED, 0.75f).withLocation(location.add(0, 0.4, 0)));
                });
        Cosmetic tornado = new Cosmetic("Tornado", new ItemStack(Material.LIGHT_GRAY_DYE), LEGENDARY_PRICE, CosmeticRarity.LEGENDARY)
                .setLocationConsumer(location -> {
                    new BukkitRunnable() {
                        int angle = 0;

                        @Override
                        public void run() {
                            final int max_height = 4;
                            final double max_radius = 2;
                            final int lines = 4;
                            final double height_increasement = 0.25;
                            final double radius_increasement = max_radius / max_height;

                            for (int l = 0; l < lines; ++l) {
                                for (double y = 0.0; y < max_height; y += height_increasement) {
                                    final double radius = y * radius_increasement;
                                    final double x = Math.cos(Math.toRadians(360.0 / lines * l + y * 30.0 - this.angle)) * radius;
                                    final double z = Math.sin(Math.toRadians(360.0 / lines * l + y * 30.0 - this.angle)) * radius;

                                    Location particleLocation = location.clone().add(x, y, z);
                                    location.getWorld().spawnParticle(Particle.CLOUD, particleLocation, 1, 0.01, 0.01, 0.01, 0.0);
                                }
                            }
                            ++this.angle;

                            if (this.angle == 40) {
                                this.cancel();
                            }
                        }
                    }.runTaskTimer(GameAPI.getInstance(), 2L, 0L);
                });
        Cosmetic rainbow = new Cosmetic("Rainbow", new ItemStack(Material.RED_DYE), EPIC_PRICE, CosmeticRarity.EPIC)
                .setLocationConsumer(location -> {
                    XParticle.rainbow(0.5, 15, 3, 4, 0.3, ParticleDisplay.of(Particle.DUST).withLocation(location.add(0, 0.4, 0)));
                });
        Cosmetic blood = new Cosmetic("Blood", new ItemStack(Material.RED_DYE), COMMON_PRICE, CosmeticRarity.COMMON)
                .setLocationConsumer(location -> {
                    Particle.DustOptions dustOptions = new Particle.DustOptions(org.bukkit.Color.RED, 1.0f);
                    location.getWorld().spawnParticle(Particle.DUST, location, 50, 0.5, 1, 0.5, dustOptions);
                });
        Cosmetic sparkle = new Cosmetic("Sparkle", new ItemStack(Material.WHITE_DYE), EPIC_PRICE, CosmeticRarity.EPIC)
                .setLocationConsumer(location -> {
                    location.getWorld().spawnParticle(Particle.END_ROD, location, 45, 0.5, 0.7, 0.5);
                });
        Cosmetic glow = new Cosmetic("Glow", new ItemStack(Material.GLOW_SQUID_SPAWN_EGG), RARE_PRICE, CosmeticRarity.RARE)
                .setLocationConsumer(location -> {
                    location.getWorld().spawnParticle(Particle.GLOW, location, 20, MathUtils.randomRange(-0.7f, 0.7f), 0.5, MathUtils.randomRange(-0.7f, 0.7f));
                });
        Cosmetic musical = new Cosmetic("Musical", new ItemStack(Material.JUKEBOX), COMMON_PRICE, CosmeticRarity.COMMON)
                .setLocationConsumer(location -> {
                    location.getWorld().spawnParticle(Particle.NOTE, location, 25, 1, 1, 1);
                });
        Cosmetic blossom = new Cosmetic("Blossom", new ItemStack(Material.PINK_DYE), UNCOMMON_PRICE, CosmeticRarity.UNCOMMON)
                .setLocationConsumer(location -> {
                    location.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR, location, 75, 1.15, 1, 1);
                    location.getWorld().spawnParticle(Particle.FALLING_SPORE_BLOSSOM, location, 55, 1, 0.7, 1);
                });
        Cosmetic presentExplosion = new Cosmetic("Present Explosion", GameAPI.getInstance().getVersionSupport().getCustomHead("12919c67317c7678438ff520c98dde0e3b4d68769c8938a5a3de2968edfc7314"), EPIC_PRICE, CosmeticRarity.EPIC)
                .setLocationConsumer(location -> {
                    List<Item> items = new ArrayList<>();


                    for (int i = 0; i <= 20; i++) {
                        ItemStack item =  new ItemBuilder(GameAPI.getInstance().getVersionSupport().getCustomHead("12919c67317c7678438ff520c98dde0e3b4d68769c8938a5a3de2968edfc7314")).setName(UUID.randomUUID().toString()).toItemStack();
                        ItemUtils.getInstance().markAsNoPickup(item);

                        Location spawnLocation = location.clone().add(0, 0.5 * (i * 0.1), 0);

                        Item entity = spawnLocation.getWorld().dropItem(spawnLocation, item);
                        Vector direction = new Vector((Math.random() - 0.5) * 2, 1, (Math.random() - 0.5) * 2);
                        entity.setVelocity(direction.multiply(0.2));
                        items.add(entity);
                    }
                    Bukkit.getScheduler().runTaskLater(GameAPI.getInstance(), task -> items.forEach(Entity::remove), 30L);
                });
        Cosmetic pumpkinExplosion = new Cosmetic("Pumpkin Explosion", new ItemStack(Material.PUMPKIN), RARE_PRICE, CosmeticRarity.RARE)
                .setLocationConsumer(location -> {
                    List<Item> items = new ArrayList<>();

                    for (int i = 0; i <= 20; i++) {
                        ItemStack item = new ItemBuilder(Material.PUMPKIN).setName(UUID.randomUUID().toString()).toItemStack();
                        ItemUtils.getInstance().markAsNoPickup(item);

                        Location spawnLocation = location.clone().add(0, 0.5 * (i * 0.1), 0);

                        Item entity = spawnLocation.getWorld().dropItem(spawnLocation, item);
                        Vector direction = new Vector((Math.random() - 0.5) * 2, 1.3, (Math.random() - 0.5) * 2);
                        entity.setVelocity(direction.multiply(0.2));
                        items.add(entity);
                    }
                    Bukkit.getScheduler().runTaskLater(GameAPI.getInstance(), task -> items.forEach(Entity::remove), 30L);
                });

        addCosmetic(hearth, squid, ball, tornado, blood, sparkle, musical, blossom, presentExplosion, pumpkinExplosion, glow);


        Bukkit.getPluginManager().registerEvents(this, GameAPI.getInstance());
    }


    @EventHandler
    public void onGamePlayerDeath(GamePlayerDeathEvent e) {
        GamePlayer killer = e.getKiller();
        if (killer != null && getSelectedCosmetic(killer) != null){
            getSelectedCosmetic(killer).getLocationConsumer().accept(e.getGamePlayer().getOnlinePlayer().getLocation());
        }
    }
}
