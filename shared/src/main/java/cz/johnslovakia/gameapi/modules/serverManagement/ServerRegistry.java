package cz.johnslovakia.gameapi.modules.serverManagement;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import cz.johnslovakia.gameapi.database.Database;
import cz.johnslovakia.gameapi.database.RedisManager;
import cz.johnslovakia.gameapi.modules.Module;
import cz.johnslovakia.gameapi.modules.serverManagement.gameData.GameDataManager;
import cz.johnslovakia.gameapi.utils.Logger;
import lombok.Getter;
import me.zort.sqllib.SQLDatabaseConnection;
import me.zort.sqllib.api.data.QueryResult;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Getter
public class ServerRegistry implements Module {

    private Database serverDataMySQL;
    private RedisManager serverDataRedis;

    private final List<IMinigame> minigames = new ArrayList<>();

    public ServerRegistry(Database serverDataMySQL) {
        this.serverDataMySQL = serverDataMySQL;
    }

    public ServerRegistry(RedisManager serverDataRedis) {
        this.serverDataRedis = serverDataRedis;
    }

    @Override
    public void initialize() {
        if (!useRedisForServerData()) {
            GameDataManager.createTableIfNotExists(serverDataMySQL);
            createPendingActionsTable();
        }
    }

    @Override
    public void terminate() {}

    public void addMinigame(IMinigame iMinigame) {
        if (minigames.contains(iMinigame)) return;
        minigames.add(iMinigame);
    }

    public Optional<IMinigame> getMinigame(String name) {
        return minigames.stream()
                .filter(m -> m.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    public boolean useRedisForServerData() {
        return serverDataRedis != null;
    }

    private void createPendingActionsTable() {
        String query = "CREATE TABLE IF NOT EXISTS player_pending_actions (" +
                "player VARCHAR(64) NOT NULL, " +
                "server VARCHAR(64) NOT NULL, " +
                "action_type VARCHAR(32) NOT NULL, " +
                "action_data VARCHAR(255), " +
                "expires_at TIMESTAMP NOT NULL, " +
                "PRIMARY KEY (player, server)" +
                ")";
        try (SQLDatabaseConnection connection = serverDataMySQL.getConnection()) {
            if (connection == null) {
                Logger.log("Failed to get connection for creating player_pending_actions table", Logger.LogType.ERROR);
                return;
            }
            QueryResult result = connection.exec(query);
            if (!result.isSuccessful()) {
                Logger.log("Failed to create 'player_pending_actions' table: " + result.getRejectMessage(), Logger.LogType.ERROR);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Pending action TTL in seconds. */
    private static final int PENDING_ACTION_TTL_SECONDS = 120;

    /**
     * Queues a pending action for when the player connects to the given BungeeCord server.
     * Expires after {@value #PENDING_ACTION_TTL_SECONDS} seconds.
     * Overwrites any existing action for the same player+server.
     */
    public void setPendingAction(String playerName, String serverName, PendingServerAction action) {
        if (useRedisForServerData()) {
            String key = "player:" + playerName + ":pendingAction:" + serverName;
            JsonObject json = new JsonObject();
            json.addProperty("type", action.getType().name());
            if (action.getData() != null) json.addProperty("data", action.getData());
            serverDataRedis.set(key, json.toString(), PENDING_ACTION_TTL_SECONDS);
            return;
        }
        String query = "INSERT INTO player_pending_actions (player, server, action_type, action_data, expires_at) " +
                "VALUES (?, ?, ?, ?, DATE_ADD(NOW(), INTERVAL " + PENDING_ACTION_TTL_SECONDS + " SECOND)) " +
                "ON DUPLICATE KEY UPDATE action_type = VALUES(action_type), action_data = VALUES(action_data), " +
                "expires_at = DATE_ADD(NOW(), INTERVAL " + PENDING_ACTION_TTL_SECONDS + " SECOND)";
        try (SQLDatabaseConnection connection = serverDataMySQL.getConnection()) {
            if (connection == null) return;
            try (PreparedStatement stmt = connection.getConnection().prepareStatement(query)) {
                stmt.setString(1, playerName);
                stmt.setString(2, serverName);
                stmt.setString(3, action.getType().name());
                stmt.setString(4, action.getData());
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Reads and removes the pending action for a player on the given server.
     * Call this in {@code PlayerJoinEvent} on the target server.
     * Returns empty if no action exists or it has expired.
     */
    public Optional<PendingServerAction> consumePendingAction(String playerName, String serverName) {
        if (useRedisForServerData()) {
            String key = "player:" + playerName + ":pendingAction:" + serverName;
            String raw = serverDataRedis.get(key);
            if (raw == null) return Optional.empty();
            serverDataRedis.delete(key);
            try {
                JsonObject json = JsonParser.parseString(raw).getAsJsonObject();
                PendingActionType type = PendingActionType.valueOf(json.get("type").getAsString());
                String data = json.has("data") ? json.get("data").getAsString() : null;
                return Optional.of(new PendingServerAction(type, data));
            } catch (Exception e) {
                e.printStackTrace();
                return Optional.empty();
            }
        }

        String selectQuery = "SELECT action_type, action_data FROM player_pending_actions " +
                "WHERE player = ? AND server = ? AND expires_at > NOW()";
        String deleteQuery = "DELETE FROM player_pending_actions WHERE player = ? AND server = ?";

        try (SQLDatabaseConnection connection = serverDataMySQL.getConnection()) {
            if (connection == null) {
                Logger.log("consumePendingAction: database connection is null!", Logger.LogType.ERROR);
                return Optional.empty();
            }

            PendingServerAction result = null;

            try (PreparedStatement stmt = connection.getConnection().prepareStatement(selectQuery)) {
                stmt.setString(1, playerName);
                stmt.setString(2, serverName);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        PendingActionType type = PendingActionType.valueOf(rs.getString("action_type"));
                        String data = rs.getString("action_data");
                        result = new PendingServerAction(type, data);
                    }
                }
            }

            if (result != null) {
                try (PreparedStatement deleteStmt = connection.getConnection().prepareStatement(deleteQuery)) {
                    deleteStmt.setString(1, playerName);
                    deleteStmt.setString(2, serverName);
                    deleteStmt.executeUpdate();
                }
                return Optional.of(result);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }
}