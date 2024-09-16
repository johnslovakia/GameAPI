package cz.johnslovakia.gameapi.game.cosmetics.defaultCosmetics.trails.cosmetics;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.game.cosmetics.Cosmetic;
import cz.johnslovakia.gameapi.game.cosmetics.CosmeticRarity;
import cz.johnslovakia.gameapi.game.cosmetics.CosmeticsCategory;
import cz.johnslovakia.gameapi.game.cosmetics.defaultCosmetics.trails.TrailsCategory;
import cz.johnslovakia.gameapi.users.GamePlayer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.inventory.ItemStack;

public class GreenSparks implements Cosmetic {
    @Override
    public String getName() {
        return "Green Sparks";
    }

    @Override
    public ItemStack getIcon() {
        return new ItemStack(Material.EMERALD);
    }

    @Override
    public int getPrice() {
        return 2500;
    }

    @Override
    public CosmeticRarity getRarity() {
        return CosmeticRarity.COMMON;
    }

    @Override
    public void execute(Location location) {
        location.getWorld().spawnParticle(Particle.COMPOSTER, location.getX(), location.getY(), location.getZ(), 2, 0.1, 0.1, 0.1, 1);
    }
}
