package cz.johnslovakia.gameapi.modules.scoreboard;

import cz.johnslovakia.gameapi.modules.game.GameState;
import lombok.Getter;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Getter
final class ScoreboardTemplate {

    private final String language;
    private final Map<GameState, String> titles = new EnumMap<>(GameState.class);
    private final Map<GameState, List<String>> lines = new EnumMap<>(GameState.class);

    private ScoreboardTemplate(String language) {
        this.language = language;
    }

    static ScoreboardTemplate load(String language, YamlConfiguration config, String section) {
        ScoreboardTemplate template = new ScoreboardTemplate(language);

        for (GameState state : GameState.values()) {
            String stateKey = findStateKey(config, section, state);
            if (stateKey == null) {
                continue;
            }

            String title = config.getString(section + "." + stateKey + ".title");
            if (title != null) {
                template.titles.put(state, title);
            }

            String linesPath = section + "." + stateKey + ".lines";
            if (config.contains(linesPath)) {
                template.lines.put(state, config.getStringList(linesPath));
            }
        }

        return template;
    }

    String getTitle(GameState state) {
        return titles.get(state);
    }

    List<String> getLines(GameState state) {
        return lines.get(state);
    }

    private static String findStateKey(YamlConfiguration config, String section, GameState state) {
        for (String key : stateKeys(state)) {
            if (config.contains(section + "." + key)) {
                return key;
            }
        }
        return null;
    }

    private static List<String> stateKeys(GameState state) {
        return switch (state) {
            case LOADING -> List.of("Loading", "LOADING", "loading");
            case WAITING -> List.of("Waiting", "WAITING", "waiting");
            case STARTING -> List.of("Starting", "STARTING", "starting");
            case INGAME -> List.of("InGame", "Ingame", "INGAME", "ingame", "In_Game");
            case ENDING -> List.of("Ending", "ENDING", "ending");
            case RESTARTING -> List.of("Restarting", "RESTARTING", "restarting");
        };
    }
}
