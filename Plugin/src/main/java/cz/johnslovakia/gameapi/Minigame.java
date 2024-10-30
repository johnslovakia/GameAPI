package cz.johnslovakia.gameapi;

import com.infernalsuite.aswm.loaders.mysql.MysqlLoader;
import cz.johnslovakia.gameapi.datastorage.MinigameTable;
import cz.johnslovakia.gameapi.economy.Economy;
import cz.johnslovakia.gameapi.game.Game;

import cz.johnslovakia.gameapi.utils.InputStreamWithName;
import lombok.Getter;
import me.zort.sqllib.SQLConnectionBuilder;
import me.zort.sqllib.SQLDatabaseConnection;
import org.bukkit.plugin.Plugin;

import java.sql.SQLException;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public interface Minigame {

    Plugin getPlugin();
    String getMinigameName();
    String getDescriptionTranslateKey();
    List<InputStreamWithName> getLanguageFiles();
    MinigameSettings getSettings();
    List<Economy> getEconomies();
    Database getDatabase();
    MinigameTable getMinigameTable();
    EndGame getEndGameFunction();

    void setupPlayerScores();
    void setupGames();
    void setupOther();

    record EndGame(Predicate<Game> validator, Consumer<Game> response) {}


    @Getter
    class Database {

        SQLDatabaseConnection connection;
        MysqlLoader aswmLoader;

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
            try {
                this.aswmLoader = new MysqlLoader(sqlURL, host, port, database, useSSL, username, password);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
