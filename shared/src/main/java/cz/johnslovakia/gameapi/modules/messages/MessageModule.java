package cz.johnslovakia.gameapi.modules.messages;

import cz.johnslovakia.gameapi.Core;
import cz.johnslovakia.gameapi.database.PlayerTable;
import cz.johnslovakia.gameapi.modules.Module;
import cz.johnslovakia.gameapi.users.PlayerIdentity;
import cz.johnslovakia.gameapi.users.PlayerIdentityRegistry;
import cz.johnslovakia.gameapi.utils.Logger;

import me.zort.sqllib.SQLDatabaseConnection;
import me.zort.sqllib.api.data.QueryResult;
import me.zort.sqllib.api.data.Row;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.jar.JarFile;
import java.util.logging.Level;

public class MessageModule implements Module, Listener {

    private final JavaPlugin plugin;
    private List<FileGroup> fileGroups;

    private Map<String, Map<Language, String>> messages = new HashMap<>();
    private ConcurrentMap<PlayerIdentity, Language> playerLanguages = new ConcurrentHashMap<>();

    public MessageModule(JavaPlugin plugin, List<FileGroup> fileGroups) {
        this.plugin = plugin;
        this.fileGroups = fileGroups;
    }

    public MessageModule(JavaPlugin plugin, FileGroup... fileGroups) {
        this.plugin = plugin;
        this.fileGroups = Arrays.asList(fileGroups);
    }

    /**
     * Discovers all {@code .yml} language files in {@code resourceDir} inside the JAR loaded by
     * {@code classLoader} and returns one {@link FileGroup} per file.
     *
     * <p>This is the preferred way to initialise {@link MessageModule} in a Lobby plugin or any
     * plugin that only needs GameAPI's built-in messages:</p>
     * <pre>{@code
     * List<FileGroup> groups = MessageModule.discoverFileGroups(
     *         GameAPI.class.getClassLoader(), "gLanguages");
     * moduleManager.registerModule(new MessageModule(plugin, groups));
     * }</pre>
     *
     * @param classLoader the classloader whose JAR is scanned (e.g. {@code GameAPI.class.getClassLoader()})
     * @param resourceDir the resource directory to scan (e.g. {@code "gLanguages"})
     * @return list of file groups, one per discovered language file
     */
    public static List<FileGroup> discoverFileGroups(ClassLoader classLoader, String resourceDir) {
        List<FileGroup> groups = new ArrayList<>();
        String prefix = resourceDir.endsWith("/") ? resourceDir : resourceDir + "/";
        try {
            URL url = classLoader.getResource(resourceDir);
            if (url == null) return groups;

            URI uri = url.toURI();
            if ("jar".equals(uri.getScheme())) {
                String ssp = uri.getRawSchemeSpecificPart(); // file:/path/to/plugin.jar!/dir
                String filePart = ssp.substring("file:".length(), ssp.indexOf("!"));
                try (JarFile jar = new JarFile(new File(URI.create("file:" + filePart).getPath()))) {
                    jar.stream()
                            .filter(e -> e.getName().startsWith(prefix) && e.getName().endsWith(".yml"))
                            .forEach(entry -> {
                                // Use only the base file name to prevent any path traversal from entry names.
                                String langName = new File(entry.getName()).getName().replace(".yml", "");
                                if (langName.isEmpty() || !langName.matches("[a-zA-Z0-9_\\-]+")) return;
                                InputStream stream = classLoader.getResourceAsStream(entry.getName());
                                if (stream != null) groups.add(new FileGroup(langName, stream));
                            });
                }
            }
        } catch (Exception e) {
            // If scanning fails, return whatever was found so far.
            Logger.log("Failed to auto-scan resource directory '" + resourceDir + "': " + e.getMessage(), Logger.LogType.WARNING);
        }
        return groups;
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
                        syncNewMessages(check, mainFile);
                        check.delete();
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
        Bukkit.getScheduler().runTaskAsynchronously(Core.getInstance().getPlugin(), task -> {
            if (Core.getInstance().getDatabase() == null) return;

            try (SQLDatabaseConnection connection = Core.getInstance().getDatabase().getConnection()) {
                if (connection == null) return;

                QueryResult result = connection.update()
                        .table(PlayerTable.TABLE_NAME)
                        .set("Language", language.getName())
                        .where().isEqual("Nickname", playerIdentity.getOnlinePlayer().getName())
                        .execute();

                if (result.isSuccessful()) {
                    playerLanguages.put(playerIdentity, language);
                    if (message) {
                        getMessage(playerIdentity, "chat.language.changed")
                                .replace("%language%", StringUtils.capitalize(language.getName()))
                                .send();
                    }
                } else {
                    if (message) {
                        playerIdentity.getOnlinePlayer().sendMessage("§cSomething went wrong. I can't change your language.");
                    }
                    Logger.log(result.getRejectMessage(), Logger.LogType.ERROR);
                }

            } catch (Exception e) {
                Logger.log("Failed to set language for " + playerIdentity.getOnlinePlayer().getName() + ": " + e.getMessage(), Logger.LogType.ERROR);
                e.printStackTrace();
            }
        });
    }

