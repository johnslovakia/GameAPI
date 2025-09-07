package cz.johnslovakia.gameapi.messages;

import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.utils.ActionBarManager;
import cz.johnslovakia.gameapi.utils.ItemBuilder;
import cz.johnslovakia.gameapi.utils.Logger;
import cz.johnslovakia.gameapi.utils.StringUtils;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class Message {

    //private final List<GamePlayer> audience;
    private final Map<GamePlayer, Component> messages = new HashMap<>();
    private boolean placeholdersProcessed = false;

    public Message(List<GamePlayer> audience, String key) {
        for (GamePlayer recipient : audience) {
            Language language = recipient.getLanguage();
            if (language == null){
                messages.put(recipient, Component.text("§cSomething went wrong, your language could not be retrieved. Sorry for the inconvenience."));
                continue;
            }

            Map<Language, String> langMap = MessageManager.getMessages(key);
            if (langMap == null) {
                messages.put(recipient, Component.text("§cNo translation found for message key: " + key + " (Language: " + language.getName() + ")"));
                continue;
            }

            String msg = langMap.get(language);
            if (msg != null) {
                Component component = Component.empty();
                String text = StringUtils.colorizer(processPlaceholders(recipient, msg));
                String[] lines = text.split("(/newline/|\\n)");

                LegacyComponentSerializer serializer = LegacyComponentSerializer.builder()
                        .hexColors()
                        .build();

                for (int i = 0; i < lines.length; i++) {
                    if (i != 0) {
                        component = component.append(Component.newline());
                    }
                    component = component.append(serializer.deserialize(lines[i]));
                }
                //component = component.font(Key.key("jsplugins", "gameapi"));

                messages.put(recipient, component);
            } else {
                messages.put(recipient, Component.text("§cNo translation found for message key: " + key + " (Language: " + language.getName() + ")"));
            }
        }
    }

    public int getAudienceSize(){
        return messages.size();
    }

    public Message replace(String replace, Component replacement) {
        if (replace == null || replacement == null) {
            Logger.log("Null replacement in message.replace(): replace=" + replace + ", replacement=" + replacement, Logger.LogType.WARNING);
            return this;
        }
        //messages.replaceAll((key, oldValue) -> oldValue.replaceText(config -> config.match(replace).replacement(replacement.mergeStyle(oldValue)/*.style(oldValue.style())*)));
        messages.replaceAll((key, oldValue) -> oldValue.replaceText(TextReplacementConfig.builder()
                .matchLiteral(replace)
                .replacement(replacement)
                .build()));
        return this;
    }

    @Deprecated
    public Message replace(String replace, String replacement) {
        return replace(replace, Component.text(replacement));
    }

    public Message replaceWithComponent(String replace, Function<GamePlayer, Component> replacement) {
        if (replace == null || replacement == null) {
            Logger.log("Null replacement in message.replaceWithComponent(): replace=" + replace + ", replacement=" + replacement, Logger.LogType.WARNING);
            return this;
        }
        messages.replaceAll((player, oldValue) -> oldValue.replaceText(TextReplacementConfig.builder().matchLiteral(replace).replacement(replacement.apply(player).style(oldValue.style())).build()));
        return this;
    }

    @Deprecated
    public Message replace(String replace, Function<GamePlayer, String> replacement) {
        Function<GamePlayer, Component> componentReplacement = gp -> {
            String text = replacement.apply(gp);
            return Component.text(text);
        };
        return replaceWithComponent(replace, componentReplacement);
    }

    public Message addAndTranslate(String translateKey, Predicate<GamePlayer> validator) {
        if (translateKey == null || translateKey.isEmpty()) return this;

        for (GamePlayer gamePlayer : messages.keySet()) {
            if (validator == null || validator.test(gamePlayer)) {
                Component addMessage = MessageManager.get(gamePlayer, translateKey).getTranslated();
                if (!PlainTextComponentSerializer.plainText().serialize(addMessage).startsWith(" ")){
                    addMessage = Component.text(" ").append(addMessage);
                }
                //messages.computeIfPresent(gamePlayer, (k, v) -> v + (addMessage.startsWith(" ") ? "" : " ") + addMessage);
                Component finalAddMessage = addMessage;
                messages.computeIfPresent(gamePlayer, (k, v) -> v.append(finalAddMessage));
            }
        }
        return this;
    }

    public Message addAndTranslate(String translateKey) {
        return addAndTranslate(translateKey, null);
    }

    public Message add(Component addMessage, Predicate<GamePlayer> validator) {
        if (addMessage == null) return this;

        for (GamePlayer gamePlayer : messages.keySet()) {
            if (validator == null || validator.test(gamePlayer)) {
                //messages.computeIfPresent(gamePlayer, (k, v) -> v + (addMessage.startsWith(" ") ? "" : " ") + addMessage);
                messages.computeIfPresent(gamePlayer, (k, v) -> v.append(Component.text(" ")).append(addMessage));
            }
        }
        return this;
    }

    @Deprecated
    public Message add(String addMessage, Predicate<GamePlayer> validator) {
        return add(Component.text(addMessage), validator);
    }

    public Message add(Component addMessage) {
        return add(addMessage, null);
    }

    public Message add(String addMessage) {
        return add(Component.text(addMessage), null);
    }

    public Component getTranslated() {
        if (getAudienceSize() != 1) {
            return Component.text("§cIncorrect use of 'getTranslated()' – use only for 1 player.");
        }
        return messages.values().stream().toList().get(0);
    }

    public void addToItemLore(ItemBuilder itemBuilder) {
        itemBuilder.addLoreLine(getTranslated());
    }

    public void send() {
        if (getAudienceSize() == 0) return;

        MessageType msgType = MessageType.CHAT;
        Component firstMessage = messages.values().iterator().next();
        for (MessageType type : MessageType.values()) {
            if (LegacyComponentSerializer.legacySection().serialize(firstMessage).startsWith(type.getKey())) {
                msgType = type;
                break;
            }
        }

        send(msgType);
    }

    public void send(MessageType msgType) {

        for (GamePlayer recipient : messages.keySet()) {
            Player player = recipient.getOnlinePlayer();
            Component finalMsg = messages.get(recipient);
            if (finalMsg == null) continue;

            //String finalMsg = StringUtils.colorizer(rawMessage.replace(msgType.getKey(), ""));

            switch (msgType) {
                case ACTIONBAR -> ActionBarManager.sendActionBar(player, finalMsg);
                case CHAT -> {
                    player.sendMessage(finalMsg);
                }
                case KICK -> player.kick(finalMsg);
                case SUBTITLE -> player.showTitle(Title.title(Component.empty(), finalMsg.replaceText(TextReplacementConfig.builder().match("/subtitle/").replacement("").build())));//GameAPI.getInstance().getUserInterface().sendTitle(player, "", finalMsg.replaceText(TextReplacementConfig.builder().match("/subtitle%").replacement("").build()));
                case TITLE -> {
                    String legacy = LegacyComponentSerializer.legacySection().serialize(finalMsg);
                    String[] parts = legacy.split(Pattern.quote("/subtitle/"), 2);

                    Component title = LegacyComponentSerializer.legacySection().deserialize(parts[0].replaceFirst(Pattern.quote(MessageType.TITLE.getKey()), ""));
                    Component subtitle = parts.length > 1 ? LegacyComponentSerializer.legacySection().deserialize(parts[1].replaceFirst(Pattern.quote(MessageType.SUBTITLE.getKey()), "")) : Component.empty();

                    player.showTitle(Title.title(title, subtitle));
                }
            }
        }
    }


    private String processPlaceholders(GamePlayer gamePlayer, String msg) {
        if (placeholdersProcessed) return msg;
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) return msg;

        Player bukkitPlayer = gamePlayer.getOnlinePlayer();
        if (msg.contains("%")) {
            placeholdersProcessed = true;
            return PlaceholderAPI.setPlaceholders(bukkitPlayer, msg);
        }
        return msg;
    }
}
