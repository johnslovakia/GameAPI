package cz.johnslovakia.gameapi.messages;

import lombok.Getter;

@Getter
public enum MessageType {

    CHAT("/chat/"),
    ACTIONBAR("/actionbar/"),
    TITLE("/title/"),
    SUBTITLE("/subtitle/"),
    KICK("/kick/");

    private final String key;

    MessageType(String key){
        this.key = key;
    }

}