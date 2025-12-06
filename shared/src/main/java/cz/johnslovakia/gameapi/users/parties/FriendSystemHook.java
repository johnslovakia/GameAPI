package cz.johnslovakia.gameapi.users.parties;

import cz.johnslovakia.gameapi.users.PlayerIdentity;
import cz.johnslovakia.gameapi.users.PlayerIdentityRegistry;
import me.sk8ingduck.friendsystem.SpigotAPI;
import me.sk8ingduck.friendsystem.manager.PartyManager;
import me.sk8ingduck.friendsystem.util.Party;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FriendSystemHook implements PartyInterface{

    private final PlayerIdentity gamePlayer;
    private final SpigotAPI api;
    private final PartyManager manager;

    public FriendSystemHook(PlayerIdentity gamePlayer) {
        this.gamePlayer = gamePlayer;
        this.api = SpigotAPI.getInstance();
        this.manager = api.getPartyManager();
    }

    @Override
    public boolean isInParty() {
        return manager.getParty(gamePlayer.getOnlinePlayer().getUniqueId()) != null;
    }

    @Override
    public List<PlayerIdentity> getAllOnlinePlayers() {
        List<PlayerIdentity> online = new ArrayList<>();

        if (isInParty()){
            Party party = manager.getParty(gamePlayer.getOnlinePlayer().getUniqueId());

            if (!party.getAllMembers().isEmpty()){
                for (UUID uuid : party.getAllMembers()){
                    if (PlayerIdentityRegistry.exists(uuid)){
                        online.add(PlayerIdentityRegistry.get(Bukkit.getPlayer(uuid)));
                    }
                }
            }
        }

        return online;
    }

    @Override
    public PlayerIdentity getLeader() {
        if (isInParty()) {
            Party party = manager.getParty(gamePlayer.getOnlinePlayer().getUniqueId());
            UUID leader = party.getLeaderUUID();
            if (Bukkit.getPlayer(leader) != null && PlayerIdentityRegistry.exists(leader)){
                return PlayerIdentityRegistry.get(Bukkit.getPlayer(leader));
            }
        }
        return null;
    }
}
