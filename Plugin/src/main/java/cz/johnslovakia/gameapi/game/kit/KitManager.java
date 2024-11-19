package cz.johnslovakia.gameapi.game.kit;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.datastorage.Type;
import cz.johnslovakia.gameapi.economy.Economy;
import cz.johnslovakia.gameapi.game.Game;
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
            Bukkit.getPluginManager().registerEvents(manager, GameAPI.getInstance());
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


    private Game game;
    private final Economy economy;

    private final List<Kit> kits = new ArrayList<>();
    @Setter
    private Kit defaultKit;

    private final boolean purchaseKitForever;
    private final boolean giveAfterDeath;

    public KitManager(Economy economy, boolean buyingForever, boolean giveAfterDeath) {
        this.economy = economy;
        this.purchaseKitForever = buyingForever;
        this.giveAfterDeath = giveAfterDeath;

        GameAPI.getInstance().getMinigame().getMinigameTable().createNewColumn(Type.JSON, "KitInventories");
        GameAPI.getInstance().getMinigame().getMinigameTable().createNewColumn(Type.VARCHAR128, "DefaultKit");

        addKitManager(this);
    }

    public KitManager(Game game, Economy economy, boolean buyingForever, boolean giveAfterDeath) {
        this.game = game;
        this.economy = economy;
        this.purchaseKitForever = buyingForever;
        this.giveAfterDeath = giveAfterDeath;

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
                return;
            }
            this.kits.add(kit);
        }
    }
    public void unregisterKit(String kitName) {
        if (getKit(kitName) != null) {
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
        for (Kit kit : kits){
            if (kit.getName().equals(name)){
                return kit;
            }
        }
        return null;
    }

}