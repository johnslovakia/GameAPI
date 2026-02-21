package cz.johnslovakia.gameapi.modules.game;

import lombok.Getter;
import net.kyori.adventure.text.Component;

@Getter
public class Placement<T> {

    private final T entity;
    private final int place;

    public Placement(T entity, int place) {
        this.entity = entity;
        this.place = place;
    }

    public String getPlaceName() {
        return switch (place) {
            case 1 -> "1st";
            case 2 -> "2nd";
            case 3 -> "3rd";
            default -> place + "th";
        };
    }

    public Component getPlaceComponent() {
        return Component.text(getPlaceName());
    }
}