package cz.johnslovakia.gameapi.game.perk;

public enum PerkType {

    CHANCE("%"), SECONDS("s");

    String s;

    PerkType(String s) {
        this.s = s;
    }
}
