package cz.johnslovakia.gameapi.modules.resources.storage;

import cz.johnslovakia.gameapi.users.PlayerIdentity;
import net.milkbowl.vault.economy.Economy;

import java.util.concurrent.CompletableFuture;

public class VaultStorage implements ResourceStorage{

    private final Economy vaultEconomy;

    public VaultStorage(Economy vaultEconomy) {
        this.vaultEconomy = vaultEconomy;
    }

    @Override
    public void deposit(PlayerIdentity playerIdentity, int amount) {
        vaultEconomy.depositPlayer(playerIdentity.getOfflinePlayer(), amount);
    }

    @Override
    public void withdraw(PlayerIdentity playerIdentity, int amount) {
        vaultEconomy.withdrawPlayer(playerIdentity.getOfflinePlayer(), amount);
    }

    @Override
    public CompletableFuture<Integer> getBalance(PlayerIdentity playerIdentity) {
        return CompletableFuture.supplyAsync(() -> (int) vaultEconomy.getBalance(playerIdentity.getOfflinePlayer()));
    }

    @Override
    public int getBalanceCached(PlayerIdentity playerIdentity) {
        return (int) vaultEconomy.getBalance(playerIdentity.getOfflinePlayer());
    }

    @Override
    public void shutdown() {

    }
}
