package cz.johnslovakia.gameapi.users.parties;

import cz.johnslovakia.gameapi.users.PlayerIdentity;
import cz.johnslovakia.gameapi.users.PlayerIdentityRegistry;
import de.simonsator.partyandfriends.spigot.api.pafplayers.PAFPlayer;
import de.simonsator.partyandfriends.spigot.api.pafplayers.PAFPlayerManager;
import de.simonsator.partyandfriends.spigot.api.party.PartyManager;
import de.simonsator.partyandfriends.spigot.api.party.PlayerParty;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;

public class PartyAndFriendsHook implements PartyInterface {

    private final PlayerIdentity gamePlayer;

    public PartyAndFriendsHook(PlayerIdentity gamePlayer) {
        this.gamePlayer = gamePlayer;
    }

    @Override
    public boolean isInParty() {
        PAFPlayer pafPlayer = PAFPlayerManager.getInstance().getPlayer(gamePlayer.getOnlinePlayer().getUniqueId());
        PlayerParty party = PartyManager.getInstance().getParty(pafPlayer);

        return pafPlayer != null && party != null;
    }

    @Override
    public List<PlayerIdentity> getAllOnlinePlayers() {
        PAFPlayer pafPlayer = PAFPlayerManager.getInstance().getPlayer(gamePlayer.getOnlinePlayer().getUniqueId());
        PlayerParty party = PartyManager.getInstance().getParty(pafPlayer);

        List<PlayerIdentity> online = new ArrayList<>();

        if (pafPlayer != null && party != null){
            for (PAFPlayer p : party.getAllPlayers()){
                if (PlayerIdentityRegistry.exists(p.getUniqueId()) && Bukkit.getPlayer(p.getUniqueId()) != null){
                    online.add(PlayerIdentityRegistry.get(Bukkit.getPlayer(p.getUniqueId())));
                }
            }
        }
        return online;
    }

    @Override
    public PlayerIdentity getLeader() {
        PAFPlayer pafPlayer = PAFPlayerManager.getInstance().getPlayer(gamePlayer.getOnlinePlayer().getUniqueId());
        PlayerParty party = PartyManager.getInstance().getParty(pafPlayer);

        if (pafPlayer != null && party != null){
            PAFPlayer pafLeader = party.getLeader();
            if (PlayerIdentityRegistry.exists(pafLeader.getUniqueId()) && Bukkit.getPlayer(pafLeader.getUniqueId()) != null) {
                return PlayerIdentityRegistry.get(Bukkit.getPlayer(pafLeader.getUniqueId()));
            }
        }

        return null;
    }
}
