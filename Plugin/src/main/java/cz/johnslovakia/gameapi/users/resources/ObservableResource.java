package cz.johnslovakia.gameapi.users.resources;

import cz.johnslovakia.gameapi.users.GamePlayer;

import java.util.ArrayList;
import java.util.List;

public class ObservableResource implements ResourceInterface {

    private final ResourceInterface delegate;
    private final List<ResourceChangeListener> listeners = new ArrayList<>();

    public ObservableResource(ResourceInterface delegate) {
        this.delegate = delegate;
    }

    public void addListener(ResourceChangeListener listener) {
        listeners.add(listener);
    }

    private void notify(GamePlayer gamePlayer, int amount, ResourceChangeType type) {
        for (ResourceChangeListener listener : listeners) {
            listener.onChange(gamePlayer, amount, type);
        }
    }

    @Override
    public void deposit(GamePlayer gamePlayer, int amount) {
        delegate.deposit(gamePlayer, amount);
        notify(gamePlayer, amount, ResourceChangeType.DEPOSIT);
    }

    @Override
    public void withdraw(GamePlayer gamePlayer, int amount) {
        delegate.withdraw(gamePlayer, amount);
        notify(gamePlayer, amount, ResourceChangeType.WITHDRAW);
    }

    @Override
    public void setBalance(GamePlayer gamePlayer, int amount) {
        delegate.setBalance(gamePlayer, amount);
        notify(gamePlayer, amount, ResourceChangeType.SET);
    }

    @Override
    public int getBalance(GamePlayer gamePlayer) {
        return delegate.getBalance(gamePlayer);
    }
}