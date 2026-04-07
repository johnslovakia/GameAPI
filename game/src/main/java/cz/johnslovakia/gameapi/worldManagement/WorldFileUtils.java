package cz.johnslovakia.gameapi.worldManagement;

import cz.johnslovakia.gameapi.utils.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

public class WorldFileUtils {

    private WorldFileUtils() {}

    public static void deleteFolder(File folder) {
        if (!folder.exists()) return;
        try {
            Files.walk(folder.toPath())
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            Logger.log("Failed to delete folder: " + folder.getAbsolutePath(), Logger.LogType.ERROR);
            e.printStackTrace();
        }
    }
}