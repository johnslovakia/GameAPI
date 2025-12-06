package cz.johnslovakia.gameapi.modules.messages;

import cz.johnslovakia.gameapi.Shared;
import cz.johnslovakia.gameapi.database.PlayerTable;
import cz.johnslovakia.gameapi.modules.Module;
import cz.johnslovakia.gameapi.users.PlayerIdentity;
import cz.johnslovakia.gameapi.users.PlayerIdentityRegistry;
import cz.johnslovakia.gameapi.utils.InputStreamWithName;
import cz.johnslovakia.gameapi.utils.Logger;

import lombok.Getter;
import me.zort.sqllib.SQLDatabaseConnection;
import me.zort.sqllib.api.data.QueryResult;
import me.zort.sqllib.api.data.Row;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;

public class MessageModule implements Module, Listener {

    private final JavaPlugin plugin;
    private List<FileGroup> fileGroups = new ArrayList<>();

    private Map<String, Map<Language, String>> messages = new HashMap<>();
    private ConcurrentMap<PlayerIdentity, Language> playerLanguages = new ConcurrentHashMap<>();

    public MessageModule(JavaPlugin plugin, List<FileGroup> fileGroups) {
        this.plugin = plugin;
        this.fileGroups = fileGroups;
    }


    @Override
    public void initialize() {
        if (fileGroups.isEmpty()) return;
        try {
            Bukkit.getLogger().log(Level.INFO, "Processing language files...");

            File pluginLanguagesFolder = new File(plugin.getDataFolder(), "languages");
            if (!pluginLanguagesFolder.exists())
                pluginLanguagesFolder.mkdirs();


            for (FileGroup fileGroup : fileGroups) {
                long startTime = System.currentTimeMillis();

                String name = fileGroup.getName();

                File mainFile = new File(pluginLanguagesFolder, name + ".yml");
                if (!mainFile.exists()) mainFile.createNewFile();

                for (InputStream inputStream : fileGroup.getFiles()) {
                    try (InputStream in = inputStream) {
                        File check = File.createTempFile("cFile", ".yml");
                        FileUtils.copyInputStreamToFile(in, check);
                        checkMessages(check, mainFile);
                    }
                }


                loadMessagesFromFile(mainFile);
                Bukkit.getLogger().log(Level.INFO, "Processing of language file " + name + " completed (" + (System.currentTimeMillis() - startTime) + "ms)");
            }
        } catch (IOException e) {
            Logger.log("Something went wrong when retrieving messages! The following message is for Developers: ", Logger.LogType.ERROR);
            e.printStackTrace();
        }
    }


    @Override
    public void terminate() {
        fileGroups = null;
        messages = null;
        playerLanguages = null;
    }

    public void setPlayerLanguage(PlayerIdentity playerIdentity, Language language) {
        setPlayerLanguage(playerIdentity, language, false);
    }

    public void setPlayerLanguage(PlayerIdentity playerIdentity, Language language, boolean message) {
        Bukkit.getScheduler().runTaskAsynchronously(Shared.getInstance().getPlugin(), task -> {
            SQLDatabaseConnection connection = Shared.getInstance().getDatabase().getConnection();
            if (connection == null) {
                return;
            }
            QueryResult result = connection.update()
                    .table(PlayerTable.TABLE_NAME)
                    .set("Language", language.getName())
                    .where().isEqual("Nickname", playerIdentity.getOnlinePlayer().getName())
                    .execute();

            if (result.isSuccessful()) {
                playerLanguages.put(playerIdentity, language);
                if (message)
                    get(playerIdentity, "chat.language.changed")
                            .replace("%language%", StringUtils.capitalize(language.getName()))
                            .send();
            } else {
                if (message)
                    playerIdentity.getOnlinePlayer().sendMessage("§cSomething went wrong. I can't change your language.");
                Logger.log(result.getRejectMessage(), Logger.LogType.ERROR);
            }
        });
    }