    public Language getPlayerLanguage(PlayerIdentity playerIdentity) {
        return playerLanguages.computeIfAbsent(playerIdentity, player -> {
            try (SQLDatabaseConnection connection = Core.getInstance().getDatabase().getConnection()) {
                if (connection == null) return Language.getDefaultLanguage();

                Optional<Row> result = connection.select()
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

    /**
     * Copies any message keys (and their associated comment/blank lines) from {@code resourceFile}
     * that are not yet present in {@code serverFile}. Preserves comment lines ({@code #}) and
     * blank lines from the resource file so the server file stays organised.
     *
     * @param resourceFile temporary file copied from plugin resources
     * @param serverFile   the live language file in {@code plugins/<plugin>/languages/}
     */
    public void syncNewMessages(File resourceFile, File serverFile) {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(resourceFile), StandardCharsets.UTF_8))) {

            if (!serverFile.exists()) return;

            // Ensure the server file ends with a newline before appending.
            try (RandomAccessFile raf = new RandomAccessFile(serverFile, "rw")) {
                if (raf.length() != 0) {
                    raf.seek(raf.length() - 1);
                    if (raf.readByte() != '\n') {
                        raf.seek(raf.length());
                        raf.write('\n');
                    }
                }
            }

            // Buffer lines from the resource file so we can write comment/blank lines
            // that precede a new key together with that key.
            List<String> pendingLines = new ArrayList<>();
            String line;
            boolean firstLine = true;

            while ((line = br.readLine()) != null) {
                if (firstLine) {
                    line = stripBOM(line);
                    firstLine = false;
                }

                String trimmed = line.trim();

                // Accumulate comment and blank lines; they will be flushed together with
                // the next new key so the server file keeps the same visual grouping.
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    pendingLines.add(line);
                    continue;
                }

                int colonIndex = line.indexOf(':');
                if (colonIndex == -1) {
                    // Not a key=value line and not a comment — skip.
                    pendingLines.clear();
                    continue;
                }

                String key = line.substring(0, colonIndex).trim();

                if (containsKey(serverFile, key)) {
                    // Key already present: discard buffered comments for this key.
                    pendingLines.clear();
                    continue;
                }

                // New key: write the buffered comment/blank lines followed by the key line.
                try (BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(new FileOutputStream(serverFile, true), StandardCharsets.UTF_8))) {
                    for (String pending : pendingLines) {
                        writer.append(pending).append("\n");
                    }
                    writer.append(line).append("\n");
                }
                pendingLines.clear();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean containsKey(File file, String key) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            String line;
            boolean firstLine = true;
            while ((line = reader.readLine()) != null) {
                if (firstLine) {
                    line = stripBOM(line);
                    firstLine = false;
                }
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                if (line.startsWith(key + ":")) {
                    return true;
                }
            }
        }
        return false;
    }

