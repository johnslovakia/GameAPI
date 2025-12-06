package cz.johnslovakia.gameapi.users;


import org.bukkit.OfflinePlayer;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class PlayerIdentityRegistry {

    public static final ConcurrentMap<UUID, PlayerIdentity> map = new ConcurrentHashMap<>();

    public static PlayerIdentity get(UUID uuid) {
        return map.get(uuid);
    }

    public static PlayerIdentity get(OfflinePlayer offline) {
        return map.get(offline.getUniqueId());
    }

    public static <T extends PlayerIdentity> Optional<T> getAs(UUID uuid, Class<T> type) {
        PlayerIdentity identity = map.get(uuid);
        if (identity != null && type.isInstance(identity)) {
            return Optional.of(type.cast(identity));
        }
        return Optional.empty();
    }

    public static <T extends PlayerIdentity> Optional<T> getAs(OfflinePlayer offline, Class<T> type) {
        return getAs(offline.getUniqueId(), type);
    }

    public static void register(PlayerIdentity identity) {
        if (identity != null) {
            map.put(identity.getUniqueId(), identity);
        }
    }

    public static void unregister(UUID uuid) {
        map.remove(uuid);
    }

    public static boolean exists(UUID uuid){
        return map.containsKey(uuid);
    }

    public static boolean isRegisteredAs(UUID uuid, Class<? extends PlayerIdentity> type) {
        PlayerIdentity identity = map.get(uuid);
        return identity != null && type.isInstance(identity);
    }
}