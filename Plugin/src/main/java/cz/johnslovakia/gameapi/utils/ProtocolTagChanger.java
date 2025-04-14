package cz.johnslovakia.gameapi.utils;

import com.comphenix.protocol.events.*;
import com.google.common.base.Strings;
import com.google.gson.JsonParser;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;
import org.bukkit.event.Listener;
import org.bukkit.event.EventPriority;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.HashMap;
import java.util.Map;

public class ProtocolTagChanger extends PacketAdapter {

    private final Map<String, String> jsonCache = new HashMap<>();
    private final Pattern fontPattern = Pattern.compile("\"font\":\"(.*?)\"");
    private final Pattern textPattern = Pattern.compile("\"text\":\"(.*?)\"");
    private final JsonParser jsonParser = new JsonParser();

    public ProtocolTagChanger(Plugin plugin, ListenerPriority listenerPriority, PacketType packetType) {
        super(plugin, listenerPriority, packetType);
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        PacketContainer packet = event.getPacket();
        if (packet.getType() == PacketType.Play.Server.BOSS) {
            try {
                if (packet.getStructures() != null && packet.getStructures().read(1) != null && packet.getStructures().read(1).getChatComponents() != null) {
                    WrappedChatComponent baseWrap = packet.getStructures().read(1).getChatComponents().read(0);

                    if (baseWrap != null && !Strings.isNullOrEmpty(baseWrap.getJson())) {
                        String originalJson = baseWrap.getJson();

                        // Use cached result if available
                        String newJson = jsonCache.get(originalJson);
                        if (newJson == null) {
                            String text = TextComponent.toLegacyText(ComponentSerializer.parse(originalJson));
                            String font = extractFont(text);
                            String message = extractMessage(text);
                            newJson = convertToJson(message, font);
                            jsonCache.put(originalJson, newJson);
                        }

                        packet.getStructures().read(1).getChatComponents().write(0, WrappedChatComponent.fromJson(newJson));
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }

    public String extractFont(String input) {
        Matcher matcher = fontPattern.matcher(input);
        return matcher.find() ? matcher.group(1) : "minecraft:default";
    }

    public String extractMessage(String input) {
        Matcher matcher = textPattern.matcher(input);
        return matcher.find() ? matcher.group(1) : " ";
    }

    public String convertToJson(String input, String font) {
        StringBuilder jsonOutput = new StringBuilder();
        StringBuilder currentText = new StringBuilder();
        ChatColor currentColor = null;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (c == '§' && i + 1 < input.length()) {
                char colorCode = input.charAt(i + 1);
                ChatColor color = ChatColor.getByChar(colorCode);

                if (!currentText.isEmpty()) {
                    appendJsonPart(jsonOutput, currentText.toString(), currentColor, font);
                    currentText.setLength(0);
                }

                currentColor = color;
                i++;
            } else {
                currentText.append(c);
            }
        }

        if (!currentText.isEmpty()) {
            appendJsonPart(jsonOutput, currentText.toString(), currentColor, font);
        }

        String result = jsonOutput.toString();
        if (result.endsWith(",")) {
            result = result.substring(0, result.length() - 1);
        }

        return "[" + result + "]";
    }

    private void appendJsonPart(StringBuilder jsonOutput, String text, ChatColor color, String font) {
        jsonOutput.append("{\"text\":\"")
                .append(text)
                .append("\",\"color\":\"")
                .append(color != null ? color.name().toLowerCase() : "white")
                .append("\",\"font\":\"")
                .append(font)
                .append("\"},");
    }
}
