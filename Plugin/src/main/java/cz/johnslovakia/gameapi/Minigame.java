package cz.johnslovakia.gameapi;

import com.infernalsuite.aswm.loaders.mysql.MysqlLoader;
import cz.johnslovakia.gameapi.datastorage.MinigameTable;
import cz.johnslovakia.gameapi.datastorage.RedisManager;
import cz.johnslovakia.gameapi.game.Game;
import cz.johnslovakia.gameapi.game.cosmetics.CosmeticsManager;
import cz.johnslovakia.gameapi.game.perk.PerkManager;
import cz.johnslovakia.gameapi.listeners.TestServerListener;
import cz.johnslovakia.gameapi.serverManagement.DataManager;
import cz.johnslovakia.gameapi.serverManagement.gameData.JSONProperty;
import cz.johnslovakia.gameapi.users.achievements.AchievementManager;
import cz.johnslovakia.gameapi.levelSystem.LevelManager;
import cz.johnslovakia.gameapi.users.quests.QuestManager;
import cz.johnslovakia.gameapi.users.resources.Resource;
import cz.johnslovakia.gameapi.users.stats.StatsManager;
import cz.johnslovakia.gameapi.utils.InputStreamWithName;
import cz.johnslovakia.gameapi.utils.Logger;
import cz.johnslovakia.gameapi.utils.UpdateChecker;

import lombok.Getter;
import lombok.Setter;

import me.zort.sqllib.SQLConnectionBuilder;
import me.zort.sqllib.SQLDatabaseConnection;
import me.zort.sqllib.api.data.QueryResult;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Getter @Setter
public abstract class Minigame {

    @Getter
    public static Minigame instance;

    private final JavaPlugin plugin;
    private final String name;
    private UpdateChecker updateChecker;
    private MinigameSettings settings;
    private Database database;
    private EndGame endGameFunction;
    private MinigameTable minigameTable;
    private String descriptionTranslateKey;

    private List<JSONProperty> properties = new ArrayList<>();

    private List<InputStreamWithName> languageFiles = new ArrayList<>();
    private List<Resource> economies = new ArrayList<>();


    private CosmeticsManager cosmeticsManager;
    private PerkManager perkManager;
    private StatsManager statsManager;
    private QuestManager questManager;
    private AchievementManager achievementManager;
    private LevelManager levelManager;

    private boolean testServer = false;

    public Minigame(JavaPlugin plugin, String name) {
        instance = this;
        this.plugin = plugin;
        this.name = name;
        this.updateChecker = new UpdateChecker(this, "https://raw.githubusercontent.com/johnslovakia/GameAPI/master/updateChecker/" + name + ".json");

        FileConfiguration config = plugin.getConfig();
        if (config.get("testServer") != null && config.getBoolean("testServer")){
            this.testServer = true;
            Bukkit.getPluginManager().registerEvents(new TestServerListener(), plugin);
        }
    }

    public void addEconomy(Resource... economies){
        assert economies != null;
        for (Resource resource : economies){
            if (!this.economies.contains(resource)){
                this.economies.add(resource);
            }
        }
    }

    public void addLanguage(InputStreamWithName... languages){
        for (InputStreamWithName language : languages){
            if (!this.languageFiles.contains(language)){
                this.languageFiles.add(language);
            }
        }
    }

    public void setDatabase(Database database){
        this.database = database;

        SQLDatabaseConnection connection = database.getConnection();
        if (connection == null){
            return;
        }
        QueryResult result = connection.exec(() ->
                "CREATE TABLE IF NOT EXISTS TestServer ("
                        + "id INT AUTO_INCREMENT PRIMARY KEY," +
                        "Nickname VARCHAR(32)," +
                        "Minigame VARCHAR(64)," +
                        "Version VARCHAR(32)," +
                        "Stars INT," +
                        "Feedback TEXT" +
                        ");");

        if (!result.isSuccessful()) {
            Logger.log("Failed to create TestServer table!", Logger.LogType.ERROR);
            Logger.log(result.getRejectMessage(), Logger.LogType.ERROR);
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
    public abstract Game setupGame(String name);
    public abstract void setupOther();

    public record EndGame(Predicate<Game> validator, Consumer<Game> response) {}

    @Getter
    public class Database {

        private final SQLDatabaseConnection connection;
        private MysqlLoader aswmLoader;

        private final String sqlURL, host, database, username, password;
        private final int port;
        private final boolean useSSL;

        public Database(String sqlURL, String host, String database, String username, String password, int port, boolean useSSL) {
            this.sqlURL = sqlURL;
            this.host = host;
            this.database = database;
            this.username = username;
            this.password = password;
            this.port = port;
            this.useSSL = useSSL;

            this.connection = new SQLConnectionBuilder(host, port, database, username, password).build();
            if (Bukkit.getPluginManager().getPlugin("SlimeWorldPlugin"/*"SlimeWorldManager"*/) != null) {
                try {
                    this.aswmLoader = new MysqlLoader(sqlURL, host, port, database, useSSL, username, password);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
