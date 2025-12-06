package cz.johnslovakia.gameapi.users.friends;

import cz.johnslovakia.gameapi.users.PlayerIdentity;
import cz.johnslovakia.gameapi.users.PlayerIdentityRegistry;
import de.simonsator.partyandfriends.spigot.api.pafplayers.PAFPlayer;
import de.simonsator.partyandfriends.spigot.api.pafplayers.PAFPlayerManager;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;

public class PartyAndFriendsHook implements FriendsInterface{

    private final PlayerIdentity gamePlayer;

    public PartyAndFriendsHook(PlayerIdentity gamePlayer) {
        this.gamePlayer = gamePlayer;
    }

    @Override
    public boolean hasFriends() {
        PAFPlayer pafPlayer = PAFPlayerManager.getInstance().getPlayer(gamePlayer.getOnlinePlayer().getUniqueId());

        return pafPlayer != null && !pafPlayer.getFriends().isEmpty();
    }

    @Override
    public List<PlayerIdentity> getAllOnlinePlayers() {
        PAFPlayer pafPlayer = PAFPlayerManager.getInstance().getPlayer(gamePlayer.getOnlinePlayer().getUniqueId());

        List<PlayerIdentity> online = new ArrayList<>();

        if (pafPlayer != null && hasFriends()){
            for (PAFPlayer p : pafPlayer.getFriends()){
                if (PlayerIdentityRegistry.exists(p.getUniqueId()) && Bukkit.getPlayer(p.getUniqueId()) != null){
                    online.add(PlayerIdentityRegistry.get(Bukkit.getPlayer(p.getUniqueId())));
                }
            }
        }
        return online;
    }

    @Override
    public boolean isFriendWith(PlayerIdentity gamePlayer) {
        if (hasFriends()){
            return getAllOnlinePlayers().contains(gamePlayer);
        }
        return false;
    }
}
