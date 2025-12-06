package cz.johnslovakia.gameapi.users.parties;

import com.alessiodp.parties.api.Parties;
import com.alessiodp.parties.api.interfaces.PartiesAPI;
import com.alessiodp.parties.api.interfaces.Party;
import com.alessiodp.parties.api.interfaces.PartyPlayer;
import cz.johnslovakia.gameapi.users.PlayerIdentity;

import cz.johnslovakia.gameapi.users.PlayerIdentityRegistry;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PartiesHook implements PartyInterface {

    private final PlayerIdentity playerIdentity;
    private final PartiesAPI api;

    public PartiesHook(PlayerIdentity playerIdentity) {
        this.playerIdentity = playerIdentity;
        this.api = Parties.getApi();
    }

    @Override
    public boolean isInParty() {
        PartyPlayer partyPlayer = api.getPartyPlayer(playerIdentity.getUniqueId());

        if (partyPlayer == null){
            return false;
        }

        return partyPlayer.isInParty();
    }

    @Override
    public List<PlayerIdentity> getAllOnlinePlayers() {
        List<PlayerIdentity> online = new ArrayList<>();

        if (isInParty()){
            Party party = api.getParty(playerIdentity.getUniqueId());
            if (party == null) return online;
            for (UUID uuid : party.getMembers().stream().filter(PlayerIdentityRegistry.map::containsKey).toList()) {
                if (Bukkit.getPlayer(uuid) == null){
                    continue;
                }
                online.add(PlayerIdentityRegistry.get(uuid));
            }
        }

        return online;
    }

    @Override
    public PlayerIdentity getLeader() {
        if (isInParty()){
            Party party = api.getParty(playerIdentity.getUniqueId());
            if (party == null) return null;
            UUID leader = party.getLeader();
            if (leader == null) return null;

            if (PlayerIdentityRegistry.exists(leader) && Bukkit.getPlayer(leader) != null){
                return PlayerIdentityRegistry.get(leader);
            }
        }
        return null;
    }
}
