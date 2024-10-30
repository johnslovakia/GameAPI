package cz.johnslovakia.gameapi.game.perk;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.datastorage.Type;
import cz.johnslovakia.gameapi.economy.Economy;
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
public class PerkManager {

    private final String name;

    private final List<Perk> perks = new ArrayList<>();
    private final Economy economy;

    public PerkManager(String name, Economy economy){
        this.name = name;
        this.economy = economy;

        GameAPI.getInstance().getMinigame().getMinigameTable().addRow(Type.JSON, "Perks");
    }

    public void registerPerk(Perk... perks){
        for (Perk perk : perks) {
            if (!this.perks.contains(perk)) {
                this.perks.add(perk);
                for (Trigger<?> t : perk.getTriggers()) {
                    GameAPI.getInstance().getServer().getPluginManager().registerEvent(t.getEventClass(), new Listener() {
                    }, EventPriority.NORMAL, (listener, event) -> onEventCall(perk, event), GameAPI.getInstance());
                }
            }
        }
    }

    public Perk getPerk(String name){
        for (Perk perk : perks){
            if (perk.getName().equals(name)){
                return perk;
            }
        }
        return null;
    }

    public PerkLevel getNextPlayerPerkLevel(GamePlayer gamePlayer, Perk perk){
        int currentLevel = gamePlayer.getPlayerData().getPerkLevel(perk).level();
        if (perk.getLevels().size() > currentLevel){
            return perk.getLevels().get(currentLevel + 1);
        }
        return null;
    }


    private boolean checkConditions(Perk perk, GamePlayer target) {
        boolean result = true;
        boolean alternativeResult = false;

        for(Method method : perk.getClass().getDeclaredMethods()){

            if(!method.isAnnotationPresent(Condition.class)) continue;
            if(!method.getReturnType().equals(boolean.class))
                if(method.getParameterCount() > 1) continue;
            method.setAccessible(true);

            Condition condition = method.getAnnotation(Condition.class);

            boolean invokeResult = false;
            try {
                if (method.getParameterCount() == 0) {
                    invokeResult = (boolean) method.invoke(perk);
                } else {
                    invokeResult = (boolean) method.invoke(perk, target);
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

    private void onEventCall(Perk perk, Event event){
        for (Trigger<?> trigger : perk.getTriggers()) {
            Class<? extends Event> clazz = trigger.getEventClass();
            if (clazz.equals(event.getClass())) {
                if (!trigger.validate(clazz.cast(event))) continue;
                GamePlayer gamePlayer = trigger.compute(clazz.cast(event));
                if (checkConditions(perk, gamePlayer)) {
                    trigger.getResponse().accept(gamePlayer);
                    return;
                }
            }
        }
    }
}