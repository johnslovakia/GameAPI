package cz.johnslovakia.gameapi.levelSystem;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.ShadowColor;
import org.bukkit.inventory.ItemStack;

public record LevelEvolution(int startLevel, String icon, int itemCustomModelData, int blinkingItemCustomModelData) {

    public Component getIcon(){
        return Component.text("§r§f" + icon)
                .color(NamedTextColor.WHITE)
                .font(Key.key("jsplugins", "level_evolutions"))
                .shadowColor(ShadowColor.shadowColor(0));
    }
}