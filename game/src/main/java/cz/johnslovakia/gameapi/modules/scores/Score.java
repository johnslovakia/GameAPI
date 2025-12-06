package cz.johnslovakia.gameapi.modules.scores;

import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.events.PlayerScoreEvent;
import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.resources.Resource;
import cz.johnslovakia.gameapi.modules.resources.ResourcesModule;
import cz.johnslovakia.gameapi.modules.stats.Stat;
import cz.johnslovakia.gameapi.modules.stats.StatsModule;
import cz.johnslovakia.gameapi.rewards.PlayerRewardRecord;
import cz.johnslovakia.gameapi.rewards.Reward;
import cz.johnslovakia.gameapi.rewards.RewardItem;
import cz.johnslovakia.gameapi.users.*;
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
public class Score {

    private final String name;
    private final String pluralName;
    private final String displayName;

    private final int rewardLimit;
    private final Stat linkedStat;
    private final Reward reward;
    private Set<Trigger<?>> triggers = new HashSet<>();;

    private final boolean allowedMessage;
    private final boolean scoreRanking;

    public Score(Builder builder) {
        this.name = builder.name;
        this.displayName = builder.displayName;
        this.pluralName = builder.pluralName;
        this.reward = builder.reward;
        this.allowedMessage = builder.message;
        this.scoreRanking = builder.scoreRanking;
        this.linkedStat = builder.linkedStat;
        this.rewardLimit = builder.limit;

        if (builder.getTriggers() != null){
            this.triggers = builder.getTriggers();

            for(Trigger<?> t : getTriggers()){
                Minigame.getInstance().getPlugin().getServer().getPluginManager().registerEvent(t.getEventClass(), new Listener() { }, EventPriority.NORMAL, (listener, event) -> onEventCall(event), Minigame.getInstance().getPlugin());
            }
        }
    }

    public String getDisplayName(boolean plural){
        if (plural && pluralName != null){
            return getPluralName();
        }
        return displayName;
    }

    public String getDisplayName(GamePlayer gamePlayer){
        if (gamePlayer.getGameSession().getScore(getName()) > 1 && pluralName != null){
            return getPluralName();
        }
        return displayName;
    }

    public String getPluralName(){
        if (pluralName == null || pluralName.equals(name)){
            return getDisplayName();
        }
        return pluralName;
    }

    public boolean hasPluralName(){
        return pluralName != null;
    }

    private boolean checkConditions(PlayerIdentity target) {
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
                for (PlayerIdentity playerIdentity : trigger.compute(clazz.cast(event))) {
                    if (checkConditions(playerIdentity)) {
                        if (trigger.getResponse() != null) {
                            trigger.getResponse().accept(playerIdentity);
                        } else {
                            if (playerIdentity instanceof GamePlayer gamePlayer)
                                ModuleManager.getModule(ScoreModule.class).incrementScore(gamePlayer, getName());
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

        private String name, displayName, linkRewardMessageKey;
        private String pluralName = null;
        @Setter
        private Reward reward;
        private boolean message = true;

        private boolean scoreRanking = false;
        private int limit = 0;
        private Stat linkedStat;
        @Getter
        private Set<Trigger<?>> triggers = new HashSet<>();

        public Builder() {}

        private Builder(String name) {
            this.name = name;
            //this.pluralName = name;
        }
        private Builder(String name, String pluralName) {
            this.name = name;
            this.pluralName = pluralName;
        }

        public Builder setName(String name) {
            this.name = name;
            //this.pluralName = name;
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
            reward.setSource(forWhat);
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

        public Builder addReward(String source, Resource resource, int amount){
            addReward(source, RewardItem.builder(resource).setAmount(amount).build());
            return this;
        }

        public Builder addReward(String resourceName, int amount){
            addReward(ModuleManager.getModule(ResourcesModule.class).getResourceByName(resourceName), amount);
            return this;
        }

        public Builder addReward(String forWhat, String resourceName, int amount){
            addReward(forWhat, ModuleManager.getModule(ResourcesModule.class).getResourceByName(resourceName), amount);
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

        public Builder linkedStat(Stat stat) {
            this.linkedStat = stat;
            return this;
        }

        public Builder createStat(){
            createStat(this.name);
            return this;
        }

        public Builder createStat(String name){
            Stat stat = new Stat(name);
            this.linkedStat = stat;
            ModuleManager.getModule(StatsModule.class).registerStat(stat);
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

        public Score build() {
            if (displayName == null){
                displayName = name;
            }
            if (reward != null)
                reward.setSource(displayName);

            //TODO: nefunguje
            /*if (linkRewardMessageKey != null){
                ModuleManager.getModule(MessageModule.class).addLinkedRewardMessage(linkRewardMessageKey, reward);
            }*/

            return new Score(this);
        }
    }
}