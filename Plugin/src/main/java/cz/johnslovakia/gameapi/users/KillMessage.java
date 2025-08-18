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
        addMessage(EntityDamageEvent.DamageCause.BLOCK_EXPLOSION, "chat.explosion");
        addMessage(EntityDamageEvent.DamageCause.ENTITY_EXPLOSION, "chat.explosion");
        addMessage(EntityDamageEvent.DamageCause.WORLD_BORDER, "chat.border");
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
