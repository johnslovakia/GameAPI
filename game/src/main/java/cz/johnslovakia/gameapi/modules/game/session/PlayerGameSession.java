package cz.johnslovakia.gameapi.modules.game.session;

import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.game.GameInstance;
import cz.johnslovakia.gameapi.modules.kits.Kit;
import cz.johnslovakia.gameapi.modules.game.team.GameTeam;
import cz.johnslovakia.gameapi.modules.resources.Resource;
import cz.johnslovakia.gameapi.modules.scores.ScoreModule;
import cz.johnslovakia.gameapi.rewards.PlayerRewardRecord;
import cz.johnslovakia.gameapi.users.GamePlayerState;
import cz.johnslovakia.gameapi.users.PlayerIdentity;
import cz.johnslovakia.gameapi.modules.scores.Score;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter @Setter
public class PlayerGameSession {

    private final PlayerIdentity playerIdentity;
    private final GameInstance gameInstance;
    //private final Instant startTime;
    //private Instant endTime;

    private final Map<Score, Integer> scores = new HashMap<>();
    private final Map<Score, List<PlayerRewardRecord>> earnedScoreRewards = new HashMap<>();

    private Kit selectedKit;
    private GameTeam team;
    private GamePlayerState state;

    private boolean enabledPVP = true;
    private boolean enabledMovement = true;
    private boolean limited = false;

    public PlayerGameSession(PlayerIdentity playerIdentity, GameInstance gameInstance) {
        this.playerIdentity = playerIdentity;
        this.gameInstance = gameInstance;
        //this.startTime = Instant.now();
    }

    public int getScore(String scoreName){
        return ModuleManager.getModule(ScoreModule.class)
                .getScore(scoreName)
                .map(score -> scores.getOrDefault(score, 0))
                .orElse(0);
    }

    public boolean earnedSomething(){
        return !earnedScoreRewards.isEmpty();
    }

    public Map<Resource, Integer> getTotalEarned() {
        Map<Resource, Integer> total = new HashMap<>();

        earnedScoreRewards.values().stream()
                .flatMap(List::stream)
                .forEach(record -> {
                    record.earned().forEach((resource, amount) ->
                            total.merge(resource, amount, Integer::sum)
                    );
                });

        return total;
    }

    public Map<Score, Integer> getEarnedBySource(Resource resource) {
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

    public int getRewardCount(Score score) {
        List<PlayerRewardRecord> records = earnedScoreRewards.get(score);
        return records != null ? records.size() : 0;
    }



    /*public Instant getEndTime() {
        this.endTime = Instant.now();
        return endTime;
    }*/
}