    private void loadMessagesFromFile(File file) {
        try (BufferedReader br = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            String line;
            boolean firstLine = true;
            while ((line = br.readLine()) != null) {
                if (firstLine) {
                    line = stripBOM(line);
                    firstLine = false;
                }

                String trimmed = line.trim();
                // Skip blank lines and comments.
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;

                int colonIndex = line.indexOf(':');
                if (colonIndex == -1) continue;

                String key = line.substring(0, colonIndex).trim();
                String value = line.substring(colonIndex + 1).trim();

                if (file.getName().contains("scoreboard")) continue;

                String languageName = file.getName().replace(".yml", "");
                Language language = Language.getLanguage(languageName);
                if (language == null) {
                    language = Language.addLanguage(new Language(languageName));
                }

                String message = value.replace("\"", "");
                addMessage(key, language, message);
            }
        } catch (IOException e) {
            Logger.log("An error occurred while loading the file: " + file.getName() + ": " + e.getMessage(), Logger.LogType.WARNING);
        }
    }

    // -------------------------------------------------------------------------
    // Message storage
    // -------------------------------------------------------------------------

    public void addMessage(String name, Language language, String message) {
        if (messages.containsKey(name)) {
            messages.get(name).put(language, message);
        } else {
            Map<Language, String> map = new HashMap<>();
            map.put(language, message);
            messages.put(name, map);
        }
    }

    public void addMessage(String name, String message) {
        List<Language> langs = Language.getLanguages();
        if (langs.isEmpty()) return;
        Map<Language, String> map = messages.computeIfAbsent(name, k -> new HashMap<>());
        for (Language language : langs) {
            map.putIfAbsent(language, message);
        }
    }

    // -------------------------------------------------------------------------
    // Retrieve messages
    // -------------------------------------------------------------------------

    /**
     * Returns a {@link Message} builder for the given player.
     */
    public Message getMessage(Player player, String key) {
        return getMessage(PlayerIdentityRegistry.get(player), key);
    }

    /**
     * Returns a {@link Message} builder for the given player identity.
     */
    public Message getMessage(PlayerIdentity playerIdentity, String key) {
        return getMessage(List.of(playerIdentity), key);
    }

    /**
     * Returns a {@link Message} builder for the given audience.
     */
    public Message getMessage(List<? extends PlayerIdentity> audience, String key) {
        return new Message(audience, key);
    }

    /**
     * Returns the raw translated string for the given language and key,
     * or a red error string if the key does not exist.
     */
    public String getTranslation(Language language, String key) {
        if (!hasMessage(key)) {
            return "§c" + key;
        }
        return messages.get(key).get(language);
    }

    /**
     * Returns all translations for the given key, keyed by language.
     */
    public Map<Language, String> getMessages(String key) {
        return messages.get(key);
    }

