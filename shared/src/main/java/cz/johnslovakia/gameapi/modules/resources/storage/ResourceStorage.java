package cz.johnslovakia.gameapi.modules.resources.storage;

import cz.johnslovakia.gameapi.users.PlayerIdentity;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface ResourceStorage {

    void deposit(PlayerIdentity playerIdentity, int amount);
    void withdraw(PlayerIdentity playerIdentity, int amount);
    CompletableFuture<Integer> getBalance(PlayerIdentity playerIdentity);
    int getBalanceCached(PlayerIdentity playerIdentity);
    default void onEnable(){};
    default CompletableFuture<Void> preload(Iterable<PlayerIdentity> players) {
        return CompletableFuture.completedFuture(null);
    }
    void shutdown();

}
