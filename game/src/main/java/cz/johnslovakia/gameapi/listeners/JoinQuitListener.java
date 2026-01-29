package cz.johnslovakia.gameapi.listeners;

import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.inventoryBuilder.InventoryBuilder;
import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.cosmetics.CosmeticsModule;
import cz.johnslovakia.gameapi.modules.game.GameInstance;
import cz.johnslovakia.gameapi.modules.game.GameService;
import cz.johnslovakia.gameapi.modules.game.GameState;
import cz.johnslovakia.gameapi.events.GameQuitEvent;
import cz.johnslovakia.gameapi.modules.levels.LevelModule;
import cz.johnslovakia.gameapi.modules.serverManagement.DataManager;
import cz.johnslovakia.gameapi.modules.stats.StatsModule;
import cz.johnslovakia.gameapi.users.PlayerIdentityRegistry;
import cz.johnslovakia.gameapi.users.PlayerManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.utils.Logger;
import cz.johnslovakia.gameapi.utils.PlayerBossBar;
import cz.johnslovakia.gameapi.utils.StringUtils;
import cz.johnslovakia.gameapi.utils.UpdateChecker;
import me.zort.sqllib.SQLDatabaseConnection;
import me.zort.sqllib.api.data.Row;

import net.kyori.adventure.text.Component;

import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;
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

        if (gameService.getGames().size() > 1) {
            if (DataManager.getInstance() != null) {
                String gameIdentifier = null;

                if (DataManager.getInstance().useRedisForServerData()) {
                    String key = "player:" + player.getName() + ":game";
                    gameIdentifier = DataManager.getInstance().getServerDataRedis().get(key);
                } else {
                    try (SQLDatabaseConnection connection = minigame.getDatabase().getConnection()) {
                        if (connection != null) {
                            Optional<Row> result = connection.select()
                                    .from(minigame.getMinigameTable().getTableName())
                                    .where().isEqual("Nickname", player.getName())
                                    .obtainOne();

                            gameIdentifier = result.map(row -> row.getString("game")).orElse(null);
                        }
                    } catch (Exception exception) {
                        Logger.log("Failed to load player's game identifier from DB: " + exception.getMessage(), Logger.LogType.ERROR);
                        exception.printStackTrace();
                    }
                }

                if (gameIdentifier != null) {
                    String finalGameIdentifier = gameIdentifier;
                    List<GameInstance> game = gameService.getGames().values().stream()
                            .filter(g -> g.getName().endsWith(finalGameIdentifier) &&
                                    (g.getState().equals(GameState.WAITING) || g.getState().equals(GameState.STARTING)))
                            .toList();
                    if (!game.isEmpty()) game.get(0).joinPlayer(player);
                    return;
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
                gameService.newArena(player, true);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        e.setQuitMessage(null);

        Player player = e.getPlayer();
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);

        LevelModule levelModule = ModuleManager.getModule(LevelModule.class);
        if (levelModule != null) {
            if (!gamePlayer.getGame().getState().equals(GameState.INGAME)) {
                levelModule.getCache().remove(gamePlayer);
            }
        }

        Optional.ofNullable(gamePlayer.getGame())
                .ifPresent(game -> game.quitPlayer(player));

        PlayerBossBar.removeBossBar(player.getUniqueId());
    }

    @EventHandler
    public void onGameQuit(GameQuitEvent e) {
        GamePlayer gamePlayer = e.getGamePlayer();

        InventoryBuilder currentInventory = InventoryBuilder.getPlayerCurrentInventory(gamePlayer);
        if (currentInventory != null){
            currentInventory.unloadInventory(gamePlayer);
        }

        ModuleManager.getModule(StatsModule.class).getStatsHolograms().remove(gamePlayer);
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
        }else if (gameInstance.getState().equals(GameState.INGAME)/* && !gameInstance.getSettings().isEnabledReJoin()*/){
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