package cz.johnslovakia.gameapi.messages;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.game.Game;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.utils.ItemBuilder;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;


import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class Message {

    private List<GamePlayer> audience = new ArrayList<>();
    private Map<GamePlayer, String> messages = new HashMap<>();
    private List<AddToMessage> addToMessage = new ArrayList<>();

    public Message(List<GamePlayer> audience, String key) {
        this.audience = audience;

        for (GamePlayer recipient : audience){
            Language language = recipient.getLanguage();
            if (MessageManager.getMessages(key) == null){
                messages.put(recipient, "§cNo translation found for message key: " + key + " (Language: " + language.getName() + ")");
                return;
            }
            String msg = MessageManager.getMessages(key).get(language);
            messages.put(recipient, Objects.requireNonNullElseGet(msg, () -> "§cNo translation found for message key: " + key + " (Language: " + language.getName() + ")"));
        }
    }

    public Message replace(String oldChar, String newChar) {
        String regex = "(?i)" + Pattern.quote(oldChar);
        messages.replaceAll((key, oldValue) -> oldValue.replaceAll(regex, newChar));
        return this;
    }

    public Message replace(String oldChar, Function<GamePlayer, String> newChar) {
        String regex = "(?i)" + Pattern.quote(oldChar);
        messages.replaceAll((key, oldValue) -> oldValue.replaceAll(regex, newChar.apply(key)));
        return this;
    }

    public Message addAndTranslate(String translateKey){
        if (!translateKey.isEmpty()){
            addAndTranslate(translateKey, null);
        }
        return this;
    }

    public Message addAndTranslate(String translateKey, Predicate<GamePlayer> validator){
        if (!translateKey.isEmpty()){
            addToMessage.add(new AddToMessage(translateKey, validator, true));
        }
        return this;
    }

    public Message add(String message){
        if (!message.isEmpty()){
            add(message, null);
        }
        return this;
    }

    public Message add(String message, Predicate<GamePlayer> validator){
        if (!message.isEmpty()){
            addToMessage.add(new AddToMessage(message, validator, true));
        }
        return this;
    }

    public String getTranslated(){
        if (audience.size() > 1){
            return "§cIncorrect use of the 'getTranslated()' method! This method can only be used if you only want to get a message for one player.";
        }

        return messages.values().stream().toList().get(0);
    }

    public void addToItemLore(ItemBuilder itemBuilder){
        itemBuilder.addLoreLine(getTranslated());
    }


    public void send(){
        MessageType msgType = MessageType.CHAT;
        for (MessageType type : MessageType.values()) {
            if (messages.values().stream().toList().get(0).startsWith(type.getKey())) {
                msgType = type;
            }
        }

        send(msgType);
    }

    public void send(MessageType msgType){
        for (GamePlayer recipient : audience){
            Player player = recipient.getOnlinePlayer();

            StringBuilder finalMessage = new StringBuilder(messages.get(recipient).replace(msgType.getKey(), ""));
            finalMessage = new StringBuilder(ChatColor.translateAlternateColorCodes('&', finalMessage.toString()));
            if (!addToMessage.isEmpty()){
                for (AddToMessage add : addToMessage){
                    if (add.getValidator().test(recipient)) {
                        finalMessage.append(" ").append(add.getMessage(recipient));
                    }
                }
            }

            if (msgType.equals(MessageType.ACTIONBAR)){
                GameAPI.getInstance().getUserInterface().sendAction(player, finalMessage.toString());
            }else if(msgType.equals(MessageType.CHAT)){
                if (finalMessage.toString().contains("\n")){
                    String[] arrSplit = finalMessage.toString().split("\n");
                    player.sendMessage(arrSplit);
                    return;
                }else if (finalMessage.toString().contains("/newline/")){
                    String[] arrSplit = finalMessage.toString().split("/newline/");
                    player.sendMessage(arrSplit);
                    return;
                }
                player.sendMessage(finalMessage.toString());
            }else if(msgType.equals(MessageType.KICK)){
                player.kickPlayer(finalMessage.toString());
            }else if (msgType.equals(MessageType.SUBTITLE)){
                if (finalMessage.toString().contains("/subtitle/")){
                    GameAPI.getInstance().getUserInterface().sendTitle(
                            player,
                            "",
                            finalMessage.toString().replace(Pattern.quote(msgType.getKey()), "").replace("/subtitle/", "")
                    );

                    //String[] arr = translated.split("/subtitle/");
                    //TitleAPI.send(player, arr[0].replace(msgType.getKey(), "").replace("/title/", ""), arr[1].replace(msgType.getKey(), "").replace("/subtitle/", ""));
                }
            }else if(msgType.equals(MessageType.TITLE)){
                if (finalMessage.toString().contains("/subtitle/")){
                    String[] arr = finalMessage.toString().split(Pattern.quote("/subtitle/"));
                    if (arr.length > 1) {
                        GameAPI.getInstance().getUserInterface().sendTitle(
                                player,
                                arr[0].replace(Pattern.quote(msgType.getKey()), "").replace("/title/", ""),
                                arr[1].replace(Pattern.quote(msgType.getKey()), "").replace("/subtitle/", "")
                        );
                    }else{
                        GameAPI.getInstance().getUserInterface().sendTitle(
                                player,
                                "",
                                arr[0].replace(Pattern.quote(msgType.getKey()), "").replace("/subtitle/", "")
                        );
                    }
                }else {
                    GameAPI.getInstance().getUserInterface().sendTitle(player, finalMessage.toString(), null);
                }
            }
        }
    }
}
