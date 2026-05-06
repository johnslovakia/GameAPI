package cz.johnslovakia.gameapi.listeners;

import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.modules.game.GameInstance;
import cz.johnslovakia.gameapi.modules.game.GameState;
import cz.johnslovakia.gameapi.modules.game.lobby.LobbyModule;
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

        if (!game.getState().equals(GameState.INGAME)) {
            if (game.getState().equals(GameState.WAITING) || game.getState().equals(GameState.STARTING)) {
                LobbyModule lobbyModule = game.getModule(LobbyModule.class);
                if (lobbyModule != null && lobbyModule.getLobbyLocation() != null) {
                    e.setRespawnLocation(lobbyModule.getLobbyLocation().getLocation());
                }
            }else{
                e.setRespawnLocation(GameUtils.getNonRespawnLocation(game));
            }
            return;
        }
        GameMap playingMap = game.getCurrentMap();


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
                        if (!player.isOnline() || gamePlayer.getGame() == null) {
                            this.cancel();
                            return;
                        }

                        if (second == 0) {
                            player.teleport(playingMap.getPlayerToLocation(gamePlayer));
                            this.cancel();
                        }else{
                            ModuleManager.getModule(MessageModule.class).getMessage(gamePlayer, "title.respawn")
                                    .replace("%time%", "" + second)
                                    .send();
                        }
                        second--;
                    }
                }.runTaskTimer(Minigame.getInstance().getPlugin(), 0L, 20L);
            }
        }else if (player.getLastDamageCause() == null || (player.getLastDamageCause() != null && player.getLastDamageCause().getCause().equals(EntityDamageEvent.DamageCause.VOID)) || gamePlayer.getMetadata().containsKey("diedInVoid")){
            e.setRespawnLocation(GameUtils.getNonRespawnLocation(game));
        }else{
            Location deathLoc = (Location) gamePlayer.getMetadata().get("death_location");
            if (deathLoc != null) {
                e.setRespawnLocation(deathLoc);
                gamePlayer.getMetadata().remove("death_location");
            } else {
                e.setRespawnLocation(GameUtils.getNonRespawnLocation(game));
            }
        }

        if (gamePlayer.getMetadata().containsKey("pending_spectator_visuals")) {
            boolean teamSelector = (boolean) gamePlayer.getMetadata().remove("pending_spectator_visuals");
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline()) return;
                    gamePlayer.applySpectatorVisuals(teamSelector);
                }
            }.runTaskLater(Minigame.getInstance().getPlugin(), 1L);
        }
    }
}
