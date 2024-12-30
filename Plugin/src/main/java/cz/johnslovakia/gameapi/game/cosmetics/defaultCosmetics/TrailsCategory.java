package cz.johnslovakia.gameapi.game.cosmetics.defaultCosmetics;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.game.cosmetics.*;
import cz.johnslovakia.gameapi.users.PlayerManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TrailsCategory implements CosmeticsCategory {
    private static final List<Cosmetic> predefinedCosmetics;

    static {
        predefinedCosmetics = new ArrayList<>();
        
        Cosmetic greenSparks = new Cosmetic("Green Sparks", new ItemStack(Material.EMERALD), 2500, CosmeticRarity.COMMON)
                .setLocationConsumer(location -> location.getWorld().spawnParticle(Particle.COMPOSTER, location.getX(), location.getY(), location.getZ(), 2, 0.1, 0.1, 0.1, 1));
        Cosmetic lava = new Cosmetic("Lava", new ItemStack(Material.LAVA_BUCKET), 2500, CosmeticRarity.COMMON)
                .setLocationConsumer(location -> location.getWorld().spawnParticle(Particle.LAVA, location.getX(), location.getY(), location.getZ(), 2, 0.1, 0.1, 0.1, 1));

        predefinedCosmetics.add(greenSparks);
        predefinedCosmetics.add(lava);
    }
    
    
    @Override
    public CosmeticsManager getManager() {
        return GameAPI.getInstance().getCosmeticsManager();
    }

    @Override
    public String getName() {
        return "Trails";
    }

    @Override
    public ItemStack getIcon() {
        return new ItemStack(Material.ARROW);
    }

    @Override
    public List<Cosmetic> getCosmetics() {
        return predefinedCosmetics;
    }

    @Override
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

                    new BukkitRunnable(){
                        @Override
                        public void run() {
                            if (projectileLaunchEvent.getEntity().isOnGround() || projectileLaunchEvent.getEntity().isDead())
                                return;

                            Location location = projectileLaunchEvent.getEntity().getLocation();
                            cosmetic.getLocationConsumer().accept(location);
                        }
                    }.runTaskTimer(GameAPI.getInstance(), 0L, 2L);
                    cosmetic.getLocationConsumer().accept(projectileLaunchEvent.getLocation());

                }); //TODO: někde zavolat onEventCall..

        triggers.add(trigger);
        return triggers;
    }
}
