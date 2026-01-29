package cz.johnslovakia.gameapi.modules.serverManagement;

import cz.johnslovakia.gameapi.Shared;
import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.game.GameState;
import cz.johnslovakia.gameapi.modules.messages.MessageModule;
import cz.johnslovakia.gameapi.users.PlayerIdentity;
import cz.johnslovakia.gameapi.utils.Utils;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.scheduler.BukkitRunnable;

@Getter
public class IGame {

    private final IMinigame minigame;
    private final String name;

    @Setter @Getter(AccessLevel.PACKAGE)
    private GameData gameData;

    public IGame(IMinigame minigame, String name) {
        this.minigame = minigame;
        this.name = name;
    }

    public boolean isOpen() {
        GameData gameData = minigame.getGameDataByGame(this);
        if (gameData == null) return false;

        if (minigame.getName().toLowerCase().contains("minianni")) {
            if (gameData.getGameState().equals(GameState.INGAME)) {
                if (gameData.getJsonObject() != null && gameData.getJsonObject().has("Phase")) {
                    int phase = gameData.getJsonObject().get("Phase").getAsInt();
                    return phase == 1 || phase == 2;
                }
            }
        }

        return gameData.getGameState().equals(GameState.WAITING)
                || gameData.getGameState().equals(GameState.STARTING);
    }

    public String getBungeecordServerName() {
        if (name.isEmpty()) return name;

        char lastChar = name.charAt(name.length() - 1);
        if (!Character.isDigit(lastChar)) {
            return name.substring(0, name.length() - 1);
        }
        return name;
    }

    public boolean isMultiArena() {
        if (name.isEmpty()) return false;
        return !Character.isDigit(name.charAt(name.length() - 1));
    }

    public String getArenaID() {
        if (name.isEmpty()) return "";
        return String.valueOf(name.charAt(name.length() - 1));
    }

    public void sendPlayerToServer(PlayerIdentity playerIdentity) {
        String server = getBungeecordServerName();

        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    if (isMultiArena()) {
                        setPlayerArenaPreference(playerIdentity);
                    }
                    Utils.sendToServer(playerIdentity.getOnlinePlayer(), server);
                } catch (Exception exception) {
                    ModuleManager.getModule(MessageModule.class)
                            .get(playerIdentity, "chat.something_wrong.new_game")
                            .send();
                    exception.printStackTrace();
                }
            }
        }.runTaskAsynchronously(Shared.getInstance().getPlugin());
    }

    private void setPlayerArenaPreference(PlayerIdentity playerIdentity) {
        String playerName = playerIdentity.getOnlinePlayer().getName();

        if (DataManager.getInstance().useRedisForServerData()) {
            String key = "player:" + playerName + ":game";
            minigame.getDataManager().getServerDataRedis().set(key, getArenaID(), 60);
        } else {
            // TODO: MySQL implementace
            // String query = "INSERT INTO player_arena_preference (player, arena, expires_at) VALUES (?, ?, DATE_ADD(NOW(), INTERVAL 60 SECOND)) ON DUPLICATE KEY UPDATE arena=?, expires_at=DATE_ADD(NOW(), INTERVAL 60 SECOND)";
        }
    }
}