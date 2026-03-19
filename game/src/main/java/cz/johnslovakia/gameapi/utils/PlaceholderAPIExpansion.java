package cz.johnslovakia.gameapi.utils;

import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.game.GameInstance;
import cz.johnslovakia.gameapi.modules.resources.Resource;
import cz.johnslovakia.gameapi.modules.resources.ResourcesModule;
import cz.johnslovakia.gameapi.modules.scores.Score;
import cz.johnslovakia.gameapi.modules.scores.ScoreModule;
import cz.johnslovakia.gameapi.modules.stats.Stat;
import cz.johnslovakia.gameapi.modules.stats.StatsModule;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;

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
        GameInstance game = gamePlayer.getGame();

        StatsModule statsModule = ModuleManager.getModule(StatsModule.class);
        for (Stat stat : statsModule.getStats()) {
            String statKey = stat.getName().replace(" ", "").toLowerCase() + "_stat";
            if (identifier.equalsIgnoreCase(statKey)) {
                return String.valueOf(statsModule.getPlayerStat(gamePlayer, stat.getName()));
            }

            if (identifier.toLowerCase().contains("rankings")) {
                List<Map.Entry<String, Integer>> topList = statsModule.getStatsTable()
                        .topStats(stat.getName(), 15)
                        .entrySet()
                        .stream()
                        .sorted((o1, o2) -> -Integer.compare(o1.getValue(), o2.getValue()))
                        .limit(15)
                        .toList();

                for (int i = 0; i < topList.size(); i++) {
                    String topKey = stat.getName().replace(" ", "") + "_top" + (i + 1);
                    if (identifier.equalsIgnoreCase(topKey)) {
                        return topList.get(i).getKey();
                    }
                }
            }
        }

        ResourcesModule resourcesModule = ModuleManager.getModule(ResourcesModule.class);
        for (Resource resource : resourcesModule.getResources()){
            if (identifier.equalsIgnoreCase("balance_" + resource.getName())){
                return "" + resourcesModule.getPlayerBalanceCached(player, resource);
            }
        }

        if (minigame.getSettings().isUseTeams()){
            if (identifier.equalsIgnoreCase("teamcolor")){
                return "" + gamePlayer.getGameSession().getTeam().getChatColor();
            }
        }

        if (game != null){
            if (identifier.contains("gamerankings")) {
                String rankingScore = "kills";
                for (Score score : ModuleManager.getModule(ScoreModule.class).getScores().values()) {
                    if (score.isScoreRanking()){
                        rankingScore = score.getName();
                        break;
                    }
                }
                Map<GamePlayer, Integer> ranking = GameUtils.getTopPlayers(game, rankingScore, 3, 3);

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