    // -------------------------------------------------------------------------
    // Existence checks
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} if any translation exists for the given key.
     */
    public boolean hasMessage(String name) {
        return messages.get(name) != null;
    }

    /**
     * Returns {@code true} if a translation for the given key exists in the given language.
     */
    public boolean hasMessage(Language language, String name) {
        if (messages.get(name) == null) return false;
        return messages.get(name).get(language) != null;
    }

    /**
     * Returns {@code true} if a translation for the given key exists in the player's language.
     */
    public boolean hasMessage(PlayerIdentity playerIdentity, String name) {
        if (messages.get(name) == null) return false;
        return messages.get(name).get(getPlayerLanguage(playerIdentity)) != null;
    }

    // -------------------------------------------------------------------------
    // Lookup utilities
    // -------------------------------------------------------------------------

    /**
     * Finds all translation values for the key whose value matches the given message string.
     *
     * @return the list of translations for the matching key, or {@code null} if not found
     */
    public List<String> findKeyByValue(String message) {
        for (String name : messages.keySet()) {
            List<String> translations = getAllTranslations(name);
            if (translations != null && translations.contains(message)) {
                return translations;
            }
        }
        return null;
    }

    /**
     * Returns all translation values (one per registered language) for the given key.
     */
    public List<String> getAllTranslations(String name) {
        if (!hasMessage(name)) return null;
        List<String> list = new ArrayList<>();
        for (Language language : Language.getLanguages()) {
            list.add(messages.get(name).get(language));
        }
        return list;
    }

    // -------------------------------------------------------------------------
    // File utilities
    // -------------------------------------------------------------------------

    /**
     * Removes the given message key from every language file in
     * {@code plugins/<plugin>/languages/}. This is useful in migration actions
     * when a key needs to be reset to its resource default or permanently deleted.
     *
     * @param key the message key to remove
     */
    public void removeMessageFromFiles(String key) {
        File languagesFolder = new File(plugin.getDataFolder(), "languages");
        if (!languagesFolder.exists()) return;

        File[] files = languagesFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            removeKeyFromFile(file, key);
        }
    }

    private void removeKeyFromFile(File file, String key) {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            String line;
            boolean firstLine = true;
            while ((line = reader.readLine()) != null) {
                if (firstLine) {
                    line = stripBOM(line);
                    firstLine = false;
                }
                // Keep the line unless it starts with the key followed by a colon.
                if (line.startsWith(key + ":")) {
                    continue;
                }
                lines.add(line);
            }
        } catch (IOException e) {
            Logger.log("Failed to read " + file.getName() + " for key removal: " + e.getMessage(), Logger.LogType.ERROR);
            return;
        }

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file, false), StandardCharsets.UTF_8))) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            Logger.log("Failed to write " + file.getName() + " after key removal: " + e.getMessage(), Logger.LogType.ERROR);
        }
    }

    // -------------------------------------------------------------------------
    // Deprecated / compatibility aliases
    // -------------------------------------------------------------------------

    /** @deprecated Use {@link #getMessage(Player, String)} instead. */
    @Deprecated
    public Message get(Player player, String key) {
        return getMessage(player, key);
    }

    /** @deprecated Use {@link #getMessage(PlayerIdentity, String)} instead. */
    @Deprecated
    public Message get(PlayerIdentity playerIdentity, String key) {
        return getMessage(playerIdentity, key);
    }

    /** @deprecated Use {@link #getMessage(List, String)} instead. */
    @Deprecated
    public Message get(List<? extends PlayerIdentity> audience, String key) {
        return getMessage(audience, key);
    }

    /** @deprecated Use {@link #getTranslation(Language, String)} instead. */
    @Deprecated
    public String get(Language language, String key) {
        return getTranslation(language, key);
    }

    /** @deprecated Use {@link #hasMessage(String)} instead. */
    @Deprecated
    public boolean existMessage(String name) {
        return hasMessage(name);
    }

    /** @deprecated Use {@link #hasMessage(Language, String)} instead. */
    @Deprecated
    public boolean existMessage(Language language, String name) {
        return hasMessage(language, name);
    }

    /** @deprecated Use {@link #hasMessage(PlayerIdentity, String)} instead. */
    @Deprecated
    public boolean existMessage(PlayerIdentity playerIdentity, String name) {
        return hasMessage(playerIdentity, name);
    }

    /** @deprecated Use {@link #findKeyByValue(String)} instead. */
    @Deprecated
    public List<String> getMessagesByMSG(String message) {
        return findKeyByValue(message);
    }

    /** @deprecated Use {@link #getAllTranslations(String)} instead. */
    @Deprecated
    public List<String> getMessagesByName(String name) {
        return getAllTranslations(name);
    }

    /** @deprecated Use {@link #syncNewMessages(File, File)} instead. */
    @Deprecated
    public void checkMessages(File gFile, File mainFile) {
        syncNewMessages(gFile, mainFile);
    }

    // -------------------------------------------------------------------------
    // DB-backed messages (TODO)
    // -------------------------------------------------------------------------
    // TODO: Add support for storing messages in the database so server owners with
    //       multiple servers don't have to edit language files on every server.
    //       Suggested approach:
    //         1. Add an optional flag (e.g. dbBackedMessages = true in MinigameSettings).
    //         2. On startup, query JSConfigs (or a dedicated `game_messages` table) for
    //            all messages of this plugin+language combination.
    //         3. If rows are found, use them instead of (or to override) file-based messages.
    //         4. Changes in DB then propagate to all servers on next reload.
    //         5. Provide an admin command to push the current file-based messages to DB.

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private static String stripBOM(String line) {
        if (line != null && !line.isEmpty() && line.charAt(0) == '\uFEFF') {
            return line.substring(1);
        }
        return line;
    }
}