package cz.johnslovakia.gameapi.modules.serverManagement;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import cz.johnslovakia.gameapi.modules.game.GameState;
import cz.johnslovakia.gameapi.users.PlayerIdentity;
import cz.johnslovakia.gameapi.utils.Logger;
import lombok.Getter;
import me.zort.sqllib.SQLDatabaseConnection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

@Getter
public class IMinigame {

    private final ServerRegistry serverRegistry;
    private final String name;
    private final Map<String, IGame> games = new ConcurrentHashMap<>();

    private static final int STALE_THRESHOLD_SECONDS = 90;
    private static final int GAME_LIST_REFRESH_INTERVAL_SECONDS = 30;
    private static final int DATA_REFRESH_INTERVAL_SECONDS = 3;

    private volatile Instant lastGameListRefresh;
    private volatile Instant lastDataRefresh;

    private final AtomicReference<CompletableFuture<Void>> activeDataRefresh = new AtomicReference<>();
    private final AtomicReference<CompletableFuture<Void>> activeGameListRefresh = new AtomicReference<>();

    @Getter
    private volatile boolean initialized = false;

    public IMinigame(ServerRegistry serverRegistry, String name) {
        this.serverRegistry = serverRegistry;
        this.name = name;

        loadGameList()
                .thenCompose(v -> refreshAllGameData())
                .whenComplete((v, ex) -> {
                    initialized = true;
                    if (ex != null) {
                        Logger.log("IMinigame: init failed for " + name + ": " + ex.getMessage(), Logger.LogType.ERROR);
                    }
                });
    }

    private CompletableFuture<Void> tryRefresh(AtomicReference<CompletableFuture<Void>> slot, Supplier<CompletableFuture<Void>> refreshAction) {
        CompletableFuture<Void> existing = slot.get();
        if (existing != null && !existing.isDone()) return existing;

        CompletableFuture<Void> future = new CompletableFuture<>();

        if (!slot.compareAndSet(existing, future)) {
            CompletableFuture<Void> winner = slot.get();
            return winner != null ? winner : CompletableFuture.completedFuture(null);
        }

        refreshAction.get().whenComplete((result, ex) -> {
            if (ex != null) {
                future.completeExceptionally(ex);
            } else {
                future.complete(null);
            }
            slot.compareAndSet(future, null);
        });

        return future;
    }

    private CompletableFuture<Void> loadGameList() {
        return CompletableFuture.runAsync(() -> {
            if (serverRegistry.useRedisForServerData()) {
                loadGameListFromRedis();
            } else {
                loadGameListFromMySQL();
            }
            lastGameListRefresh = Instant.now();
        }).exceptionally(ex -> {
            Logger.log("IMinigame: Failed to load game list for " + name + ": " + ex.getMessage(), Logger.LogType.ERROR);
            return null;
        });
    }

    public CompletableFuture<Void> refreshGameListIfNeeded() {
        if (lastGameListRefresh != null &&
                Duration.between(lastGameListRefresh, Instant.now()).getSeconds() < GAME_LIST_REFRESH_INTERVAL_SECONDS) {
            return CompletableFuture.completedFuture(null);
        }
        return tryRefresh(activeGameListRefresh, this::loadGameList);
    }

    private void loadGameListFromRedis() {
        Set<String> keys = serverRegistry.getServerDataRedis().scanKeys("minigame." + name + ".*");
        for (String key : keys) {
            String[] parts = key.split("\\.");
            if (parts.length >= 3) {
                games.putIfAbsent(parts[2], new IGame(this, parts[2]));
            }
        }
        //Logger.log("IMinigame: Loaded " + games.size() + " games for " + name + " (Redis)", Logger.LogType.DEBUG);
    }

    private void loadGameListFromMySQL() {
        try (SQLDatabaseConnection connection = serverRegistry.getServerDataMySQL().getConnection()) {
            if (connection == null) {
                Logger.log("loadGameListFromMySQL: connection is null for " + name, Logger.LogType.ERROR);
                return;
            }
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
        } catch (Exception e) {
            e.printStackTrace();
        }
        //Logger.log("IMinigame: Loaded " + games.size() + " games for " + name + " (MySQL)", Logger.LogType.DEBUG);
    }

    private CompletableFuture<Void> refreshAllGameData() {
        return CompletableFuture.runAsync(() -> {
            if (serverRegistry.useRedisForServerData()) {
                refreshAllGameDataFromRedis();
            } else {
                refreshAllGameDataFromMySQL();
            }
            lastDataRefresh = Instant.now();
        }).exceptionally(ex -> {
            Logger.log("IMinigame: Failed to refresh game data for " + name + ": " + ex.getMessage(), Logger.LogType.ERROR);
            return null;
        });
    }

    public CompletableFuture<Void> refreshDataIfNeeded() {
        if (lastDataRefresh != null && Duration.between(lastDataRefresh, Instant.now()).getSeconds() < DATA_REFRESH_INTERVAL_SECONDS)
            return CompletableFuture.completedFuture(null);
        return tryRefresh(activeDataRefresh, this::refreshAllGameData);
    }

    private void refreshAllGameDataFromRedis() {
        for (IGame game : games.values()) {
            try {
                String key = "minigame." + name + "." + game.getName();
                String raw = serverRegistry.getServerDataRedis().get(key);
                if (raw == null) {
                    applyDefaultGameData(game);
                    continue;
                }

                JsonObject json = JsonParser.parseString(raw).getAsJsonObject();

                if (json.has("LastUpdated")) {
                    try {
                        long lastUpdatedMs = json.get("LastUpdated").getAsLong();
                        long diffSeconds = (System.currentTimeMillis() - lastUpdatedMs) / 1000;
                        if (diffSeconds >= STALE_THRESHOLD_SECONDS) {
                            applyDefaultGameData(game);
                            continue;
                        }
                    } catch (Exception ignored) {}
                }

                applyParsedGameData(game, json, json);
            } catch (Exception e) {
                Logger.log("IMinigame: Redis fetch failed for " + game.getName() + ": " + e.getMessage(), Logger.LogType.ERROR);
                applyDefaultGameData(game);
            }
        }
    }

