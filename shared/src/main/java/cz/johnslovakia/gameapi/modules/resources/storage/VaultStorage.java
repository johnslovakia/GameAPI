package cz.johnslovakia.gameapi.modules.resources.storage;

import cz.johnslovakia.gameapi.utils.Logger;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;

import java.util.concurrent.CompletableFuture;

public class VaultStorage implements ResourceStorage {

    private final Economy vaultEconomy;

    public VaultStorage(Economy vaultEconomy) {
        this.vaultEconomy = vaultEconomy;
    }

    @Override
    public void deposit(OfflinePlayer player, int amount) {
        EconomyResponse response = vaultEconomy.depositPlayer(player, amount);
        if (response == null) response = vaultEconomy.depositPlayer(player.getName(), amount);
        if (response == null) return;

        if (!response.transactionSuccess()) Logger.log("Vault deposit failed for " + player.getName() + " amount=" + amount + " error=" + response.errorMessage + " type=" + response.type, Logger.LogType.ERROR);
    }

    @Override
    public void withdraw(OfflinePlayer player, int amount) {
        EconomyResponse response = vaultEconomy.withdrawPlayer(player, amount);
        if (response == null) response = vaultEconomy.withdrawPlayer(player.getName(),amount);
        if (response == null) return;

        if (!response.transactionSuccess()) Logger.log("Vault withdraw failed for " + player.getName() + " amount=" + amount + " error=" + response.errorMessage + " type=" + response.type, Logger.LogType.ERROR);
    }

    @Override
    public CompletableFuture<Integer> getBalance(OfflinePlayer player) {
        return CompletableFuture.supplyAsync(() -> {
            double balance = vaultEconomy.getBalance(player);
            if (balance == 0 && player.getName() != null) balance = vaultEconomy.getBalance(player.getName());
            return (int) balance;
        });
    }

    @Override
    public int getBalanceCached(OfflinePlayer player) {
        double balance = vaultEconomy.getBalance(player);
        if (balance == 0 && player.getName() != null) balance = vaultEconomy.getBalance(player.getName());

        return (int) balance;
    }

    @Override
    public void shutdown() {

    }
}