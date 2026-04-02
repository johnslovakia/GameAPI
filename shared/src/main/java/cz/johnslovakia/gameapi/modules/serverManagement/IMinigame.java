package cz.johnslovakia.gameapi.modules.serverManagement;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import cz.johnslovakia.gameapi.modules.game.GameState;
import cz.johnslovakia.gameapi.users.PlayerIdentity;
import lombok.Getter;
import me.zort.sqllib.SQLDatabaseConnection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class IMinigame {

    private final ServerRegistry serverRegistry;
    private final String name;
    private final Map<String, IGame> games = new ConcurrentHashMap<>();

    /** How many seconds without a heartbeat before a game server is considered dead. */
    private static final int STALE_THRESHOLD_SECONDS = 90;

    /** How many seconds between full game-list refreshes from the data source. */
    private static final int GAME_LIST_REFRESH_INTERVAL_SECONDS = 30;

    private Instant lastGameListRefresh;

    public IMinigame(ServerRegistry serverRegistry, String name) {
        this.serverRegistry = serverRegistry;
        this.name = name;
        load();
    }

    public void load() {
        if (serverRegistry.useRedisForServerData()) {
            loadFromRedis();
        } else {
            loadFromMySQL();
        }
        lastGameListRefresh = Instant.now();
    }

    /**
     * Checks if the game list needs refreshing (new arenas added to DB/Redis)
     * and refreshes if enough time has passed. Called automatically from getServersData().
     */
    public void refreshIfNeeded() {
        if (lastGameListRefresh == null ||
                java.time.Duration.between(lastGameListRefresh, Instant.now()).getSeconds() >= GAME_LIST_REFRESH_INTERVAL_SECONDS) {
            load();
        }
    }

    private void loadFromRedis() {
        Set<String> keys = serverRegistry.getServerDataRedis().scanKeys("minigame." + name + ".*");
        for (String key : keys) {
            String[] parts = key.split("\\.");
            if (parts.length >= 3) {
                games.putIfAbsent(parts[2], new IGame(this, parts[2]));
            }
        }
    }

    private void loadFromMySQL() {
        try (SQLDatabaseConnection connection = serverRegistry.getServerDataMySQL().getConnection()) {
            if (connection == null) return;
            String query = "SELECT name FROM games WHERE minigame = ?";
            try (PreparedStatement statement = Objects.requireNonNull(connection.getConnection()).prepareStatement(query)) {
                statement.setString(1, getName());
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        String arenaName = resultSet.getString("name");
                        games.putIfAbsent(arenaName, new IGame(this, arenaName));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the most populated open server, ignoring player limits.
     * Prefer {@link #getBestServer(PlayerIdentity)} to respect full arena permissions.
     */
    public IGame getBestServer() {
        return getServersData().stream()
                .filter(data -> data.getServer().isOpen())
                .max(Comparator.comparingInt(GameData::getPlayers))
                .map(GameData::getServer)
                .orElse(null);
    }

    /**
     * Returns the best available server for the given player,
     * taking full arena permission into account.
     */
    public IGame getBestServer(PlayerIdentity playerIdentity) {
        return getServersData().stream()
                .filter(data -> data.getServer().isAvailableFor(playerIdentity))
                .max(Comparator.comparingInt(GameData::getPlayers))
                .map(GameData::getServer)
                .orElse(null);
    }

    /** Returns all games that the given player can join. */
    public List<IGame> getAvailableGames(PlayerIdentity playerIdentity) {
        List<IGame> available = new ArrayList<>();
        for (IGame game : games.values()) {
            if (game.isAvailableFor(playerIdentity)) {
                available.add(game);
            }
        }
        return available;
    }

    public List<GameData> getServersData() {
        refreshIfNeeded();
        List<GameData> list = new ArrayList<>();
        for (IGame arena : games.values()) {
            GameData data = getGameDataByGame(arena);
            if (data != null) list.add(data);
        }
        return list;
    }

    /** Returns true if any game is currently in an open state, regardless of player count. */
    public boolean isThereFreeGame() {
        return games.values().stream().anyMatch(IGame::isOpen);
    }

    /**
     * Returns true if there is a game available for this player specifically
     * (respects full arena permission check).
     */
    public boolean isThereFreeGameFor(PlayerIdentity playerIdentity) {
        return games.values().stream().anyMatch(game -> game.isAvailableFor(playerIdentity));
    }

    public GameData getGameDataByGame(IGame server) {
        GameData arenaData = server.getData();
        if (arenaData != null && !arenaData.shouldUpdate()) {
            return arenaData;
        }
        if (serverRegistry.useRedisForServerData()) {
            return fetchGameDataFromRedis(server);
        } else {
            return fetchGameDataFromMySQL(server);
        }
    }

    private GameData fetchGameDataFromRedis(IGame server) {
        try {
            String key = "minigame." + name + "." + server.getName();
            String data = serverRegistry.getServerDataRedis().get(key);
            if (data == null) return createDefaultGameData(server);
            JsonObject jsonData = JsonParser.parseString(data).getAsJsonObject();
            return parseGameData(server, jsonData, jsonData);
        } catch (Exception e) {
            e.printStackTrace();
            return server.getData() != null ? server.getData() : createDefaultGameData(server);
        }
    }

    private GameData fetchGameDataFromMySQL(IGame server) {
        String query = "SELECT * FROM games WHERE name = ? LIMIT 1";
        try (SQLDatabaseConnection connection = serverRegistry.getServerDataMySQL().getConnection()) {
            if (connection == null) return createDefaultGameData(server);
            try (PreparedStatement statement = Objects.requireNonNull(connection.getConnection()).prepareStatement(query)) {
                statement.setString(1, server.getName());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        Timestamp lastUpdated = resultSet.getTimestamp("last_updated");
                        if (lastUpdated == null) return createDefaultGameData(server);
                        long diffSeconds = (System.currentTimeMillis() - lastUpdated.getTime()) / 1000;
                        if (diffSeconds >= STALE_THRESHOLD_SECONDS) {
                            return createDefaultGameData(server);
                        }
                        int maxPlayers = resultSet.getInt("max_players");
                        String data = resultSet.getString("data");
                        if (data == null) return createDefaultGameData(server);
                        JsonObject jsonData = JsonParser.parseString(data).getAsJsonObject();
                        return parseGameData(server, jsonData, jsonData, maxPlayers);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return createDefaultGameData(server);
    }

    private GameData parseGameData(IGame server, JsonObject jsonData, JsonObject fullData) {
        return parseGameData(server, jsonData, fullData,
                jsonData.has("MaxPlayers") ? jsonData.get("MaxPlayers").getAsInt() : -1);
    }

    private GameData parseGameData(IGame server, JsonObject jsonData, JsonObject fullData, int maxPlayers) {
        GameData newData = new GameData(server);
        try {
            if (!jsonData.has("GameState") || !jsonData.has("Players")) {
                return createDefaultGameData(server);
            }
            newData.setGameState(GameState.valueOf(jsonData.get("GameState").getAsString()));
            newData.setPlayers(jsonData.get("Players").getAsInt());
            newData.setMaxPlayers(maxPlayers);
            newData.setJsonObject(fullData);
            newData.setLastUpdate(Instant.now());
            server.setData(newData);
        } catch (IllegalArgumentException e) {
            // Invalid GameState enum value
            e.printStackTrace();
            return createDefaultGameData(server);
        } catch (Exception e) {
            e.printStackTrace();
            return createDefaultGameData(server);
        }
        return newData;
    }

    private GameData createDefaultGameData(IGame server) {
        GameData data = new GameData(server);
        data.setGameState(GameState.LOADING);
        data.setPlayers(0);
        data.setMaxPlayers(-1);
        data.setLastUpdate(Instant.now());
        server.setData(data);
        return data;
    }
}