    public Language getPlayerLanguage(PlayerIdentity playerIdentity) {
        return playerLanguages.computeIfAbsent(playerIdentity, player -> {
            try {
                Optional<Row> result = Shared.getInstance().getDatabase().getConnection().select()
                        .from(PlayerTable.TABLE_NAME)
                        .where().isEqual("Nickname", player.getOnlinePlayer().getName())
                        .obtainOne();
                return result
                        .map(row -> Language.getLanguage(row.getString("Language")))
                        .orElse(Language.getDefaultLanguage());
            } catch (Exception e) {
                e.printStackTrace();
                return Language.getDefaultLanguage();
            }
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        playerLanguages.remove(PlayerIdentityRegistry.get(e.getPlayer()));
    }

    public void checkMessages(File gFile, File mainFile){
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(gFile), StandardCharsets.UTF_8))) {
            if (!mainFile.exists()/* || mainFile.length() == 0*/) return;

            try (RandomAccessFile raf = new RandomAccessFile(mainFile, "rw")) {
                if (raf.length() != 0) {
                    raf.seek(raf.length() - 1);
                    byte lastByte = raf.readByte();
                    if (lastByte != '\n') {
                        raf.seek(raf.length());
                        raf.write('\n');
                    }
                }
            }

            String line;
            while ((line = br.readLine()) != null) {
                //if (line.trim().isEmpty() || line.trim().startsWith("#")) continue;

                int colonIndex = line.indexOf(':');
                if (colonIndex == -1) continue;

                String key = line.substring(0, colonIndex).trim();
                if (containsKey(mainFile, key)) continue;


                try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(mainFile, true), StandardCharsets.UTF_8))) {
                    writer.append(line).append("\n");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean containsKey(File file, String key) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(key + ":")) {
                    return true;
                }
            }
        }
        return false;
    }

    private void loadMessagesFromFile(File file) {
        String nr = "\n";

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                int colonIndex = line.indexOf(':');
                if (colonIndex != -1) {
                    String key = line.substring(0, colonIndex).trim();
                    String value = line.substring(colonIndex + 1).trim();

                    if (file.getName().contains("scoreboard")){
                        continue;
                    }
                    String languageName = file.getName().replace(".yml", "");
                    Language language = Language.getLanguage(languageName);
                    if (language == null){
                        language = Language.addLanguage(new Language(languageName));
                    }

                    String message = value.replace("\"", "");
                    addMessage(key, language, message);
                }
            }
        } catch (IOException e) {
            Logger.log("An error occurred while loading the file: " + file.getName() + ": " + e.getMessage(), Logger.LogType.WARNING);
        }
    }



    public void addMessage(String name, Language language, String message){
        if (messages.containsKey(name)){
            messages.get(name).put(language, message);
        }else{
            Map<Language, String> map = new HashMap<>();
            map.put(language, message);
            messages.put(name, map);
        }
    }

    public void addMessage(String name, String message){
        Map<Language, String> map = messages.computeIfAbsent(name, k -> new HashMap<>());
        for (Language language : Language.getLanguages()) {
            map.putIfAbsent(language, message);
        }
        messages.put(name, map);
    }

    public Message get(Player player, String key){
        return get(PlayerIdentityRegistry.get(player), key);
    }

    public Message get(PlayerIdentity playerIdentity, String key){
        return get(List.of(playerIdentity), key);
    }

    public Message get(List<? extends PlayerIdentity> audience, String key){
        return new Message(audience, key);
    }

    public String get(Language language, String key){
        if (!existMessage(key)) {
            return "§c" + key;
        } else{
            return messages.get(key).get(language);
        }
    }

    public Map<Language, String> getMessages(String key){
        return messages.get(key);
    }

    public boolean existMessage(String name){
        return messages.get(name) != null;
    }

    public boolean existMessage(Language language, String name){
        if (messages.get(name) == null){
            return false;
        }

        return messages.get(name).get(language) != null;
    }

    public boolean existMessage(PlayerIdentity playerIdentity, String name){
        if (messages.get(name) == null){
            return false;
        }

        return messages.get(name).get(getPlayerLanguage(playerIdentity)) != null;
    }

    public List<String> getMessagesByMSG(String message){
        Map<String, String> messagesMap = new HashMap<>();

        for (String name : messages.keySet()){
            for (Language language : Language.getLanguages()){
                messagesMap.put(name, messages.get(name).get(language));
            }
        }

        for (String name : messagesMap.keySet()){
            if (getMessagesByName(name).contains(message)){
                return getMessagesByName(name);
            }
        }
        return null;
    }

    public List<String> getMessagesByName(String name){
        List<String> messagesList = new ArrayList<>();
        for (Language language : Language.getLanguages()) {
            messagesList.add(messages.get(name).get(language));
        }

        return messagesList;
    }
}