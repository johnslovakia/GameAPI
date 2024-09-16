package cz.johnslovakia.gameapi.listeners;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.game.Game;
import cz.johnslovakia.gameapi.game.GameState;
import cz.johnslovakia.gameapi.game.map.GameMap;
import cz.johnslovakia.gameapi.users.PlayerManager;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class RespawnListener implements Listener {

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent e) {
        Player player = e.getPlayer();
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);
        Game game = PlayerManager.getGamePlayer(player).getPlayerData().getGame();
        GameMap playingMap = game.getPlayingMap();

        player.setFireTicks(0);
        player.setVisualFire(false);

        if (game.getState() == GameState.INGAME) {
            if (gamePlayer.isRespawning()){
                if (game.getSettings().getRespawnCooldown() != -1) {
                    Location location = playingMap.getPlayerToLocation(gamePlayer);
                    e.setRespawnLocation(location);
                }else{
                    e.setRespawnLocation(getNonRespawnLocation(game));

                    new BukkitRunnable(){
                        int second = game.getSettings().getRespawnCooldown();
                        @Override
                        public void run() {
                            if (second == 0) {
                                player.teleport(playingMap.getPlayerToLocation(gamePlayer));
                            }else{
                                MessageManager.get(gamePlayer, "title.respawn")
                                        .replace("%time%", "" + second)
                                        .send();
                            }
                            second--;
                        }
                    }.runTaskLater(GameAPI.getInstance(), 20L);
                }
            }else if (player.getLastDamageCause() == null || player.getLastDamageCause().getCause().equals(EntityDamageEvent.DamageCause.VOID)){
                e.setRespawnLocation(getNonRespawnLocation(game));
            }
        } else if (game.getState() == GameState.WAITING || game.getState() == GameState.STARTING){
            e.setRespawnLocation(game.getLobbyPoint());
        }
    }

    public static Location getNonRespawnLocation(Game game){
        GameMap playingMap = game.getPlayingMap();

        Location spectatorLocation = playingMap.getSpectatorSpawn().getLocationForMap(playingMap);
        if (spectatorLocation == null){
            Location center = playingMap.getMainArea().getCenter().add(0, 20, 0);
            while (center.getBlock().getType().equals(Material.AIR)){
                center.add(0, 1, 0);
            }
            return center;
        }

        return spectatorLocation;
    }
}
