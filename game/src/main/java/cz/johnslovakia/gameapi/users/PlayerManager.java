package cz.johnslovakia.gameapi.users;

import org.bukkit.OfflinePlayer;

public class PlayerManager {

    public static GamePlayer register(OfflinePlayer offlinePlayer){
        GamePlayer gamePlayer = new GamePlayer(offlinePlayer);
        PlayerIdentityRegistry.register(new GamePlayer(offlinePlayer));
        return gamePlayer;
    }

    public static GamePlayer getGamePlayer(OfflinePlayer player) {
        if (PlayerIdentityRegistry.isRegisteredAs(player.getUniqueId(), GamePlayer.class)){
            return PlayerIdentityRegistry.getAs(player, GamePlayer.class).get();
        }else{
            return register(player);
        }
    }
}