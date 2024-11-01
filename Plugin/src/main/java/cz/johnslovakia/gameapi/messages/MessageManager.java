package cz.johnslovakia.gameapi.messages;

import cz.johnslovakia.gameapi.users.PlayerManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.*;

public class MessageManager {

    @Getter
    private static Map<String, Map<Language, String>> messages = new HashMap<>();

    public static void addMessage(String name, Language language, String message){
        if (messages.containsKey(name)){
            messages.get(name).put(language, message);
        }else{
            Map<Language, String> map = new HashMap<>();
            map.put(language, message);
            messages.put(name, map);
        }
    }

    public static void addMessage(String name, String message){
        for (Language language : Language.getLanguages()) {
            Map<Language, String> map = new HashMap<>();
            map.put(language, message);
            messages.put(name, map);
        }
    }

    public static Message get(Player player, String key){
        return get(PlayerManager.getGamePlayer(player), key);
    }

    public static Message get(GamePlayer gamePlayer, String key){
        return new Message(Collections.singletonList(gamePlayer), key);
    }

    public static Message get(List<GamePlayer> audience, String key){
        return new Message(audience, key);
    }

    public static Map<Language, String> getMessages(String key){
        return messages.get(key);
    }

    public static boolean existMessage(String name){
        return messages.get(name) != null;
    }

    public static boolean existMessage(Language language, String name){
        if (messages.get(name) == null){
            return false;
        }

        return messages.get(name).get(language) != null;
    }

    public static List<String> getMessagesByMSG(String message){
        Map<String, String> messagesMap = new HashMap<>();

        for (String name : messages.keySet()){
            for (Language language : Language.getLanguages()){
                messagesMap.put(name, messages.get(name).get(language));
            }
        }

        for (String name : messagesMap.keySet()){
            if (getMessagesByName(name).contains(message)){
                return getMessagesByName(name);
            }
        }
        return null;
    }

    public static List<String> getMessagesByName(String name){
        List<String> messagesList = new ArrayList<>();
        for (Language language : Language.getLanguages()) {
            messagesList.add(messages.get(name).get(language));
        }

        return messagesList;
    }
}