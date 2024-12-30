package cz.johnslovakia.gameapi.game.kit;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.datastorage.Type;
import cz.johnslovakia.gameapi.users.resources.Resource;
import cz.johnslovakia.gameapi.game.Game;
import cz.johnslovakia.gameapi.game.map.GameMap;
import cz.johnslovakia.gameapi.guis.KitInventoryEditor;
import cz.johnslovakia.gameapi.users.GamePlayer;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@Getter
public class KitManager implements Listener {

    private static final List<KitManager> kitManagers = new ArrayList<>();

    public static void addKitManager(KitManager manager){
        if (!kitManagers.contains(manager)){
            kitManagers.add(manager);

            Bukkit.getPluginManager().registerEvents(manager, GameAPI.getInstance());
            Bukkit.getPluginManager().registerEvents(new KitInventoryEditor(), GameAPI.getInstance());
            Objects.requireNonNull(GameAPI.getInstance().getCommand("saveinventory")).setExecutor(new KitInventoryEditor.SaveCommand());
        }
    }

    public static KitManager getKitManager(Game game){
        for (KitManager kitManager : kitManagers.stream().filter(kitManager -> kitManager.getGame() != null).toList()){
            if (kitManager.getGame().equals(game)){
                return kitManager;
            }
        }
        return kitManagers.get(0);
    }

    public static KitManager getKitManager(GameMap gameMap){
        for (KitManager kitManager : kitManagers.stream().filter(kitManager -> (kitManager.getGameMap() != null || kitManager.getGame() != null)).toList()){
            if (kitManager.getGameMap().equals(gameMap) || kitManager.getGame().getCurrentMap().equals(gameMap)){
                return kitManager;
            }
        }
        return kitManagers.get(0);
    }


    private Game game;
    private GameMap gameMap;
    private final Resource resource;

    private final List<Kit> kits = new ArrayList<>();
    @Setter
    private Kit defaultKit;

    private final boolean purchaseKitForever;

    public KitManager(Resource resource, boolean buyingForever) {
        this.resource = resource;
        this.purchaseKitForever = buyingForever;

        GameAPI.getInstance().getMinigame().getMinigameTable().createNewColumn(Type.JSON, "KitInventories");
        GameAPI.getInstance().getMinigame().getMinigameTable().createNewColumn(Type.VARCHAR128, "DefaultKit");

        addKitManager(this);
    }

    public KitManager(Game game, Resource resource, boolean buyingForever) {
        this.game = game;
        this.resource = resource;
        this.purchaseKitForever = buyingForever;

        GameAPI.getInstance().getMinigame().getMinigameTable().createNewColumn(Type.JSON, "KitInventories");
        GameAPI.getInstance().getMinigame().getMinigameTable().createNewColumn(Type.VARCHAR128, "DefaultKit");

        addKitManager(this);
    }

    public KitManager(GameMap gameMap, Resource resource, boolean buyingForever) {
        this.gameMap = gameMap;
        this.game = gameMap.getGame();
        this.resource = resource;
        this.purchaseKitForever = buyingForever;

        GameAPI.getInstance().getMinigame().getMinigameTable().createNewColumn(Type.JSON, "KitInventories");
        GameAPI.getInstance().getMinigame().getMinigameTable().createNewColumn(Type.VARCHAR128, "DefaultKit");

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

    public void activeKitsForEveryone(Game game){
        for (GamePlayer gamePlayer : game.getPlayers()) {
            Kit selected = gamePlayer.getPlayerData().getKit();
            if (selected != null) {
                selected.activate(gamePlayer);
            }
        }
    }

    public Kit getKit(String name){
        if (kits.stream().filter(kit -> kit.getName().equals(name)).count() > 1)
            throw new IllegalArgumentException("There are multiple kits with the same name (" + name + "), so I can't determine which one you mean!");

        return kits.stream()
                .filter(kit -> kit.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

}