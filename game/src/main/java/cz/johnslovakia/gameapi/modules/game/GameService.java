package cz.johnslovakia.gameapi.modules.game;

import cz.johnslovakia.gameapi.Core;
import cz.johnslovakia.gameapi.modules.Module;
import cz.johnslovakia.gameapi.modules.game.map.GameMap;
import cz.johnslovakia.gameapi.modules.game.runtime.RestartScheduler;
import cz.johnslovakia.gameapi.modules.kits.KitManager;
import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.events.GameResetEvent;
import cz.johnslovakia.gameapi.events.NewArenaEvent;
import cz.johnslovakia.gameapi.modules.levels.LevelModule;
import cz.johnslovakia.gameapi.modules.messages.MessageModule;
import cz.johnslovakia.gameapi.modules.serverManagement.IGame;
import cz.johnslovakia.gameapi.modules.serverManagement.IMinigame;
import cz.johnslovakia.gameapi.modules.serverManagement.ServerRegistry;
import cz.johnslovakia.gameapi.users.PlayerIdentityRegistry;
import cz.johnslovakia.gameapi.users.PlayerManager;
import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.utils.GameUtils;
import cz.johnslovakia.gameapi.utils.Logger;
import cz.johnslovakia.gameapi.worldManagement.WorldManager;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@Getter
@NoArgsConstructor
public class GameService implements Module {

    private Map<String, GameInstance> games = new HashMap<>();
    private List<String> ids = new ArrayList<>();

    @Override
    public void initialize() {}

    @Override
    public void terminate() {
        games = null;
        ids = null;
    }

    public void registerGame(GameInstance game) {
        RestartScheduler scheduler = ModuleManager.getModule(RestartScheduler.class);
        if (scheduler != null && scheduler.isRestartPending()) {
            Logger.log("Game registration blocked — restart pending. (" + game.getID() + ")",
                    Logger.LogType.WARNING);
            return;
        }

        game.finishSetup(success -> {
            if (!success) {
                Logger.log("Game (" + game.getID() + ") setup failed.", Logger.LogType.INFO);
            } else {
                games.put(game.getID(), game);
                Logger.log("Added Game " + game.getID(), Logger.LogType.INFO);
            }
        });
    }

    public void newArena(Player player, boolean sendToLobbyIfNoArena) {
        newArena(player, sendToLobbyIfNoArena, false);
    }

    public void newArena(Player player, boolean sendToLobbyIfNoArena, boolean includeOtherServers) {
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);

        RestartScheduler scheduler = ModuleManager.getModule(RestartScheduler.class);
        if (scheduler != null && scheduler.isRestartPending()) {
            if (isBungeecord()) {
                findBestRemoteServer(gamePlayer).thenAccept(best -> {
                    Bukkit.getScheduler().runTask(Core.getInstance().getPlugin(), () -> {
                        if (best != null) {
                            best.sendPlayerToServer(player);
                        } else {
                            GameUtils.sendToLobby(player, false);
                        }
                    });
                });
                return;
            }
            GameUtils.sendToLobby(player, false);
            return;
        }

        NewArenaEvent ev = new NewArenaEvent(gamePlayer);
        Bukkit.getPluginManager().callEvent(ev);
        if (ev.isCancelled()) return;

        if (includeOtherServers && isBungeecord()) {
            findBestRemoteServer(gamePlayer).thenAccept(best -> {
                Bukkit.getScheduler().runTask(Core.getInstance().getPlugin(), () -> {
                    if (best != null) {
                        Logger.log("newArena(): Sending " + player.getName() + " to server: " + best.getBungeecordServerName(), Logger.LogType.DEBUG);
                        best.sendPlayerToServer(player);
                        Bukkit.getScheduler().runTaskLater(Core.getInstance().getPlugin(), task -> {
                            if (player.isOnline()) newArena(player, true, false);
                        }, 50L);
                    } else {
                        tryLocalOrFallback(player, gamePlayer, sendToLobbyIfNoArena, includeOtherServers);
                    }
                });
            });
            return;
        }

