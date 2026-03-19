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
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

@Getter
public class IGame {

    private final IMinigame minigame;
    private final String name;

    @Setter (AccessLevel.PACKAGE)
    private GameData data;

    public IGame(IMinigame minigame, String name) {
        this.minigame = minigame;
        this.name = name;
    }

    /** Returns true if the game is in a joinable state (waiting or starting), ignoring player count. */
    public boolean isOpen() {
        GameData data = minigame.getGameDataByGame(this);
        if (data == null) return false;

        if (minigame.getName().toLowerCase().contains("minianni")) {
            if (data.getGameState().equals(GameState.INGAME)) {
                if (data.getJsonObject() != null && data.getJsonObject().has("Phase")) {
                    int phase = data.getJsonObject().get("Phase").getAsInt();
                    return phase == 1 || phase == 2;
                }
            }
        }

        return data.getGameState().equals(GameState.WAITING)
                || data.getGameState().equals(GameState.STARTING);
    }

    /**
     * Returns true if this game is joinable for the given player.
     * If the arena is full, requires the {@code game.joinfullarena} permission.
     */
    public boolean isAvailableFor(PlayerIdentity playerIdentity) {
        if (!isOpen()) return false;
        GameData data = minigame.getGameDataByGame(this);
        if (data == null) return false;
        if (data.getMaxPlayers() > 0 && data.getPlayers() >= data.getMaxPlayers()) {
            return playerIdentity.getOnlinePlayer().hasPermission("game.joinfullarena");
        }
        return true;
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

    public GameData getGameData(){
        return minigame.getGameDataByGame(this);
    }

    public GameState getGameState(){
        return getGameData().getGameState();
    }

    public int getPlayers(){
        return getGameData().getPlayers();
    }

    public void spectateGame(Player player){
        sendPlayerToServer(player, new PendingServerAction(PendingActionType.SPECTATE, "game:" + getName()));
    }

    /** Sends the player to this game's server with no pending action. */
    public void sendPlayerToServer(Player player) {
        sendPlayerToServer(player, null);
    }

    /**
     * Sends the player to this game's server, optionally with a pending action.
     * For multi-arena servers a {@link PendingActionType#JOIN_ARENA} action is queued automatically
     * so the target server knows which arena to put the player in.
     * You can pass an additional action (e.g. {@link PendingActionType#SPECTATE}) that overrides this.
     *
     * @param pendingAction optional action to execute when the player connects. Can be null.
     */
    public void sendPlayerToServer(Player player, PendingServerAction pendingAction) {
        String server = getBungeecordServerName();

        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    PendingServerAction actionToQueue = pendingAction;

                    if (actionToQueue == null && isMultiArena()) {
                        actionToQueue = new PendingServerAction(PendingActionType.JOIN_ARENA, getArenaID());
                    }

                    ServerRegistry serverRegistry = ModuleManager.getModule(ServerRegistry.class);
                    if (actionToQueue != null && serverRegistry != null) {
                        serverRegistry.setPendingAction(
                                player.getName(),
                                server,
                                actionToQueue
                        );
                    }

                    Utils.sendToServer(player, server);
                } catch (Exception e) {
                    ModuleManager.getModule(MessageModule.class)
                            .get(player, "chat.something_wrong.new_game")
                            .send();
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(Shared.getInstance().getPlugin());
    }
}