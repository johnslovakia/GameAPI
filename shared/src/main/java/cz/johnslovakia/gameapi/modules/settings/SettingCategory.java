package cz.johnslovakia.gameapi.modules.settings;

import org.bukkit.Material;
import org.bukkit.entity.Player;

public interface SettingCategory {
    String getName();
    Material getIcon();
    String[] getLore();
    void open(Player player);
}