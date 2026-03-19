package cz.johnslovakia.gameapi.modules.resources;

import cz.johnslovakia.gameapi.modules.resources.storage.ResourceStorage;
import cz.johnslovakia.gameapi.users.PlayerIdentity;
import org.bukkit.OfflinePlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ObservableResource implements ResourceStorage {

    private final ResourceStorage delegate;
    private final List<ResourceChangeListener> listeners = new ArrayList<>();

    public ObservableResource(ResourceStorage delegate) {
        this.delegate = delegate;
    }

    public void addListener(ResourceChangeListener listener) {
        listeners.add(listener);
    }

    private void notify(OfflinePlayer offlinePlayer, int amount, ResourceChangeType type) {
        for (ResourceChangeListener listener : listeners) {
            listener.onChange(offlinePlayer, amount, type);
        }
    }

    @Override
    public void deposit(OfflinePlayer offlinePlayer, int amount) {
        delegate.deposit(offlinePlayer, amount);
        notify(offlinePlayer, amount, ResourceChangeType.DEPOSIT);
    }

    @Override
    public void withdraw(OfflinePlayer offlinePlayer, int amount) {
        delegate.withdraw(offlinePlayer, amount);
        notify(offlinePlayer, amount, ResourceChangeType.WITHDRAW);
    }

    @Override
    public CompletableFuture<Integer> getBalance(OfflinePlayer offlinePlayer) {
        return delegate.getBalance(offlinePlayer);
    }

    @Override
    public int getBalanceCached(OfflinePlayer offlinePlayer) {
        return delegate.getBalanceCached(offlinePlayer);
    }

    @Override
    public void shutdown() {

    }
}