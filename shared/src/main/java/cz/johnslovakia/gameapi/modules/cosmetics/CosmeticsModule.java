package cz.johnslovakia.gameapi.modules.cosmetics;

import cz.johnslovakia.gameapi.Shared;
import cz.johnslovakia.gameapi.database.CosmeticsStorage;
import cz.johnslovakia.gameapi.database.PlayerTable;
import cz.johnslovakia.gameapi.database.Type;
import cz.johnslovakia.gameapi.modules.Module;
import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.cosmetics.defaultCosmetics.*;
import cz.johnslovakia.gameapi.modules.resources.Resource;
import cz.johnslovakia.gameapi.modules.resources.ResourcesModule;
import cz.johnslovakia.gameapi.users.PlayerIdentity;
import cz.johnslovakia.gameapi.users.PlayerIdentityRegistry;
import cz.johnslovakia.gameapi.utils.Logger;
import cz.johnslovakia.gameapi.utils.eTrigger.Condition;

import lombok.Getter;
import me.zort.sqllib.SQLDatabaseConnection;
import me.zort.sqllib.api.data.QueryResult;
import me.zort.sqllib.api.data.Row;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

@Getter
public class CosmeticsModule implements Listener, Module {

    private List<CosmeticsCategory> categories = new ArrayList<>();
    private Map<PlayerIdentity, Map<CosmeticsCategory, Cosmetic>> selectedCosmetics = new HashMap<>();
    private Map<PlayerIdentity, List<Cosmetic>> purchasedCosmetics = new HashMap<>();

    @Override
    public void initialize() {
        new PlayerTable().createNewColumn(Type.JSON, "Cosmetics");

        addCategory(new KillMessagesCategory(this));
        addCategory(new KillSoundsCategory(this));
        addCategory(new KillEffectsCategory(this));
        addCategory(new TrailsCategory(this));
        addCategory(new HatsCategory(this));

        Bukkit.getPluginManager().registerEvents(this, Shared.getInstance().getPlugin());
    }

    @Override
    public void terminate() {
        categories = null;
        selectedCosmetics = null;
        purchasedCosmetics = null;

        HandlerList.unregisterAll(this);
    }


    public void setPlayerSelectedCosmetic(PlayerIdentity playerIdentity, Cosmetic cosmetic){
        CosmeticsCategory category = getCategory(cosmetic);
        if (category == null) return;

        selectedCosmetics
                .computeIfAbsent(playerIdentity, k -> new HashMap<>())
                .put(category, cosmetic);
        Bukkit.getScheduler().runTaskAsynchronously(Shared.getInstance().getPlugin(), task -> {savePlayerCosmetics(playerIdentity);});
    }

    public Cosmetic getPlayerSelectedCosmetic(PlayerIdentity playerIdentity, CosmeticsCategory category){
        return selectedCosmetics
                .getOrDefault(playerIdentity, Collections.emptyMap())
                .get(category);
    }

    public void grantCosmeticToPlayer(PlayerIdentity playerIdentity, Cosmetic cosmetic){
        purchasedCosmetics
                .computeIfAbsent(playerIdentity, k -> new ArrayList<>())
                .add(cosmetic);
        Bukkit.getScheduler().runTaskAsynchronously(Shared.getInstance().getPlugin(), task -> {savePlayerCosmetics(playerIdentity);});
    }

    public void savePlayerCosmetics(PlayerIdentity playerIdentity){
        SQLDatabaseConnection connection = Shared.getInstance().getDatabase().getConnection();

        QueryResult cosmeticResult = connection.update()
                .table(PlayerTable.TABLE_NAME)
                .set("Cosmetics", CosmeticsStorage.toJSON(playerIdentity, selectedCosmetics.get(playerIdentity), purchasedCosmetics.get(playerIdentity)).toString())
                .where().isEqual("Nickname", playerIdentity.getOnlinePlayer().getName())
                .execute();
        if (!cosmeticResult.isSuccessful()) {
            Logger.log("Something went wrong when saving cosmetics data! The following message is for Developers: ", Logger.LogType.ERROR);
            Logger.log(cosmeticResult.getRejectMessage(), Logger.LogType.ERROR);
            playerIdentity.getOnlinePlayer().sendMessage("§cAn error occurred while saving cosmetics data. Sorry for the inconvenience.");
        }
    }


