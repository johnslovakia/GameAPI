package cz.johnslovakia.gameapi.modules.messages;

import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.users.PlayerIdentity;
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

    private final Map<PlayerIdentity, Component> messages = new HashMap<>();
    private final Map<PlayerIdentity, String> rawMessages = new HashMap<>();
    private boolean isDirty = false;

    public Message(List<? extends PlayerIdentity> audience, String key) {
        for (PlayerIdentity recipient : audience) {
            Language language = ModuleManager.getModule(MessageModule.class).getPlayerLanguage(recipient);
            if (language == null){
                rawMessages.put(recipient, "§cSomething went wrong, your language could not be retrieved. Sorry for the inconvenience.");
                continue;
            }

            Map<Language, String> langMap = ModuleManager.getModule(MessageModule.class).getMessages(key);
            if (langMap == null) {
                rawMessages.put(recipient, "§c" + key);
                continue;
            }

            String msg = langMap.get(language);
            if (msg != null) {
                String text = StringUtils.colorizer(processPlaceholders(recipient, msg));
                rawMessages.put(recipient, text);
            } else {
                rawMessages.put(recipient, "§c" + key);
            }
        }
        isDirty = true;
    }

    public int getAudienceSize(){
        return rawMessages.size();
    }

    public Message replace(String replace, Component replacement) {
        if (replace == null || replacement == null) {
            Logger.log("Null replacement in message.replace(): replace=" + replace + ", replacement=" + replacement, Logger.LogType.WARNING);
            return this;
        }

        LegacyComponentSerializer serializer = LegacyComponentSerializer.builder()
                .hexColors()
                .useUnusualXRepeatedCharacterHexFormat()
                .build();
        String replacementStr = serializer.serialize(replacement);
        
        return replace(replace, replacementStr);
    }

    public Message replace(String replace, String replacement) {
        if (replace == null || replacement == null) {
            Logger.log("Null replacement in message.replace(): replace=" + replace + ", replacement=" + replacement, Logger.LogType.WARNING);
            return this;
        }

        rawMessages.replaceAll((key, oldValue) -> oldValue.replace(replace, replacement));
        isDirty = true;
        return this;
    }

    public Message replaceWithComponent(String replace, Function<PlayerIdentity, Component> replacement) {
        if (replace == null || replacement == null) {
            Logger.log("Null replacement in message.replaceWithComponent(): replace=" + replace + ", replacement=" + replacement, Logger.LogType.WARNING);
            return this;
        }
        
        LegacyComponentSerializer serializer = LegacyComponentSerializer.builder()
                .hexColors()
                .useUnusualXRepeatedCharacterHexFormat()
                .build();
        
        rawMessages.replaceAll((player, oldValue) -> {
            Component comp = replacement.apply(player);
            String replacementStr = serializer.serialize(comp);
            return oldValue.replace(replace, replacementStr);
        });
        isDirty = true;
        return this;
    }

    public Message replace(String replace, Function<PlayerIdentity, String> replacement) {
        if (replace == null || replacement == null) {
            Logger.log("Null replacement in message.replace(): replace=" + replace + ", replacement=" + replacement, Logger.LogType.WARNING);
            return this;
        }
        
        rawMessages.replaceAll((player, oldValue) -> {
            String replacementStr = replacement.apply(player);
            return oldValue.replace(replace, replacementStr);
        });
        isDirty = true;
        return this;
    }

    public Message addAndTranslate(String translateKey, Predicate<PlayerIdentity> validator) {
        if (translateKey == null || translateKey.isEmpty()) return this;

        for (PlayerIdentity gamePlayer : rawMessages.keySet()) {
            if (validator == null || validator.test(gamePlayer)) {
                String addMessage = ModuleManager.getModule(MessageModule.class).get(gamePlayer, translateKey).getRawTranslated();
                if (!addMessage.startsWith(" ")){
                    addMessage = " " + addMessage;
                }
                String finalAddMessage = addMessage;
                rawMessages.computeIfPresent(gamePlayer, (k, v) -> v + finalAddMessage);
                isDirty = true;
            }
        }
        return this;
    }

    public Message addAndTranslate(String translateKey) {
        return addAndTranslate(translateKey, null);
    }

    public Message add(Component addMessage, Predicate<PlayerIdentity> validator) {
        if (addMessage == null) return this;

        LegacyComponentSerializer serializer = LegacyComponentSerializer.builder()
                .hexColors()
                .useUnusualXRepeatedCharacterHexFormat()
                .build();
        String addStr = serializer.serialize(addMessage);
        
        return add(addStr, validator);
    }

    public Message add(String addMessage, Predicate<PlayerIdentity> validator) {
        if (addMessage == null) return this;

        for (PlayerIdentity gamePlayer : rawMessages.keySet()) {
            if (validator == null || validator.test(gamePlayer)) {
                rawMessages.computeIfPresent(gamePlayer, (k, v) -> v + " " + addMessage);
                isDirty = true;
            }
        }
        return this;
    }

    public Message add(Component addMessage) {
        return add(addMessage, null);
    }

    public Message add(String addMessage) {
        return add(addMessage, null);
    }

    private void buildComponentsIfDirty() {
        if (!isDirty) return;
        
        messages.clear();
        LegacyComponentSerializer serializer = LegacyComponentSerializer.builder()
                .hexColors()
                .build();
        
        for (Map.Entry<PlayerIdentity, String> entry : rawMessages.entrySet()) {
            Component component = Component.empty();
            String[] lines = entry.getValue().split("(/newline/|\\n)");
            
            for (int i = 0; i < lines.length; i++) {
                if (i != 0) {
                    component = component.append(Component.newline());
                }
                component = component.append(serializer.deserialize(lines[i]));
            }
            
            messages.put(entry.getKey(), component);
        }
        
        isDirty = false;
    }

    public Component getTranslated() {
        if (getAudienceSize() != 1) {
            return Component.text("§cIncorrect use of 'getTranslated()' – use only for 1 player.");
        }
        buildComponentsIfDirty();
        return messages.values().stream().toList().get(0);
    }

    public String getRawTranslated() {
        if (getAudienceSize() != 1) {
            return "§cIncorrect use of 'getRawTranslated()' – use only for 1 player.";
        }
        return rawMessages.values().stream().toList().get(0);
    }

    public void addToItemLore(ItemBuilder itemBuilder) {
        itemBuilder.addLoreLine(getTranslated());
    }

    public void send() {
        if (getAudienceSize() == 0) return;

        MessageType msgType = MessageType.CHAT;
        String firstRawMessage = rawMessages.values().iterator().next();
        
        for (MessageType type : MessageType.values()) {
            if (firstRawMessage.startsWith(type.getKey())) {
                msgType = type;
                break;
            }
        }

        send(msgType);
    }

    public void send(MessageType msgType) {
        buildComponentsIfDirty();
        
        for (PlayerIdentity recipient : messages.keySet()) {
            Player player = recipient.getOnlinePlayer();
            Component finalMsg = messages.get(recipient);
            if (finalMsg == null) continue;

            switch (msgType) {
                case ACTIONBAR -> ActionBarManager.sendActionBar(player, finalMsg);
                case CHAT -> player.sendMessage(finalMsg);
                case KICK -> player.kick(finalMsg);
                case SUBTITLE -> player.showTitle(Title.title(Component.empty(), finalMsg.replaceText(TextReplacementConfig.builder().match("/subtitle/").replacement("").build())));
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

    private String processPlaceholders(PlayerIdentity gamePlayer, String msg) {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) return msg;

        Player bukkitPlayer = gamePlayer.getOnlinePlayer();
        if (msg.contains("%")) {
            return PlaceholderAPI.setPlaceholders(bukkitPlayer, msg);
        }
        return msg;
    }
}