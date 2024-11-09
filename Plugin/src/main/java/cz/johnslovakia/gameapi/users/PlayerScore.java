package cz.johnslovakia.gameapi.users;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.economy.Economy;
import cz.johnslovakia.gameapi.economy.RewardTypeComparator;
import cz.johnslovakia.gameapi.events.PlayerScoreEvent;
import cz.johnslovakia.gameapi.game.Game;
import cz.johnslovakia.gameapi.messages.Message;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.users.stats.Stat;
import cz.johnslovakia.gameapi.utils.Logger;
import cz.johnslovakia.gameapi.utils.eTrigger.Condition;
import cz.johnslovakia.gameapi.utils.eTrigger.Trigger;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;


@Getter
public class PlayerScore implements Comparable<PlayerScore> {

    private final String pluralName;
    private String displayName;
    private final String name;
    private GamePlayer gamePlayer;
    @Setter
    private int score = 0;
    private final int rewardLimit;
    private Stat stat;

    private final Map<Economy, Integer> earned = new HashMap<>();
    private Map<Economy, Integer> rewardTypes = new HashMap<>();
    private Set<Trigger<?>> triggers = new HashSet<>();;

    private boolean allowedMessage;
    private boolean scoreRanking;

    public PlayerScore(GamePlayer gamePlayer, PlayerManager.Score builder) {
        this.gamePlayer = gamePlayer;
        this.name = builder.getName();
        this.displayName = builder.getDisplayName();
        this.pluralName = builder.getPluralName();
        this.rewardTypes = builder.getRewardTypes();
        this.allowedMessage = builder.isMessage();
        this.scoreRanking = builder.isScoreRanking();
        this.stat = builder.getStat();
        this.rewardLimit = builder.getLimit();


        if (builder.getTriggers() != null){
            this.triggers = builder.getTriggers();

            for(Trigger<?> t : getTriggers()){
                GameAPI.getInstance().getServer().getPluginManager().registerEvent(t.getEventClass(), new Listener() { }, EventPriority.NORMAL, (listener, event) -> onEventCall(event), GameAPI.getInstance());
            }
        }
    }

    public void reward(){
        if (getRewardTypes() == null){
            return;
        }

        List<Economy> list =  new ArrayList<>(getRewardTypes().keySet());

        RewardTypeComparator comparator = new RewardTypeComparator();
        list.sort(comparator);

        StringBuilder text = new StringBuilder();

        if (!getRewardTypes().isEmpty()/* && (score == 0 || score < rewardLimit)*/) {
            int i = 0;
            for (Economy economy : list) {
                int reward = getRewardTypes().get(economy);
                addEarning(economy, reward);

                Game game = getGamePlayer().getPlayerData().getGame();

                if (game != null){
                    if (isAllowedMessage()) {
                        if (i >= 1){
                            text.append("§7, ");
                        }
                        text.append(economy.getChatColor() + "+" + reward + " " + economy.getName());
                    }
                }
                /*if (economy.getEconomyInterface() != null) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            economy.getEconomyInterface().deposit(gamePlayer, reward);
                        }
                    }.runTaskAsynchronously(GameAPI.getInstance());
                }*/

                i++;
            }

            if (isAllowedMessage()) {
                MessageManager.get(getGamePlayer(), "chat.economy.reward")
                        .replace("%reward%", text.toString())
                        .replace("%gameplayerscore%", getDisplayName())
                        .send();

                if ((score + 1) == rewardLimit){
                    MessageManager.get(gamePlayer, "chat.economy.reward.limit")
                            .replace("%score%", getDisplayName(true))
                            .send();
                }
            }
        }
    }

    public void increaseScore() {
        increaseScore(true);
    }

    public void increaseScore(boolean reward) {
        setScore(getScore() + 1);
        PlayerScoreEvent event = new PlayerScoreEvent(getGamePlayer(), this, ScoreAction.INCREASE);
        Bukkit.getPluginManager().callEvent(event);

        if (getStat() != null) {
            getStat().getPlayerStat(getGamePlayer()).increase();
        }
        if (reward) {
            reward();
        }
    }

    public void decreaseScore() {
        setScore(getScore() - 1);
        PlayerScoreEvent event = new PlayerScoreEvent(getGamePlayer(), this, ScoreAction.DECREASE);
        Bukkit.getPluginManager().callEvent(event);
    }

    public void resetScore() {
        setScore(0);
        PlayerScoreEvent event = new PlayerScoreEvent(getGamePlayer(), this, ScoreAction.RESET);
        Bukkit.getPluginManager().callEvent(event);
    }

    public void addScore(int score) {
        this.setScore(this.getScore() + score);
        PlayerScoreEvent event = new PlayerScoreEvent(getGamePlayer(), this, ScoreAction.ADD);
        Bukkit.getPluginManager().callEvent(event);
        for (int i = 0; i < score; i++){
            if (getStat() != null) {
                getStat().getPlayerStat(getGamePlayer()).increase();
            }


            reward();
        }
    }

    public void removeScore(int score) {
        this.setScore(this.getScore() - score);
        PlayerScoreEvent event = new PlayerScoreEvent(getGamePlayer(), this, ScoreAction.REMOVE);
        Bukkit.getPluginManager().callEvent(event);
    }

    public void addEarning(Economy rewardType, Integer reward){
        getEarned().put(rewardType, (getEarned().get(rewardType) != null ? getEarned().get(rewardType) : 0) + reward);
    }

