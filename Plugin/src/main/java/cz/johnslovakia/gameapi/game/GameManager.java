package cz.johnslovakia.gameapi.game;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.datastorage.Type;
import cz.johnslovakia.gameapi.events.GameResetEvent;
import cz.johnslovakia.gameapi.events.NewArenaEvent;
import cz.johnslovakia.gameapi.game.map.GameMap;
import cz.johnslovakia.gameapi.serverManagement.IGame;
import cz.johnslovakia.gameapi.serverManagement.gameData.GameDataManager;
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
        if (games.contains(game)) return;

        games.add(game);
        game.finishSetup();
        Logger.log("Added Game " + game.getID(), Logger.LogType.INFO);

        if (games.size() > 1 && GameAPI.getInstance().getMinigame().getDatabase() != null){
            GameAPI.getInstance().getMinigame().getMinigameTable().createNewColumn(Type.VARCHAR128, "game");
        }

    }

    public static void newArena(Player player, boolean sendToLobbyIfNoArena) {
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);

        NewArenaEvent ev = new NewArenaEvent(PlayerManager.getGamePlayer(player));
        Bukkit.getPluginManager().callEvent(ev);

        if (ev.isCancelled()) return;

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
        }else if (GameAPI.getInstance().getMinigame().getDataManager() != null){
            IGame bestArena = GameAPI.getInstance().getMinigame().getDataManager().getBestServer();
            if ((GameAPI.getInstance().getMinigame().getServerDataMySQL() != null || GameAPI.getInstance().getMinigame().getServerDataRedis() != null) && bestArena != null) {
                bestArena.sendPlayerToServer(gamePlayer);
                return;
            }
        }

        if (sendToLobbyIfNoArena){
            MessageManager.get(gamePlayer, "chat.sending_to_lobby.no_arena_found").send();
            Utils.sendToLobby(player, false);
        }else{
            MessageManager.get(gamePlayer, "chat.no_arena_found").send();
        }
    }

    public static Game getHighestGame() {
        Game highest = null;

        if (GameManager.getGames().isEmpty()) return null;

        for(Game game : GameManager.getGames()) {
            if (game.getState() != GameState.WAITING && game.getState() != GameState.STARTING) {
                if (!game.getSettings().isAllowedJoiningAfterStart()){
                    continue;
                }
            }
            if (game.getPlayers().size() >= game.getSettings().getMaxPlayers()) continue;

            if (highest == null || game.getPlayers().size() > highest.getPlayers().size()) {
                highest = game;
            }
        }


        return highest;
    }

    public static void resetGame(Game game){
        if (game.getSettings().restartServerAfterEnd()){
            game.getParticipants().forEach(gp -> GameManager.newArena(gp.getOnlinePlayer(), true));
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
        game.getTeamManager().resetTeamsAndRegisterForNewGame(newGame);

        newGame.setMapManager(game.getMapManager());
        game.getMapManager().setVoting(true);
        game.getMapManager().setGame(newGame);
        for (GameMap map : newGame.getMapManager().getMaps()) {
            map.setGame(newGame);
            map.setVotes(0);
            map.setPlayed(false);
            map.setWinned(false);
            map.setWorld(null);
        }


        List<Player> players = new ArrayList<>();
        if (!game.getParticipants().isEmpty()) {
            for (int i = 0; i < game.getParticipants().size(); i++) {
                GamePlayer gamePlayer = game.getParticipants().get(i);
                Player player = gamePlayer.getOnlinePlayer();

                if (gamePlayer.getOnlinePlayer().isOnline()) {
                    players.add(player);
                }
                PlayerManager.removeGamePlayer(player);
            }
        }

        registerGame(newGame);

        if (!players.isEmpty()) {
            for (Player player : players) {
                GameManager.newArena(player, true);
            }
        }

        GameResetEvent ev = new GameResetEvent(game, newGame);
        Bukkit.getPluginManager().callEvent(ev);

        games.remove(game);
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