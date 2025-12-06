package cz.johnslovakia.gameapi.modules.kits;

import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.database.Type;
import cz.johnslovakia.gameapi.modules.game.GameInstance;
import cz.johnslovakia.gameapi.modules.resources.Resource;
import cz.johnslovakia.gameapi.users.GamePlayer;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;


@Getter
public class KitManager implements Listener {

    @Getter
    public static List<KitManager> kitManagers = new ArrayList<>();

    public static void removeKitManager(GameInstance game){
        if (getKitManager(game) != null && getKitManager(game).getGame() != null) {
            kitManagers.remove(getKitManager(game));
        }
    }

    public static void addKitManager(KitManager manager){
        if (!kitManagers.contains(manager)){
            kitManagers.add(manager);

            //Bukkit.getPluginManager().registerEvents(manager, Minigame.getInstance().getPlugin());
        }
    }

    public static KitManager getKitManager(GameInstance game){
        for (KitManager kitManager : kitManagers.stream().filter(kitManager -> kitManager.getGame() != null || kitManager.getGameMap() != null).toList()){
            if ((kitManager.getGame() != null && kitManager.getGame().equals(game)) || (kitManager.getGameMap() != null && game.getCurrentMap() != null && (kitManager.getGameMap().equalsIgnoreCase(game.getCurrentMap().getName()) || kitManager.getGameMap().equalsIgnoreCase(game.getCurrentMap().getName().replaceAll(" ", "_"))))){
                return kitManager;
            }
        }
        return kitManagers.get(0);
    }

    public static KitManager getKitManager(String gameMap){
        for (KitManager kitManager : kitManagers.stream().filter(kitManager -> (kitManager.getGameMap() != null)).toList()){
            if (kitManager.getGameMap().equalsIgnoreCase(gameMap) || kitManager.getGameMap().equalsIgnoreCase(gameMap.replaceAll(" ", "_"))){
                return kitManager;
            }
        }
        return kitManagers.get(0);
    }


    @Setter
    private GameInstance game;
    private String gameMap;
    private final Resource resource;

    private final List<Kit> kits = new ArrayList<>();
    @Setter
    private Kit defaultKit;

    private final boolean purchaseKitForever;

    public KitManager(Resource resource, boolean buyingForever) {
        this.resource = resource;
        this.purchaseKitForever = buyingForever;

        Minigame.getInstance().getMinigameTable().createNewColumn(Type.JSON, "KitInventories");
        Minigame.getInstance().getMinigameTable().createNewColumn(Type.VARCHAR128, "DefaultKit");

        addKitManager(this);
    }

    public KitManager(GameInstance game, Resource resource, boolean buyingForever) {
        this.game = game;
        this.resource = resource;
        this.purchaseKitForever = buyingForever;

        Minigame.getInstance().getMinigameTable().createNewColumn(Type.JSON, "KitInventories");
        Minigame.getInstance().getMinigameTable().createNewColumn(Type.VARCHAR128, "DefaultKit");

        addKitManager(this);
    }

    public KitManager(String gameMap, Resource resource, boolean buyingForever) {
        this.gameMap = gameMap;
        this.resource = resource;
        this.purchaseKitForever = buyingForever;

        Minigame.getInstance().getMinigameTable().createNewColumn(Type.JSON, "KitInventories");
        Minigame.getInstance().getMinigameTable().createNewColumn(Type.VARCHAR128, "DefaultKit");

        addKitManager(this);
    }

    public boolean hasKitPermission(GamePlayer gamePlayer, Kit kit){
        Player player = gamePlayer.getOnlinePlayer();
        String kitPermission = "kit." + kit.getName().replace(" ", "_").toLowerCase();

        return player.hasPermission("kits.free") || player.hasPermission(kitPermission);
    }


    public void registerKit(Kit... kits) {
        for (Kit kit : kits) {
            if (getKit(kit.getName()) != null) {
                continue;
            }
            kit.setKitManager(this);
            this.kits.add(kit);
        }
    }
    public void unregisterKit(String kitName) {
        Kit kit = getKit(kitName);
        if (kit != null) {
            kit.setKitManager(null);
            kits.remove(getKit(kitName));
        }
    }

    public void activeKitsForEveryone(GameInstance game){
        for (GamePlayer gamePlayer : game.getPlayers()) {
            Kit selected = gamePlayer.getGameSession().getSelectedKit();
            if (selected != null) {
                selected.activate(gamePlayer);
            }
        }
    }

    public Kit getKit(String name){
        if (kits.stream().filter(kit -> kit.getName().equalsIgnoreCase(name)).count() > 1)
            throw new IllegalArgumentException("There are multiple kits with the same name (" + name + "), so I can't determine which one you mean!");

        return kits.stream()
                .filter(kit -> kit.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

}