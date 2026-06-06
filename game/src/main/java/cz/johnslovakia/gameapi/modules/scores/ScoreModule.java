package cz.johnslovakia.gameapi.modules.scores;

import cz.johnslovakia.gameapi.events.PlayerScoreEvent;
import cz.johnslovakia.gameapi.modules.Module;
import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.game.session.GameSessionModule;
import cz.johnslovakia.gameapi.modules.game.session.PlayerGameSession;
import cz.johnslovakia.gameapi.modules.stats.Stat;
import cz.johnslovakia.gameapi.modules.stats.StatsModule;
import cz.johnslovakia.gameapi.rewards.PlayerRewardRecord;
import cz.johnslovakia.gameapi.rewards.Reward;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerIdentity;
import cz.johnslovakia.gameapi.users.ScoreAction;
import lombok.Getter;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Getter
public class ScoreModule implements Module {

    private Map<String, Score> scores = new HashMap<>();
    private Map<String, ScoreGroup> scoreGroups = new HashMap<>();

    @Override
    public void initialize() {

    }

    @Override
    public void terminate() {
        scores = null;
        scoreGroups = null;
    }

    public void registerScoreGroup(ScoreGroup group) {
        scoreGroups.put(group.getKey(), group);
    }

    public Optional<ScoreGroup> getScoreGroup(String key) {
        return Optional.ofNullable(scoreGroups.get(key));
    }

    public void registerScore(Score score){
        scores.put(score.getName(), score);
    }

    public int getPlayerScore(PlayerIdentity playerIdentity, String name){
        return ((GamePlayer) playerIdentity).getGameSession().getScore(name);
    }

    public int getPlayerScore(PlayerIdentity playerIdentity, Score score){
        return ((GamePlayer) playerIdentity).getGameSession().getScore(score.getName());
    }

    public Optional<Score> getScore(String name){
        return Optional.ofNullable(scores.get(name));
    }
}
