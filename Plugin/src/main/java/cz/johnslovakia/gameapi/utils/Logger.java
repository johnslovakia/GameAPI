package cz.johnslovakia.gameapi.utils;

import static org.bukkit.Bukkit.getServer;

public class Logger {

    public enum LogType {
        INFO, ERROR, WARNING;
    }

    public static void log(String message, LogType type) {
        getServer().getConsoleSender().sendMessage((type.equals(LogType.ERROR) ? "§4" : "") + "MinigameAPI: [" + type.name() + "] " + message);
    }
}