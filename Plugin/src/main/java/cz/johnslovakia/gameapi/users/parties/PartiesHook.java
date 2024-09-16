package cz.johnslovakia.gameapi.users.parties;

import com.alessiodp.parties.api.Parties;
import com.alessiodp.parties.api.interfaces.PartiesAPI;
import com.alessiodp.parties.api.interfaces.Party;
import com.alessiodp.parties.api.interfaces.PartyPlayer;
import cz.johnslovakia.gameapi.users.PlayerManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PartiesHook implements PartyInterface {

    private final GamePlayer gamePlayer;
    private final PartiesAPI api;

    public PartiesHook(GamePlayer gamePlayer) {
        this.gamePlayer = gamePlayer;
        this.api = Parties.getApi();
    }

    @Override
    public boolean isInParty() {
        PartyPlayer partyPlayer = api.getPartyPlayer(gamePlayer.getOnlinePlayer().getUniqueId());

        if (partyPlayer == null){
            return false;
        }

        return partyPlayer.isInParty();
    }

    @Override
    public List<GamePlayer> getAllOnlinePlayers() {
        List<GamePlayer> online = new ArrayList<>();

        if (isInParty()){
            PartyPlayer partyPlayer = api.getPartyPlayer(gamePlayer.getOnlinePlayer().getUniqueId());
            Party party = api.getParty(partyPlayer.getPartyId());
            for (UUID uuid : party.getMembers().stream().filter(PlayerManager::exists).toList()) {
                if (Bukkit.getPlayer(uuid) == null){
                    continue;
                }
                online.add(PlayerManager.getGamePlayer(Bukkit.getPlayer(uuid)));
            }
        }

        return online;
    }

    @Override
    public GamePlayer getLeader() {
        if (isInParty()){
            PartyPlayer partyPlayer = api.getPartyPlayer(gamePlayer.getOnlinePlayer().getUniqueId());
            Party party = api.getParty(partyPlayer.getPartyId());
            UUID leader = party.getLeader();

            if (PlayerManager.exists(leader) && Bukkit.getPlayer(leader) != null){
                return PlayerManager.getGamePlayer(Bukkit.getPlayer(leader));
            }
        }
        return null;
    }
}
