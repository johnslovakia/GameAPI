package cz.johnslovakia.gameapi.utils.eTrigger;

import cz.johnslovakia.gameapi.users.PlayerIdentity;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public interface Mapper<E> {
    List<PlayerIdentity> compute(E event);

    class SingleMapper<E> implements Mapper<E> {
        private final Function<E, PlayerIdentity> mapper;

        public SingleMapper(Function<E, PlayerIdentity> mapper) {
            this.mapper = mapper;
        }

        @Override
        public List<PlayerIdentity> compute(E event) {
            return Collections.singletonList(mapper.apply(event));
        }
    }

    class ListMapper<E> implements Mapper<E> {
        private final Function<E, List<PlayerIdentity>> mapper;

        public ListMapper(Function<E, List<PlayerIdentity>> mapper) {
            this.mapper = mapper;
        }

        @Override
        public List<PlayerIdentity> compute(E event) {
            return mapper.apply(event);
        }
    }
}
