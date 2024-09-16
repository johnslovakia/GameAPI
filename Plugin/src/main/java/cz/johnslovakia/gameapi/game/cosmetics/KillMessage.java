package cz.johnslovakia.gameapi.game.cosmetics;

import java.util.HashMap;
import java.util.Map;

public class KillMessage {

    public String name;
    public CosmeticsCategory category;
    public DeadCause cause;
    public Map<DeadCause, String> messages = new HashMap<>();

    public KillMessage(CosmeticsCategory category, String name) {
        this.name = name;
        this.category = category;
    }

    public void addMessage(DeadCause cause, String messageKey){
        if (messages.containsKey(cause)){
            return;
        }
        messages.put(cause, messageKey);
    }

    public String getName() {
        return name;
    }

    public String getMessageKey(DeadCause cause) {
        for (DeadCause deadCause : messages.keySet()){
            if (deadCause == cause){
                return messages.get(deadCause);
            }
        }
        return null;
    }

    public DeadCause getCause() {
        return cause;
    }

    public Cosmetic getCosmetic(){
        return category.getManager().getCosmetic(category.getName(), getName());
    }

    public enum DeadCause{
        MELEE, VOID, FALL, RANGED;
    }
}
