package cz.johnslovakia.gameapi.utils.eTrigger;

import cz.johnslovakia.gameapi.users.GamePlayer;
import org.bukkit.event.Event;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public interface Mapper<E> {
    List<GamePlayer> compute(E event);

    class SingleMapper<E> implements Mapper<E> {
        private final Function<E, GamePlayer> mapper;

        public SingleMapper(Function<E, GamePlayer> mapper) {
            this.mapper = mapper;
        }

        @Override
        public List<GamePlayer> compute(E event) {
            return Collections.singletonList(mapper.apply(event));
        }
    }

    class ListMapper<E> implements Mapper<E> {
        private final Function<E, List<GamePlayer>> mapper;

        public ListMapper(Function<E, List<GamePlayer>> mapper) {
            this.mapper = mapper;
        }

        @Override
        public List<GamePlayer> compute(E event) {
            return mapper.apply(event);
        }
    }
}
