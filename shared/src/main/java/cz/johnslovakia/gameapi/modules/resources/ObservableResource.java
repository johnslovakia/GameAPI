package cz.johnslovakia.gameapi.modules.resources;

import cz.johnslovakia.gameapi.modules.resources.storage.ResourceStorage;
import cz.johnslovakia.gameapi.users.PlayerIdentity;

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

    private void notify(PlayerIdentity playerIdentity, int amount, ResourceChangeType type) {
        for (ResourceChangeListener listener : listeners) {
            listener.onChange(playerIdentity, amount, type);
        }
    }

    @Override
    public void deposit(PlayerIdentity playerIdentity, int amount) {
        delegate.deposit(playerIdentity, amount);
        notify(playerIdentity, amount, ResourceChangeType.DEPOSIT);
    }

    @Override
    public void withdraw(PlayerIdentity playerIdentity, int amount) {
        delegate.withdraw(playerIdentity, amount);
        notify(playerIdentity, amount, ResourceChangeType.WITHDRAW);
    }

    @Override
    public CompletableFuture<Integer> getBalance(PlayerIdentity playerIdentity) {
        return delegate.getBalance(playerIdentity);
    }

    @Override
    public int getBalanceCached(PlayerIdentity playerIdentity) {
        return delegate.getBalanceCached(playerIdentity);
    }

    @Override
    public void shutdown() {

    }
}