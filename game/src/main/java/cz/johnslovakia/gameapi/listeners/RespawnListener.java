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
import cz.johnslovakia.gameapi.utils.GameUtils;
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
        if (!gamePlayer.isInGame()) return;
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
                e.setRespawnLocation(GameUtils.getNonRespawnLocation(game));

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
                }.runTaskTimer(Minigame.getInstance().getPlugin(), 0L, 20L);
            }
        }else if (player.getLastDamageCause() == null || (player.getLastDamageCause() != null && player.getLastDamageCause().getCause().equals(EntityDamageEvent.DamageCause.VOID))){
            e.setRespawnLocation(GameUtils.getNonRespawnLocation(game));
        }else{
            e.setRespawnLocation((Location) gamePlayer.getMetadata().get("death_location"));
            gamePlayer.getMetadata().remove("death_location");
        }
    }
}
