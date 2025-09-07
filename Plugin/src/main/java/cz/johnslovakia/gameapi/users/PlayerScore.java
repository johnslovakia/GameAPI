package cz.johnslovakia.gameapi.users;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.users.resources.Resource;
import cz.johnslovakia.gameapi.users.resources.ResourceComparator;
import cz.johnslovakia.gameapi.events.PlayerScoreEvent;
import cz.johnslovakia.gameapi.game.Game;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.users.resources.ResourcesManager;
import cz.johnslovakia.gameapi.users.stats.Stat;
import cz.johnslovakia.gameapi.utils.Logger;
import cz.johnslovakia.gameapi.utils.eTrigger.Condition;
import cz.johnslovakia.gameapi.utils.eTrigger.Trigger;
import cz.johnslovakia.gameapi.utils.rewards.PlayerRewardRecord;
import cz.johnslovakia.gameapi.utils.rewards.Reward;
import cz.johnslovakia.gameapi.utils.rewards.RewardItem;
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
    private final GamePlayer gamePlayer;
    @Setter
    private int score = 0;
    private final int rewardLimit;
    private Stat stat;

    private final Map<Resource, Integer> earned = new HashMap<>();
    private final Reward reward;
    private Set<Trigger<?>> triggers = new HashSet<>();;

    private boolean allowedMessage;
    private boolean scoreRanking;

    public PlayerScore(GamePlayer gamePlayer, PlayerManager.Score builder) {
        this.gamePlayer = gamePlayer;
        this.name = builder.getName();
        this.displayName = builder.getDisplayName();
        this.pluralName = builder.getPluralName();
        this.reward = builder.getReward();
        this.allowedMessage = builder.isMessage();
        this.scoreRanking = builder.isScoreRanking();
        this.stat = builder.getStat();
        this.rewardLimit = builder.getLimit();

        if (builder.getTriggers() != null){
            this.triggers = builder.getTriggers();

            for(Trigger<?> t : getTriggers()){
                Minigame.getInstance().getPlugin().getServer().getPluginManager().registerEvent(t.getEventClass(), new Listener() { }, EventPriority.NORMAL, (listener, event) -> onEventCall(event), Minigame.getInstance().getPlugin());
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
            getGamePlayer().getPlayerData().getPlayerStat(getStat()).increase();
        }
        if (reward && this.reward != null) {
            PlayerRewardRecord record = getReward().applyReward(gamePlayer, isAllowedMessage());
            record.earned().forEach(this::addEarning);
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
                getGamePlayer().getPlayerData().getPlayerStat(getStat()).increase();
            }

            if (this.reward != null){
                PlayerRewardRecord record = getReward().applyReward(gamePlayer, isAllowedMessage());
                record.earned().forEach(this::addEarning);
            }
        }
    }

    public void removeScore(int score) {
        this.setScore(this.getScore() - score);
        PlayerScoreEvent event = new PlayerScoreEvent(getGamePlayer(), this, ScoreAction.REMOVE);
        Bukkit.getPluginManager().callEvent(event);
    }

    public void addEarning(Resource rewardType, Integer reward){
        getEarned().put(rewardType, getEarned().getOrDefault(rewardType, 0) + reward);
    }

    public int getEarned(Resource rewardType){
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
                GamePlayer gamePlayer = trigger.compute(clazz.cast(event)).stream().filter(g -> g == getGamePlayer()).findFirst().orElse(null);
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
    public static Builder builder(String name) { return new Builder(name); }
    public static Builder builder(String name, String pluralName) { return new Builder(name, pluralName); }

    public static final class Builder {

        private String name, pluralName, displayName, linkRewardMessageKey;
        @Setter
        private Reward reward;
        private boolean message = true;

        private boolean scoreRanking = false;
        private int limit = 0;
        private Stat stat;
        @Getter
        private Set<Trigger<?>> triggers = new HashSet<>();

        public Builder() {}

        private Builder(String name) {
            this.name = name;
            this.pluralName = name;
        }
        private Builder(String name, String pluralName) {
            this.name = name;
            this.pluralName = pluralName;
        }

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
        public Builder setExperiencePointsReward(int xp){
            addReward(new RewardItem("ExperiencePoints", xp));
            return this;
        }

        public Builder addReward(RewardItem... rewardItems){
            if (reward == null) reward = new Reward();
            for (RewardItem item : rewardItems){
                reward.addRewardItem(item);;
            }
            return this;
        }
        public Builder addReward(String forWhat, RewardItem... rewardItems){
            if (reward == null) reward = new Reward();
            reward.setForWhat(forWhat);
            for (RewardItem item : rewardItems){
                reward.addRewardItem(item);
            }
            return this;
        }

        public Builder setLinkRewardMessageKey(String linkRewardMessageKey) {
            this.linkRewardMessageKey = linkRewardMessageKey;
            return this;
        }

        public Builder addReward(Resource resource, int amount){
            addReward(RewardItem.builder(resource).setAmount(amount).build());
            return this;
        }

        public Builder addReward(String forWhat, Resource resource, int amount){
            addReward(forWhat, RewardItem.builder(resource).setAmount(amount).build());
            return this;
        }

        public Builder addReward(String resourceName, int amount){
            addReward(ResourcesManager.getResourceByName(resourceName), amount);
            return this;
        }

        public Builder addReward(String forWhat, String resourceName, int amount){
            addReward(forWhat, ResourcesManager.getResourceByName(resourceName), amount);
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
            Minigame.getInstance().getStatsManager().registerStat(stat);
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
            score.setReward(this.reward);
            score.setAllowedMessage(this.message);
            score.setScoreRanking(this.scoreRanking);
            score.setStat(this.stat);
            score.setTriggers(this.triggers);
            score.setLimit(limit);

            //TODO: nefunguje
            /*if (linkRewardMessageKey != null){
                MessageManager.addLinkedRewardMessage(linkRewardMessageKey, reward);
            }*/
            if (reward != null)
                reward.setForWhat(score.getDisplayName());

            return score;
        }
    }
}