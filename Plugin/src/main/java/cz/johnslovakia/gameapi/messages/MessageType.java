package cz.johnslovakia.gameapi.messages;

public enum MessageType {

    CHAT("/chat/"), ACTIONBAR("/actionbar/"), TITLE("/title/"), SUBTITLE("/subtitle/"), KICK("/kick/");

    private String key;

    private MessageType(String key){
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}