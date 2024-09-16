package cz.johnslovakia.gameapi.listeners;

import cz.johnslovakia.gameapi.events.PlayerEnterAreaEvent;
import cz.johnslovakia.gameapi.events.PlayerLeaveAreaEvent;
import cz.johnslovakia.gameapi.game.Game;
import cz.johnslovakia.gameapi.game.GameState;
import cz.johnslovakia.gameapi.game.map.Area;
import cz.johnslovakia.gameapi.users.PlayerManager;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class AreaListener implements Listener {

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(e.getPlayer());
        Game game = gamePlayer.getPlayerData().getGame();
        Player player = gamePlayer.getOnlinePlayer();
        if (game != null) {
            if (game.getState() != GameState.INGAME) return;
            if (game.getPlayingMap() == null) return;

            for (Area area : game.getPlayingMap().getAreas()) {
                if(area.isBorder()){
                    if (!area.isInArea(e.getBlock().getLocation())){
                        MessageManager.get(player, "chat.cant_break").send();
                        e.setCancelled(true);
                        return;
                    }
                }
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(e.getPlayer());
        Game game = gamePlayer.getPlayerData().getGame();
        Player player = gamePlayer.getOnlinePlayer();
        if (game != null) {
            if (game.getState() != GameState.INGAME) return;
            if (game.getPlayingMap() == null) return;

            for (Area area : game.getPlayingMap().getAreas()) {
                if(area.isBorder()){
                    if (!area.isInArea(e.getBlock().getLocation())){
                        MessageManager.get(player, "chat.cant_place").send();
                        e.setCancelled(true);
                        return;
                    }
                }
            }
        }
    }


    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(e.getPlayer());
        Game game = gamePlayer.getPlayerData().getGame();

        if (game != null) {
            if (!gamePlayer.isEnabledMovement()) {
                Location from = e.getFrom();
                Location to = e.getTo();

                if (from.getX() != to.getX() || /*from.getY() != to.getY() ||*/ from.getZ() != to.getZ()) {
                    Location loc = e.getFrom();
                    e.getPlayer().teleport(loc.setDirection(to.getDirection()));
                }
            }
            

            if (game.getState() != GameState.INGAME) return;
            if (game.getPlayingMap() == null) return;

            for (Area area : game.getPlayingMap().getAreas()) {
                Location f = e.getFrom();
                Location t = e.getTo();
                boolean fromIsIn = false;
                boolean toIsIn = false;

                if (area.isInArea(f)) {
                    fromIsIn = true;
                }
                if (area.isInArea(t)) {
                    toIsIn = true;
                }

                if (fromIsIn && !toIsIn) {
                    PlayerLeaveAreaEvent ev = new PlayerLeaveAreaEvent(gamePlayer, area);
                    Bukkit.getPluginManager().callEvent(ev);
                    e.setCancelled(ev.isCancelled());
                } else if (!fromIsIn && toIsIn) {
                    PlayerEnterAreaEvent ev = new PlayerEnterAreaEvent(gamePlayer, area);
                    Bukkit.getPluginManager().callEvent(ev);
                    e.setCancelled(ev.isCancelled());
                }
            }
        }
    }

    @EventHandler
    public void onPlayerLeaveArea(PlayerLeaveAreaEvent e) {
        GamePlayer gamePlayer = e.getGamePlayer();

        if (e.getArea().isBorder()){
            if (gamePlayer.getOnlinePlayer().getLocation().getY() <= 5) {
                return;
            }
            gamePlayer.getOnlinePlayer().teleport(e.getMoveEvent().getFrom());
            MessageManager.get(gamePlayer, "chat.move_border").send();
        }
    }
}