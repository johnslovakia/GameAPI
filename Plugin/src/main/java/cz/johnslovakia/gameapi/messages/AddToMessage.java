package cz.johnslovakia.gameapi.messages;

import cz.johnslovakia.gameapi.users.GamePlayer;

import java.util.function.Predicate;

public class AddToMessage {

    String message;
    Predicate<GamePlayer> validator;
    boolean translate = false;

    public AddToMessage(String message, Predicate<GamePlayer> validator, boolean translate) {
        this.message = message;
        this.validator = validator;
        this.translate = translate;
    }

    public String getMessage(GamePlayer gamePlayer){
        if (validator == null || validator.test(gamePlayer)) {
            String finalMessage = message;
            if (translate) {
                return finalMessage = MessageManager.get(gamePlayer, message).getTranslated();
            }else{
                return finalMessage;
            }
        }
        return "not_valid";
    }
}
