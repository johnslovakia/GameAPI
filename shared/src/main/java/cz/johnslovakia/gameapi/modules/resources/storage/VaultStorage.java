package cz.johnslovakia.gameapi.modules.resources.storage;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;

import java.util.concurrent.CompletableFuture;

public class VaultStorage implements ResourceStorage {

    private final Economy vaultEconomy;

    public VaultStorage(Economy vaultEconomy) {
        this.vaultEconomy = vaultEconomy;
    }

    @Override
    public void deposit(OfflinePlayer player, int amount) {
        vaultEconomy.depositPlayer(player, amount);
    }

    @Override
    public void withdraw(OfflinePlayer player, int amount) {
        vaultEconomy.withdrawPlayer(player, amount);
    }

    @Override
    public CompletableFuture<Integer> getBalance(OfflinePlayer player) {
        return CompletableFuture.supplyAsync(() -> (int) vaultEconomy.getBalance(player));
    }

    @Override
    public int getBalanceCached(OfflinePlayer player) {
        return (int) vaultEconomy.getBalance(player);
    }

    @Override
    public void shutdown() {

    }
}