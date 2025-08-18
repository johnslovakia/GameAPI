package cz.johnslovakia.gameapi.users.resources;

import cz.johnslovakia.gameapi.users.GamePlayer;

@FunctionalInterface
public interface ResourceChangeListener {
    void onChange(GamePlayer gamePlayer, int amount, ResourceChangeType type);

}