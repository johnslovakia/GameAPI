package cz.johnslovakia.gameapi.game.cosmetics;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.datastorage.Type;
import cz.johnslovakia.gameapi.economy.Economy;
import cz.johnslovakia.gameapi.game.perk.Perk;
import cz.johnslovakia.gameapi.users.PlayerManager;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerData;
import cz.johnslovakia.gameapi.utils.eTrigger.Condition;
import cz.johnslovakia.gameapi.utils.eTrigger.Trigger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class CosmeticsManager implements Listener{

    private String name;
    private List<CosmeticsCategory> categories = new ArrayList<>();
    private Inventory inv;

    private Economy economy;

    public CosmeticsManager(String name, Economy economy) {
        this.name = name;
        this.economy = economy;

        GameAPI.getInstance().getMinigame().getMinigameTable().addRow(Type.JSON, "Cosmetics");
    }

    public CosmeticsCategory getCategoryByName(String name){
        for (CosmeticsCategory category : categories){
            if (category.getName().equalsIgnoreCase(name)){
                return category;
            }
        }
        return null;
    }

    public Cosmetic getCosmetic(String categoryName, String name){
        CosmeticsCategory category = getCategoryByName(categoryName);
        if (category != null){
            return category.getCosmetics().stream().filter(cosmetic -> cosmetic.getName().equalsIgnoreCase(name)).toList().get(0);
        }
        return null;
    }

    public boolean hasPurchased(GamePlayer gamePlayer, Cosmetic cosmetic){
        return gamePlayer.getPlayerData().getPurchasedCosmetics().contains(cosmetic);
    }

    public boolean hasSelected(GamePlayer gamePlayer, Cosmetic cosmetic){
        return gamePlayer.getPlayerData().getSelectedCosmetics().containsValue(cosmetic);
    }

    public boolean hasPlayer(GamePlayer gamePlayer, Cosmetic cosmetic){
        return hasPurchased(gamePlayer, cosmetic)
                || hasSelected(gamePlayer, cosmetic)
                || gamePlayer.getOnlinePlayer().hasPermission("cosmetics.free");
    }

    public CosmeticsCategory getCategory(Cosmetic cosmetic){
        for (CosmeticsCategory category : categories){
            if (category.getCosmetics().contains(cosmetic)){
                return category;
            }
        }
        return null;
    }

    public Cosmetic getSelectedCosmetic(CosmeticsCategory category, Player player){
        return PlayerManager.getGamePlayer(player).getPlayerData().getSelectedCosmetics().get(category);
    }

    public String getName() {
        return name;
    }

    public List<CosmeticsCategory> getCategories() {
        return categories;
    }

    public void addCategory(CosmeticsCategory category){
        if (categories.contains(category)){
            return;
        }
        categories.add(category);
        for(CTrigger<?> t : category.getTriggers()){
            GameAPI.getInstance().getServer().getPluginManager().registerEvent(t.getEventClass(), new Listener() { }, EventPriority.NORMAL, (listener, event) -> onEventCall(category, event), GameAPI.getInstance());
        }
        //TODO: registrace category, automaticky to udělat?
    }

    public Economy getEconomy() {
        return economy;
    }



    private boolean checkConditions(CosmeticsCategory category, GamePlayer target) {
        boolean result = true;
        boolean alternativeResult = false;

        for(Method method : category.getClass().getDeclaredMethods()){

            if(!method.isAnnotationPresent(Condition.class)) continue;
            if(!method.getReturnType().equals(boolean.class))
                if(method.getParameterCount() > 1) continue;
            method.setAccessible(true);

            Condition condition = method.getAnnotation(Condition.class);

            boolean invokeResult = false;
            try {
                if (method.getParameterCount() == 0) {
                    invokeResult = (boolean) method.invoke(category);
                } else {
                    invokeResult = (boolean) method.invoke(category, target);
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

    private <E extends Event> void onEventCall(CosmeticsCategory category, E event) {
        for (CTrigger<? extends Event> trigger : category.getTriggers()) {
            Class<E> clazz = (Class<E>) trigger.getEventClass();

            if (clazz.isInstance(event)) {
                CTrigger<E> typedTrigger = (CTrigger<E>) trigger;

                if (!typedTrigger.validate(event)) continue;

                GamePlayer gamePlayer = typedTrigger.compute(event);

                if (checkConditions(category, gamePlayer)) {
                    typedTrigger.getResponse().accept(event);
                    return;
                }
            }
        }
    }

}