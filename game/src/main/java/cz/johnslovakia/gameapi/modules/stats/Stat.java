package cz.johnslovakia.gameapi.modules.stats;

import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.messages.MessageModule;
import cz.johnslovakia.gameapi.users.PlayerIdentity;

import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;

@Getter
public class Stat{

    private final String name;

    private String translationKey;
    private boolean showToPlayer = true;

    public Stat(String name) {
        this.name = name;
        this.translationKey = "stat." + name.toLowerCase().replace(" ", "_");
    }

    public Component getTranslated(PlayerIdentity playerIdentity){
        if (ModuleManager.getModule(MessageModule.class).existMessage(playerIdentity, translationKey)){
            return ModuleManager.getModule(MessageModule.class).get(playerIdentity, translationKey).getTranslated();
        }else{
            return Component.text(getName());
        }
    }

    public Stat setTranslationKey(String translationKey) {
        this.translationKey = translationKey;
        return this;
    }

    public Stat hideFromPlayer() {
        this.showToPlayer = false;
        return this;
    }
}
