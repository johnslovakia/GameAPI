package cz.johnslovakia.gameapi.modules.scoreboard;

import cz.johnslovakia.gameapi.users.PlayerIdentity;
import cz.johnslovakia.gameapi.utils.eTrigger.Trigger;
import lombok.Getter;
import org.bukkit.event.Event;

import java.util.Objects;

public final class ScoreboardUpdateTrigger<E extends Event> {

    private final Trigger<E> trigger;
    @Getter
    private final ScoreboardUpdateScope scope;

    public ScoreboardUpdateTrigger(Trigger<E> trigger, ScoreboardUpdateScope scope) {
        this.trigger = Objects.requireNonNull(trigger, "trigger");
        this.scope = Objects.requireNonNull(scope, "scope");
    }

    public static <E extends Event> ScoreboardUpdateTrigger<E> player(Trigger<E> trigger) {
        return new ScoreboardUpdateTrigger<>(trigger, ScoreboardUpdateScope.PLAYER);
    }

    public static <E extends Event> ScoreboardUpdateTrigger<E> game(Trigger<E> trigger) {
        return new ScoreboardUpdateTrigger<>(trigger, ScoreboardUpdateScope.GAME);
    }

    public static <E extends Event> ScoreboardUpdateTrigger<E> all(Trigger<E> trigger) {
        return new ScoreboardUpdateTrigger<>(trigger, ScoreboardUpdateScope.ALL);
    }

    public Class<E> getEventClass() {
        return trigger.getEventClass();
    }

    public boolean validate(Event event) {
        return trigger.validate(event);
    }

    public Iterable<PlayerIdentity> compute(Event event) {
        return trigger.compute(event);
    }

}
