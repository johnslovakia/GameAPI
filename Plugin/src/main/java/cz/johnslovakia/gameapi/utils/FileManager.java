package cz.johnslovakia.gameapi.utils;

import java.io.*;

public class FileManager {

    public static void copyFolder(File src, File destination) throws IOException {
        if (src.isDirectory()) {

            if (!destination.exists()) {
                destination.mkdir();
            }

            String files[] = src.list();

            if (files != null) {
                for (String file : files) {
                    File srcFile = new File(src, file);
                    File destFile = new File(destination, file);

                    copyFolder(srcFile, destFile);
                }
            }
        } else {
            InputStream in = new FileInputStream(src);
            OutputStream out = new FileOutputStream(destination);

            byte[] buffer = new byte[1024];

            int length;

            while ((length = in.read(buffer)) > 0){
                out.write(buffer, 0, length);
            }

            in.close();
            out.close();
        }
    }

    public static void deleteFile(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files == null) return;
            for (File child : files) {
                deleteFile(child);
            }
        }

        file.delete();
    }
}
