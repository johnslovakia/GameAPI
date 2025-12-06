package cz.johnslovakia.gameapi.modules.resources;

import cz.johnslovakia.gameapi.users.PlayerIdentity;

@FunctionalInterface
public interface ResourceChangeListener {
    void onChange(PlayerIdentity playerIdentity, int amount, ResourceChangeType type);

}