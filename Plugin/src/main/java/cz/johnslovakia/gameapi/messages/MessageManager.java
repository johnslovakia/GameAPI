package cz.johnslovakia.gameapi.messages;

import cz.johnslovakia.gameapi.users.PlayerManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.utils.rewards.Reward;
import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.*;

public class MessageManager {

    @Getter
    private static Map<String, Map<Language, String>> messages = new HashMap<>();
    @Getter
    public static Map<String, Reward> linkedRewardMessages = new HashMap<>();

    public static void addMessage(String name, Language language, String message){
        if (messages.containsKey(name)){
            messages.get(name).put(language, message);
        }else{
            Map<Language, String> map = new HashMap<>();
            map.put(language, message);
            messages.put(name, map);
        }
    }

    public static void addLinkedRewardMessage(String key, Reward reward){
        if (!messages.containsKey(key)){
            linkedRewardMessages.put(key, reward);
        }
    }

    public static void addMessage(String name, String message){
        Map<Language, String> map = messages.computeIfAbsent(name, k -> new HashMap<>());
        for (Language language : Language.getLanguages()) {
            map.putIfAbsent(language, message);
        }
        messages.put(name, map);
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

    public static String get(Language language, String key){
        if (!existMessage(key) || messages.get(key).get(language) == null){
            return "§cNo translation found for message key: " + key + " (Language: " + language.getName() + ")";
        }else{
            return messages.get(key).get(language);
        }
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