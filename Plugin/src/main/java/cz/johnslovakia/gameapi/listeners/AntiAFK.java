package cz.johnslovakia.gameapi.listeners;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.game.Game;
import cz.johnslovakia.gameapi.game.GameState;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerManager;
import cz.johnslovakia.gameapi.utils.Utils;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

//Not fully my code, source code: https://github.com/Neast1337/AntiAFK/blob/main/src/main/java/org/neast/antiafk/Antiafk.java
//Edited by me (john.slovakia)

public class AntiAFK implements Listener {

    private Map<UUID, BukkitRunnable> afkTasks = new ConcurrentHashMap<>();
    private Map<UUID, Boolean> isAfk = new ConcurrentHashMap<>();
    private Map<UUID, Boolean> afkByJoin = new ConcurrentHashMap<>();

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);

        if (gamePlayer.getPlayerData() == null){
            return;
        }
        Game game = gamePlayer.getPlayerData().getGame();
        if (gamePlayer.getPlayerData().getGame() == null){
            return;
        }
        long afkCheckDelay = (game.getState().equals(GameState.WAITING) || game.getState().equals(GameState.STARTING) ? (5 * 60) * 20 : 45 * 20);

        UUID playerId = player.getUniqueId();

        if (event.getFrom().distanceSquared(event.getTo()) > 0.01) {
            cancelAfkTask(playerId);
            if (isAfk.getOrDefault(playerId, false)) {
                if (afkByJoin.getOrDefault(playerId, false)) {
                    afkByJoin.remove(playerId);
                }
                isAfk.put(playerId, false);
            }
        } else {
            if (!afkTasks.containsKey(playerId)) {
                BukkitRunnable task = new BukkitRunnable() {
                    @Override
                    public void run() {
                        isAfk.put(playerId, true);
                        kickPlayerIfAfk(player);
                    }
                };
                afkTasks.put(playerId, task);
                task.runTaskLaterAsynchronously(GameAPI.getInstance(), afkCheckDelay);
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);

        if (gamePlayer.getPlayerData() == null){
            return;
        }
        Game game = gamePlayer.getPlayerData().getGame();
        if (gamePlayer.getPlayerData().getGame() == null){
            return;
        }
        long afkCheckDelay = (game.getState().equals(GameState.WAITING) || game.getState().equals(GameState.STARTING) ? (5 * 60) * 20 : 45 * 20);

        UUID playerId = player.getUniqueId();

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                isAfk.put(playerId, true);
                afkByJoin.put(playerId, true);
                kickPlayerIfAfk(player);
            }
        };
        afkTasks.put(playerId, task);
        task.runTaskLaterAsynchronously(GameAPI.getInstance(), afkCheckDelay);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        cancelAfkTask(playerId);
        isAfk.remove(playerId);
        afkByJoin.remove(playerId);
    }

    private void cancelAfkTask(UUID playerId) {
        BukkitRunnable task = afkTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
    }

    private void kickPlayerIfAfk(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_BREAK, 1.0f, 1.0f);

        new BukkitRunnable(){
            int i = 15;

            @Override
            public void run() {
                if (i == 0) {
                    if (player.isOnline() && !player.isDead() && afkTasks.containsKey(player.getUniqueId()) && isAfk.getOrDefault(player.getUniqueId(), false)) {
                        Utils.sendToLobby(player);
                        cancelAfkTask(player.getUniqueId());
                    }
                    this.cancel();
                }else{
                    if (i == 15 || i == 10 || i <= 5){
                        MessageManager.get(player, "chat.afk_kick_countdown")
                                .replace("%seconds%", "" + i)
                                .send();
                        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f);
                    }
                }
                i--;
            }
        }.runTaskTimer(GameAPI.getInstance(), 0L, 20L);

    }
}
