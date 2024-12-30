package cz.johnslovakia.gameapi.users;

import lombok.Getter;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.HashMap;
import java.util.Map;

@Getter
public class KillMessage {

    public final GamePlayer gamePlayer;
    public Map<EntityDamageEvent.DamageCause, String> messages = new HashMap<>();

    public KillMessage(GamePlayer gamePlayer) {
        this.gamePlayer = gamePlayer;
        messages.put(EntityDamageEvent.DamageCause.BLOCK_EXPLOSION, "§7[§rẃ§7] §a%dead%§r §7was killed by an explosion caused by §a%killer%§r");
        messages.put(EntityDamageEvent.DamageCause.ENTITY_EXPLOSION, "§7[§rẃ§7] §a%dead%§r §7was killed by an explosion caused by §a%killer%§r");
    }

    public void addMessage(EntityDamageEvent.DamageCause cause, String messageKey){
        if (messages.containsKey(cause)){
            return;
        }
        messages.put(cause, messageKey);
    }


    public String getMessageKey(EntityDamageEvent.DamageCause cause) {
        for (EntityDamageEvent.DamageCause deadCause : messages.keySet()){
            if (deadCause == cause){
                return messages.get(deadCause);
            }
        }
        return "chat.kill";
    }
}
