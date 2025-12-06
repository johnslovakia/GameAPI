package cz.johnslovakia.gameapi.listeners;

import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.modules.game.GameInstance;
import cz.johnslovakia.gameapi.modules.game.GameState;
import cz.johnslovakia.gameapi.events.GameQuitEvent;
import cz.johnslovakia.gameapi.events.GameStartEvent;
import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.messages.MessageModule;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerManager;
import cz.johnslovakia.gameapi.utils.GameUtils;
import cz.johnslovakia.gameapi.utils.Utils;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

//source code: https://github.com/Neast1337/AntiAFK/blob/main/src/main/java/org/neast/antiafk/Antiafk.java

public class AntiAFK implements Listener {


    //TODO: optimalizovat, někdy to bere jako AFK i když se hýbe, možná když nehýbe myší
    private final Map<GamePlayer, BukkitRunnable> afkTasks = new ConcurrentHashMap<>();
    private final Map<GamePlayer, Boolean> isAfk = new ConcurrentHashMap<>();
    private final Map<GamePlayer, Boolean> afkByJoin = new ConcurrentHashMap<>();

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("*") || player.isOp()) return;

        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);

        if (gamePlayer.getPlayerData() == null){
            return;
        }
        GameInstance game = gamePlayer.getGame();
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
                task.runTaskLaterAsynchronously(Minigame.getInstance().getPlugin(), afkCheckDelay);
            }
            }
        }
    }

    @EventHandler
    public void onGameStart(GameStartEvent event) {
        for (GamePlayer gamePlayer : event.getGame().getParticipants()) {
            Player player = gamePlayer.getOnlinePlayer();
            if (player.hasPermission("*") || player.isOp()) return;

            if (gamePlayer.getPlayerData() == null) {
                return;
            }
            GameInstance game = gamePlayer.getGame();
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
            task.runTaskLaterAsynchronously(Minigame.getInstance().getPlugin(), afkCheckDelay);
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
        if (player.hasPermission("*") || player.isOp()) return;

        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);
        if (gamePlayer.getPlayerData() == null || gamePlayer.getGame() == null) return;

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
                        GameUtils.sendToLobby(player);
                        cancelAfkTask(gamePlayer);
                    }
                    this.cancel();
                }else{
                    if (i == 15 || i == 10 || i <= 5){
                        ModuleManager.getModule(MessageModule.class).get(player, "chat.afk_kick_countdown")
                                .replace("%seconds%", "" + i)
                                .send();
                        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f);
                    }
                }
                i--;
            }
        }.runTaskTimer(Minigame.getInstance().getPlugin(), 0L, 20L);

    }
}
