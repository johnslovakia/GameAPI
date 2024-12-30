package cz.johnslovakia.gameapi.utils.eTrigger;

import cz.johnslovakia.gameapi.users.GamePlayer;
import org.apache.commons.lang3.Validate;
import org.bukkit.event.Event;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

//NOT MY CODE
//https://github.com/TheKaVu/GameAPI/blob/master/src/main/java/dev/kavu/gameapi/statistic/Trigger.java

/**
 * Representation of automatic statistic trigger. It is used to detect event calls and perform specific action on statistics dependently on which one is called.
 * @param <E> Event to be caught if called
 */
public class Trigger<E extends Event> {

    private final Class<E> eventClass;
    private final Mapper<E> mapper;
    private final Predicate<E> validator;
    private final Consumer<GamePlayer> response;

    public Trigger(Class<E> eventClass, Mapper<E> mapper) {
        Validate.notNull(eventClass, "eventClass cannot be null");
        Validate.notNull(mapper, "mapper cannot be null");

        this.eventClass = eventClass;
        this.mapper = mapper;
        validator = event -> true;
        response = null;
    }

    public Trigger(Class<E> eventClass, Mapper<E> mapper, Predicate<E> validator) {
        Validate.notNull(eventClass, "eventClass cannot be null");
        Validate.notNull(mapper, "mapper cannot be null");
        Validate.notNull(validator, "validator cannot be null");

        this.eventClass = eventClass;
        this.mapper = mapper;
        this.validator = validator;
        response = null;
    }

    public <T extends Number> Trigger(Class<E> eventClass, Mapper<E> mapper, Consumer<GamePlayer> response) {
        Validate.notNull(eventClass, "eventClass cannot be null");
        Validate.notNull(mapper, "mapper cannot be null");
        Validate.notNull(response, "response cannot be null");

        this.eventClass = eventClass;
        this.mapper = mapper;
        this.response = response;
        validator = e -> true;
    }

    public <T extends Number> Trigger(Class<E> eventClass, Mapper<E> mapper, Predicate<E> validator, Consumer<GamePlayer> response) {
        Validate.notNull(eventClass, "eventClass cannot be null");
        Validate.notNull(mapper, "mapper cannot be null");
        Validate.notNull(validator, "validator cannot be null");
        Validate.notNull(response, "response cannot be null");

        this.eventClass = eventClass;
        this.mapper = mapper;
        this.validator = validator;
        this.response = response;
    }


    /**
     * @return Monitored event's class
     */
    public Class<E> getEventClass(){
        return eventClass;
    }

    /**
     * Retrieves {@link GamePlayer} object from event using mapper, representing a member
     * @param event Incoming event
     * @return {@link GamePlayer} representation of member
     */
    public List<GamePlayer> compute(Event event){
        Validate.notNull(event, "event cannot be null");

        //return mapper.apply(eventClass.cast(event));
        return mapper.compute(eventClass.cast(event));
    }

    /**
     * Validates event with validator, if set.
     * @param event Incoming event
     * @return {@link true} if event passed the test, {@link false} otherwise
     */
    public boolean validate(Event event){
        Validate.notNull(event, "event cannot be null");

        return validator.test(eventClass.cast(event));
    }

    public Consumer<GamePlayer> getResponse() {
        return response;
    }
}