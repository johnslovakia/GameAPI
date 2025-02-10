package cz.johnslovakia.gameapi.listeners;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.events.GameJoinEvent;
import cz.johnslovakia.gameapi.events.GameQuitEvent;
import cz.johnslovakia.gameapi.events.GameStartEvent;
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

    private Map<GamePlayer, BukkitRunnable> afkTasks = new ConcurrentHashMap<>();
    private Map<GamePlayer, Boolean> isAfk = new ConcurrentHashMap<>();
    private Map<GamePlayer, Boolean> afkByJoin = new ConcurrentHashMap<>();

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);

        if (gamePlayer.getPlayerData() == null || event.getTo() == null){
            return;
        }
        Game game = gamePlayer.getPlayerData().getGame();
        if (game == null){
            return;
        }
        long afkCheckDelay = (game.getState().equals(GameState.WAITING) || game.getState().equals(GameState.STARTING) ? (5 * 60) * 20 : 90 * 20);
        

        if (event.getFrom().distanceSquared(event.getTo()) > 0.01) {
            cancelAfkTask(gamePlayer);
            if (isAfk.getOrDefault(gamePlayer, false)) {
                if (afkByJoin.getOrDefault(gamePlayer, false)) {
                    afkByJoin.remove(gamePlayer);
                }
                isAfk.put(gamePlayer, false);
            }
        } else {
            if (game.getState().equals(GameState.INGAME)){
            if (!afkTasks.containsKey(gamePlayer)) {
                BukkitRunnable task = new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (PlayerManager.getGamePlayer(player) != gamePlayer) {
                            this.cancel();
                            return;
                        }
                        isAfk.put(gamePlayer, true);
                        kickPlayerIfAfk(player);
                    }
                };
                afkTasks.put(gamePlayer, task);
                task.runTaskLaterAsynchronously(GameAPI.getInstance(), afkCheckDelay);
            }
            }
        }
    }

    @EventHandler
    public void onGameStart(GameStartEvent event) {
        for (GamePlayer gamePlayer : event.getGame().getParticipants()) {
            Player player = gamePlayer.getOnlinePlayer();

            if (gamePlayer.getPlayerData() == null) {
                return;
            }
            Game game = gamePlayer.getPlayerData().getGame();
            if (game == null) {
                return;
            }
            long afkCheckDelay = (game.getState().equals(GameState.WAITING) || game.getState().equals(GameState.STARTING) ? (5 * 60) * 20 : 90 * 20);


            BukkitRunnable task = new BukkitRunnable() {
                @Override
                public void run() {
                    if (PlayerManager.getGamePlayer(player) != gamePlayer) {
                        this.cancel();
                        return;
                    }

                    isAfk.put(gamePlayer, true);
                    afkByJoin.put(gamePlayer, true);
                    kickPlayerIfAfk(player);
                }
            };
            afkTasks.put(gamePlayer, task);
            task.runTaskLaterAsynchronously(GameAPI.getInstance(), afkCheckDelay);
        }
    }

    @EventHandler
    public void onGameQuit(GameQuitEvent event) {
        GamePlayer gamePlayer = event.getGamePlayer();

        isAfk.remove(gamePlayer);
        afkByJoin.remove(gamePlayer);
        afkTasks.remove(gamePlayer);

    }

    private void cancelAfkTask(GamePlayer gamePlayer) {
        BukkitRunnable task = afkTasks.remove(gamePlayer);
        if (task != null) {
            task.cancel();
        }
    }

    private void kickPlayerIfAfk(Player player) {
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);

        if (gamePlayer.getPlayerData() == null){
            return;
        }
        if (gamePlayer.getPlayerData().getGame() == null){
            return;
        }
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_BREAK, 1.0f, 1.0f);

        new BukkitRunnable(){
            int i = 15;

            @Override
            public void run() {
                if (PlayerManager.getGamePlayer(player) != gamePlayer || !afkTasks.containsKey(gamePlayer) && !isAfk.getOrDefault(gamePlayer, true)){
                    this.cancel();
                    return;
                }
                if (i == 0) {
                    if (player.isOnline() && !player.isDead() && afkTasks.containsKey(gamePlayer) && isAfk.getOrDefault(gamePlayer, false)) {
                        Utils.sendToLobby(player);
                        cancelAfkTask(gamePlayer);
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
