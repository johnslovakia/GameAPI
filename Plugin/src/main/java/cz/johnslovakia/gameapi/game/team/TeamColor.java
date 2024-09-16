package cz.johnslovakia.gameapi.game.team;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.DyeColor;

public enum TeamColor {

    RED, BLUE, GREEN, YELLOW, AQUA, WHITE, PINK, GRAY;

    public String formattedName() {
        String caps = this.toString().toLowerCase();
        return caps.substring(0, 1).toUpperCase() + caps.substring(1);
    }

    public ChatColor getChatColor() {
        if (this == PINK) {
            return ChatColor.LIGHT_PURPLE;
        }
        return ChatColor.valueOf(this.toString());
    }

    public DyeColor getDyeColor() {
        return switch (this) {
            case RED -> DyeColor.RED;
            case GRAY -> DyeColor.GRAY;
            case GREEN -> DyeColor.LIME;
            case AQUA -> DyeColor.LIGHT_BLUE;
            case BLUE -> DyeColor.BLUE;
            case WHITE -> DyeColor.WHITE;
            case YELLOW -> DyeColor.YELLOW;
            case PINK -> DyeColor.PINK;
        };
    }

    public Color getColor() {
        return getDyeColor().getColor();
    }
}
