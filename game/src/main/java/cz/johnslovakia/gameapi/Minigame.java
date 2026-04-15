package cz.johnslovakia.gameapi;

import cz.johnslovakia.gameapi.database.Database;
import cz.johnslovakia.gameapi.database.MinigameTable;
import cz.johnslovakia.gameapi.database.RedisManager;
import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.game.GameInstance;
import cz.johnslovakia.gameapi.listeners.TestServerListener;
import cz.johnslovakia.gameapi.modules.updateTasks.UpdateTaskModule;
import cz.johnslovakia.gameapi.modules.perks.PerkManager;
import cz.johnslovakia.gameapi.modules.quests.QuestManager;
import cz.johnslovakia.gameapi.modules.serverManagement.IMinigame;
import cz.johnslovakia.gameapi.modules.serverManagement.ServerRegistry;
import cz.johnslovakia.gameapi.modules.serverManagement.gameData.JSONProperty;
import cz.johnslovakia.gameapi.utils.InputStreamWithName;
import cz.johnslovakia.gameapi.utils.Logger;
import cz.johnslovakia.gameapi.utils.UpdateChecker;

import cz.johnslovakia.gameapi.worldManagement.SlimeWorldLoader;
import lombok.Getter;
import lombok.Setter;

import me.zort.sqllib.SQLDatabaseConnection;
import me.zort.sqllib.api.data.QueryResult;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.net.URISyntaxException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.jar.JarFile;

@Getter @Setter
public abstract class Minigame {

    @Getter
    public static Minigame instance;

    private final JavaPlugin plugin;
    private final String name;
    private String fullName;
    private final ModuleManager moduleManager;

    private UpdateChecker updateChecker;
    private MinigameSettings settings;
    private Database database;
    private EndGame endGameFunction;
    private MinigameTable minigameTable;
    private String descriptionTranslateKey;

    private List<JSONProperty<GameInstance>> properties = new ArrayList<>();
    private Map<String, InputStreamWithName> languageFiles = new HashMap<>();

    @Getter
    private UpdateTaskModule migrationManager;

    @Setter
    private QuestManager questManager;
    @Setter
    private PerkManager perkManager;

    private boolean testServer = false;

    public Minigame(JavaPlugin plugin, String name) {
        instance = this;
        this.plugin = plugin;
        this.name = name;
        this.moduleManager = new ModuleManager(plugin);
        this.migrationManager = new UpdateTaskModule(plugin);

        this.updateChecker = new UpdateChecker(this, "https://raw.githubusercontent.com/johnslovakia/GameAPI/master/updateChecker/" + name + ".json");

        FileConfiguration config = plugin.getConfig();
        if (config.get("testServer") != null && config.getBoolean("testServer")){
            this.testServer = true;
            Bukkit.getPluginManager().registerEvents(new TestServerListener(), plugin);
        }
    }

    public boolean hasSpectatePermission(Player player){
        String permission = getName().toLowerCase() + ".spectate";
        return player.hasPermission(permission);
    }

    public void updateSettings(Consumer<MinigameSettings.Builder> updater) {
        MinigameSettings.Builder builder = this.settings.toBuilder();
        updater.accept(builder);
        this.settings = builder.build();
    }

    /**
     * Manually registers a language file for this minigame.
     * Auto-detection via JAR scanning is preferred; only use this for edge cases.
     *
     * @deprecated Languages are now auto-detected from the {@code languages/} directory in the
     *             plugin's JAR. You only need to call this if auto-detection does not find your file.
     */
    @Deprecated
    public void addLanguage(String languageName, InputStreamWithName... languages){
        for (InputStreamWithName language : languages){
            this.languageFiles.put(languageName, language);
        }
    }

    /**
     * Returns the set of language names available for this minigame.
     *
     * <p>Names are discovered by scanning the {@code languages/} directory inside the plugin's JAR.
     * Any languages registered via the now-deprecated {@link #addLanguage} are included as well.</p>
     *
     * @return a mutable, ordered set of language names (e.g. {@code "english"}, {@code "czech"})
     */
    public Set<String> detectLanguageNames() {
        Set<String> names = new LinkedHashSet<>(languageFiles.keySet());

        try {
            File jarFile = new File(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
            try (JarFile jar = new JarFile(jarFile)) {
                jar.stream()
                        .filter(entry -> entry.getName().startsWith("languages/") && entry.getName().endsWith(".yml") && !entry.getName().contains("scoreboard"))
                        .map(entry -> new File(entry.getName()).getName().replace(".yml", ""))
                        .filter(n -> !n.isEmpty() && n.matches("[a-zA-Z0-9_\\-]+"))
                        .forEach(names::add);
            }
        } catch (URISyntaxException e) {
            Logger.log("Could not resolve plugin JAR path for language auto-detection: " + e.getMessage(), Logger.LogType.WARNING);
        } catch (Exception e) {
            Logger.log("Could not auto-scan languages from plugin JAR: " + e.getMessage(), Logger.LogType.WARNING);
        }

        return names;
    }

    public void setDatabase(Database database) {
        this.database = database;

        if (database == null) {
            return;
        }

        if (Bukkit.getPluginManager().getPlugin("SlimeWorldPlugin") != null) {
            new SlimeWorldLoader(database.getAswmLoader());
        }

        try (SQLDatabaseConnection connection = database.getConnection()) {
            if (connection == null) {
                return;
            }

            QueryResult result = connection.exec(() ->
                    "CREATE TABLE IF NOT EXISTS TestServer (" +
                            "id INT AUTO_INCREMENT PRIMARY KEY," +
                            "Nickname VARCHAR(32)," +
                            "Minigame VARCHAR(64)," +
                            "Version VARCHAR(32)," +
                            "Stars INT," +
                            "Feedback TEXT" +
                            ");"
            );

            if (!result.isSuccessful()) {
                Logger.log("Failed to create TestServer table!", Logger.LogType.ERROR);
                Logger.log(result.getRejectMessage(), Logger.LogType.ERROR);
            }
        }
    }

    //TODO: rewrite
    public Minigame setServerDataMySQL(Database serverDataMySQL) {
        ServerRegistry registry = moduleManager.registerModule(new ServerRegistry(serverDataMySQL));
        registry.addMinigame(new IMinigame(registry, getFullName()));
        return this;
    }

    //TODO: rewrite
    public Minigame setServerDataRedis(RedisManager serverDataRedis) {
        ServerRegistry registry = moduleManager.registerModule(new ServerRegistry(serverDataRedis));
        registry.addMinigame(new IMinigame(registry, getFullName()));
        //moduleManager.registerModule(new ServerRegistry(serverDataRedis));
        return this;
    }


    public String getFullName(){
        return fullName == null ? name : fullName;
    }

    public abstract void setupPlayerScores();
    public abstract GameInstance setupGame(String name);
    public abstract void setupOther();

    public record EndGame(Predicate<GameInstance> validator, Consumer<GameInstance> response) {}
}
