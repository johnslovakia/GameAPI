package cz.johnslovakia.gameapi.listeners;

import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.inventoryBuilder.InventoryBuilder;
import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.cosmetics.CosmeticsModule;
import cz.johnslovakia.gameapi.modules.game.GameInstance;
import cz.johnslovakia.gameapi.modules.game.GameService;
import cz.johnslovakia.gameapi.modules.game.GameState;
import cz.johnslovakia.gameapi.events.GameQuitEvent;
import cz.johnslovakia.gameapi.modules.game.session.PlayerGameSession;
import cz.johnslovakia.gameapi.modules.levels.LevelModule;
import cz.johnslovakia.gameapi.modules.serverManagement.PendingActionType;
import cz.johnslovakia.gameapi.modules.serverManagement.PendingServerAction;
import cz.johnslovakia.gameapi.modules.serverManagement.ServerRegistry;
import cz.johnslovakia.gameapi.modules.stats.StatsModule;
import cz.johnslovakia.gameapi.users.PlayerIdentityRegistry;
import cz.johnslovakia.gameapi.users.PlayerManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.utils.*;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Optional;

public class JoinQuitListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        e.setJoinMessage(null);

        Player player = e.getPlayer();
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);
        gamePlayer.setOfflinePlayer(e.getPlayer());

        Bukkit.getScheduler().runTaskAsynchronously(Minigame.getInstance().getPlugin(), task -> ModuleManager.getModule(CosmeticsModule.class).loadPlayerCosmetics(gamePlayer));

        Minigame minigame = Minigame.getInstance();
        if (minigame == null) return;
        GameService gameService = ModuleManager.getModule(GameService.class);

        if (gameService.getGames().isEmpty()){
            player.sendMessage("");
            player.sendMessage("§cI can't find any game... set up a map or look for an error message in the console.");
            player.sendMessage("");
            return;
        }

        if (gameService.getGames().size() > 1 && ModuleManager.getModule(ServerRegistry.class) != null) {
            String currentServer = minigame.getSettings().getServerName();
            Optional<PendingServerAction> pending = ModuleManager.getModule(ServerRegistry.class)
                    .consumePendingAction(player.getName(), currentServer);

            if (pending.isPresent()) {
                PendingServerAction action = pending.get();
                if (action.getType() == PendingActionType.JOIN_ARENA) {
                    String arenaId = action.getData();
                    Optional<GameInstance> optionalGameInstance = gameService.getGames().values().stream()
                            .filter(g -> g.getName().endsWith(arenaId))
                            .toList()
                            .stream().findFirst();
                    if (optionalGameInstance.isPresent()) {
                        GameInstance game = optionalGameInstance.get();
                        if (gameService.isAvailableFor(player, game)) {
                            game.joinPlayer(player);
                        }else if (Minigame.getInstance().hasSpectatePermission(player)){
                            game.spectate(player);
                        }
                        return;
                    }
                } else if (action.getType() == PendingActionType.SPECTATE) {
                    String data = action.getData();
                    String[] parts = data.split(":");
                    if (parts.length == 2) {
                        if (parts[0].equalsIgnoreCase("player")) {
                            String targetName = parts[1];
                            Player targetPlayer = Bukkit.getPlayer(targetName);
                            if (targetPlayer != null) {
                                GamePlayer targetGamePlayer = PlayerManager.getGamePlayer(targetPlayer);
                                if (targetGamePlayer != null && targetGamePlayer.isOnline() && targetGamePlayer.isInGame()) {
                                    targetGamePlayer.getGame().spectate(player);
                                    Bukkit.getScheduler().runTaskLater(Minigame.getInstance().getPlugin(), task -> player.teleport(targetPlayer.getLocation()), 5L);
                                    return;
                                }
                            }
                        }else if (parts[0].equalsIgnoreCase("game")) {
                            String gameName = parts[1];
                            if (gameService.getGameByName(gameName).isPresent()){
                                gameService.getGameByName(gameName).get().spectate(player);
                                return;
                            }
                        }
                    }
                }
            }
        }


        if (Minigame.getInstance().getSettings().isAutoBestGameJoin()) {
            Optional<GameInstance> game = Optional.ofNullable(
                    gamePlayer.getGame()
            );

            if (game.isPresent() && Minigame.getInstance().getSettings().isEnabledReJoin() && game.get().getState().equals(GameState.INGAME)) {
                game.get().joinPlayer(player);
            } else {
                if (!gameService.getFreeGames(player).isEmpty()) {
                    gameService.newArena(player, true);
                }else if (Minigame.getInstance().hasSpectatePermission(player)){
                    Optional<GameInstance> gameToSpectate = gameService.getGames().values().stream().filter(gameInstance -> gameInstance.getState().equals(GameState.INGAME)).findFirst();
                    if (gameToSpectate.isPresent()) {
                        gameToSpectate.get().spectate(player);
                    }else{
                        player.sendMessage("§cNo running game found to watch. Sending you to lobby.");
                        GameUtils.sendToLobby(player);
                    }
                }
            }
        }
    }

    @EventHandler (priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent e) {
        e.setQuitMessage(null);

        Player player = e.getPlayer();
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);


        PlayerBossBar.removeBossBar(player.getUniqueId());

        if (gamePlayer.isInGame()){
            gamePlayer.getGame().quitPlayer(player);
        }


        LevelModule levelModule = ModuleManager.getModule(LevelModule.class);
        if (levelModule != null) {
            /*if (gamePlayer.isInGame() && !gamePlayer.getGame().getState().equals(GameState.INGAME)) {
                levelModule.getCache().remove(gamePlayer.getName());
            }else{
                levelModule.getCache().remove(gamePlayer.getName());
            }*/
            levelModule.getCache().remove(gamePlayer.getName());
        }
    }

    @EventHandler (priority = EventPriority.HIGHEST)
    public void onGameQuit(GameQuitEvent e) {
        GamePlayer gamePlayer = e.getGamePlayer();
        GameInstance game = gamePlayer.getGame();

        InventoryBuilder currentInventory = InventoryBuilder.getPlayerCurrentInventory(gamePlayer);
        if (currentInventory != null){
            currentInventory.unloadInventory(gamePlayer);
        }

        Utils.clearHeadCache(gamePlayer.getUniqueId());

        if (game.getState().equals(GameState.INGAME) && !game.getSettings().isUseTeams() && !gamePlayer.isRespawning()){ //TODO: check
            PlayerGameSession session = gamePlayer.getGameSession();
            if (session != null && session.isParticipatedAsPlayer()) {
                ModuleManager.getModule(StatsModule.class).setPlayerStat(gamePlayer, "Winstreak", 0);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onGameQuit2(GameQuitEvent e) {
        GamePlayer gamePlayer = e.getGamePlayer();
        Player player = gamePlayer.getOnlinePlayer();
        GameInstance gameInstance = e.getGame();

        if (gameInstance.getState().equals(GameState.WAITING) || gameInstance.getState().equals(GameState.STARTING)){
            if (gamePlayer.getGameSession() != null && gamePlayer.getGameSession().getSelectedKit() != null && gamePlayer.getPlayerData().getDefaultKit() != null)
                gamePlayer.getGameSession().setSelectedKit(gamePlayer.getPlayerData().getDefaultKit());

            Bukkit.getScheduler().runTaskLater(Minigame.getInstance().getPlugin(), task -> {
                if (Bukkit.getPlayer(player.getUniqueId()) == null) PlayerIdentityRegistry.unregister(player.getUniqueId());
            }, 10 * 20L);
        }else if (gameInstance.getState().equals(GameState.INGAME) && !gameInstance.getSettings().isEnabledReJoin()){
            /*Bukkit.getScheduler().runTaskLater(Minigame.getInstance().getPlugin(), task -> {
                if (Bukkit.getPlayer(player.getUniqueId()) == null) PlayerIdentityRegistry.unregister(player.getUniqueId());
            }, 20L);*/
            Bukkit.getScheduler().runTaskLater(Minigame.getInstance().getPlugin(), task -> {
                if (Bukkit.getPlayer(player.getUniqueId()) == null) {
                    gamePlayer.getPlayerData().saveAll();
                    gamePlayer.cleanUpHeavyData();
                }
            }, 3 * 20L);
        }
    }
}