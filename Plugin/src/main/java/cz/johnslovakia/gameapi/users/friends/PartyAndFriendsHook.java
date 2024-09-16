package cz.johnslovakia.gameapi.users.friends;

import cz.johnslovakia.gameapi.users.PlayerManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import de.simonsator.partyandfriends.spigot.api.pafplayers.PAFPlayer;
import de.simonsator.partyandfriends.spigot.api.pafplayers.PAFPlayerManager;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;

public class PartyAndFriendsHook implements FriendsInterface{

    private final GamePlayer gamePlayer;

    public PartyAndFriendsHook(GamePlayer gamePlayer) {
        this.gamePlayer = gamePlayer;
    }

    @Override
    public boolean hasFriends() {
        PAFPlayer pafPlayer = PAFPlayerManager.getInstance().getPlayer(gamePlayer.getOnlinePlayer().getUniqueId());

        return pafPlayer != null && !pafPlayer.getFriends().isEmpty();
    }

    @Override
    public List<GamePlayer> getAllOnlinePlayers() {
        PAFPlayer pafPlayer = PAFPlayerManager.getInstance().getPlayer(gamePlayer.getOnlinePlayer().getUniqueId());

        List<GamePlayer> online = new ArrayList<>();

        if (pafPlayer != null && hasFriends()){
            for (PAFPlayer p : pafPlayer.getFriends()){
                if (PlayerManager.exists(p.getUniqueId()) && Bukkit.getPlayer(p.getUniqueId()) != null){
                    online.add(PlayerManager.getGamePlayer(Bukkit.getPlayer(p.getUniqueId())));
                }
            }
        }
        return online;
    }

    @Override
    public boolean isFriendWith(GamePlayer gamePlayer) {
        if (hasFriends()){
            return getAllOnlinePlayers().contains(gamePlayer);
        }
        return false;
    }
}
