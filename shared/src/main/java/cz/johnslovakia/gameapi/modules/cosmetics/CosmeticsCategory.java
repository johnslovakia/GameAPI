package cz.johnslovakia.gameapi.modules.cosmetics;

import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.users.PlayerIdentityRegistry;
import cz.johnslovakia.gameapi.utils.Logger;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Getter
public class CosmeticsCategory {

    private final String name;
    private final ItemStack icon;
    @Setter
    private CosmeticsModule cosmeticsModule;
    @Setter
    private Set<CTrigger<?>> triggers;

    private final List<Cosmetic> cosmetics = new ArrayList<>();

    public CosmeticsCategory(String name, ItemStack icon) {
        this.name = name;
        this.icon = icon;
    }

    public void addCosmetic(Cosmetic... cosmetics) {
        for (Cosmetic cosmetic : cosmetics) {
            if (this.cosmetics.contains(cosmetic)) {
                Logger.log("Cosmetic " + cosmetic.getName() + " is already added!", Logger.LogType.WARNING);
                return;
            }
            this.cosmetics.add(cosmetic);
            cosmetic.setCategory(this);
        }
    }

    public Cosmetic getCosmeticByName(String name) {
        for (Cosmetic cosmetic : getCosmetics()) {
            if (cosmetic.getName().equalsIgnoreCase(name)) {
                return cosmetic;
            }
        }
        return null;
    }

    public Cosmetic getSelectedCosmetic(Player player){
        return ModuleManager.getModule(CosmeticsModule.class).getPlayerSelectedCosmetic(PlayerIdentityRegistry.get(player), this);
    }

}
