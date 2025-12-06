package cz.johnslovakia.gameapi.modules.game.team;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.DyeColor;

public enum TeamColor {

    RED, BLUE, GREEN, YELLOW, AQUA, DARK_AQUA, WHITE, PINK, GRAY, PURPLE, DARK_GREEN, ORANGE;

    public String formattedName() {
        String name = this.toString().toLowerCase();
        name = name.substring(0, 1).toUpperCase() + name.substring(1);
        return StringUtils.capitalize(name.replace("_", " "));
    }

    public ChatColor getChatColor() {
        if (this == PINK) {
            return ChatColor.LIGHT_PURPLE;
        }else if(this == PURPLE){
            return ChatColor.DARK_PURPLE;
        }else if (this == ORANGE){
            return ChatColor.GOLD;
        }
        return ChatColor.valueOf(this.toString());
    }

    public TextColor getTextColor() {
        return switch (this) {
            case RED -> NamedTextColor.RED;
            case GRAY -> NamedTextColor.GRAY;
            case GREEN -> NamedTextColor.GREEN;
            case AQUA -> NamedTextColor.AQUA;
            case DARK_AQUA -> NamedTextColor.DARK_AQUA;
            case BLUE -> NamedTextColor.BLUE;
            case WHITE -> NamedTextColor.WHITE;
            case YELLOW -> NamedTextColor.YELLOW;
            case PINK -> NamedTextColor.LIGHT_PURPLE;
            case PURPLE -> NamedTextColor.DARK_PURPLE;
            case DARK_GREEN -> NamedTextColor.DARK_GREEN;
            case ORANGE -> NamedTextColor.GOLD;
        };
    }

    public DyeColor getDyeColor() {
        return switch (this) {
            case RED -> DyeColor.RED;
            case GRAY -> DyeColor.GRAY;
            case GREEN -> DyeColor.LIME;
            case AQUA -> DyeColor.LIGHT_BLUE;
            case DARK_AQUA -> DyeColor.CYAN;
            case BLUE -> DyeColor.BLUE;
            case WHITE -> DyeColor.WHITE;
            case YELLOW -> DyeColor.YELLOW;
            case PINK -> DyeColor.PINK;
            case PURPLE -> DyeColor.PURPLE;
            case DARK_GREEN -> DyeColor.GREEN;
            case ORANGE -> DyeColor.ORANGE;
        };
    }

    public Color getColor() {
        return getDyeColor().getColor();
    }
}
