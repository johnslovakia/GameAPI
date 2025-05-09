package cz.johnslovakia.gameapi.utils;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.game.Game;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerManager;
import cz.johnslovakia.gameapi.users.PlayerScore;
import cz.johnslovakia.gameapi.users.resources.Resource;
import cz.johnslovakia.gameapi.users.stats.Stat;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Objects;

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
        Game game = gamePlayer.getPlayerData().getGame();

        for (Stat stat : GameAPI.getInstance().getStatsManager().getStats()){
            if(identifier.equalsIgnoreCase("stat_" + stat.getName().replace(" ", "_").toLowerCase())) {
                return "" + stat.getPlayerStat(gamePlayer).getStatScore();
            }

            if (identifier.contains("rankings")){
                List<Map.Entry<String, Integer>> topMap = GameAPI.getInstance().getStatsManager().getStatsTable().topStats(stat.getName(), 15).entrySet()
                        .stream()
                        .sorted((o1, o2) -> -Integer.compare(o1.getValue(), o2.getValue()))
                        .limit(15)
                        .toList();

                if (identifier.equalsIgnoreCase("rankings_" + stat.getName() + "_top1")){
                    return topMap.get(0).getKey();
                }else if (identifier.equalsIgnoreCase("_" + stat.getName() + "_top2")){
                    return topMap.get(1).getKey();
                }else if (identifier.equalsIgnoreCase("_" + stat.getName() + "_top3")){
                    return topMap.get(2).getKey();
                }else if (identifier.equalsIgnoreCase("_" + stat.getName() + "_top4")){
                    return topMap.get(3).getKey();
                }else if (identifier.equalsIgnoreCase("_" + stat.getName() + "_top5")){
                    return topMap.get(4).getKey();
                }else if (identifier.equalsIgnoreCase("_" + stat.getName() + "_top6")){
                    return topMap.get(5).getKey();
                }else if (identifier.equalsIgnoreCase("_" + stat.getName() + "_top7")){
                    return topMap.get(6).getKey();
                }else if (identifier.equalsIgnoreCase("_" + stat.getName() + "_top8")){
                    return topMap.get(7).getKey();
                }else if (identifier.equalsIgnoreCase("_" + stat.getName() + "_top9")){
                    return topMap.get(8).getKey();
                }else if (identifier.equalsIgnoreCase("_" + stat.getName() + "_top10")){
                    return topMap.get(9).getKey();
                }else if (identifier.equalsIgnoreCase("_" + stat.getName() + "_top11")){
                    return topMap.get(10).getKey();
                }else if (identifier.equalsIgnoreCase("_" + stat.getName() + "_top12")){
                    return topMap.get(11).getKey();
                }else if (identifier.equalsIgnoreCase("_" + stat.getName() + "_top13")){
                    return topMap.get(12).getKey();
                }else if (identifier.equalsIgnoreCase("_" + stat.getName() + "_top14")){
                    return topMap.get(13).getKey();
                }else if (identifier.equalsIgnoreCase("_" + stat.getName() + "_top15")){
                    return topMap.get(14).getKey();
                }
            }
        }

        for (Resource resource : minigame.getEconomies()){
            if (identifier.equalsIgnoreCase("balance_" + resource.getName())){
                return "" + gamePlayer.getPlayerData().getBalance(resource);
            }
        }

        if (minigame.getSettings().isUseTeams()){
            if (identifier.equalsIgnoreCase("teamcolor")){
                return "" + gamePlayer.getPlayerData().getTeam().getChatColor();
            }
        }

        if (game != null){
            if (identifier.contains("gamerankings")) {
                String rankingScore = "kills";
                for (PlayerManager.Score score : PlayerManager.getScores()) {
                    if (score.isScoreRanking()){
                        rankingScore = score.getName();
                        break;
                    }
                }
                Map<GamePlayer, Integer> ranking = Utils.getTopPlayers(game, rankingScore, 3);

                if (identifier.equalsIgnoreCase("gamerankings_top1")) {
                    return ranking.entrySet().stream()
                            .sorted(Map.Entry.comparingByValue())
                            .findFirst()
                            .map(entry -> entry.getKey().getOnlinePlayer().getName())
                            .orElse("");
                }else if (identifier.equalsIgnoreCase("gamerankings_top2")) {
                    return ranking.entrySet().stream()
                            .sorted(Map.Entry.comparingByValue())
                            .skip(1)
                            .findFirst()
                            .map(entry -> entry.getKey().getOnlinePlayer().getName())
                            .orElse("-");
                }else if (identifier.equalsIgnoreCase("gamerankings_top3")) {
                    return ranking.entrySet().stream()
                            .sorted(Map.Entry.comparingByValue())
                            .skip(2)
                            .findFirst()
                            .map(entry -> entry.getKey().getOnlinePlayer().getName())
                            .orElse("-");
                }
            }
        }

        return null;
    }
}
