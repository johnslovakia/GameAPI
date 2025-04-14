package cz.johnslovakia.gameapi.game.cosmetics.defaultCosmetics;

import com.cryptomorin.xseries.particles.ParticleDisplay;
import com.cryptomorin.xseries.particles.XParticle;
import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.game.cosmetics.*;
import cz.johnslovakia.gameapi.users.PlayerManager;
import cz.johnslovakia.gameapi.utils.ItemBuilder;
import cz.johnslovakia.gameapi.utils.ItemUtils;
import cz.johnslovakia.gameapi.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.awt.*;
import java.util.*;
import java.util.List;

public class TrailsCategory extends CosmeticsCategory {

    public TrailsCategory() {
        super("Projectile Trails", new ItemStack(Material.ARROW));

        FileConfiguration config = GameAPI.getInstance().getMinigame().getPlugin().getConfig();
        int LEGENDARY_PRICE = Utils.getPrice(config, "projectile_trails.legendary", 15000);
        int EPIC_PRICE = Utils.getPrice(config, "projectile_trails.epic", 12000);
        int RARE_PRICE = Utils.getPrice(config, "projectile_trails.rare", 10000);
        int UNCOMMON_PRICE = Utils.getPrice(config, "projectile_trails.uncommon", 8000);
        int COMMON_PRICE = Utils.getPrice(config, "projectile_trails.common", 5000);

        Cosmetic greenSparks = new Cosmetic("Green Sparks", new ItemStack(Material.EMERALD), COMMON_PRICE, CosmeticRarity.COMMON)
                .setLocationConsumer(location -> location.getWorld().spawnParticle(Particle.COMPOSTER, location.getX(), location.getY(), location.getZ(), 2, 0.1, 0.1, 0.1, 1));
        Cosmetic lava = new Cosmetic("Lava", new ItemStack(Material.LAVA_BUCKET), COMMON_PRICE, CosmeticRarity.COMMON)
                .setLocationConsumer(location -> location.getWorld().spawnParticle(Particle.LAVA, location.getX(), location.getY(), location.getZ(), 2, 0.1, 0.1, 0.1, 1));
        Cosmetic hearth = new Cosmetic("Hearth", new ItemStack(Material.REDSTONE), COMMON_PRICE, CosmeticRarity.COMMON)
                .setLocationConsumer(location -> location.getWorld().spawnParticle(Particle.HEART, location.getX(), location.getY(), location.getZ(), 2, 0.1, 0.1, 0.1, 1));
        Cosmetic slime = new Cosmetic("Slime", new ItemStack(Material.SLIME_BALL), UNCOMMON_PRICE, CosmeticRarity.UNCOMMON)
                .setLocationConsumer(location -> location.getWorld().spawnParticle(Particle.ITEM_SLIME, location.getX(), location.getY(), location.getZ(), 2, 0.1, 0.1, 0.1, 1));
        Cosmetic notes = new Cosmetic("Notes", new ItemStack(Material.JUKEBOX), COMMON_PRICE, CosmeticRarity.COMMON)
                .setLocationConsumer(location -> location.getWorld().spawnParticle(Particle.NOTE, location.getX(), location.getY(), location.getZ(), 2, 0.1, 0.1, 0.1, 1));
        Cosmetic glow = new Cosmetic("Glow", new ItemStack(Material.GLOW_SQUID_SPAWN_EGG), RARE_PRICE, CosmeticRarity.RARE)
                .setLocationConsumer(location -> location.getWorld().spawnParticle(Particle.GLOW, location.getX(), location.getY(), location.getZ(), 2, 0.1, 0.1, 0.1, 1));
        Cosmetic head = new Cosmetic("Skeleton Heads", new ItemStack(Material.SKELETON_SKULL), UNCOMMON_PRICE, CosmeticRarity.UNCOMMON)
                .setLocationConsumer(location -> {
                    ItemStack itemStack =  new ItemBuilder(Material.SKELETON_SKULL).setName(UUID.randomUUID().toString()).toItemStack();
                    ItemUtils.getInstance().markAsNoPickup(itemStack);

                    Item item = location.getWorld().dropItem(location, new ItemStack(Material.SKELETON_SKULL));
                    Bukkit.getScheduler().runTaskLater(GameAPI.getInstance(), task -> item.remove(), 10L);
                });
        Cosmetic pumpkins = new Cosmetic("Pumpkins", new ItemStack(Material.PUMPKIN), RARE_PRICE, CosmeticRarity.RARE)
                .setLocationConsumer(location -> {
                    ItemStack itemStack =  new ItemBuilder(Material.PUMPKIN).setName(UUID.randomUUID().toString()).toItemStack();
                    ItemUtils.getInstance().markAsNoPickup(itemStack);

                    Item item = location.getWorld().dropItem(location, new ItemStack(Material.PUMPKIN));
                    Bukkit.getScheduler().runTaskLater(GameAPI.getInstance(), task -> item.remove(), 10L);
                });
        Cosmetic presents = new Cosmetic("Presents", GameAPI.getInstance().getVersionSupport().getCustomHead("12919c67317c7678438ff520c98dde0e3b4d68769c8938a5a3de2968edfc7314"), EPIC_PRICE, CosmeticRarity.EPIC)
                .setLocationConsumer(location -> {
                    ItemStack itemStack =  new ItemBuilder(GameAPI.getInstance().getVersionSupport().getCustomHead("12919c67317c7678438ff520c98dde0e3b4d68769c8938a5a3de2968edfc7314")).setName(UUID.randomUUID().toString()).toItemStack();
                    ItemUtils.getInstance().markAsNoPickup(itemStack);

                    Item item = location.getWorld().dropItem(location, itemStack);
                    Bukkit.getScheduler().runTaskLater(GameAPI.getInstance(), task -> item.remove(), 10L);
                });
        Cosmetic usaFlag = new Cosmetic("USA Flag", GameAPI.getInstance().getVersionSupport().getCustomHead("46c9923bebd9ad90a80a0731c3f3b9db729b0785015e18e3ec07e4e91099be06"), EPIC_PRICE, CosmeticRarity.EPIC)
                .setLocationConsumer(location -> {
                    ItemStack itemStack =  new ItemBuilder(GameAPI.getInstance().getVersionSupport().getCustomHead("46c9923bebd9ad90a80a0731c3f3b9db729b0785015e18e3ec07e4e91099be06")).setName(UUID.randomUUID().toString()).toItemStack();
                    ItemUtils.getInstance().markAsNoPickup(itemStack);

                    Item item = location.getWorld().dropItem(location, itemStack);
                    Bukkit.getScheduler().runTaskLater(GameAPI.getInstance(), task -> item.remove(), 10L);
                });
        Cosmetic czechFlag = new Cosmetic("Czech Flag", GameAPI.getInstance().getVersionSupport().getCustomHead("48152b7334d7ecf335e47a4f35defbd2eb6957fc7bfe94212642d62f46e61e"), EPIC_PRICE, CosmeticRarity.EPIC)
                .setLocationConsumer(location -> {
                    ItemStack itemStack =  new ItemBuilder(GameAPI.getInstance().getVersionSupport().getCustomHead("48152b7334d7ecf335e47a4f35defbd2eb6957fc7bfe94212642d62f46e61e")).setName(UUID.randomUUID().toString()).toItemStack();
                    ItemUtils.getInstance().markAsNoPickup(itemStack);

                    Item item = location.getWorld().dropItem(location, itemStack);
                    Bukkit.getScheduler().runTaskLater(GameAPI.getInstance(), task -> item.remove(), 10L);
                });
        Cosmetic ukFlag = new Cosmetic("UK Flag", GameAPI.getInstance().getVersionSupport().getCustomHead("8831c73f5468e888c3019e2847e442dfaa88898d50ccf01fd2f914af544d5368"), EPIC_PRICE, CosmeticRarity.EPIC)
                .setLocationConsumer(location -> {
                    ItemStack itemStack =  new ItemBuilder(GameAPI.getInstance().getVersionSupport().getCustomHead("8831c73f5468e888c3019e2847e442dfaa88898d50ccf01fd2f914af544d5368")).setName(UUID.randomUUID().toString()).toItemStack();
                    ItemUtils.getInstance().markAsNoPickup(itemStack);

                    Item item = location.getWorld().dropItem(location, itemStack);
                    Bukkit.getScheduler().runTaskLater(GameAPI.getInstance(), task -> item.remove(), 10L);
                });
        Cosmetic roses = new Cosmetic("Roses", new ItemStack(Material.POPPY), COMMON_PRICE, CosmeticRarity.COMMON)
                .setLocationConsumer(location -> {
                    ItemStack itemStack =  new ItemBuilder(Material.POPPY).setName(UUID.randomUUID().toString()).toItemStack();
                    ItemUtils.getInstance().markAsNoPickup(itemStack);

                    Item item = location.getWorld().dropItem(location, new ItemStack(Material.POPPY));
                    Bukkit.getScheduler().runTaskLater(GameAPI.getInstance(), task -> item.remove(), 10L);
                });


        addCosmetic(notes, greenSparks, lava, hearth, slime, head, pumpkins, presents, usaFlag, czechFlag, ukFlag, roses, glow);

        setTriggers(getTriggers());
    }

