package cz.johnslovakia.gameapi.users;

import cz.johnslovakia.gameapi.economy.Economy;
import cz.johnslovakia.gameapi.users.stats.Stat;
import cz.johnslovakia.gameapi.utils.eTrigger.Trigger;
import lombok.Getter;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.*;

public class PlayerManager {

    public static HashMap<UUID, GamePlayer> playerMap = new HashMap<>();
    @Getter
    public static HashMap<Score, List<PlayerScore>> playerScores = new HashMap<>();

    public static void removeGamePlayer(Player player){
        UUID uuid = player.getUniqueId();

        if (playerMap.containsKey(uuid)){
            playerMap.remove(uuid);
        }
    }

    public static boolean exists(UUID uuid){
        return playerMap.containsKey(uuid);
    }


    public static GamePlayer getGamePlayer(OfflinePlayer player){
        GamePlayer pl = null;
        if(playerMap.containsKey(player.getUniqueId())){
            pl = playerMap.get(player.getUniqueId());
            boolean isAdded = false;
            for (List<PlayerScore> ts : playerScores.values()) {
                for (PlayerScore ts2 : ts) {
                    if (ts2.getGamePlayer().equals(pl)) {
                        isAdded = true;
                        break;
                    }
                }
            }
            if (!isAdded) {
                for (Score key : playerScores.keySet()) {
                    playerScores.get(key).add(new PlayerScore(key.getName(), pl, key.getRewardTypes()).setDisplayName(key.displayName).setAllowedMessage(key.isMessage()).setScoreRanking(key.scoreRanking).setStat(key.getStat()).setTriggers(key.getTriggers()));
                }
            }
        }else{
            GamePlayer gpl = new GamePlayer(player);
            boolean isAdded = false;
            for (List<PlayerScore> ts : playerScores.values()) {
                for (PlayerScore ts2 : ts) {
                    if (ts2.getGamePlayer().equals(gpl)) {
                        isAdded = true;
                        break;
                    }
                }
            }
            if (!isAdded) {
                for (Score key : playerScores.keySet()) {
                    playerScores.get(key).add(new PlayerScore(key.getName(), gpl, key.getRewardTypes()).setDisplayName(key.displayName).setAllowedMessage(key.isMessage()).setScoreRanking(key.scoreRanking).setStat(key.getStat()).setTriggers(key.getTriggers()));
                }
            }
            playerMap.put(player.getUniqueId(), gpl);
            pl = gpl;
        }
        return pl;
    }

    public static List<PlayerScore> getScoresByName(String scoreName) {
        if (playerScores.containsKey(scoreName)) return  playerScores.get(scoreName);
        return null;
    }

    public static List<PlayerScore> getScoresByPlayer(GamePlayer gp) {
        List<PlayerScore> pls = new ArrayList<>();
        for (List<PlayerScore> psl : playerScores.values()) {
            for (PlayerScore ps : psl) {
                if (!ps.getGamePlayer().equals(gp)) {
                    continue;
                }
                pls.add(ps);
            }
        }
        return pls;
    }

    public static void registerPlayersScore(Score score){
        if (playerScores.containsKey(score.getName()))return;

        List<PlayerScore> sc = new ArrayList<>();
        for (GamePlayer gp : playerMap.values()) {
            PlayerScore ps = new PlayerScore(score.getName(), gp, score.getRewardTypes()).setDisplayName(score.getDisplayName()).setAllowedMessage(score.isMessage()).setScoreRanking(score.scoreRanking);

            if (score.getStat() != null){
                ps.setStat(score.getStat());
            }
            if (score.getTriggers() != null){
                ps.setTriggers(score.getTriggers());
            }

            sc.add(ps);
        }
        playerScores.put(score, sc);
    }



    public static class Score{

        private String name;
        private String displayName;
        private Map<Economy, Integer> rewardTypes = new HashMap<>();
        private boolean message = true;

        private boolean scoreRanking = false;
        private Stat stat;
        private Set<Trigger<?>> triggers;

        public Score(String name){
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public String getDisplayName() {
            if (displayName == null){
                return name;
            }
            return displayName;
        }

        public Map<Economy, Integer> getRewardTypes() {
            return rewardTypes;
        }

        public boolean isMessage() {
            return message;
        }

        public boolean isScoreRanking() {
            return scoreRanking;
        }

        public void setName(String name) {
            this.name = name;

        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;

        }

        public void setRewardTypes(Map<Economy, Integer> rewardTypes) {
            this.rewardTypes = rewardTypes;

        }

        public void setEconomyReward(Economy economy, Integer reward){
            if (!rewardTypes.containsKey(economy)){
                rewardTypes.put(economy, reward);
            }

        }

        public void setAllowedMessage(boolean message) {
            this.message = message;
        }

        public Set<Trigger<?>> getTriggers() {
            return triggers;
        }

        public Score setTriggers(Set<Trigger<?>> triggers) {
            this.triggers = triggers;
            return this;
        }

        public void setScoreRanking(boolean scoreRanking) {
            this.scoreRanking = scoreRanking;
        }

        public void setStat(Stat stat) {
            this.stat = stat;
        }

        public Stat getStat() {
            return stat;
        }
    }
}