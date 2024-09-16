package cz.johnslovakia.gameapi.game.cosmetics.defaultCosmetics.trails;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.game.cosmetics.CTrigger;
import cz.johnslovakia.gameapi.game.cosmetics.Cosmetic;
import cz.johnslovakia.gameapi.game.cosmetics.CosmeticsCategory;
import cz.johnslovakia.gameapi.game.cosmetics.CosmeticsManager;
import cz.johnslovakia.gameapi.game.cosmetics.defaultCosmetics.trails.cosmetics.GreenSparks;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerManager;
import cz.johnslovakia.gameapi.utils.eTrigger.Trigger;
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
        List<Cosmetic> cosmetics = new ArrayList<>();

        cosmetics.add(new GreenSparks());

        return cosmetics;
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
                    new BukkitRunnable(){
                        @Override
                        public void run() {
                            if (projectileLaunchEvent.getEntity().isOnGround() || projectileLaunchEvent.getEntity().isDead())
                                return;

                            Location location = projectileLaunchEvent.getEntity().getLocation();
                            cosmetic.execute(location);
                        }
                    }.runTaskTimer(GameAPI.getInstance(), 0L, 2L);
                    cosmetic.execute(projectileLaunchEvent.getLocation());

                }); //TODO: někde zavolat onEventCall..

        triggers.add(trigger);
        return triggers;
    }
}
