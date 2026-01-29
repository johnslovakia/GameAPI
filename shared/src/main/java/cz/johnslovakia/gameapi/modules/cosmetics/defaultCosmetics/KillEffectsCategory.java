package cz.johnslovakia.gameapi.modules.cosmetics.defaultCosmetics;

import com.cryptomorin.xseries.particles.ParticleDisplay;
import com.cryptomorin.xseries.particles.Particles;
import cz.johnslovakia.gameapi.Shared;
import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.cosmetics.Cosmetic;
import cz.johnslovakia.gameapi.modules.cosmetics.CosmeticRarity;
import cz.johnslovakia.gameapi.modules.cosmetics.CosmeticsCategory;
import cz.johnslovakia.gameapi.modules.cosmetics.CosmeticsModule;
import cz.johnslovakia.gameapi.modules.resources.ResourcesModule;
import cz.johnslovakia.gameapi.utils.*;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class KillEffectsCategory extends CosmeticsCategory implements Listener {

    public KillEffectsCategory(CosmeticsModule manager) {
        super("Kill Effects", new ItemStack(Material.IRON_SWORD));

        FileConfiguration config = Shared.getInstance().getPlugin().getConfig();

        int LEGENDARY_COINS_PRICE = Utils.getPrice(config, "kill_effects.legendary", 18000);
        int EPIC_COINS_PRICE = Utils.getPrice(config, "kill_effects.epic", 14000);
        int RARE_COINS_PRICE = Utils.getPrice(config, "kill_effects.rare", 8000);
        int UNCOMMON_COINS_PRICE = Utils.getPrice(config, "kill_effects.uncommon", 6000);
        int COMMON_COINS_PRICE = Utils.getPrice(config, "kill_effects.common", 4000);

        int LEGENDARY_TOKEN_PRICE = 7;
        int EPIC_TOKEN_PRICE = 5;
        int RARE_TOKEN_PRICE = 4;
        int UNCOMMON_TOKEN_PRICE = 3;
        int COMMON_TOKEN_PRICE = 2;

        Cosmetic hearth = new Cosmetic("Hearth", new ItemStack(Material.REDSTONE), CosmeticRarity.COMMON)
                .addCost(manager.getMainResource(), COMMON_COINS_PRICE)
                .addCost(ModuleManager.getModule(ResourcesModule.class).getResourceByName("CosmeticTokens"), COMMON_TOKEN_PRICE)
                .setAsPurchasable()
                .setLocationConsumer(location -> {
                    for (double height = 0.0; height < 1.0; height += 0.2) {
                        location.getWorld().spawnParticle(Particle.HEART, location.clone().add((double) RandomUtils.randomFloat(-1.0f, 1.0f), height, (double)RandomUtils.randomFloat(-1.0f, 1.0f)), 1);
                    }
                });
        
        Cosmetic squid = new Cosmetic("Squid", new ItemStack(Material.SQUID_SPAWN_EGG), CosmeticRarity.EPIC)
                .addCost(manager.getMainResource(), EPIC_COINS_PRICE)
                .addCost(ModuleManager.getModule(ResourcesModule.class).getResourceByName("CosmeticTokens"), EPIC_TOKEN_PRICE)
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
                    }.runTaskTimer(Shared.getInstance().getPlugin(), 1, 0);
                });
        
        Cosmetic ball = new Cosmetic("Ball", new ItemStack(Material.SNOWBALL), CosmeticRarity.UNCOMMON)
                .addCost(manager.getMainResource(), UNCOMMON_COINS_PRICE)
                .addCost(ModuleManager.getModule(ResourcesModule.class).getResourceByName("CosmeticTokens"), UNCOMMON_TOKEN_PRICE)
                .setAsPurchasable()
                .setLocationConsumer(location -> {
                    Particles.sphere(1.5, 12, ParticleDisplay.of(Particle.DUST).withColor(Color.RED, 0.75f).withLocation(location.add(0, 0.4, 0)));
                });
        
        Cosmetic line = new Cosmetic("Line into the sky", new ItemStack(Material.BLAZE_ROD), CosmeticRarity.RARE)
                .addCost(manager.getMainResource(), RARE_COINS_PRICE)
                .addCost(ModuleManager.getModule(ResourcesModule.class).getResourceByName("CosmeticTokens"), RARE_TOKEN_PRICE)
                .setLocationConsumer(location -> {
                    Particles.line(location.subtract(0, 1, 0),  location.add(0, 60, 0), 12, ParticleDisplay.of(Particle.DUST).withColor(Color.RED, 0.75f).withLocation(location.add(0, 0.4, 0)));
                });

        Cosmetic tornado = new Cosmetic("Tornado", new ItemStack(Material.LIGHT_GRAY_DYE), CosmeticRarity.LEGENDARY)
                .addCost(manager.getMainResource(), LEGENDARY_COINS_PRICE)
                .addCost(ModuleManager.getModule(ResourcesModule.class).getResourceByName("CosmeticTokens"), LEGENDARY_TOKEN_PRICE)
                .setLocationConsumer(location -> {
                    new BukkitRunnable() {
                        int angle = 0;

                        @Override
                        public void run() {
                            final int max_height = 2;
                            final double max_radius = 1.2;
                            final int lines = 3;
                            final double height_increasement = 0.3;
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

                            if (this.angle == 20) {
                                this.cancel();
                            }
                        }
                    }.runTaskTimer(Shared.getInstance().getPlugin(), 2L, 1L);
                });
        
        Cosmetic rainbow = new Cosmetic("Rainbow", new ItemStack(Material.RED_DYE), CosmeticRarity.EPIC)
                .addCost(manager.getMainResource(), EPIC_COINS_PRICE)
                .addCost(ModuleManager.getModule(ResourcesModule.class).getResourceByName("CosmeticTokens"), EPIC_TOKEN_PRICE)
                .setLocationConsumer(location -> {
                    Particles.rainbow(0.5, 15, 3, 4, 0.3, ParticleDisplay.of(Particle.DUST).withLocation(location.add(0, 0.4, 0)));
                });
        
        Cosmetic blood = new Cosmetic("Blood", new ItemStack(Material.RED_DYE), CosmeticRarity.COMMON)
                .addCost(manager.getMainResource(), COMMON_COINS_PRICE)
                .addCost(ModuleManager.getModule(ResourcesModule.class).getResourceByName("CosmeticTokens"), COMMON_TOKEN_PRICE)
                .setAsPurchasable()
                .setLocationConsumer(location -> {
                    Particle.DustOptions dustOptions = new Particle.DustOptions(org.bukkit.Color.RED, 1.0f);
                    location.getWorld().spawnParticle(Particle.DUST, location, 50, 0.5, 1, 0.5, dustOptions);
                });
        
        Cosmetic sparkle = new Cosmetic("Sparkle", new ItemStack(Material.WHITE_DYE), CosmeticRarity.EPIC)
                .addCost(manager.getMainResource(), EPIC_COINS_PRICE)
                .addCost(ModuleManager.getModule(ResourcesModule.class).getResourceByName("CosmeticTokens"), EPIC_TOKEN_PRICE)
                .setLocationConsumer(location -> {
                    location.getWorld().spawnParticle(Particle.END_ROD, location, 45, 0.5, 0.7, 0.5);
                });
        
        Cosmetic glow = new Cosmetic("Glow", new ItemStack(Material.GLOW_SQUID_SPAWN_EGG), CosmeticRarity.RARE)
                .addCost(manager.getMainResource(), RARE_COINS_PRICE)
                .addCost(ModuleManager.getModule(ResourcesModule.class).getResourceByName("CosmeticTokens"), RARE_TOKEN_PRICE)
                .setLocationConsumer(location -> {
                    location.getWorld().spawnParticle(Particle.GLOW, location, 20, RandomUtils.randomFloat(-0.7f, 0.7f), 0.5, RandomUtils.randomFloat(-0.7f, 0.7f));
                });
        
        Cosmetic musical = new Cosmetic("Musical", new ItemStack(Material.JUKEBOX), CosmeticRarity.COMMON)
                .addCost(manager.getMainResource(), COMMON_COINS_PRICE)
                .addCost(ModuleManager.getModule(ResourcesModule.class).getResourceByName("CosmeticTokens"), COMMON_TOKEN_PRICE)
                .setAsPurchasable()
                .setLocationConsumer(location -> {
                    location.getWorld().spawnParticle(Particle.NOTE, location, 25, 1, 1, 1);
                });
        
        Cosmetic blossom = new Cosmetic("Blossom", new ItemStack(Material.PINK_DYE), CosmeticRarity.UNCOMMON)
                .addCost(manager.getMainResource(), UNCOMMON_COINS_PRICE)
                .addCost(ModuleManager.getModule(ResourcesModule.class).getResourceByName("CosmeticTokens"), UNCOMMON_TOKEN_PRICE)
                .setLocationConsumer(location -> {
                    location.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR, location, 75, 1.15, 1, 1);
                    location.getWorld().spawnParticle(Particle.FALLING_SPORE_BLOSSOM, location, 55, 1, 0.7, 1);
                });
        
        Cosmetic presentExplosion = new Cosmetic("Present Explosion", Utils.getCustomHead("12919c67317c7678438ff520c98dde0e3b4d68769c8938a5a3de2968edfc7314"), CosmeticRarity.EPIC)
                .addCost(manager.getMainResource(), EPIC_COINS_PRICE)
                .addCost(ModuleManager.getModule(ResourcesModule.class).getResourceByName("CosmeticTokens"), EPIC_TOKEN_PRICE)
                .setLocationConsumer(location -> {
                    List<Item> items = new ArrayList<>();

                    for (int i = 0; i <= 20; i++) {
                        ItemStack item =  new ItemBuilder(Utils.getCustomHead("12919c67317c7678438ff520c98dde0e3b4d68769c8938a5a3de2968edfc7314")).setName(UUID.randomUUID().toString()).toItemStack();
                        ItemUtils.getInstance().markAsNoPickup(item);

                        Location spawnLocation = location.clone().add(0, 0.5 * (i * 0.1), 0);

                        Item entity = spawnLocation.getWorld().dropItem(spawnLocation, item);
                        Vector direction = new Vector((Math.random() - 0.5) * 2, 1, (Math.random() - 0.5) * 2);
                        entity.setVelocity(direction.multiply(0.2));
                        items.add(entity);
                    }
                    Bukkit.getScheduler().runTaskLater(Shared.getInstance().getPlugin(), task -> items.forEach(Entity::remove), 30L);
                });
        
        Cosmetic pumpkinExplosion = new Cosmetic("Pumpkin Explosion", new ItemStack(Material.PUMPKIN), CosmeticRarity.RARE)
                .addCost(manager.getMainResource(), RARE_COINS_PRICE)
                .addCost(ModuleManager.getModule(ResourcesModule.class).getResourceByName("CosmeticTokens"), RARE_TOKEN_PRICE)
                .setAsPurchasable()
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
                    Bukkit.getScheduler().runTaskLater(Shared.getInstance().getPlugin(), task -> items.forEach(Entity::remove), 30L);
                });
        
        Cosmetic lightning = new Cosmetic("Lightning Strike", new ItemStack(Material.LIGHTNING_ROD), CosmeticRarity.LEGENDARY)
                .addCost(manager.getMainResource(), LEGENDARY_COINS_PRICE)
                .addCost(ModuleManager.getModule(ResourcesModule.class).getResourceByName("CosmeticTokens"), LEGENDARY_TOKEN_PRICE)
                .setLocationConsumer(location -> {
                    location.getWorld().strikeLightningEffect(location);
                    location.getWorld().spawnParticle(Particle.FLASH, location, 3);
                    location.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, location, 50, 0.5, 1, 0.5, 0.1);
                });

        Cosmetic firework = new Cosmetic("Firework", new ItemStack(Material.FIREWORK_ROCKET), CosmeticRarity.RARE)
                .addCost(manager.getMainResource(), RARE_COINS_PRICE)
                .addCost(ModuleManager.getModule(ResourcesModule.class).getResourceByName("CosmeticTokens"), RARE_TOKEN_PRICE)
                .setAsPurchasable()
                .setLocationConsumer(location -> {
                    location.getWorld().spawnParticle(Particle.FIREWORK, location, 100, 0.5, 0.5, 0.5, 0.15);
                    Bukkit.getScheduler().runTaskLater(Shared.getInstance().getPlugin(), () -> {
                        location.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, location, 1);
                    }, 10L);
                });

        Cosmetic souls = new Cosmetic("Soul Reaper", new ItemStack(Material.SOUL_LANTERN), CosmeticRarity.LEGENDARY)
                .addCost(manager.getMainResource(), LEGENDARY_COINS_PRICE)
                .addCost(ModuleManager.getModule(ResourcesModule.class).getResourceByName("CosmeticTokens"), LEGENDARY_TOKEN_PRICE)
                .setLocationConsumer(location -> {
                    new BukkitRunnable() {
                        int ticks = 0;
                        @Override
                        public void run() {
                            for (int i = 0; i < 3; i++) {
                                double angle = Math.toRadians(ticks * 20 + i * 120);
                                double x = Math.cos(angle) * 1.0;
                                double z = Math.sin(angle) * 1.0;
                                Location particleLoc = location.clone().add(x, ticks * 0.1, z);
                                location.getWorld().spawnParticle(Particle.SOUL, particleLoc, 2, 0.05, 0.05, 0.05, 0.01);
                            }
                            ticks++;
                            if (ticks >= 15) {
                                cancel();
                            }
                        }
                    }.runTaskTimer(Shared.getInstance().getPlugin(), 0L, 1L);
                });

        Cosmetic freeze = new Cosmetic("Frozen", new ItemStack(Material.ICE), CosmeticRarity.UNCOMMON)
                .addCost(manager.getMainResource(), UNCOMMON_COINS_PRICE)
                .addCost(ModuleManager.getModule(ResourcesModule.class).getResourceByName("CosmeticTokens"), UNCOMMON_TOKEN_PRICE)
                .setAsPurchasable()
                .setLocationConsumer(location -> {
                    Particle.DustOptions dustOptions = new Particle.DustOptions(org.bukkit.Color.fromRGB(173, 216, 230), 1.2f);
                    location.getWorld().spawnParticle(Particle.DUST, location, 60, 0.6, 0.8, 0.6, dustOptions);
                    location.getWorld().spawnParticle(Particle.SNOWFLAKE, location, 30, 0.7, 0.5, 0.7, 0.02);
                });

        Cosmetic poison = new Cosmetic("Toxic Cloud", new ItemStack(Material.FERMENTED_SPIDER_EYE), CosmeticRarity.RARE)
                .addCost(manager.getMainResource(), RARE_COINS_PRICE)
                .addCost(ModuleManager.getModule(ResourcesModule.class).getResourceByName("CosmeticTokens"), RARE_TOKEN_PRICE)
                .setLocationConsumer(location -> {
                    location.getWorld().spawnParticle(Particle.MYCELIUM, location, 80, 1, 0.5, 1, 0.05);
                    Particle.DustOptions dustOptions = new Particle.DustOptions(org.bukkit.Color.fromRGB(50, 205, 50), 1.0f);
                    location.getWorld().spawnParticle(Particle.DUST, location, 40, 0.8, 0.4, 0.8, dustOptions);
                });

        Cosmetic flames = new Cosmetic("Ring of Fire", new ItemStack(Material.FIRE_CHARGE), CosmeticRarity.EPIC)
                .addCost(manager.getMainResource(), EPIC_COINS_PRICE)
                .addCost(ModuleManager.getModule(ResourcesModule.class).getResourceByName("CosmeticTokens"), EPIC_TOKEN_PRICE)
                .setLocationConsumer(location -> {
                    for (int i = 0; i < 360; i += 15) {
                        double angle = Math.toRadians(i);
                        double x = Math.cos(angle) * 1.5;
                        double z = Math.sin(angle) * 1.5;
                        Location flameLoc = location.clone().add(x, 0, z);
                        location.getWorld().spawnParticle(Particle.FLAME, flameLoc, 8, 0.1, 0.3, 0.1, 0.02);
                        location.getWorld().spawnParticle(Particle.SMOKE, flameLoc, 3, 0.1, 0.2, 0.1, 0.01);
                    }
                });

        Cosmetic enchant = new Cosmetic("Enchanted", new ItemStack(Material.ENCHANTING_TABLE), CosmeticRarity.UNCOMMON)
                .addCost(manager.getMainResource(), UNCOMMON_COINS_PRICE)
                .addCost(ModuleManager.getModule(ResourcesModule.class).getResourceByName("CosmeticTokens"), UNCOMMON_TOKEN_PRICE)
                .setAsPurchasable()
                .setLocationConsumer(location -> {
                    location.getWorld().spawnParticle(Particle.ENCHANT, location, 100, 1, 1.5, 1, 1);
                });

        Cosmetic wither = new Cosmetic("Withering", new ItemStack(Material.WITHER_ROSE), CosmeticRarity.LEGENDARY)
                .addCost(manager.getMainResource(), LEGENDARY_COINS_PRICE)
                .addCost(ModuleManager.getModule(ResourcesModule.class).getResourceByName("CosmeticTokens"), LEGENDARY_TOKEN_PRICE)
                .setLocationConsumer(location -> {
                    new BukkitRunnable() {
                        int ticks = 0;
                        @Override
                        public void run() {
                            location.getWorld().spawnParticle(Particle.SMOKE, location, 15, 0.5, 0.3, 0.5, 0.05);
                            Particle.DustOptions dustOptions = new Particle.DustOptions(org.bukkit.Color.fromRGB(20, 20, 20), 1.5f);
                            location.getWorld().spawnParticle(Particle.DUST, location.clone().add(0, ticks * 0.15, 0), 10, 0.4, 0.1, 0.4, dustOptions);
                            
                            ticks++;
                            if (ticks >= 12) {
                                location.getWorld().spawnParticle(Particle.EXPLOSION, location, 2);
                                cancel();
                            }
                        }
                    }.runTaskTimer(Shared.getInstance().getPlugin(), 0L, 2L);
                });

        Cosmetic coins = new Cosmetic("Money Rain", new ItemStack(Material.GOLD_INGOT), CosmeticRarity.RARE)
                .addCost(manager.getMainResource(), RARE_COINS_PRICE)
                .addCost(ModuleManager.getModule(ResourcesModule.class).getResourceByName("CosmeticTokens"), RARE_TOKEN_PRICE)
                .setLocationConsumer(location -> {
                    List<Item> items = new ArrayList<>();
                    
                    for (int i = 0; i <= 15; i++) {
                        ItemStack item = new ItemBuilder(Material.GOLD_NUGGET).setName(UUID.randomUUID().toString()).toItemStack();
                        ItemUtils.getInstance().markAsNoPickup(item);
                        
                        Location spawnLocation = location.clone().add(
                            RandomUtils.randomFloat(-0.5f, 0.5f), 
                            2.0, 
                            RandomUtils.randomFloat(-0.5f, 0.5f)
                        );
                        
                        Item entity = spawnLocation.getWorld().dropItem(spawnLocation, item);
                        Vector direction = new Vector(
                            (Math.random() - 0.5) * 0.3, 
                            -0.5, 
                            (Math.random() - 0.5) * 0.3
                        );
                        entity.setVelocity(direction);
                        items.add(entity);
                    }
                    location.getWorld().spawnParticle(Particle.GLOW, location.clone().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
                    Bukkit.getScheduler().runTaskLater(Shared.getInstance().getPlugin(), task -> items.forEach(Entity::remove), 40L);
                });

        addCosmetic(hearth, squid, ball, tornado, blood, sparkle, musical, blossom, presentExplosion, 
                    pumpkinExplosion, glow, firework, souls, freeze, poison, flames,
                    enchant, wither, coins, line, rainbow);
    }
}