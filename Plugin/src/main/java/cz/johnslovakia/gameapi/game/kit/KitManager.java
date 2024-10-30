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


@Getter
public class KitManager implements Listener {

    private String name;
    private Economy economy;

    private List<Kit> kits = new ArrayList<>();
    @Setter
    private Kit defaultKit;

    private final boolean purchaseKitForever;
    private final boolean giveAfterDeath;

    /** @param buyingForever If the boolean is "true", the player will buy kits forever,
     * if the boolean is "false", the player will buy a kit for one game.
     */
    public KitManager(String name, Economy economy, boolean buyingForever, boolean giveAfterDeath) {
        this.name = name;
        this.economy = economy;
        this.purchaseKitForever = buyingForever;
        this.giveAfterDeath = giveAfterDeath;
        GameAPI.getInstance().setKitManager(this);

        GameAPI.getInstance().getMinigame().getMinigameTable().addColumn(Type.JSON, "KitInventories");
        GameAPI.getInstance().getMinigame().getMinigameTable().addColumn(Type.VARCHAR128, "DefaultKit");
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