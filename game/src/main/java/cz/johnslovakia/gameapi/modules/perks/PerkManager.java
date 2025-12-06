package cz.johnslovakia.gameapi.modules.perks;

import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.database.Type;
import cz.johnslovakia.gameapi.modules.resources.Resource;
import cz.johnslovakia.gameapi.users.GamePlayer;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class PerkManager {

    private final String name;

    private final List<Perk> perks = new ArrayList<>();
    private final Resource resource;

    public PerkManager(String name, Resource resource){
        this.name = name;
        this.resource = resource;

        Minigame.getInstance().getMinigameTable().createNewColumn(Type.JSON, "Perks");
    }

    public void registerPerk(Perk... perks){
        for (Perk perk : perks) {
            if (!this.perks.contains(perk)) {
                this.perks.add(perk);
            }
        }
    }

    public Perk getPerk(String name){
        for (Perk perk : perks){
            if (perk.getName().equals(name)){
                return perk;
            }
        }
        return null;
    }

    public PerkLevel getNextPlayerPerkLevel(GamePlayer gamePlayer, Perk perk) {
        PerkLevel level = gamePlayer.getPlayerData().getPerkLevel(perk);

        if (level == null) return perk.getLevels().get(0);

        return level.level() < perk.getLevels().size() ? perk.getLevels().get(level.level()) : null;
    }
}