    public Set<CTrigger<?>> getTriggers() {
        Set<CTrigger<?>> triggers = new HashSet<>();

        CTrigger<ProjectileLaunchEvent> trigger = new CTrigger<>(ProjectileLaunchEvent.class,
                projectileLaunchEvent -> PlayerManager.getGamePlayer(((Player) projectileLaunchEvent.getEntity().getShooter()).getPlayer()),
                e -> e.getEntity().getShooter() instanceof Player,
                projectileLaunchEvent -> {
                    Player player = (Player) projectileLaunchEvent.getEntity().getShooter();
                    Cosmetic cosmetic = PlayerManager.getGamePlayer(player).getPlayerData().getSelectedCosmetics().get(this);
                    if (cosmetic == null){
                        return;
                    }
                    if (!(projectileLaunchEvent.getEntity() instanceof Arrow || projectileLaunchEvent.getEntity() instanceof Trident || projectileLaunchEvent.getEntity() instanceof Snowball || projectileLaunchEvent.getEntity() instanceof Egg)){
                        return;
                    }

                    Bukkit.getScheduler().runTaskLater(GameAPI.getInstance(), task -> {
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (projectileLaunchEvent.getEntity().isOnGround() || projectileLaunchEvent.getEntity().isDead()) {
                                    this.cancel();
                                    return;
                                }

                                Location location = projectileLaunchEvent.getEntity().getLocation();
                                cosmetic.getLocationConsumer().accept(location);
                            }
                        }.runTaskTimer(GameAPI.getInstance(), 0L, 1L);
                    }, 2L);
                    //cosmetic.getLocationConsumer().accept(projectileLaunchEvent.getLocation());

                });

        triggers.add(trigger);
        return triggers;
    }
}
