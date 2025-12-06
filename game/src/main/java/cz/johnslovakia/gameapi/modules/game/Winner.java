package cz.johnslovakia.gameapi.modules.game;

import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.messages.MessageModule;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerIdentity;
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

        public Component getTranslatedName(PlayerIdentity playerIdentity) {
            return ModuleManager.getModule(MessageModule.class).get(playerIdentity, "winnerType." + this.name().toLowerCase())
                    .getTranslated();
        }
    }
}