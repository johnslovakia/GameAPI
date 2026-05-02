package cz.johnslovakia.gameapi.modules.game.session;

import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.game.GameInstance;
import cz.johnslovakia.gameapi.modules.kits.Kit;
import cz.johnslovakia.gameapi.modules.game.team.GameTeam;
import cz.johnslovakia.gameapi.modules.resources.Resource;
import cz.johnslovakia.gameapi.modules.resources.ResourceComparator;
import cz.johnslovakia.gameapi.modules.scores.ScoreGroup;
import cz.johnslovakia.gameapi.modules.scores.ScoreModule;
import cz.johnslovakia.gameapi.rewards.PlayerRewardRecord;
import cz.johnslovakia.gameapi.users.GamePlayerState;
import cz.johnslovakia.gameapi.users.PlayerIdentity;
import cz.johnslovakia.gameapi.modules.scores.Score;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

@Getter @Setter
public class PlayerGameSession {

    private final PlayerIdentity playerIdentity;
    private final GameInstance gameInstance;

    private final Map<Score, Integer> scores = new HashMap<>();
    private final Map<Score, List<PlayerRewardRecord>> earnedScoreRewards = new HashMap<>();

    private Kit selectedKit;
    private List<Kit> purchasedKitsThisGame = new ArrayList<>();
    private GameTeam team;
    private GamePlayerState state = GamePlayerState.UNKNOWN;

    private boolean enabledPVP = true;
    private boolean enabledMovement = true;
    private boolean limited = false;
    private boolean participatedAsPlayer = false;

    public PlayerGameSession(PlayerIdentity playerIdentity, GameInstance gameInstance) {
        this.playerIdentity = playerIdentity;
        this.gameInstance = gameInstance;
    }

    public void markAsParticipated() {
        this.participatedAsPlayer = true;
    }

    public boolean hasEarnedSomething(){
        return !earnedScoreRewards.isEmpty();
    }

    public int getScore(String scoreName){
        return ModuleManager.getModule(ScoreModule.class)
                .getScore(scoreName)
                .map(score -> scores.getOrDefault(score, 0))
                .orElse(0);
    }

    public Map<Resource, Integer> getEarnedRewards() {
        Map<Resource, Integer> total = new TreeMap<>(new ResourceComparator());

        earnedScoreRewards.values().stream()
                .flatMap(List::stream)
                .forEach(record -> {
                    record.earned().forEach((resource, amount) ->
                            total.merge(resource, amount, Integer::sum)
                    );
                });

        return total;
    }

    public Map<Score, Integer> getEarnedRewardsBySource(Resource resource) {
        Map<Score, Integer> breakdown = new HashMap<>();

        earnedScoreRewards.forEach((score, records) -> {
            int total = records.stream()
                    .mapToInt(record -> record.earned().getOrDefault(resource, 0))
                    .sum();

            if (total > 0) {
                breakdown.put(score, total);
            }
        });

        return breakdown;
    }

    public int getGroupScoreSum(ScoreGroup scoreGroup) {
        return earnedScoreRewards.keySet().stream()
                .filter(score -> scoreGroup.getKey().equals(score.getGroupKey()))
                .mapToInt(score -> getScore(score.getName()))
                .sum();
    }

    /*public int getGroupRewardedScoreCount(ScoreGroup scoreGroup) {
        return Math.toIntExact(earnedScoreRewards.keySet().stream()
                .filter(score -> scoreGroup.getKey().equals(score.getGroupKey()) && getScore(score.getName()) > 0)
                .count());
    }*/


    public void addPurchasedKitThisGame(Kit kit){
        if (purchasedKitsThisGame.contains(kit)) return;
        purchasedKitsThisGame.add(kit);
    }
}
