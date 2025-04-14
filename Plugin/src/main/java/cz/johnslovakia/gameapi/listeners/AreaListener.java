package cz.johnslovakia.gameapi.listeners;

import cz.johnslovakia.gameapi.events.PlayerEnterAreaEvent;
import cz.johnslovakia.gameapi.events.PlayerLeaveAreaEvent;
import cz.johnslovakia.gameapi.game.Game;
import cz.johnslovakia.gameapi.game.GameState;
import cz.johnslovakia.gameapi.game.map.Area;
import cz.johnslovakia.gameapi.users.PlayerManager;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.utils.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

public class AreaListener implements Listener {

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(e.getPlayer());
        Game game = gamePlayer.getPlayerData().getGame();
        Player player = gamePlayer.getOnlinePlayer();
        if (game != null) {
            if (game.getState() != GameState.INGAME) return;
            if (game.getCurrentMap() == null) return;

            for (Area area : game.getCurrentMap().getAreas()) {
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
            if (game.getCurrentMap() == null) return;

            for (Area area : game.getCurrentMap().getAreas()) {
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

        Location from = e.getFrom();
        Location to = e.getTo();

        if (!gamePlayer.isEnabledMovement()) {

            if (from.getX() != to.getX() || from.getZ() != to.getZ()) {
                Location loc = e.getFrom();
                e.getPlayer().teleport(loc.setDirection(to.getDirection()));
            }
        }

        if (game != null) {
            if (game.getState() != GameState.INGAME) return;
            if (game.getCurrentMap() == null) return;


            if (game.getCurrentMap().getMainArea() != null) {
                Area borderArea = game.getCurrentMap().getMainArea();
                if (to != null) {
                    if (borderArea.isBorder() && !borderArea.isInArea(to, 12) && from.getY() <= to.getY()) {
                        Vector direction = borderArea.getCenter().clone().subtract(from).toVector();
                        direction.normalize().multiply(0.5);

                        gamePlayer.getOnlinePlayer().teleport(to.clone().add(direction));
                        MessageManager.get(gamePlayer, "chat.move_border").send();
                    }
                }
            }


            for (Area area : game.getCurrentMap().getAreas()) {
                boolean fromIsIn = false;
                boolean toIsIn = false;

                if (area.isInArea(from)) {
                    fromIsIn = true;
                }
                assert to != null;
                if (area.isInArea(to)) {
                    toIsIn = true;
                }

                if (fromIsIn && !toIsIn) {
                    PlayerLeaveAreaEvent ev = new PlayerLeaveAreaEvent(gamePlayer, area, e);
                    Bukkit.getPluginManager().callEvent(ev);
                    e.setCancelled(ev.isCancelled());
                } else if (!fromIsIn && toIsIn) {
                    PlayerEnterAreaEvent ev = new PlayerEnterAreaEvent(gamePlayer, area, e);
                    Bukkit.getPluginManager().callEvent(ev);
                    e.setCancelled(ev.isCancelled());
                }
            }
        }
    }
}