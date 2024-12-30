package cz.johnslovakia.gameapi.serverManagement.gameData;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.game.Game;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class JSONProperty {

    @Getter
    private String property;
    private UpdatedValueInterface valueInterface;

    public JSONProperty(String property, UpdatedValueInterface valueInterface) {
        this.property = property;
        this.valueInterface = valueInterface;
    }


    public void update(Game game, String value) {
        Minigame minigame = GameAPI.getInstance().getMinigame();
        if (minigame.useRedisForServerData()){
            game.getServerDataManager().updateGame();
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(GameAPI.getInstance(), task -> {
            String query = "UPDATE games SET data = JSON_SET(data, '$." + property + "', ?), last_updated = CURRENT_TIMESTAMP WHERE name = ?";

            PreparedStatement statement = null;
            try {
                statement = minigame.getServerDataMySQL().getConnection().getConnection().prepareStatement(query);
                statement.setString(1, value);
                statement.setString(2, game.getName());
                statement.executeUpdate();
                statement.execute();
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                if (statement != null) {
                    try {
                        statement.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public void update(Game game, Integer value) {
        Minigame minigame = GameAPI.getInstance().getMinigame();
        if (minigame.useRedisForServerData()){
            game.getServerDataManager().updateGame();
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(GameAPI.getInstance(), task -> {
            String query = "UPDATE games SET data = JSON_SET(data, '$." + property + "', ?), last_updated = CURRENT_TIMESTAMP WHERE name = ?";

            PreparedStatement statement = null;
            try {
                statement = minigame.getServerDataMySQL().getConnection().getConnection().prepareStatement(query);
                statement.setInt(1, value);
                statement.setString(2, game.getName());
                statement.executeUpdate();
                statement.execute();
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                if (statement != null) {
                    try {
                        statement.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public void update(Game game, Double value) {
        Minigame minigame = GameAPI.getInstance().getMinigame();
        if (minigame.useRedisForServerData()){
            game.getServerDataManager().updateGame();
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(GameAPI.getInstance(), task -> {
            String query = "UPDATE games SET data = JSON_SET(data, '$." + property + "', ?), last_updated = CURRENT_TIMESTAMP WHERE name = ?";

            PreparedStatement statement = null;
            try {
                statement = minigame.getServerDataMySQL().getConnection().getConnection().prepareStatement(query);
                statement.setDouble(1, value);
                statement.setString(2, game.getName());
                statement.executeUpdate();
                statement.execute();
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                if (statement != null) {
                    try {
                        statement.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public void update(Game game, Boolean value) {
        Minigame minigame = GameAPI.getInstance().getMinigame();
        if (minigame.useRedisForServerData()){
            game.getServerDataManager().updateGame();
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(GameAPI.getInstance(), task -> {
            String query = "UPDATE games SET data = JSON_SET(data, '$." + property + "', ?), last_updated = CURRENT_TIMESTAMP WHERE name = ?";

            PreparedStatement statement = null;
            try {
                statement = minigame.getServerDataMySQL().getConnection().getConnection().prepareStatement(query);
                statement.setBoolean(1, value);
                statement.setString(2, game.getName());
                statement.executeUpdate();
                statement.execute();
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                if (statement != null) {
                    try {
                        statement.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public UpdatedValueInterface getUpdatedValueInterface() {
        return valueInterface;
    }
}
