package cz.johnslovakia.gameapi.game.cosmetics;

import cz.johnslovakia.gameapi.utils.eTrigger.Trigger;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Set;

public interface CosmeticsCategory {

    CosmeticsManager getManager();
    String getName();
    ItemStack getIcon();
    List<Cosmetic> getCosmetics();
    Set<CTrigger<?>> getTriggers();
}
