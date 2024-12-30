package cz.johnslovakia.gameapi.utils;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.google.common.base.Strings;

import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

//https://www.spigotmc.org/threads/boss-bar-and-textcomponent.484472/
//https://github.com/zippo-store/HypeGradients/blob/cb70c9b583714b88539b43362c9300d0288e4e92/HypeGradients-API/src/main/java/me/doublenico/hypegradients/api/packet/AbstractPacket.java#L24

//TODO: zkusit udělat lépe a nebude fungovat §l myslím
public class ProtocolTagChanger extends PacketAdapter {

    public ProtocolTagChanger(Plugin plugin, ListenerPriority listenerPriority, PacketType packetType) {
        super(plugin, listenerPriority, packetType);
    }


    @Override
    public void onPacketSending(PacketEvent event) {

        PacketContainer packet = event.getPacket();
        if (packet.getType() == PacketType.Play.Server.BOSS) {
            try {
                if (packet.getStructures() != null || packet.getStructures().read(1) != null || packet.getStructures().read(1).getChatComponents() != null) {
                    WrappedChatComponent baseWrap = packet.getStructures().read(1).getChatComponents().read(0);

                    if (baseWrap != null && !Strings.isNullOrEmpty(baseWrap.getJson())) {
                        String text = TextComponent.toLegacyText(ComponentSerializer.parse(baseWrap.getJson()));
                        //Logger.log("1 - " + text, Logger.LogType.INFO);
                        String font = extractFont(text);
                        //Logger.log("2 - " + font, Logger.LogType.INFO);
                        String message = extractMessage(text);
                        //Logger.log("3 - " + message, Logger.LogType.INFO);
                        String json = convertToJson(message, font);

                        packet.getStructures().read(1).getChatComponents().write(0, WrappedChatComponent.fromJson(json));
                    }
                }
            }catch (Exception ignored){}
        }
    }

    public static String extractFont(String input) {
        Pattern pattern = Pattern.compile("\"font\":\"(.*?)\"");
        Matcher matcher = pattern.matcher(input);

        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return "minecraft:default";
        }
    }

    public static String extractMessage(String input) {
        Pattern pattern = Pattern.compile("\"text\":\"(.*?)\"");
        Matcher matcher = pattern.matcher(input);

        if (matcher.find()) {
            return matcher.group(1);
        } else {
            Logger.log("ProtocolTagChanger: Text not found (input: " + input + ")", Logger.LogType.WARNING);
            return " ";
        }
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
                    jsonOutput.append("{\"text\":\"")
                            .append(currentText.toString())
                            .append("\",\"color\":\"")
                            .append(currentColor != null ? currentColor.name().toLowerCase() : "white")
                            .append("\",\"font\":\"")
                            .append(font)
                            .append("\"},");
                    currentText.setLength(0);
                }

                currentColor = color;
                i++;
            } else {
                currentText.append(c);
            }
        }

        if (currentText.length() > 0) {
            jsonOutput.append("{\"text\":\"")
                    .append(currentText.toString())
                    .append("\",\"color\":\"")
                    .append(currentColor != null ? currentColor.name().toLowerCase() : "white")
                    .append("\",\"font\":\"")
                    .append(font)
                    .append("\"}");
        }

        String result = jsonOutput.toString();
        if (result.endsWith(",")) {
            result = result.substring(0, result.length() - 1);
        }

        return "[" + result + "]";
    }


}