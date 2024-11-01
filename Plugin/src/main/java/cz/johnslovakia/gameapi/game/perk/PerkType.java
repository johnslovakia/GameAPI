package cz.johnslovakia.gameapi.game.perk;

import lombok.Getter;

public enum PerkType {

    CHANCE("%"), SECONDS("s");

    @Getter
    private final String type;

    PerkType(String s) {
        this.type = s;
    }
}
