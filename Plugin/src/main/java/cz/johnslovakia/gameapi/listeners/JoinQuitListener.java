package cz.johnslovakia.gameapi.listeners;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.events.GameJoinEvent;
import cz.johnslovakia.gameapi.events.GameQuitEvent;
import cz.johnslovakia.gameapi.game.Game;
import cz.johnslovakia.gameapi.game.GameManager;
import cz.johnslovakia.gameapi.users.PlayerManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.stats.StatsHolograms;
import cz.johnslovakia.gameapi.utils.ConfigAPI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class JoinQuitListener implements Listener {


    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        e.setJoinMessage(null);

        Player player = e.getPlayer();
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);
        gamePlayer.setPlayer(e.getPlayer());

        if (!e.getPlayer().hasPlayedBefore()){
            gamePlayer.getPlayerData().getPlayerTable().newUser(gamePlayer);
            GameAPI.getInstance().getMinigame().getMinigameTable().newUser(gamePlayer);
        }else{
            new BukkitRunnable(){
                @Override
                public void run() {
                    gamePlayer.getPlayerData().getPlayerTable().newUser(gamePlayer);
                    GameAPI.getInstance().getMinigame().getMinigameTable().newUser(gamePlayer);
                }
            }.runTaskAsynchronously(GameAPI.getInstance());
        }



        if (GameManager.getGames().isEmpty()){
            player.sendMessage("");
            player.sendMessage("§cI can't find any game... set up a map or look for an error message in the console.");
            player.sendMessage("");
            return;
        }




        if (GameAPI.getInstance().getMinigame().getSettings().isAutoBestGameJoin()) {
            Game game = gamePlayer.getPlayerData().getGame();
            //TODO: vyřešit lépe
            if (game != null) {
                game.joinPlayer(player);
            } else {
                //TODO: možná PlayerManager.removeGamePlayer(...)?
                GameManager.newArena(player, true);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        e.setQuitMessage(null);

        Player player = e.getPlayer();
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);

        Game game = gamePlayer.getPlayerData().getGame();
        if (game != null){
            game.quitPlayer(player);
        }
    }


    @EventHandler
    public void onGameJoin(GameJoinEvent e) {
        Player player = e.getGamePlayer().getOnlinePlayer();

        ConfigAPI config = new ConfigAPI(GameAPI.getInstance().getMinigameDataFolder().toString(), "config.yml", GameAPI.getInstance());
        if (config.getLocation("statsHologram") != null){
            GameAPI.getInstance().getStatsManager().createPlayerStatisticsHologram(config.getLocation("statsHologram"), player);
        }
        if (config.getLocation("topStatsHologram") != null){
            GameAPI.getInstance().getStatsManager().createTOPStatisticsHologram(config.getLocation("topStatsHologram"), player);
        }
    }

    @EventHandler
    public void onGameQuit(GameQuitEvent e) {
        Player player = e.getGamePlayer().getOnlinePlayer();
        
        if (GameAPI.useDecentHolograms()){
            StatsHolograms.remove(player);
        }
    }
}