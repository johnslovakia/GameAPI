package cz.johnslovakia.gameapi.messages;

import cz.johnslovakia.gameapi.users.GamePlayer;
import lombok.Getter;

import java.util.function.Predicate;

@Getter
public record AddToMessage(String message, Predicate<GamePlayer> validator, boolean translate) {

    public String getMessage(GamePlayer gamePlayer) {
        if (validator == null || validator.test(gamePlayer)) {
            if (translate) {
                return MessageManager.get(gamePlayer, message).getTranslated();
            }else{
                return message;
            }
        }
        return "not_valid";
    }
}
