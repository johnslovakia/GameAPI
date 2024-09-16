package cz.johnslovakia.gameapi.api;

import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

public interface Schematic {

    public void pasteSchematic(Plugin plugin, Location pasteLocation, String schematicFileName, String path);
}
