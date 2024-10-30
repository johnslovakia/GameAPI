package cz.johnslovakia.gameapi.game;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.events.GameResetEvent;
import cz.johnslovakia.gameapi.events.NewArenaEvent;
import cz.johnslovakia.gameapi.game.map.GameMap;
import cz.johnslovakia.gameapi.users.PlayerManager;
import cz.johnslovakia.gameapi.game.team.TeamManager;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.utils.Utils;
import cz.johnslovakia.gameapi.utils.Logger;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public class GameManager {

    @Getter
    private static final List<Game> games = new ArrayList<>();
    @Getter
    private static final List<String> ids = new ArrayList<>();

    public static void registerGame(Game game){
        if (games.contains(game)){
            return;
        }
        Logger.log("Added Game " + game.getID(), Logger.LogType.INFO);
        games.add(game);
    }


    public static void newArena(Player player, boolean sendToLobbyIfNoArena) {
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);


        NewArenaEvent ev = new NewArenaEvent(PlayerManager.getGamePlayer(player));
        Bukkit.getPluginManager().callEvent(ev);

        if (ev.isCancelled()){
            return;
        }

        Game game = getHighestGame();
        if (gamePlayer.getParty().isInParty()){
            for (GamePlayer partyMember : gamePlayer.getParty().getAllOnlinePlayers()){
                if (partyMember.isInGame()){
                    game = partyMember.getPlayerData().getGame();
                }
            }
        }
        if (game != null) {
            if (gamePlayer.getPlayerData().getGame() != null) {
                gamePlayer.getPlayerData().getGame().quitPlayer(player);
            }
            game.joinPlayer(player);
            return;
        }

        MessageManager.get(gamePlayer, "chat.no_arena_found").send();
        if (sendToLobbyIfNoArena){
            Utils.sendToLobby(player);
        }
    }

    public static Game getHighestGame() {
        Game highest = null;

        if (GameManager.getGames().isEmpty()){
            return null;
        }

        for(Game game : GameManager.getGames()) {
            if (game.getState() != GameState.WAITING && game.getState() != GameState.STARTING) {
                if (!game.getSettings().isAllowedJoiningAfterStart()){
                    continue;
                }
            }
            if (game.getPlayers().size() >= game.getSettings().getMaxPlayers()){
                continue;
            }

            if (highest == null || game.getPlayers().size() > highest.getPlayers().size()) {
                highest = game;
            }
        }


        return highest;
    }

    public static void resetGame(Game game){
        if (game.getSettings().restartServerAfterEnd()){
            game.getPlayers().forEach(gp -> GameManager.newArena(gp.getOnlinePlayer(), true));
            new BukkitRunnable(){
                @Override
                public void run() {
                    if (!Bukkit.getOnlinePlayers().isEmpty()){
                        Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage("§cRestarting server..."));
                    }
                    Bukkit.shutdown();
                }
            }.runTaskLater(GameAPI.getInstance(), 40L);
            return;
        }


        Game newGame = new Game(game.getName(), game.getLobbyInventory(), game.getLobbyPoint());
        newGame.setMapManager(game.getMapManager());
        TeamManager.resetTeamsAndRegisterForNewGame(game, newGame);

        newGame.getMapManager().getMaps().forEach(a -> a.setGame(newGame));


        List<Player> players = new ArrayList<>();
        if (!game.getPlayers().isEmpty()) {
            for (int i = 0; i < game.getPlayers().size(); i++) {
                GamePlayer gamePlayer = game.getPlayers().get(i);
                Player player = gamePlayer.getOnlinePlayer();

                //gamePlayer.getGame().leavePlayer(player);
                //gamePlayer.setGame(null);

                if (gamePlayer.getOnlinePlayer().isOnline()) {
                    players.add(player);
                }
                PlayerManager.removeGamePlayer(player);
            }
        }

        games.remove(game);
        registerGame(newGame);

        GameResetEvent ev = new GameResetEvent(game, newGame);
        Bukkit.getPluginManager().callEvent(ev);

        for (GameMap map : newGame.getMapManager().getMaps()) {
            map.setVotes(0);
            map.setPlayed(false);
            map.setPlaying(false);
            map.setWinned(false);
            map.setWorld(null);
        }

        if (!players.isEmpty()) {
            for (Player player : players) {
                //game.leavePlayer(player);
                GameManager.newArena(player, true);
                //newGame.joinPlayer(player);
            }
        }
    }

    public static Game getGameByID(String id){
        for (Game game : games){
            if (game.getID().equals(id)){
                return game;
            }
        }
        return null;
    }

    public static Game getGameByName(String name){
        for (Game game : games){
            if (game.getName().equals(name)){
                return game;
            }
        }
        return null;
    }

    public static void addID(String id){
        if (!ids.contains(id)){
            ids.add(id);
        }
    }

    public static boolean isDuplicate(String id){
        return ids.contains(id);
    }
}