    private void refreshAllGameDataFromMySQL() {
        if (games.isEmpty()) return;

        StringJoiner placeholders = new StringJoiner(", ");
        List<String> names = new ArrayList<>();
        for (IGame game : games.values()) {
            placeholders.add("?");
            names.add(game.getName());
        }

        String query = "SELECT * FROM games WHERE name IN (" + placeholders + ")";
        Set<String> fetched = new HashSet<>();

        try (SQLDatabaseConnection connection = serverRegistry.getServerDataMySQL().getConnection()) {
            if (connection == null) {
                Logger.log("refreshAllGameDataFromMySQL: connection is null", Logger.LogType.ERROR);
                games.values().forEach(this::applyDefaultGameData);
                return;
            }

            try (PreparedStatement stmt = Objects.requireNonNull(connection.getConnection()).prepareStatement(query)) {
                for (int i = 0; i < names.size(); i++) {
                    stmt.setString(i + 1, names.get(i));
                }

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String gameName = rs.getString("name");
                        IGame game = games.get(gameName);
                        if (game == null) continue;

                        fetched.add(gameName);

                        Timestamp lastUpdated = rs.getTimestamp("last_updated");
                        if (lastUpdated == null) {
                            applyDefaultGameData(game);
                            continue;
                        }

                        long diffSeconds = (System.currentTimeMillis() - lastUpdated.getTime()) / 1000;
                        if (diffSeconds >= STALE_THRESHOLD_SECONDS) {
                            applyDefaultGameData(game);
                            continue;
                        }

                        int maxPlayers = rs.getInt("max_players");
                        String data = rs.getString("data");
                        if (data == null) {
                            applyDefaultGameData(game);
                            continue;
                        }

                        JsonObject json = JsonParser.parseString(data).getAsJsonObject();
                        applyParsedGameData(game, json, json, maxPlayers);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        for (IGame game : games.values()) {
            if (!fetched.contains(game.getName())) {
                applyDefaultGameData(game);
            }
        }
    }

    private void applyParsedGameData(IGame server, JsonObject jsonData, JsonObject fullData) {
        applyParsedGameData(server, jsonData, fullData,
                jsonData.has("MaxPlayers") ? jsonData.get("MaxPlayers").getAsInt() : -1);
    }

    private void applyParsedGameData(IGame server, JsonObject jsonData, JsonObject fullData, int maxPlayers) {
        try {
            if (!jsonData.has("GameState") || !jsonData.has("Players")) {
                applyDefaultGameData(server);
                return;
            }

            GameData newData = new GameData(server);
            newData.setGameState(GameState.valueOf(jsonData.get("GameState").getAsString()));
            newData.setPlayers(jsonData.get("Players").getAsInt());
            newData.setMaxPlayers(maxPlayers);
            newData.setJsonObject(fullData);
            newData.setLastUpdate(Instant.now());
            server.setData(newData);
        } catch (Exception e) {
            Logger.log("IMinigame: Failed to parse game data for " + server.getName() + ": " + e.getMessage(), Logger.LogType.ERROR);
            applyDefaultGameData(server);
        }
    }

    private void applyDefaultGameData(IGame server) {
        GameData data = new GameData(server);
        data.setGameState(GameState.LOADING);
        data.setPlayers(0);
        data.setMaxPlayers(-1);
        data.setLastUpdate(Instant.now());
        server.setData(data);
    }


    public CompletableFuture<GameData> getGameDataByGame(IGame server) {
        return refreshDataIfNeeded().thenApply(v -> {
            GameData cached = server.getData();
            if (cached != null) return cached;
            applyDefaultGameData(server);
            return server.getData();
        });
    }

    public CompletableFuture<List<GameData>> getServersData() {
        return refreshGameListIfNeeded()
                .thenCompose(v -> refreshDataIfNeeded())
                .thenApply(v -> {
                    List<GameData> list = new ArrayList<>();
                    for (IGame arena : games.values()) {
                        GameData data = arena.getData();
                        if (data != null) list.add(data);
                    }
                    return list;
                });
    }

    public CompletableFuture<IGame> getBestServer() {
        return getServersData().thenApply(dataList ->
                dataList.stream()
                        .filter(d -> d.getServer().isOpenCached())
                        .max(Comparator.comparingInt(GameData::getPlayers))
                        .map(GameData::getServer)
                        .orElse(null)
        );
    }

    public CompletableFuture<IGame> getBestServer(PlayerIdentity playerIdentity) {
        return getServersData().thenApply(dataList ->
                dataList.stream()
                        .filter(d -> d.getServer().isAvailableForCached(playerIdentity))
                        .max(Comparator.comparingInt(GameData::getPlayers))
                        .map(GameData::getServer)
                        .orElse(null)
        );
    }

    public CompletableFuture<List<IGame>> getAvailableGames(PlayerIdentity playerIdentity) {
        return refreshDataIfNeeded().thenApply(v -> {
            List<IGame> available = new ArrayList<>();
            for (IGame game : games.values()) {
                if (game.isAvailableForCached(playerIdentity)) {
                    available.add(game);
                }
            }
            return available;
        });
    }
}