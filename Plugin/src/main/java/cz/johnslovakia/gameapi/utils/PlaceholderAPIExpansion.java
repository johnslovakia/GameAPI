package cz.johnslovakia.gameapi.utils;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerManager;
import cz.johnslovakia.gameapi.users.resources.Resource;
import cz.johnslovakia.gameapi.users.stats.Stat;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;

public class PlaceholderAPIExpansion extends PlaceholderExpansion {

    private final Minigame minigame;

    public PlaceholderAPIExpansion(Minigame minigame) {
        this.minigame = minigame;
    }


    @Override
    public String getAuthor() {
        return "Hunzek_";
    }

    @Override
    public String getIdentifier() {
        return minigame.getName().toLowerCase();
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) {
            return "";
        }
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);

        for (Stat stat : GameAPI.getInstance().getStatsManager().getStats()){
            if(identifier.equalsIgnoreCase("stat." + stat.getName().replace(" ", "_").toLowerCase())) {
                return "" + stat.getPlayerStat(gamePlayer).getStatScore();
            }
        }

        for (Resource resource : minigame.getEconomies()){
            if (identifier.equalsIgnoreCase("balance." + resource.getName())){
                return "" + gamePlayer.getPlayerData().getBalance(resource);
            }
        }

        return null;
    }
}
