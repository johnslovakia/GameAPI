package cz.johnslovakia.gameapi.api;

import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

public interface Schematic {

    void pasteSchematic(Plugin plugin, Location pasteLocation, String schematicFileName, String path);

}
