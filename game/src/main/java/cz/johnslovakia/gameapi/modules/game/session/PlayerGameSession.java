package cz.johnslovakia.gameapi.modules.game.session;

import cz.johnslovakia.gameapi.events.PlayerScoreEvent;
import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.game.GameInstance;
import cz.johnslovakia.gameapi.modules.kits.Kit;
import cz.johnslovakia.gameapi.modules.game.team.GameTeam;
import cz.johnslovakia.gameapi.modules.resources.Resource;
import cz.johnslovakia.gameapi.modules.resources.ResourceComparator;
import cz.johnslovakia.gameapi.modules.scores.ScoreGroup;
import cz.johnslovakia.gameapi.modules.scores.ScoreModule;
import cz.johnslovakia.gameapi.modules.stats.Stat;
import cz.johnslovakia.gameapi.modules.stats.StatsModule;
import cz.johnslovakia.gameapi.rewards.PlayerRewardRecord;
import cz.johnslovakia.gameapi.rewards.Reward;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.GamePlayerState;
import cz.johnslovakia.gameapi.users.PlayerIdentity;
import cz.johnslovakia.gameapi.modules.scores.Score;
import cz.johnslovakia.gameapi.users.ScoreAction;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;

import java.util.*;

@Getter @Setter
public class PlayerGameSession {

    private final GamePlayer gamePlayer;
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

    public PlayerGameSession(GamePlayer gamePlayer, GameInstance gameInstance) {
        this.gamePlayer = gamePlayer;
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

    public void incrementScore(String scoreName){
        incrementScore(scoreName, 1);
    }

    public void incrementScore(String scoreName, int amount){
        if (amount <= 0) return;
        ScoreAction action = amount == 1 ? ScoreAction.INCREASE : ScoreAction.ADD;
        setScore(scoreName, getScore(scoreName) + amount, action);
    }

    public void decrementScore(String scoreName){
        decrementScore(scoreName, 1);
    }

    public void decrementScore(String scoreName, int amount){
        if (amount <= 0) return;
        setScore(scoreName, getScore(scoreName) - amount, ScoreAction.DECREASE);
    }

    public void setScore(String scoreName, int value){
        setScore(scoreName, value, null);
    }

    private void setScore(String scoreName, int value, ScoreAction action){
        ScoreModule scoreModule = ModuleManager.getModule(ScoreModule.class);
        scoreModule.getScore(scoreName).ifPresent(score -> {
            if (gameInstance == null) return;
            int previousScore = getScore(scoreName);
            int currentScore = Math.max(0, value);
            int difference = currentScore - previousScore;
            if (difference == 0) return;
            ScoreAction eventAction = action != null ? action : getScoreAction(currentScore, difference);

            if (currentScore == 0) {
                scores.remove(score);
            } else {
                scores.put(score, currentScore);
            }

            Stat linkedStat = score.getLinkedStat();
            if (linkedStat != null) {
                ModuleManager.getModule(StatsModule.class).increasePlayerStat(gamePlayer, linkedStat, difference);
            }

            Reward reward = score.getReward();
            if (difference > 0 && reward != null &&
                    (score.getRewardLimit() == 0 || currentScore < score.getRewardLimit())
                    && currentScore % score.getRewardFrequency() == 0) {

                PlayerRewardRecord record = reward.applyReward(gamePlayer, score.isAllowedMessage());
                earnedScoreRewards.computeIfAbsent(score, s -> new ArrayList<>()).add(record);
            }

            PlayerScoreEvent event = new PlayerScoreEvent(gamePlayer, score, eventAction);
            Bukkit.getPluginManager().callEvent(event);
        });
    }

    private ScoreAction getScoreAction(int currentScore, int difference) {
        if (currentScore == 0) return ScoreAction.RESET;
        return difference > 0 ? ScoreAction.ADD : ScoreAction.DECREASE;
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
