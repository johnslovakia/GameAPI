package cz.johnslovakia.gameapi.users.achievements;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.utils.eTrigger.Condition;
import cz.johnslovakia.gameapi.utils.eTrigger.Trigger;

import lombok.Getter;

import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@Getter
public class AchievementManager {

    private final String name;
    private final List<Achievement> achievements = new ArrayList<>();

    public AchievementManager(String name) {
        this.name = name;
    }

    public void registerAchievement(Achievement... achievements){
        for (Achievement achievement : achievements){
            if (!this.achievements.contains(achievement)){
                this.achievements.add(achievement);

                for (Trigger<?> t : achievement.getTriggers()) {
                    GameAPI.getInstance().getServer().getPluginManager().registerEvent(t.getEventClass(), new Listener() {
                    }, EventPriority.NORMAL, (listener, event) -> onEventCall(achievement, event), GameAPI.getInstance());
                }
            }
        }
    }

    public Achievement getAchievement(String name){
        for (Achievement achievement : achievements){
            if (achievement.getName().equals(name)){
                return achievement;
            }
        }
        return null;
    }

    private boolean checkConditions(Achievement achievement, GamePlayer target) {
        boolean result = true;
        boolean alternativeResult = false;

        for(Method method : achievement.getClass().getDeclaredMethods()){

            if(!method.isAnnotationPresent(Condition.class)) continue;
            if(!method.getReturnType().equals(boolean.class))
                if(method.getParameterCount() > 1) continue;
            method.setAccessible(true);

            Condition condition = method.getAnnotation(Condition.class);

            boolean invokeResult = false;
            try {
                if (method.getParameterCount() == 0) {
                    invokeResult = (boolean) method.invoke(achievement);
                } else {
                    invokeResult = (boolean) method.invoke(achievement, target);
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

    private void onEventCall(Achievement achievement, Event event){
        for (Trigger<?> trigger : achievement.getTriggers()) {
            Class<? extends Event> clazz = trigger.getEventClass();
            if (clazz.equals(event.getClass())) {
                if (!trigger.validate(clazz.cast(event))) continue;
                //GamePlayer gamePlayer = trigger.compute(clazz.cast(event));
                for (GamePlayer gamePlayer : trigger.compute(clazz.cast(event))) {
                    if (checkConditions(achievement, gamePlayer)) {
                        trigger.getResponse().accept(gamePlayer);
                        return;
                    }
                }
            }
        }
    }
}
