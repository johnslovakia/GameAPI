package cz.johnslovakia.gameapi.users;

import lombok.Getter;
import org.bukkit.damage.DamageType;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Getter
public class KillMessage {

    public final GamePlayer gamePlayer;
    public Map<DamageType, String> messages = new HashMap<>();

    public KillMessage(GamePlayer gamePlayer) {
        this.gamePlayer = gamePlayer;
        addMessage("chat.explosion", DamageType.EXPLOSION, DamageType.PLAYER_EXPLOSION);
        addMessage("chat.border", DamageType.OUTSIDE_BORDER);
    }

    public void addMessage(String messageKey, DamageType... damageTypes){
        for  (DamageType damageType : damageTypes){
            messages.put(damageType, messageKey);
        }
    }


    public String getMessageKey(DamageType damageType) {
        return messages.getOrDefault(damageType, "chat.kill");
    }
}
