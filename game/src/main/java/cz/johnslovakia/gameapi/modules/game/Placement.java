package cz.johnslovakia.gameapi.modules.game;

import me.clip.placeholderapi.libs.kyori.adventure.text.Component;

public class Placement<T> {

    private final T entity;
    private final int place;

    public Placement(T entity, int place) {
        this.entity = entity;
        this.place = place;
    }

    public T getEntity() {
        return entity;
    }

    public int getPlace() {
        return place;
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