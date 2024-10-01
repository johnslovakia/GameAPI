package cz.johnslovakia.gameapi;

import cz.johnslovakia.gameapi.datastorage.MinigameTable;
import cz.johnslovakia.gameapi.economy.Economy;
import cz.johnslovakia.gameapi.game.Game;
import cz.johnslovakia.gameapi.game.kit.KitManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import me.zort.sqllib.SQLDatabaseConnection;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public interface Minigame {

    Plugin getPlugin();
    String getMinigameName();
    String getDescriptionTranslateKey();
    //List<File> getLanguageFiles();
    MinigameSettings getSettings();
    List<Economy> getEconomies();
    SQLDatabaseConnection getDatabase();
    MinigameTable getMinigameTable();
    EndGame getEndGameFunction();

    void setupPlayerScores();
    void setupGames();
    void setupOther();

    public record EndGame(Predicate<Game> validator, Consumer<Game> response) {}
}