        tryLocalOrFallback(player, gamePlayer, sendToLobbyIfNoArena, includeOtherServers);
    }

    private void tryLocalOrFallback(Player player, GamePlayer gamePlayer, boolean sendToLobbyIfNoArena, boolean includeOtherServers) {
        GameInstance game = getHighestGame(player);

        if (gamePlayer.getParty().isInParty()) {
            for (GamePlayer member : gamePlayer.getParty().getAllOnlinePlayers().stream()
                    .map(p -> (GamePlayer) p)
                    .toList()) {
                if (member.isInGame()) {
                    GameInstance memberGame = member.getGame();
                    if (memberGame.getPlayers().size() >= memberGame.getSettings().getMaxPlayers()
                            && !player.hasPermission("game.joinfullserver")) {
                        ModuleManager.getModule(MessageModule.class).getMessage(player, "chat.party.couldnt_join").send();
                    } else {
                        game = memberGame;
                    }
                    break;
                }
            }
        }

        if (game != null) {
            game.joinPlayer(player);
            return;
        }

        if (includeOtherServers && isBungeecord()) {
            findBestRemoteServer(gamePlayer).thenAccept(best -> {
                Bukkit.getScheduler().runTask(Core.getInstance().getPlugin(), () -> {
                    if (best != null) {
                        best.sendPlayerToServer(player);
                    } else {
                        handleNoArena(player, gamePlayer, sendToLobbyIfNoArena);
                    }
                });
            });
            return;
        }

        handleNoArena(player, gamePlayer, sendToLobbyIfNoArena);
    }

    private void handleNoArena(Player player, GamePlayer gamePlayer, boolean sendToLobbyIfNoArena) {
        if (sendToLobbyIfNoArena) {
            ModuleManager.getModule(MessageModule.class).getMessage(gamePlayer, "chat.sending_to_lobby.no_arena_found").send();
            GameUtils.sendToLobby(player, false);
        } else {
            ModuleManager.getModule(MessageModule.class).getMessage(gamePlayer, "chat.no_arena_found").send();
        }
    }

    private boolean isBungeecord() {
        ServerRegistry registry = ModuleManager.getModule(ServerRegistry.class);
        return registry != null
                && (registry.getServerDataMySQL() != null || registry.getServerDataRedis() != null);
    }

    private CompletableFuture<IGame> findBestRemoteServer(GamePlayer gamePlayer) {
        ServerRegistry registry = ModuleManager.getModule(ServerRegistry.class);
        if (registry == null) return CompletableFuture.completedFuture(null);

        String currentServer = Minigame.getInstance().getSettings().getServerName();

        Optional<IMinigame> minigameOpt = registry.getMinigame(Minigame.getInstance().getFullName());
        return minigameOpt.map(iMinigame -> iMinigame.getBestServer(gamePlayer).thenApply(server -> {
            if (server != null && !server.getBungeecordServerName().equalsIgnoreCase(currentServer)) {
                return server;
            }
            return null;
        })).orElseGet(() -> CompletableFuture.completedFuture(null));

    }

    public GameInstance getHighestGame(Player player) {
        GameInstance highest = null;
        if (getGames().isEmpty()) return null;

        for (GameInstance game : getGames().values()) {
            if (!game.getState().equals(GameState.STARTING) && !game.getState().equals(GameState.WAITING)
                    && !(game.getState().equals(GameState.INGAME) && game.getSettings().isEnabledJoiningAfterStart())) {
                continue;
            }
            if (game.getPlayers().size() >= game.getSettings().getMaxPlayers()
                    && !player.hasPermission("game.joinfullserver")) continue;

            if (highest == null || game.getPlayers().size() > highest.getPlayers().size()) {
                highest = game;
            }
        }

        return highest;
    }

    public void resetGame(GameInstance toResetGame) {
        toResetGame.setState(GameState.RESTARTING);

        RestartScheduler scheduler = ModuleManager.getModule(RestartScheduler.class);
        boolean restartPending = scheduler != null && scheduler.isRestartPending();

        if (toResetGame.getSettings().isRestartServerAfterEnd()
                && Minigame.getInstance().getSettings().getGamesPerServer() == 1) {

            ServerRegistry dataManager = ModuleManager.getModule(ServerRegistry.class);
            boolean bungeecord = dataManager != null &&
                    (dataManager.getServerDataMySQL() != null || dataManager.getServerDataRedis() != null);

            if (bungeecord) {
                Optional<IMinigame> iMinigame = dataManager.getMinigame(Minigame.getInstance().getFullName());
                if (iMinigame.isPresent()) {
                    List<CompletableFuture<Void>> transfers = new ArrayList<>();

                    for (GamePlayer gamePlayer : toResetGame.getParticipants()) {
                        CompletableFuture<Void> transfer = iMinigame.get().getBestServer(gamePlayer)
                                .thenAccept(bestServer -> {
                                    Bukkit.getScheduler().runTask(Core.getInstance().getPlugin(), () -> {
                                        if (bestServer != null && !bestServer.getBungeecordServerName()
                                                .equalsIgnoreCase(Minigame.getInstance().getSettings().getServerName())) {
                                            bestServer.sendPlayerToServer(gamePlayer.getOnlinePlayer());
                                        } else {
                                            GameUtils.sendToLobby(gamePlayer.getOnlinePlayer());
                                        }
                                    });
                                });
                        transfers.add(transfer);
                    }

                    CompletableFuture.allOf(transfers.toArray(new CompletableFuture[0]))
                            .whenComplete((v, ex) -> {
                                Bukkit.getScheduler().runTaskLater(Minigame.getInstance().getPlugin(), task -> {
                                    if (!Bukkit.getOnlinePlayers().isEmpty()) {
                                        Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage("§cRestarting server..."));
                                    }
                                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "restart");
                                }, 80L);
                            });
                }
            }
            return;
        }

        KitManager.removeKitManager(toResetGame);

        String gameName = toResetGame.getName();
        games.remove(toResetGame.getID());

        List<Player> players = new ArrayList<>();
        if (!toResetGame.getParticipants().isEmpty()) {
            toResetGame.getParticipants().forEach(gamePlayer -> {
                if (gamePlayer.getOnlinePlayer().isOnline()) {
                    players.add(gamePlayer.getOnlinePlayer());
                } else {
                    LevelModule levelModule = ModuleManager.getModule(LevelModule.class);
                    if (levelModule != null) {
                        levelModule.getCache().remove(gamePlayer.getName());
                    }
                    PlayerIdentityRegistry.unregister(gamePlayer.getUniqueId());
                }
            });
        }

        GameInstance newGame = null;
        if (!restartPending) {
            newGame = Minigame.getInstance().setupGame(gameName);
        } else {
            Logger.log("[RestartScheduler] Skipping new game creation for '" + gameName + "' — restart pending.", Logger.LogType.INFO);
        }

        GameResetEvent ev = new GameResetEvent(toResetGame, newGame);
        Bukkit.getPluginManager().callEvent(ev);

        if (!players.isEmpty()) {
            for (Player p : players) {
                ModuleManager.getModule(MessageModule.class).getMessage(p, "chat.finding_new_game").send();
                Bukkit.getScheduler().runTaskLater(Minigame.getInstance().getPlugin(), task -> {
                    newArena(p, true, true);
                }, 25L);
            }
        }

        if (toResetGame.getCurrentMap() != null) {
            GameMap mapToUnload = toResetGame.getCurrentMap();
            World worldToUnload = (mapToUnload != null) ? mapToUnload.getWorld() : null;

            Bukkit.getScheduler().runTaskLater(Minigame.getInstance().getPlugin(), task -> {
                try {
                    if (worldToUnload != null) {
                        WorldManager.unload(worldToUnload);
                    } else {
                        Logger.log("resetGame: Skipping world deletion. The world will be deleted on restart.", Logger.LogType.WARNING);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                toResetGame.terminate();
            }, 70L);
        } else {
            Logger.log("resetGame: getCurrentMap() is null! Skipping world deletion. The world will be deleted on restart.", Logger.LogType.WARNING);
        }
    }

    public Optional<GameInstance> getGameByID(String id) {
        return Optional.ofNullable(games.get(id));
    }

    public Optional<GameInstance> getGameByName(String name) {
        return games.values().stream().filter(g -> g.getName().equalsIgnoreCase(name)).findAny();
    }

    public Optional<GameInstance> getGameByNameOrID(String nameOrID) {
        return games.values().stream()
                .filter(g -> g.getName().equalsIgnoreCase(nameOrID) || g.getID().equalsIgnoreCase(nameOrID))
                .findAny();
    }

    public List<GameInstance> getFreeGames(Player player) {
        return games.values().stream()
                .filter(game -> isAvailableFor(player, game))
                .toList();
    }

    public boolean isAvailableFor(Player player, GameInstance gameInstance) {
        boolean isFull = gameInstance.getPlayers().size() >= gameInstance.getSettings().getMaxPlayers();
        GameState state = gameInstance.getState();

        if (state == GameState.WAITING || state == GameState.STARTING) {
            return !isFull || player.hasPermission("game.joinfullserver");
        }

        if (state == GameState.INGAME) {
            return gameInstance.getSettings().isEnabledJoiningAfterStart() && !isFull;
        }

        return false;
    }

    public void addID(String id) {
        if (!ids.contains(id)) {
            ids.add(id);
        }
    }

    public boolean isDuplicate(String id) {
        return ids.contains(id);
    }
}