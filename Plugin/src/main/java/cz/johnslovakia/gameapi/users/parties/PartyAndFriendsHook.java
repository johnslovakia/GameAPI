package cz.johnslovakia.gameapi.users.parties;

import cz.johnslovakia.gameapi.users.PlayerManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import de.simonsator.partyandfriends.spigot.api.pafplayers.PAFPlayer;
import de.simonsator.partyandfriends.spigot.api.pafplayers.PAFPlayerManager;
import de.simonsator.partyandfriends.spigot.api.party.PartyManager;
import de.simonsator.partyandfriends.spigot.api.party.PlayerParty;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;

public class PartyAndFriendsHook implements PartyInterface {

    private final GamePlayer gamePlayer;


    public PartyAndFriendsHook(GamePlayer gamePlayer) {
        this.gamePlayer = gamePlayer;
    }

    @Override
    public boolean isInParty() {
        PAFPlayer pafPlayer = PAFPlayerManager.getInstance().getPlayer(gamePlayer.getOnlinePlayer().getUniqueId());
        PlayerParty party = PartyManager.getInstance().getParty(pafPlayer);

        return pafPlayer != null && party != null;
    }

    @Override
    public List<GamePlayer> getAllOnlinePlayers() {
        PAFPlayer pafPlayer = PAFPlayerManager.getInstance().getPlayer(gamePlayer.getOnlinePlayer().getUniqueId());
        PlayerParty party = PartyManager.getInstance().getParty(pafPlayer);

        List<GamePlayer> online = new ArrayList<>();

        if (pafPlayer != null && party != null){
            for (PAFPlayer p : party.getAllPlayers()){
                if (PlayerManager.exists(p.getUniqueId()) && Bukkit.getPlayer(p.getUniqueId()) != null){
                    online.add(PlayerManager.getGamePlayer(Bukkit.getPlayer(p.getUniqueId())));
                }
            }
        }
        return online;
    }

    @Override
    public GamePlayer getLeader() {
        PAFPlayer pafPlayer = PAFPlayerManager.getInstance().getPlayer(gamePlayer.getOnlinePlayer().getUniqueId());
        PlayerParty party = PartyManager.getInstance().getParty(pafPlayer);

        if (pafPlayer != null && party != null){
            PAFPlayer pafLeader = party.getLeader();
            if (PlayerManager.exists(pafLeader.getUniqueId()) && Bukkit.getPlayer(pafLeader.getUniqueId()) != null) {
                return PlayerManager.getGamePlayer(Bukkit.getPlayer(pafLeader.getUniqueId()));
            }
        }

        return null;
    }
}
