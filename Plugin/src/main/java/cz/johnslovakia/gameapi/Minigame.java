package cz.johnslovakia.gameapi;

import com.infernalsuite.aswm.loaders.mysql.MysqlLoader;
import cz.johnslovakia.gameapi.datastorage.MinigameTable;
import cz.johnslovakia.gameapi.datastorage.RedisManager;
import cz.johnslovakia.gameapi.game.Game;
import cz.johnslovakia.gameapi.serverManagement.DataManager;
import cz.johnslovakia.gameapi.serverManagement.gameData.GameDataManager;
import cz.johnslovakia.gameapi.serverManagement.gameData.JSONProperty;
import cz.johnslovakia.gameapi.users.resources.Resource;


import cz.johnslovakia.gameapi.utils.InputStreamWithName;
import cz.johnslovakia.gameapi.utils.UpdateChecker;
import lombok.Getter;
import lombok.Setter;
import me.zort.sqllib.SQLConnectionBuilder;
import me.zort.sqllib.SQLDatabaseConnection;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Getter @Setter
public abstract class Minigame {

    private final Plugin plugin;
    private final String name;
    private UpdateChecker updateChecker;
    private MinigameSettings settings;
    private Database database;
    private EndGame endGameFunction;
    private MinigameTable minigameTable;
    private String descriptionTranslateKey;

    private Database serverDataMySQL;
    private RedisManager serverDataRedis;
    private DataManager dataManager;
    private List<JSONProperty> properties = new ArrayList<>();

    private List<InputStreamWithName> languageFiles = new ArrayList<>();
    private List<Resource> economies = new ArrayList<>();

    public Minigame(Plugin plugin, String name) {
        this.plugin = plugin;
        this.name = name;
        this.updateChecker = new UpdateChecker(this, "https://raw.githubusercontent.com/johnslovakia/GameAPI/master/updateChecker/" + name + ".json");
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

    public boolean useRedisForServerData(){
        return dataManager != null && serverDataRedis != null;
    }

    public Minigame setServerDataMySQL(Database serverDataMySQL) {
        this.serverDataMySQL = serverDataMySQL;
        this.dataManager = new DataManager(this);
        return this;
    }

    public Minigame setServerDataRedis(RedisManager serverDataRedis) {
        this.serverDataRedis = serverDataRedis;
        this.dataManager = new DataManager(this);
        return this;
    }

    public abstract void setupPlayerScores();
    public abstract void setupGames();
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
