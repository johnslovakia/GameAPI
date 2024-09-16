package cz.johnslovakia.gameapi.game.perk;

import java.util.function.Consumer;

public class PerkListener<T> {

    private Class<T> type;
    private Consumer<T> consumer;

    public PerkListener(Class<T> type, Consumer<T> consumer) {
        this.type = type;
        this.consumer = consumer;
    }

    public void accept(T t) { consumer.accept(t); }

    public Class<T> getType() { return type; }

}