    public int getEarned(Economy rewardType){
        if (!getEarned().containsKey(rewardType)){
            return 0;
        }
        return getEarned().get(rewardType);
    }

    public String getDisplayName(boolean plural){
        if (plural && score > 1 && pluralName != null){
            return getPluralName();
        }
        return displayName;
    }

    public String getPluralName(){
        if (pluralName.equals(name)){
            return getDisplayName();
        }
        return pluralName;
    }


    @Override
    public int compareTo(PlayerScore o) {
        return o.getScore() - this.getScore();
    }

    public PlayerScore setTriggers(Set<Trigger<?>> triggers) {
        this.triggers = triggers;
        return this;
    }
    public PlayerScore setDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public PlayerScore setRewardTypes(Map<Economy, Integer> rewardTypes) {
        this.rewardTypes = rewardTypes;
        return this;
    }

    public PlayerScore setAllowedMessage(boolean allowedMessage) {
        this.allowedMessage = allowedMessage;
        return this;
    }

    public PlayerScore setScoreRanking(boolean scoreRanking) {
        this.scoreRanking = scoreRanking;
        return this;
    }

    public PlayerScore setStat(Stat stat) {
        this.stat = stat;
        return this;
    }

    private boolean checkConditions(GamePlayer target) {
        boolean result = true;
        boolean alternativeResult = false;

        for(Method method : getClass().getDeclaredMethods()){

            if(!method.isAnnotationPresent(Condition.class)) continue;
            if(!method.getReturnType().equals(boolean.class))
                if(method.getParameterCount() > 1) continue;
            method.setAccessible(true);

            Condition condition = method.getAnnotation(Condition.class);

            boolean invokeResult = false;
            try {
                if (method.getParameterCount() == 0) {
                    invokeResult = (boolean) method.invoke(this);
                } else {
                    invokeResult = (boolean) method.invoke(this, target);
                }
            } catch (InvocationTargetException | IllegalAccessException ignored) {
            }

            if(!condition.alternative()) {
                result = (invokeResult == !condition.negate()) && result;
            } else {
                alternativeResult = (invokeResult && !condition.negate()) || alternativeResult;
            }
        }
        return result || alternativeResult;
    }

    private void onEventCall(Event event){
        for(Trigger<?> trigger : getTriggers()){
            Class<? extends Event> clazz = trigger.getEventClass();
            if(clazz.equals(event.getClass())){
                if(!trigger.validate(clazz.cast(event))) continue;
                GamePlayer gamePlayer = trigger.compute(clazz.cast(event)).stream().filter(g -> g.equals(getGamePlayer())).findFirst().orElse(null);
                if (gamePlayer != null) {
                    if (checkConditions(gamePlayer)) {
                        if (trigger.getResponse() != null) {
                            trigger.getResponse().accept(gamePlayer);
                        } else {
                            increaseScore();
                        }
                        return;
                    }
                }
            }
        }
    }



    public static Builder builder() { return new Builder(); }

    public static final class Builder {

        private String name, pluralName, displayName;
        private Map<Economy, Integer> rewardTypes = new HashMap<>();
        private boolean message = true;

        private boolean scoreRanking = false;
        private int limit = 0;
        private Stat stat;
        @Getter
        private Set<Trigger<?>> triggers = new HashSet<>();


        private Builder() {}

        public Builder setName(String name) {
            this.name = name;
            this.pluralName = name;
            return this;
        }
        public Builder setName(String name, String pluralName) {
            this.name = name;
            this.pluralName = pluralName;
            return this;
        }

        public Builder setDisplayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder setDisplayName(String displayName, String pluralName) {
            this.displayName = displayName;
            this.pluralName = pluralName;
            return this;
        }

        public Builder setRewardTypes(Map<Economy, Integer> rewardTypes) {
            this.rewardTypes = rewardTypes;
            return this;
        }

        public Builder setEconomyReward(Economy economy, Integer reward){
            if (!rewardTypes.containsKey(economy)){
                rewardTypes.put(economy, reward);
            }
            return this;
        }

        public Builder setAllowedMessage(boolean message) {
            this.message = message;
            return this;
        }

        public Builder setStat(Stat stat) {
            this.stat = stat;
            return this;
        }

        public Builder createStat(){
            createStat(this.name);
            return this;
        }

        public Builder createStat(String name){
            Stat stat = new Stat(name);
            this.stat = stat;
            GameAPI.getInstance().getStatsManager().registerStat(stat);
            return this;
        }

        public Builder setRewardLimit(int limit) {
            this.limit = limit;
            return this;
        }

        public Builder setScoreRanking(boolean scoreRanking) {
            this.scoreRanking = scoreRanking;
            return this;
        }

        public Builder addTrigger(Trigger<?> trigger){
            this.triggers.add(trigger);
            return this;
        }

        public Builder setTriggers(Set<Trigger<?>> triggers) {
            this.triggers = triggers;
            return this;
        }

        public PlayerManager.Score build() {
            PlayerManager.Score score = new PlayerManager.Score(name);
            score.setName(this.name);
            score.setDisplayName((this.displayName != null ? this.displayName : this.name));
            score.setPluralName(this.pluralName);
            score.setRewardTypes(this.rewardTypes);
            score.setAllowedMessage(this.message);
            score.setScoreRanking(this.scoreRanking);
            score.setStat(this.stat);
            score.setTriggers(this.triggers);
            score.setLimit(limit);

            return score;
        }
    }
}