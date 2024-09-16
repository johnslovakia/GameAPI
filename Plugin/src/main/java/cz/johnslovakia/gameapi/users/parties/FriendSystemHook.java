package cz.johnslovakia.gameapi.users.parties;

import cz.johnslovakia.gameapi.users.PlayerManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import me.sk8ingduck.friendsystem.SpigotAPI;
import me.sk8ingduck.friendsystem.manager.PartyManager;
import me.sk8ingduck.friendsystem.util.Party;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FriendSystemHook implements PartyInterface{

    private final GamePlayer gamePlayer;
    private final SpigotAPI api;
    private final PartyManager manager;

    public FriendSystemHook(GamePlayer gamePlayer) {
        this.gamePlayer = gamePlayer;
        this.api = SpigotAPI.getInstance();
        this.manager = api.getPartyManager();
    }

    @Override
    public boolean isInParty() {
        return manager.getParty(gamePlayer.getOnlinePlayer().getUniqueId()) != null;
    }

    @Override
    public List<GamePlayer> getAllOnlinePlayers() {
        List<GamePlayer> online = new ArrayList<>();

        if (isInParty()){
            Party party = manager.getParty(gamePlayer.getOnlinePlayer().getUniqueId());

            if (!party.getAllMembers().isEmpty()){
                for (UUID uuid : party.getAllMembers()){
                    if (PlayerManager.exists(uuid)){
                        online.add(PlayerManager.getGamePlayer(Bukkit.getPlayer(uuid)));
                    }
                }
            }
        }

        return online;
    }

    @Override
    public GamePlayer getLeader() {
        if (isInParty()) {
            Party party = manager.getParty(gamePlayer.getOnlinePlayer().getUniqueId());
            UUID leader = party.getLeaderUUID();
            if (Bukkit.getPlayer(leader) != null && PlayerManager.exists(leader)){
                return PlayerManager.getGamePlayer(Bukkit.getPlayer(leader));
            }
        }
        return null;
    }
}
