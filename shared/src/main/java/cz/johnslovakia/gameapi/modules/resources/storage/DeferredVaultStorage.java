package cz.johnslovakia.gameapi.modules.resources.storage;

import cz.johnslovakia.gameapi.utils.Logger;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServiceRegisterEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public class DeferredVaultStorage implements ResourceStorage, Listener {

    private final AtomicReference<ResourceStorage> delegate;
    private final JavaPlugin plugin;

    public DeferredVaultStorage(String resourceName, String tableName, JavaPlugin plugin) {
        this.plugin = plugin;
        this.delegate = new AtomicReference<>(new BatchedStorage(resourceName, tableName));

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (isVaultHooked()) return;

            RegisteredServiceProvider<Economy> rsp =
                    Bukkit.getServicesManager().getRegistration(Economy.class);
            if (rsp != null) {
                hookVault(rsp.getProvider());
            } else {
                Bukkit.getPluginManager().registerEvents(this, plugin);
                Logger.log("Vault Economy not yet available, listening for late registration...", Logger.LogType.INFO);
            }
        });
    }

    @EventHandler
    public void onServiceRegister(ServiceRegisterEvent event) {
        if (!event.getProvider().getService().equals(Economy.class)) return;

        Bukkit.getScheduler().runTask(plugin, () -> {
            RegisteredServiceProvider<Economy> rsp =
                    Bukkit.getServicesManager().getRegistration(Economy.class);
            if (rsp != null) {
                hookVault(rsp.getProvider());
            }
            HandlerList.unregisterAll(this);
        });
    }

    private void hookVault(Economy economy) {
        if (isVaultHooked()) return;

        ResourceStorage old = delegate.getAndSet(new VaultStorage(economy));
        Logger.log("Vault Economy hooked: " + economy.getName(), Logger.LogType.INFO);

        if (old != null) {
            old.shutdown();
        }
    }

    public boolean isVaultHooked() {
        return delegate.get() instanceof VaultStorage;
    }


    @Override
    public void deposit(OfflinePlayer player, int amount) {
        delegate.get().deposit(player, amount);
    }

    @Override
    public void withdraw(OfflinePlayer player, int amount) {
        delegate.get().withdraw(player, amount);
    }

    @Override
    public CompletableFuture<Integer> getBalance(OfflinePlayer player) {
        return delegate.get().getBalance(player);
    }

    @Override
    public int getBalanceCached(OfflinePlayer player) {
        return delegate.get().getBalanceCached(player);
    }

    @Override
    public void onEnable() {
        delegate.get().onEnable();
    }

    @Override
    public CompletableFuture<Void> preload(Iterable<? extends OfflinePlayer> players) {
        return delegate.get().preload(players);
    }

    @Override
    public void shutdown() {
        HandlerList.unregisterAll(this);
        delegate.get().shutdown();
    }
}