package cz.johnslovakia.gameapi.modules.perks;

import lombok.Getter;

public enum PerkType {

    CHANCE("%"), SECONDS("s");

    @Getter
    private final String string;

    PerkType(String s) {
        this.string = s;
    }
}