    public void loadPlayerCosmetics(PlayerIdentity playerIdentity){
        SQLDatabaseConnection connection = Shared.getInstance().getDatabase().getConnection();


        Optional<Row> result = connection.select()
                .from(PlayerTable.TABLE_NAME)
                .where().isEqual("Nickname", playerIdentity.getOnlinePlayer().getName())
                .obtainOne();
        if (result.isEmpty()) {
            Logger.log("I can't get cosmetics data for player " + playerIdentity.getOnlinePlayer().getName() + ". (1)", Logger.LogType.ERROR);
            playerIdentity.getOnlinePlayer().sendMessage("Can't get your cosmetics data. Sorry for the inconvenience. (1)");
        }else {
            try{
                String jsonString = result.get().getString("Cosmetics");
                if (jsonString != null) {
                    JSONObject jsonObject = new JSONObject(jsonString);

                    for (CosmeticsCategory category : getCategories()) {
                        if (!jsonObject.getJSONArray("purchased").isEmpty())
                            purchasedCosmetics.put(playerIdentity, CosmeticsStorage.parseJsonArrayToList(jsonObject.getJSONArray("purchased")));


                        if (!jsonObject.getJSONArray("selected").isEmpty()) {
                            List<Cosmetic> cosmetics = CosmeticsStorage.parseJsonArrayToList(jsonObject.getJSONArray("selected")).stream().filter(c -> getCategory(c) != null && getCategory(c).equals(category)).toList();
                            if (!cosmetics.isEmpty()) {
                                //selectedCosmetics.put(category, cosmetics.get(0));
                                cosmetics.getFirst().select(playerIdentity, false);
                            }
                        }
                    }
                }
            } catch (Exception exception) {
                Logger.log("I can't get cosmetics data for player " + playerIdentity.getOnlinePlayer().getName() + ". (2) The following message is for Developers: " + exception.getMessage(), Logger.LogType.ERROR);
                playerIdentity.getOnlinePlayer().sendMessage("Can't get your cosmetics data. Sorry for the inconvenience. (2)");
                exception.printStackTrace();
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        selectedCosmetics.entrySet().removeIf(entry -> {
            Player online = entry.getKey().getOnlinePlayer();
            return online != null && online.getUniqueId().equals(player.getUniqueId());
        });
        purchasedCosmetics.entrySet().removeIf(entry -> {
            Player online = entry.getKey().getOnlinePlayer();
            return online != null && online.getUniqueId().equals(player.getUniqueId());
        });
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
        return getCosmetic(category, name);
    }

    public Cosmetic getCosmetic(CosmeticsCategory category, String name){
        if (category != null){
            return category.getCosmetics().stream().filter(cosmetic -> cosmetic.getName().equalsIgnoreCase(name)).toList().get(0);
        }
        return null;
    }

    public CosmeticsCategory getCategory(Cosmetic cosmetic){
        for (CosmeticsCategory category : categories){
            if (category.getCosmetics().contains(cosmetic)){
                return category;
            }
        }
        return null;
    }

    public void addCategory(CosmeticsCategory category){
        if (categories.contains(category)){
            return;
        }
        categories.add(category);
        category.setCosmeticsModule(this);
        if (category.getTriggers() != null) {
            for (CTrigger<?> t : category.getTriggers()) {
                Shared.getInstance().getPlugin().getServer().getPluginManager().registerEvent(t.getEventClass(), new Listener() {
                }, EventPriority.NORMAL, (listener, event) -> onEventCall(category, event), Shared.getInstance().getPlugin());
            }
        }
    }

    public Resource getMainResource() {
        return ModuleManager.getModule(ResourcesModule.class).getResourceByName("Coins");
    }



    private boolean checkConditions(CosmeticsCategory category, Player target) {
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

                Player gamePlayer = typedTrigger.compute(event);

                if (checkConditions(category, gamePlayer)) {
                    typedTrigger.getResponse().accept(event);
                    return;
                }
            }
        }
    }
}