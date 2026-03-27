package cz.johnslovakia.gameapi.modules.game;

import cz.johnslovakia.gameapi.modules.Module;
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
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

@Getter
@NoArgsConstructor
public class GameService implements Module {

    private Map<String, GameInstance> games = new HashMap<>();
    private List<String> ids = new ArrayList<>();

    @Override
    public void initialize() {

    }

    @Override
    public void terminate() {
        games = null;
        ids = null;
    }

    public void registerGame(GameInstance game){
        RestartScheduler scheduler = ModuleManager.getModule(RestartScheduler.class);
        if (scheduler != null && scheduler.isRestartPending()) {
            Logger.log("Game registration blocked — restart pending. (" + game.getID() + ")",
                       Logger.LogType.WARNING);
            return;
        }

        game.finishSetup(success -> {
            if (!success){
                Logger.log("Game (" + game.getID() + ") setup failed.", Logger.LogType.INFO);
            }else{
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
            ServerRegistry dataManager = ModuleManager.getModule(ServerRegistry.class);
            boolean bungeecord = dataManager != null &&
                    (dataManager.getServerDataMySQL() != null || dataManager.getServerDataRedis() != null);
            if (bungeecord) {
                Optional<IMinigame> iMinigame = dataManager.getMinigame(Minigame.getInstance().getName());
                if (iMinigame.isPresent()) {
                    IGame bestServer = iMinigame.get().getBestServer(gamePlayer);
                    if (!bestServer.getBungeecordServerName()
                            .equalsIgnoreCase(Minigame.getInstance().getSettings().getServerName())) {
                        bestServer.sendPlayerToServer(player);
                        return;
                    }
                }
            }

            GameUtils.sendToLobby(player, false);
            return;
        }

        NewArenaEvent ev = new NewArenaEvent(PlayerManager.getGamePlayer(player));
        Bukkit.getPluginManager().callEvent(ev);

        if (ev.isCancelled()) return;

        ServerRegistry dataManager = ModuleManager.getModule(ServerRegistry.class);
        boolean bungeecord = dataManager != null &&
                (dataManager.getServerDataMySQL() != null
                        || dataManager.getServerDataRedis() != null);
        if (includeOtherServers && bungeecord){
            Optional<IMinigame> iMinigame = dataManager.getMinigame(Minigame.getInstance().getName());
            if (iMinigame.isPresent()){
                IGame bestServer = iMinigame.get().getBestServer(gamePlayer);

                if (!bestServer.getBungeecordServerName().equalsIgnoreCase(Minigame.getInstance().getSettings().getServerName())){
                    bestServer.sendPlayerToServer(player);
                    Bukkit.getScheduler().runTaskLater(Minigame.getInstance().getPlugin(), task -> {
                        if (player.isOnline())
                            newArena(player, true, false);
                    }, 40L);
                    return;
                }
            }
        }

        GameInstance game = getHighestGame(player);
        if (gamePlayer.getParty().isInParty()){
            for (GamePlayer partyMember : gamePlayer.getParty().getAllOnlinePlayers().stream()
                    .map(p -> (GamePlayer) p)
                    .toList()){
                if (partyMember.isInGame()){
                    game = partyMember.getGame();
                }
            }
        }
        if (game != null) {
            game.joinPlayer(player);
            return;
        }else if (bungeecord && includeOtherServers){
            if (dataManager.getMinigame(Minigame.getInstance().getName()).isPresent()) {
                IGame bestArena = dataManager.getMinigame(Minigame.getInstance().getName()).get().getBestServer(gamePlayer);
                if ((dataManager.getServerDataMySQL() != null || dataManager.getServerDataRedis() != null) && bestArena != null) {
                    bestArena.sendPlayerToServer(player);
                    return;
                }
            }
        }

        if (sendToLobbyIfNoArena){
            ModuleManager.getModule(MessageModule.class).get(gamePlayer, "chat.sending_to_lobby.no_arena_found").send();
            GameUtils.sendToLobby(player, false);
        }else{
            ModuleManager.getModule(MessageModule.class).get(gamePlayer, "chat.no_arena_found").send();
        }
    }

    public GameInstance getHighestGame(Player player) {
        GameInstance highest = null;

        if (getGames().isEmpty()) return null;

        for(GameInstance game : getGames().values()) {
            if (!game.getState().equals(GameState.STARTING) && !game.getState().equals(GameState.WAITING)
                    && !(game.getState().equals(GameState.INGAME) && game.getSettings().isEnabledJoiningAfterStart())) {
                continue;
            }
            if (game.getPlayers().size() >= game.getSettings().getMaxPlayers() && !player.hasPermission("game.joinfullserver")) continue;

            if (highest == null || game.getPlayers().size() > highest.getPlayers().size()) {
                highest = game;
            }
        }

        return highest;
    }

    public void resetGame(GameInstance toResetGame){
        toResetGame.setState(GameState.RESTARTING);

        RestartScheduler scheduler = ModuleManager.getModule(RestartScheduler.class);
        boolean restartPending = scheduler != null && scheduler.isRestartPending();

        if (toResetGame.getSettings().isRestartServerAfterEnd() && Minigame.getInstance().getSettings().getGamesPerServer() == 1){
            ServerRegistry dataManager = ModuleManager.getModule(ServerRegistry.class);
            boolean bungeecord = dataManager != null &&
                    (dataManager.getServerDataMySQL() != null
                            || dataManager.getServerDataRedis() != null);
            if (bungeecord) {
                Optional<IMinigame> iMinigame = dataManager.getMinigame(Minigame.getInstance().getName());
                if (iMinigame.isPresent()) {
                    for (GamePlayer gamePlayer : toResetGame.getParticipants()) {
                        IGame bestServer = iMinigame.get().getBestServer(gamePlayer);

                        if (!bestServer.getBungeecordServerName().equalsIgnoreCase(Minigame.getInstance().getSettings().getServerName())) {
                            bestServer.sendPlayerToServer(gamePlayer.getOnlinePlayer());
                        }
                    }
                }
            }

            new BukkitRunnable(){
                @Override
                public void run() {
                    if (!Bukkit.getOnlinePlayers().isEmpty()){
                        Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage("§cRestarting server..."));
                    }
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "restart");
                }
            }.runTaskLater(Minigame.getInstance().getPlugin(), 80L);
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
                }else{
                    LevelModule levelModule = ModuleManager.getModule(LevelModule.class);
                    if (levelModule != null){
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
            Logger.log("[RestartScheduler] Skipping new game creation for '" + gameName +
                       "' — restart pending.", Logger.LogType.INFO);
        }

        GameResetEvent ev = new GameResetEvent(toResetGame, newGame);
        Bukkit.getPluginManager().callEvent(ev);

        if (!players.isEmpty()) {
            for (Player player : players) {
                ModuleManager.getModule(MessageModule.class).get(player, "chat.finding_new_game").send();
                Bukkit.getScheduler().runTaskLater(Minigame.getInstance().getPlugin(), task -> {
                    newArena(player, true, true);
                }, 25L);
            }
        }

        Bukkit.getScheduler().runTaskLater(Minigame.getInstance().getPlugin(), task -> {
            try {
                WorldManager.unload(toResetGame.getCurrentMap().getWorld());
            }catch (Exception e){
                e.printStackTrace();
            }
            toResetGame.terminate();
        }, 70L);
    }

    public Optional<GameInstance> getGameByID(String id){
        return Optional.ofNullable(games.get(id));
    }

    public Optional<GameInstance> getGameByName(String name){
        return games.values().stream().filter(gameInstance -> gameInstance.getName().equalsIgnoreCase(name)).findAny();
    }

    public Optional<GameInstance> getGameByNameOrID(String nameOrID) {
        return games.values().stream()
                .filter(gameInstance ->
                        gameInstance.getName().equalsIgnoreCase(nameOrID) ||
                                gameInstance.getID().equalsIgnoreCase(nameOrID)
                )
                .findAny();
    }

    public List<GameInstance> getFreeGames(Player player) {
        return games.values().stream()
                .filter(game -> isAvailableFor(player, game))
                .toList();
    }

    public boolean isAvailableFor(Player player, GameInstance gameInstance){
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

    public void addID(String id){
        if (!ids.contains(id)){
            ids.add(id);
        }
    }

    public boolean isDuplicate(String id){
        return ids.contains(id);
    }
}