package cz.johnslovakia.gameapi.listeners;

import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.modules.game.GameInstance;
import cz.johnslovakia.gameapi.modules.game.GameState;
import cz.johnslovakia.gameapi.modules.game.map.GameMap;
import cz.johnslovakia.gameapi.modules.game.map.MapLocation;
import cz.johnslovakia.gameapi.modules.messages.MessageModule;
import cz.johnslovakia.gameapi.users.PlayerManager;
import cz.johnslovakia.gameapi.modules.ModuleManager;
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
        GameInstance game = PlayerManager.getGamePlayer(player).getGame();
        GameMap playingMap = game.getCurrentMap();

        if (!game.getState().equals(GameState.INGAME)) return;

        player.setVisualFire(false);

        new BukkitRunnable(){
            @Override
            public void run() {
                player.setFireTicks(0);
            }
        }.runTaskLater(Minigame.getInstance().getPlugin(), 1L);

        if (gamePlayer.isRespawning()){
            if (game.getSettings().getRespawnCooldown() == -1) {
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
                            ModuleManager.getModule(MessageModule.class).get(gamePlayer, "title.respawn")
                                    .replace("%time%", "" + second)
                                    .send();
                        }
                        second--;
                    }
                }.runTaskLater(Minigame.getInstance().getPlugin(), 20L);
            }
        }else if (player.getLastDamageCause() == null || (player.getLastDamageCause() != null && player.getLastDamageCause().getCause().equals(EntityDamageEvent.DamageCause.VOID))){
            e.setRespawnLocation(getNonRespawnLocation(game));
        }else{
            e.setRespawnLocation((Location) gamePlayer.getMetadata().get("death_location"));
        }
    }

    public static Location getNonRespawnLocation(GameInstance game){
        GameMap playingMap = game.getCurrentMap();
        MapLocation spectatorSpawn = playingMap.getSpectatorSpawn();

        if (spectatorSpawn == null){
            if (game.getCurrentMap().getMainArea() != null) {
                Location center = playingMap.getMainArea().getCenter().add(0, 15, 0);
                while (center.getBlock().getType().equals(Material.AIR)) {
                    center.add(0, 1, 0);
                }
                return center;
            }else{
                if (game.getPlayers().get(0) != null){
                    return game.getPlayers().get(0).getOnlinePlayer().getLocation();
                }else{
                    return new Location(game.getCurrentMap().getWorld(), 0, 90, 0);
                }
            }
        }
        return spectatorSpawn.getLocation();
    }
}
