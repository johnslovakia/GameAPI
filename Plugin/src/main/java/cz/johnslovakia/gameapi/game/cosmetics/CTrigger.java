package cz.johnslovakia.gameapi.game.cosmetics;

import cz.johnslovakia.gameapi.users.GamePlayer;
import org.apache.commons.lang3.Validate;
import org.bukkit.Location;
import org.bukkit.event.Event;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

//NOT MY CODE

/**
 * Representation of automatic statistic trigger. It is used to detect event calls and perform specific action on statistics dependently on which one is called.
 * @param <E> Event to be caught if called
 */
public class CTrigger<E extends Event> {

    private final Class<E> eventClass;
    private final Function<E, GamePlayer> mapper;
    private final Predicate<E> validator;
    //private final Function<? extends Number, ? extends Number> response;
    private final Consumer<E> response;


    /**
     * Creates new instance of <tt>Trigger</tt> class with event response.
     * @param eventClass Event to be caught
     * @param mapper Mapping function retrieving {@link GamePlayer} object form {@link E} object, which will represent statistic member
     * @param response Function to be executed on member retrieved for event using mapper
     */
    public <T extends Number> CTrigger(Class<E> eventClass, Function<E, GamePlayer> mapper, Consumer<E> response) {
        Validate.notNull(eventClass, "eventClass cannot be null");
        Validate.notNull(mapper, "mapper cannot be null");
        Validate.notNull(response, "response cannot be null");

        this.eventClass = eventClass;
        this.mapper = mapper;
        this.response = response;
        validator = e -> true;
    }

    /**
     * Creates new instance of <tt>Trigger</tt> class with event validator and response.
     * @param eventClass Event to be caught
     * @param mapper Mapping function retrieving {@link GamePlayer} object form {@link E} object, which will represent statistic member
     * @param validator Validating function which tests the caught event; trigger will be run if returns {@code true}
     * @param response Function to be executed on member retrieved for event using mapper
     */
    public <T extends Number> CTrigger(Class<E> eventClass, Function<E, GamePlayer> mapper, Predicate<E> validator, Consumer<E> response) {
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
    public GamePlayer compute(Event event){
        Validate.notNull(event, "event cannot be null");

        return mapper.apply(eventClass.cast(event));
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

    public Consumer<E> getResponse() {
        return response;
    }
}