package cz.johnslovakia.gameapi.utils;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerManager;
import cz.johnslovakia.gameapi.users.resources.Resource;
import cz.johnslovakia.gameapi.users.stats.Stat;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

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

            if (identifier.contains("rankings")){
                List<Map.Entry<String, Integer>> topMap = GameAPI.getInstance().getStatsManager().getStatsTable().topStats(stat.getName(), 15).entrySet()
                        .stream()
                        .sorted((o1, o2) -> -Integer.compare(o1.getValue(), o2.getValue()))
                        .limit(15)
                        .toList();

                if (identifier.equalsIgnoreCase("rankings." + stat.getName() + ".top1")){
                    return topMap.get(0).getKey();
                }else if (identifier.equalsIgnoreCase("rankings." + stat.getName() + ".top2")){
                    return topMap.get(1).getKey();
                }else if (identifier.equalsIgnoreCase("rankings." + stat.getName() + ".top3")){
                    return topMap.get(2).getKey();
                }else if (identifier.equalsIgnoreCase("rankings." + stat.getName() + ".top4")){
                    return topMap.get(3).getKey();
                }else if (identifier.equalsIgnoreCase("rankings." + stat.getName() + ".top5")){
                    return topMap.get(4).getKey();
                }else if (identifier.equalsIgnoreCase("rankings." + stat.getName() + ".top6")){
                    return topMap.get(5).getKey();
                }else if (identifier.equalsIgnoreCase("rankings." + stat.getName() + ".top7")){
                    return topMap.get(6).getKey();
                }else if (identifier.equalsIgnoreCase("rankings." + stat.getName() + ".top8")){
                    return topMap.get(7).getKey();
                }else if (identifier.equalsIgnoreCase("rankings." + stat.getName() + ".top9")){
                    return topMap.get(8).getKey();
                }else if (identifier.equalsIgnoreCase("rankings." + stat.getName() + ".top10")){
                    return topMap.get(9).getKey();
                }else if (identifier.equalsIgnoreCase("rankings." + stat.getName() + ".top11")){
                    return topMap.get(10).getKey();
                }else if (identifier.equalsIgnoreCase("rankings." + stat.getName() + ".top12")){
                    return topMap.get(11).getKey();
                }else if (identifier.equalsIgnoreCase("rankings." + stat.getName() + ".top13")){
                    return topMap.get(12).getKey();
                }else if (identifier.equalsIgnoreCase("rankings." + stat.getName() + ".top14")){
                    return topMap.get(13).getKey();
                }else if (identifier.equalsIgnoreCase("rankings." + stat.getName() + ".top15")){
                    return topMap.get(14).getKey();
                }
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
