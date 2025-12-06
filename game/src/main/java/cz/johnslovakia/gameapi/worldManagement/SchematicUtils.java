package cz.johnslovakia.gameapi.worldManagement;

import com.fastasyncworldedit.core.FaweAPI;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;

import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;

public class SchematicUtils {

    public static void pasteSchematic(Plugin plugin, Location pasteLocation, String schematicFileName, String path) {
        try {
            File schematicFile = getFile(plugin, schematicFileName, path);
            ClipboardFormat format = ClipboardFormats.findByFile(schematicFile);

            if (format == null) {
                plugin.getLogger().severe("Unsupported schema format.");
                return;
            }

            Clipboard clipboard;
            try (ClipboardReader reader = format.getReader(new FileInputStream(schematicFile))) {
                clipboard = reader.read();
            }


            com.sk89q.worldedit.world.World adaptedWorld = FaweAPI.getWorld(pasteLocation.getWorld().getName());

            try (EditSession editSession = WorldEdit.getInstance().newEditSessionBuilder().world(adaptedWorld).build()) {
                Operation operation = new ClipboardHolder(clipboard)
                        .createPaste(editSession)
                        .to(BlockVector3.at(pasteLocation.getX(), pasteLocation.getY(), pasteLocation.getZ()))
                        .copyEntities(false)
                        .ignoreAirBlocks(true)
                        .build();
                Operations.complete(operation);
            }
        } catch (IOException | WorldEditException e) {
            throw new RuntimeException("An error occurred while pasting the schema.", e);
        }

    }

    private static File getFile(Plugin plugin, String schematicFileName, String path) {
        Path pluginFolderPath = plugin.getDataFolder().toPath().toAbsolutePath().getParent();
        //String schematicFolderPath = pluginFolderPath + File.separator + "MiniUHC" + File.separator + "schematics" + File.separator;
        File schematicFile = new File(pluginFolderPath + File.separator + path + File.separator + schematicFileName + ".schem");
        if (!schematicFile.exists()){
            schematicFile = new File(pluginFolderPath + File.separator + path + File.separator + schematicFileName + ".schematic");
        }
        return schematicFile;
    }
}
