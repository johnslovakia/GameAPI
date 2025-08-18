package cz.johnslovakia.gameapi.listeners;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.datastorage.MinigameTable;
import cz.johnslovakia.gameapi.datastorage.PlayerTable;
import cz.johnslovakia.gameapi.events.GameJoinEvent;
import cz.johnslovakia.gameapi.events.GameQuitEvent;
import cz.johnslovakia.gameapi.game.Game;
import cz.johnslovakia.gameapi.game.GameManager;
import cz.johnslovakia.gameapi.game.GameState;
import cz.johnslovakia.gameapi.game.LobbyLocation;
import cz.johnslovakia.gameapi.levelSystem.LevelProgress;
import cz.johnslovakia.gameapi.serverManagement.DataManager;
import cz.johnslovakia.gameapi.users.PlayerManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.stats.StatsHolograms;
import cz.johnslovakia.gameapi.utils.*;

import me.zort.sqllib.SQLDatabaseConnection;
import me.zort.sqllib.api.data.Row;

import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

import org.bukkit.Bukkit;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class JoinQuitListener implements Listener {


    @EventHandler(ignoreCancelled = true)
    public void onPlayerJoin2(PlayerJoinEvent e) {
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(e.getPlayer());


        Bukkit.getScheduler().runTaskAsynchronously(Minigame.getInstance().getPlugin(), task -> {
            if (Minigame.getInstance().getLevelManager() != null && Minigame.getInstance().getDatabase() != null) {
                SQLDatabaseConnection connection = Minigame.getInstance().getDatabase().getConnection();
                String tableName = PlayerTable.TABLE_NAME;

                Optional<Row> result = connection.select()
                        .from(tableName)
                        .where().isEqual("Nickname", gamePlayer.getOnlinePlayer().getName())
                        .obtainOne();
                if (result.isPresent()){
                    LocalDate today = LocalDate.now();
                    if (result.get().getString("DailyRewards_reset") == null){
                        connection.update()
                                .table(tableName)
                                .set("DailyRewards_reset", today)
                                .where().isEqual("Nickname", gamePlayer.getOnlinePlayer().getName())
                                .execute();
                    }else{
                        LocalDate lastReset = LocalDate.parse(result.get().getString("DailyRewards_reset"));
                        if (!lastReset.equals(today)) {
                            connection.update()
                                    .table(tableName)
                                    .set("DailyRewards_reset", today)
                                    .where().isEqual("Nickname", gamePlayer.getOnlinePlayer().getName())
                                    .execute();
                            connection.update()
                                    .table(tableName)
                                    .set("DailyXP", 0)
                                    .where().isEqual("Nickname", gamePlayer.getOnlinePlayer().getName())
                                    .execute();
                            connection.update()
                                    .table(tableName)
                                    .set("DailyRewards_claims", 0)
                                    .where().isEqual("Nickname", gamePlayer.getOnlinePlayer().getName())
                                    .execute();
                        }
                    }
                }
            }
        });
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        e.setJoinMessage(null);

        Player player = e.getPlayer();
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);
        gamePlayer.setPlayer(e.getPlayer());

        Minigame minigame = Minigame.getInstance();

        if (GameManager.getGames().isEmpty()){
            player.sendMessage("");
            player.sendMessage("§cI can't find any game... set up a map or look for an error message in the console.");
            player.sendMessage("");
            return;
        }

        if (GameManager.getGames().size() > 1) {
            if (DataManager.getInstance() != null) {
                String gameIdentifier;

                if (DataManager.getInstance().useRedisForServerData()) {
                    String key = "player:" + player.getName() + ":game";
                    gameIdentifier = DataManager.getInstance().getServerDataRedis().getPool().getResource().get(key);
                }else {
                    Optional<Row> result = minigame.getDatabase().getConnection().select()
                            .from(minigame.getMinigameTable().getTableName())
                            .where().isEqual("Nickname", player.getName())
                            .obtainOne();
                    gameIdentifier = result.map(row -> row.getString("game")).orElse(null);
                }

                if (gameIdentifier != null) {
                    List<Game> game = GameManager.getGames().stream().filter(g -> g.getName().substring(g.getName().length() - 1).equals(gameIdentifier) && (g.getState().equals(GameState.WAITING) || g.getState().equals(GameState.STARTING))).toList();
                    if (!game.isEmpty()) game.get(0).joinPlayer(player);
                    return;
                }
            }
        }

        if (Minigame.getInstance().getSettings().isAutoBestGameJoin()) {
            Optional<Game> game = Optional.ofNullable(
                    gamePlayer.getGame()
            );

            if (game.isPresent() && Minigame.getInstance().getSettings().isEnabledReJoin() && game.get().getState().equals(GameState.INGAME)) {
                game.get().joinPlayer(player);
            } else {
                GameManager.newArena(player, true);
            }
        }



        if (player.hasPermission(minigame.getName() + ".admin") || player.hasPermission(minigame.getName().toLowerCase() + ".admin") || player.isOp() || player.hasPermission("*")){
            UpdateChecker updateChecker = minigame.getUpdateChecker();
            if (updateChecker.isOutdated()) {
                Bukkit.getScheduler().runTaskLater(Minigame.getInstance().getPlugin(), task -> {
                    TextComponent message = new TextComponent("§c[!] §fYour version of the §a" + minigame.getName() + " §fplugin is §coutdated! §fUpdating to the latest version is §arecommended! §7(hover for more)");

                    ComponentBuilder b = new ComponentBuilder("§fLatest Version: §a" + updateChecker.getLatestVersion());
                    b.append("\n§fYour Current Version: §a" + updateChecker.getCurrentVersion());
                    if (updateChecker.getUpdateMessage() != null) {
                        b.append("\n");
                        b.append("\n§fUpdate Message:\n");
                        b.append("§7 " + updateChecker.getUpdateMessage());
                    }

                    message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, b.create()));

                    player.spigot().sendMessage(message);

                    if (updateChecker.getAnnouncement() != null)
                        player.sendMessage(" §7↳ §r" + StringUtils.colorizer(updateChecker.getAnnouncement()));
                }, 15L);
            }else if (updateChecker.isUnreleased()) {
                Bukkit.getScheduler().runTaskLater(Minigame.getInstance().getPlugin(), task -> {
                    TextComponent message = new TextComponent("§e[!] §fYou are using an §eunreleased version §fof the §a" + minigame.getName() + " §fplugin! This version may not be stable. §7(hover for more)");

                    ComponentBuilder b = new ComponentBuilder("§fLatest Public Released Version: §a" + updateChecker.getLatestVersion());
                    b.append("\n§fYour Current Version: §c" + updateChecker.getCurrentVersion());
                    b.append("\n");
                    b.append("\n§6⚠ This version is newer than the latest official release.");
                    b.append("\n§7It may be unstable, experimental, or not fully tested.");


                    message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, b.create()));

                    player.spigot().sendMessage(message);

                    if (updateChecker.getAnnouncement() != null)
                        player.sendMessage(" §7↳ §r" + StringUtils.colorizer(updateChecker.getAnnouncement()));
                }, 15L);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        e.setQuitMessage(null);

        Player player = e.getPlayer();
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);

        Optional.ofNullable(gamePlayer.getGame())
                .ifPresent(game -> game.quitPlayer(player));

        PlayerBossBar.removeBossBar(player.getUniqueId());
    }


    @EventHandler
    public void onGameJoin(GameJoinEvent e) {
        Player player = e.getGamePlayer().getOnlinePlayer();

        ConfigAPI config = new ConfigAPI(GameAPI.getInstance().getMinigameDataFolder().toString(), "config.yml", Minigame.getInstance().getPlugin());
        if (GameAPI.getInstance().useDecentHolograms()) {
            Game game = e.getGame();

            LobbyLocation statsHologram = config.getLobbyLocation(game, "statsHologram");
            if (statsHologram != null){
                Minigame.getInstance().getStatsManager().createPlayerStatisticsHologram(statsHologram.getLocation(), player);
            }
            LobbyLocation topStatsHologram = config.getLobbyLocation(game, "topStatsHologram");
            if (topStatsHologram != null){
                Minigame.getInstance().getStatsManager().createTOPStatisticsHologram(topStatsHologram.getLocation(), player);
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