package cz.johnslovakia.gameapi.game;

import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import net.kyori.adventure.text.Component;

public class Winner {

    private final WinnerType winnerType;

    public Winner(WinnerType winnerType) {
        this.winnerType = winnerType;
    }

    public WinnerType getWinnerType() {
        return winnerType;
    }

    public enum  WinnerType {
        PLAYER, TEAM;

        public String getName() {
            return (this.name().toUpperCase().substring(0, 1) + this.name().toLowerCase().substring(1, this.name().length())).replace("_", " ");
        }

        public Component getTranslatedName(GamePlayer player) {
            return MessageManager.get(player, "winnerType." + this.name().toLowerCase())
                    .getTranslated();
        }
    }
}