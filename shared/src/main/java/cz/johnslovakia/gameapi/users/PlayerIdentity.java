package cz.johnslovakia.gameapi.users;

import cz.johnslovakia.gameapi.users.friends.FriendsInterface;
import cz.johnslovakia.gameapi.users.parties.PartyInterface;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.*;

public interface PlayerIdentity {

    UUID getUniqueId();
    String getName();
    Player getOnlinePlayer();
    OfflinePlayer getOfflinePlayer();

    Map<PlayerIdentity, Map<String, Object>> META_STORAGE = new WeakHashMap<>();

    default Map<String, Object> getMetadata() {
        return META_STORAGE.computeIfAbsent(this, k -> new HashMap<>());
    }

    default FriendsInterface getFriends() {
        return EmptyFriendsInterface.INSTANCE;
    }

    default PartyInterface getParty() {
        return EmptyPartyInterface.INSTANCE;
    }

    class EmptyFriendsInterface implements FriendsInterface {
        public static final EmptyFriendsInterface INSTANCE = new EmptyFriendsInterface();

        @Override
        public boolean hasFriends() {
            return false;
        }

        @Override
        public List<PlayerIdentity> getAllOnlinePlayers() {
            return List.of();
        }

        @Override
        public boolean isFriendWith(PlayerIdentity player) {
            return false;
        }
    }

    class EmptyPartyInterface implements PartyInterface {
        public static final EmptyPartyInterface INSTANCE = new EmptyPartyInterface();

        @Override
        public boolean isInParty() {
            return false;
        }

        @Override
        public List<PlayerIdentity> getAllOnlinePlayers() {
            return List.of();
        }

        @Override
        public PlayerIdentity getLeader() {
            return null;
        }
    }
}