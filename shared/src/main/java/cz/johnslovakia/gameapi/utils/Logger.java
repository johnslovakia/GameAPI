package cz.johnslovakia.gameapi.utils;

import cz.johnslovakia.gameapi.Shared;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Logger {

    public enum LogType {
        INFO, WARNING, ERROR, DEBUG
    }

    private static final Map<String, BufferedWriter> writers = new ConcurrentHashMap<>();

    public static void log(String message, LogType type) {
        String pluginName = resolveCallingPluginName();
        String prefixColor = switch (type) {
            case ERROR -> "§4";
            case WARNING -> "§6";
            default -> "§7";
        };
        String formattedMessage = "[" + type.name() + "] " + message;

        Bukkit.getConsoleSender().sendMessage(prefixColor + pluginName + ": " + formattedMessage);

        /*if (type == LogType.ERROR || type == LogType.WARNING) {
            logToFileAsync(pluginName, formattedMessage);
        }*/
    }

    private static void logToFileAsync(String pluginName, String message) {
        Bukkit.getScheduler().runTaskAsynchronously(Shared.getInstance().getPlugin(), () -> {
            try {
                BufferedWriter writer = getWriterForPlugin(pluginName);
                writer.write("[" + LocalDateTime.now() + "] " + message);
                writer.newLine();
                writer.flush();
            } catch (IOException e) {
                Bukkit.getConsoleSender().sendMessage("§c[Logger] Chyba při zápisu logu pro plugin " + pluginName);
            }
        });
    }

    private static BufferedWriter getWriterForPlugin(String pluginName) throws IOException {
        if (writers.containsKey(pluginName)) return writers.get(pluginName);

        Plugin plugin = Shared.getInstance().getPlugin();
        if (plugin == null) throw new FileNotFoundException("Plugin " + pluginName + " nebyl nalezen");

        File logDir = new File(plugin.getDataFolder(), "logs");
        if (!logDir.exists()) logDir.mkdirs();

        closeAllWriters();
        rotateLogs(logDir);

        String fileName = "log-" + LocalDate.now() + "-" + System.currentTimeMillis() + ".log";
        File logFile = new File(logDir, fileName);
        BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true));
        writers.put(pluginName, writer);
        return writer;
    }

    private static void rotateLogs(File logDir) {
        File[] files = logDir.listFiles((dir, name) -> name.endsWith(".log"));
        if (files == null || files.length <= 10) return;

        Arrays.sort(files, Comparator.comparingLong(File::lastModified));
        for (int i = 0; i < files.length - 10; i++) {
            try {
                Files.delete(files[i].toPath());
            } catch (IOException e) {
                Bukkit.getConsoleSender().sendMessage("§c[Logger] Nelze smazat log: " + files[i].getName());
            }
        }
    }

    private static String resolveCallingPluginName() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        for (StackTraceElement element : stack) {
            try {
                Class<?> cls = Class.forName(element.getClassName());
                Plugin plugin = JavaPlugin.getProvidingPlugin(cls);
                if (plugin != null && !plugin.getName().equalsIgnoreCase("GameAPI")) {
                    return plugin.getName();
                }
            } catch (Exception ignored) {}
        }
        return "GameAPI";
    }

    public static void closeAllWriters() {
        for (BufferedWriter writer : writers.values()) {
            try {
                writer.flush();
                writer.close();
            } catch (IOException ignored) {}
        }
        writers.clear();
    }
}