package cz.johnslovakia.gameapi.users;

import cz.johnslovakia.gameapi.users.stats.Stat;
import cz.johnslovakia.gameapi.utils.Logger;
import cz.johnslovakia.gameapi.utils.eTrigger.Trigger;
import cz.johnslovakia.gameapi.utils.rewards.Reward;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class PlayerManager {

    @Getter
    public static final Map<UUID, GamePlayer> playerMap = new ConcurrentHashMap<>();
    @Getter
    public static final List<Score> scores = new ArrayList<>();

    public static void removeGamePlayer(Player player){
        UUID uuid = player.getUniqueId();
        playerMap.remove(uuid);
    }

    public static boolean exists(UUID uuid){
        return playerMap.containsKey(uuid);
    }


    public static GamePlayer getGamePlayer(OfflinePlayer player) {
        UUID uuid = player.getUniqueId();
        return playerMap.computeIfAbsent(uuid, id -> {
            GamePlayer gamePlayer = new GamePlayer(player);
            for (Score score : scores) {
                gamePlayer.getPlayerData().addPlayerScore(score);
            }
            return gamePlayer;
        });
    }


    public static void registerPlayersScore(Score score){
        scores.add(score);
        for (GamePlayer gamePlayer : playerMap.values()) {
            PlayerScore playerScore = new PlayerScore(gamePlayer, score);

            if (score.getStat() != null)
                playerScore.setStat(score.getStat());
            if (score.getTriggers() != null)
                playerScore.setTriggers(score.getTriggers());

            gamePlayer.getPlayerData().addPlayerScore(playerScore);
        }
    }



    @Getter @Setter
    public static class Score{

        private String name, pluralName, displayName;
        private Reward reward;
        private boolean message = true;

        private boolean scoreRanking = false;
        private int limit = 0;
        private Stat stat;
        private Set<Trigger<?>> triggers = new HashSet<>();

        public Score(String name){
            this.name = name;
        }

        public String getDisplayName() {
            if (displayName == null){
                return name;
            }
            return displayName;
        }

        public void setAllowedMessage(boolean message) {
            this.message = message;
        }

        public Score setTriggers(Set<Trigger<?>> triggers) {
            this.triggers = triggers;
            return this;
        }
    }
}