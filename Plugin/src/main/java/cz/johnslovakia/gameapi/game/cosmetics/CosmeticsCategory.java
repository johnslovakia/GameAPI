package cz.johnslovakia.gameapi.game.cosmetics;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.users.GamePlayer;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Getter
public class CosmeticsCategory {

    private final String name;
    private final ItemStack icon;
    @Setter
    private CosmeticsManager manager;
    @Setter
    private Set<CTrigger<?>> triggers;

    private List<Cosmetic> cosmetics = new ArrayList<>();

    public CosmeticsCategory(String name, ItemStack icon) {
        this.name = name;
        this.icon = icon;
    }

    public void addCosmetic(Cosmetic... cosmetics) {
        for (Cosmetic cosmetic : cosmetics) {
            if (this.cosmetics.contains(cosmetic)) {
                GameAPI.getInstance().getLogger().warning("Cosmetic " + cosmetic.getName() + " is already added!");
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

    public boolean hasPurchased(GamePlayer gamePlayer, Cosmetic cosmetic){
        return cosmetic.hasPurchased(gamePlayer);
    }

    public boolean hasSelected(GamePlayer gamePlayer, Cosmetic cosmetic){
        return cosmetic.hasSelected(gamePlayer);
    }

    public boolean hasPlayer(GamePlayer gamePlayer, Cosmetic cosmetic){
        return hasPurchased(gamePlayer, cosmetic)
                || hasSelected(gamePlayer, cosmetic)
                || gamePlayer.getOnlinePlayer().hasPermission("cosmetics.free");
    }

    public Cosmetic getSelectedCosmetic(GamePlayer gamePlayer){
        return gamePlayer.getPlayerData().getSelectedCosmetics().get(this);
    }

}
