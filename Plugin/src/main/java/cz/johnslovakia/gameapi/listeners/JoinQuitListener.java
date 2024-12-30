package cz.johnslovakia.gameapi.listeners;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.events.GameJoinEvent;
import cz.johnslovakia.gameapi.events.GameQuitEvent;
import cz.johnslovakia.gameapi.game.Game;
import cz.johnslovakia.gameapi.game.GameManager;
import cz.johnslovakia.gameapi.game.GameState;
import cz.johnslovakia.gameapi.users.PlayerManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.stats.StatsHolograms;
import cz.johnslovakia.gameapi.utils.ConfigAPI;
import me.zort.sqllib.api.data.Row;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Optional;

public class JoinQuitListener implements Listener {


    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        e.setJoinMessage(null);

        Player player = e.getPlayer();
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);
        gamePlayer.setPlayer(e.getPlayer());

        if (GameManager.getGames().isEmpty()){
            player.sendMessage("");
            player.sendMessage("§cI can't find any game... set up a map or look for an error message in the console.");
            player.sendMessage("");
            return;
        }

        if (GameManager.getGames().size() > 1) {
            Minigame minigame = GameAPI.getInstance().getMinigame();
            if (minigame.getDataManager() != null) {
                String gameName;

                if (minigame.useRedisForServerData()) {
                    String key = "player:" + player.getName() + ":game";
                    gameName = minigame.getServerDataRedis().getPool().getResource().get(key);
                }else {
                    Optional<Row> result = minigame.getDatabase().getConnection().select()
                            .from(minigame.getMinigameTable().getTableName())
                            .where().isEqual("Nickname", player.getName())
                            .obtainOne();
                    gameName = result.map(row -> row.getString("game")).orElse(null);
                }

                if (gameName != null) {
                    List<Game> game = GameManager.getGames().stream().filter(g -> g.getName().substring(g.getName().length() - 1).equals(gameName) && (g.getState().equals(GameState.WAITING) || g.getState().equals(GameState.STARTING))).toList();
                    if (!game.isEmpty()) game.get(0).joinPlayer(player);
                    return;
                }
            }
        }


        if (GameAPI.getInstance().getMinigame().getSettings().isAutoBestGameJoin()) {
            Optional<Game> game = Optional.ofNullable(
                    gamePlayer.getPlayerData().getGame()
            );

            if (game.isPresent() && !(game.get().getState().equals(GameState.ENDING))) {
                game.get().joinPlayer(player);
            } else {
                PlayerManager.removeGamePlayer(player);
                GameManager.newArena(player, true);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        e.setQuitMessage(null);

        Player player = e.getPlayer();
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);

        Optional.ofNullable(gamePlayer.getPlayerData().getGame())
                .ifPresent(game -> game.quitPlayer(player));
    }


    @EventHandler
    public void onGameJoin(GameJoinEvent e) {
        Player player = e.getGamePlayer().getOnlinePlayer();

        ConfigAPI config = new ConfigAPI(GameAPI.getInstance().getMinigameDataFolder().toString(), "config.yml", GameAPI.getInstance());
        if (GameAPI.getInstance().useDecentHolograms()) {
            if (config.getLocation("statsHologram") != null) {
                GameAPI.getInstance().getStatsManager().createPlayerStatisticsHologram(config.getLocation("statsHologram"), player);
            }
            if (config.getLocation("topStatsHologram") != null) {
                GameAPI.getInstance().getStatsManager().createTOPStatisticsHologram(config.getLocation("topStatsHologram"), player);
            }
        }
    }

    @EventHandler
    public void onGameQuit(GameQuitEvent e) {
        Player player = e.getGamePlayer().getOnlinePlayer();

        if (GameAPI.getInstance().useDecentHolograms()) {
            StatsHolograms.remove(player);
        }
    }
}