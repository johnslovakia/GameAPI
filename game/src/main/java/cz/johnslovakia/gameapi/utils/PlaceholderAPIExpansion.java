package cz.johnslovakia.gameapi.utils;

import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.game.GameInstance;
import cz.johnslovakia.gameapi.modules.resources.Resource;
import cz.johnslovakia.gameapi.modules.resources.ResourcesModule;
import cz.johnslovakia.gameapi.modules.scores.Score;
import cz.johnslovakia.gameapi.modules.scores.ScoreModule;
import cz.johnslovakia.gameapi.modules.stats.Stat;
import cz.johnslovakia.gameapi.modules.stats.StatPeriod;
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
        return "1.2.1";
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
        if (gamePlayer == null) {
            return "";
        }

        String id = identifier.toLowerCase();

        String result = handleStats(gamePlayer, id);
        if (result != null) return result;

        result = handleResources(player, id);
        if (result != null) return result;

        result = handleTeamColor(gamePlayer, id);
        if (result != null) return result;

        result = handleGameRankings(gamePlayer, id);
        if (result != null) return result;

        return null;
    }


    private String handleStats(GamePlayer gamePlayer, String id) {
        StatsModule statsModule = ModuleManager.getModule(StatsModule.class);
        if (statsModule == null) return null;

        for (Stat stat : statsModule.getStats()) {
            String normalizedName = stat.getName().replace(" ", "").toLowerCase();

            if (id.equals(normalizedName + "_stat")) {
                return String.valueOf(statsModule.getPlayerStat(gamePlayer, stat.getName()));
            }

            if (id.equals(normalizedName + "_rank")) {
                int rank = statsModule.getStatsTable().getPlayerRank(gamePlayer.getName(), stat.getName());
                return rank > 0 ? String.valueOf(rank) : "-";
            }

            String topPrefix = normalizedName + "_top";
            if (id.startsWith(topPrefix)) {
                String suffix = id.substring(topPrefix.length());

                TopRequest request = parseTopRequest(suffix);
                if (request == null) return null;

                int rank = request.rank;
                if (rank < 0 || rank >= 15) return null;

                List<Map.Entry<String, Integer>> topList = statsModule.getStatsTable()
                        .topStats(stat.getName(), 15, StatPeriod.LIFETIME)
                        .entrySet().stream()
                        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                        .limit(15)
                        .toList();

                if (rank >= topList.size()) {
                    return "-";
                }

                return switch (request.type) {
                    case NAME -> topList.get(rank).getKey();
                    case VALUE -> String.valueOf(topList.get(rank).getValue());
                };
            }
        }
        return null;
    }

    private String handleResources(Player player, String id) {
        ResourcesModule resourcesModule = ModuleManager.getModule(ResourcesModule.class);
        if (resourcesModule == null) return null;

        for (Resource resource : resourcesModule.getResources()) {
            if (id.equals("balance_" + resource.getName().toLowerCase())) {
                return String.valueOf(resourcesModule.getPlayerBalanceCached(player, resource));
            }
        }
        return null;
    }

    private String handleTeamColor(GamePlayer gamePlayer, String id) {
        if (!minigame.getSettings().isUseTeams()) return null;
        if (!id.equals("teamcolor")) return null;

        if (gamePlayer.getGameSession() == null) return "";
        if (gamePlayer.getGameSession().getTeam() == null) return "";

        return String.valueOf(gamePlayer.getGameSession().getTeam().getChatColor());
    }

    private String handleGameRankings(GamePlayer gamePlayer, String id) {
        GameInstance game = gamePlayer.getGame();
        if (game == null) return null;
        if (!id.startsWith("gamerankings_top")) return null;

        int rank = parseRank(id, "gamerankings_top");
        if (rank < 0 || rank >= 3) return null;

        String rankingScore = findRankingScoreName();

        Map<GamePlayer, Integer> ranking = GameUtils.getTopPlayers(game, rankingScore, 3, 3);

        List<Map.Entry<GamePlayer, Integer>> sorted = ranking.entrySet().stream()
                .sorted(Map.Entry.<GamePlayer, Integer>comparingByValue().reversed())
                .toList();

        if (rank < sorted.size()) {
            Player onlinePlayer = sorted.get(rank).getKey().getOnlinePlayer();
            return onlinePlayer != null ? onlinePlayer.getName() : "-";
        }
        return rank == 0 ? "" : "-";
    }

    private String findRankingScoreName() {
        ScoreModule scoreModule = ModuleManager.getModule(ScoreModule.class);
        if (scoreModule == null) return "kills";

        for (Score score : scoreModule.getScores().values()) {
            if (score.isScoreRanking()) {
                return score.getName();
            }
        }
        return "kills";
    }

    private int parseRank(String id, String prefix) {
        try {
            int rank = Integer.parseInt(id.substring(prefix.length()));
            return rank - 1;
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            return -1;
        }
    }

    private TopRequest parseTopRequest(String suffix) {
        TopType type;
        String rankPart;

        if (suffix.endsWith("_name")) {
            type = TopType.NAME;
            rankPart = suffix.substring(0, suffix.length() - "_name".length());
        } else if (suffix.endsWith("_value")) {
            type = TopType.VALUE;
            rankPart = suffix.substring(0, suffix.length() - "_value".length());
        } else {
            type = TopType.NAME;
            rankPart = suffix;
        }

        try {
            int rank = Integer.parseInt(rankPart) - 1;
            return new TopRequest(rank, type);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private enum TopType {
        NAME, VALUE
    }

    private record TopRequest(int rank, TopType type) {}
}