package cz.johnslovakia.gameapi.modules.serverManagement;

import cz.johnslovakia.gameapi.Core;
import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.game.GameState;
import cz.johnslovakia.gameapi.modules.messages.MessageModule;
import cz.johnslovakia.gameapi.users.PlayerIdentity;
import cz.johnslovakia.gameapi.utils.Logger;
import cz.johnslovakia.gameapi.utils.Utils;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.CompletableFuture;

@Getter
public class IGame {

    private final IMinigame minigame;
    private final String name;

    @Setter(AccessLevel.PACKAGE)
    private volatile GameData data;

    public IGame(IMinigame minigame, String name) {
        this.minigame = minigame;
        this.name = name;
    }

    public CompletableFuture<Boolean> isOpen() {
        return minigame.getGameDataByGame(this).thenApply(data -> {
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
        });
    }

    public CompletableFuture<Boolean> isAvailableFor(PlayerIdentity playerIdentity) {
        return isOpen().thenCompose(open -> {
            if (!open) return CompletableFuture.completedFuture(false);

            return minigame.getGameDataByGame(this).thenApply(data -> {
                if (data == null) return false;
                if (data.getMaxPlayers() > 0 && data.getPlayers() >= data.getMaxPlayers()) {
                    Player onlinePlayer = playerIdentity.getOnlinePlayer();
                    if (onlinePlayer == null) return false;
                    return onlinePlayer.hasPermission("game.joinfullarena");
                }
                return true;
            });
        });
    }

    boolean isOpenCached() {
        GameData d = data;
        if (d == null) return false;

        if (minigame.getName().toLowerCase().contains("minianni")) {
            if (d.getGameState().equals(GameState.INGAME)) {
                if (d.getJsonObject() != null && d.getJsonObject().has("Phase")) {
                    int phase = d.getJsonObject().get("Phase").getAsInt();
                    return phase == 1 || phase == 2;
                }
            }
        }

        return d.getGameState().equals(GameState.WAITING)
                || d.getGameState().equals(GameState.STARTING);
    }

    boolean isAvailableForCached(PlayerIdentity playerIdentity) {
        if (!isOpenCached()) return false;
        GameData d = data;
        if (d == null) return false;
        if (d.getMaxPlayers() > 0 && d.getPlayers() >= d.getMaxPlayers()) {
            Player onlinePlayer = playerIdentity.getOnlinePlayer();
            if (onlinePlayer == null) return false;
            return onlinePlayer.hasPermission("game.joinfullarena");
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

    public CompletableFuture<GameData> getGameData() {
        return minigame.getGameDataByGame(this);
    }

    public CompletableFuture<GameState> getGameState() {
        return getGameData().thenApply(gd -> gd != null ? gd.getGameState() : GameState.LOADING);
    }

    public CompletableFuture<Integer> getPlayers() {
        return getGameData().thenApply(gd -> gd != null ? gd.getPlayers() : 0);
    }

    public void spectateGame(Player player) {
        sendPlayerToServer(player, new PendingServerAction(PendingActionType.SPECTATE, "game:" + getName()));
    }

    public void sendPlayerToServer(Player player) {
        sendPlayerToServer(player, null);
    }

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

                    Bukkit.getScheduler().runTaskLater(Core.getInstance().getPlugin(), task -> {
                        if (player.isOnline()) {
                            Logger.log("sendPlayerToServer(): Transfer failed for " + player.getName(), Logger.LogType.WARNING);
                        }
                    }, 50L);
                } catch (Exception e) {
                    MessageModule messageModule = ModuleManager.getModule(MessageModule.class);
                    if (messageModule != null) {
                        messageModule.getMessage(player, "chat.something_wrong.new_game").send();
                    }
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(Core.getInstance().getPlugin());
    }
}