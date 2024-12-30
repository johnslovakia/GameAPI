package cz.johnslovakia.gameapi.game.cosmetics.defaultCosmetics;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.events.GamePlayerDeathEvent;
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

public class KillSoundsCategory implements CosmeticsCategory {
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
        return "Kill Sounds";
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

        CTrigger<GamePlayerDeathEvent> trigger = new CTrigger<>(GamePlayerDeathEvent.class,
                GamePlayerDeathEvent::getKiller,
                event -> event.getKiller() != null,
                event -> {
                    Cosmetic cosmetic = event.getKiller().getPlayerData().getSelectedCosmetics().get(this);
                    if (cosmetic == null){
                        return;
                    }

                    cosmetic.getGamePlayerConsumer().accept(event.getKiller());

                });

        triggers.add(trigger);
        return triggers;
    }
}
