package cz.johnslovakia.gameapi.users.stats;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.datastorage.StatsTable;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerData;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;

import java.util.*;

@Getter
public class Stat{

    private final String name;
    private final String translate_key;

    private boolean showToPlayer = true;

    public Stat(String name) {
        this.name = name;

        this.translate_key = "stat." + name.toLowerCase().replace(" ", "_");
    }

    public StatsTable getStatsTable() {
        return Minigame.getInstance().getStatsManager().getTable();
    }

    public Component getTranslated(GamePlayer gamePlayer){
        if (MessageManager.existMessage(gamePlayer.getLanguage(), translate_key)){
            return MessageManager.get(gamePlayer, translate_key).getTranslated();
        }else{
            return Component.text(getName());
        }
    }

    public Stat setShowToPlayer(boolean showToPlayer) {
        this.showToPlayer = showToPlayer;
        return this;
    }
}
