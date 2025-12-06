package cz.johnslovakia.gameapi.modules.levels;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.ChatColor;

public record LevelEvolution(int startLevel, String icon, TextColor color, int itemCustomModelData, int blinkingItemCustomModelData) {

    public Component getIcon(){
        return Component.text("§r§f" + icon)
                .color(NamedTextColor.WHITE)
                .font(Key.key("jsplugins", "level_evolutions"))
                .shadowColor(ShadowColor.shadowColor(0));
    }
}