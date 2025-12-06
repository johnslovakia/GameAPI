package cz.johnslovakia.gameapi.modules.killMessage;

import lombok.Getter;
import org.bukkit.damage.DamageType;

import java.util.HashMap;
import java.util.Map;

@Getter
public class KillMessage {

    public final Map<DamageType, String> messages = new HashMap<>();

    public KillMessage() {
        addMessage("chat.explosion", DamageType.EXPLOSION, DamageType.PLAYER_EXPLOSION);
        addMessage("chat.border", DamageType.OUTSIDE_BORDER);
    }

    public void addMessage(String messageKey, DamageType... damageTypes){
        for (DamageType damageType : damageTypes){
            messages.put(damageType, messageKey);
        }
    }
    public String getTranslationKey(DamageType damageType) {
        return messages.getOrDefault(damageType, "chat.kill");
    }
}
