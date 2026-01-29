package cz.johnslovakia.gameapi;

import cz.johnslovakia.gameapi.database.Database;
import cz.johnslovakia.gameapi.database.MinigameTable;
import cz.johnslovakia.gameapi.database.RedisManager;
import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.game.GameInstance;
import cz.johnslovakia.gameapi.listeners.TestServerListener;
import cz.johnslovakia.gameapi.modules.perks.PerkManager;
import cz.johnslovakia.gameapi.modules.quests.QuestManager;
import cz.johnslovakia.gameapi.modules.serverManagement.DataManager;
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
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Getter @Setter
public abstract class Minigame {

    @Getter
    public static Minigame instance;

    private final JavaPlugin plugin;
    private final String name;
    private final ModuleManager moduleManager;

    private UpdateChecker updateChecker;
    private MinigameSettings settings;
    private Database database;
    private EndGame endGameFunction;
    private MinigameTable minigameTable;
    private String descriptionTranslateKey;

    private List<JSONProperty<GameInstance>> properties = new ArrayList<>();
    private Map<String, InputStreamWithName> languageFiles = new HashMap<>();

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

        this.updateChecker = new UpdateChecker(this, "https://raw.githubusercontent.com/johnslovakia/GameAPI/master/updateChecker/" + name + ".json");

        FileConfiguration config = plugin.getConfig();
        if (config.get("testServer") != null && config.getBoolean("testServer")){
            this.testServer = true;
            Bukkit.getPluginManager().registerEvents(new TestServerListener(), plugin);
        }
    }

    public void updateSettings(Consumer<MinigameSettings.Builder> updater) {
        MinigameSettings.Builder builder = this.settings.toBuilder();
        updater.accept(builder);
        this.settings = builder.build();
    }

    public void addLanguage(String languageName, InputStreamWithName... languages){
        for (InputStreamWithName language : languages){
            this.languageFiles.put(languageName, language);
        }
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
        new DataManager(serverDataMySQL);
        return this;
    }

    //TODO: rewrite
    public Minigame setServerDataRedis(RedisManager serverDataRedis) {
        new DataManager(serverDataRedis);
        return this;
    }

    public abstract void setupPlayerScores();
    public abstract GameInstance setupGame(String name);
    public abstract void setupOther();

    public record EndGame(Predicate<GameInstance> validator, Consumer<GameInstance> response) {}
}
