package cz.johnslovakia.gameapi.game.perk;

import lombok.Getter;

import java.util.function.Consumer;

public class PerkListener<T> {

    @Getter
    private final Class<T> type;
    private final Consumer<T> consumer;

    public PerkListener(Class<T> type, Consumer<T> consumer) {
        this.type = type;
        this.consumer = consumer;
    }

    public void accept(T t) { consumer.accept(t); }

}