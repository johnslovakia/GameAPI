package cz.johnslovakia.gameapi.modules.resources.storage;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

public interface ResourceStorage {

    void deposit(OfflinePlayer player, int amount);
    void withdraw(OfflinePlayer player, int amount);
    CompletableFuture<Integer> getBalance(OfflinePlayer player);
    int getBalanceCached(OfflinePlayer player);
    default void onEnable(){};
    default CompletableFuture<Void> preload(Iterable<? extends OfflinePlayer> players) {
        return CompletableFuture.completedFuture(null);
    }
    void shutdown();

}