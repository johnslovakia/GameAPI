package cz.johnslovakia.gameapi.game;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.datastorage.Type;
import cz.johnslovakia.gameapi.events.GameResetEvent;
import cz.johnslovakia.gameapi.events.NewArenaEvent;
import cz.johnslovakia.gameapi.game.kit.KitManager;
import cz.johnslovakia.gameapi.game.map.GameMap;
import cz.johnslovakia.gameapi.serverManagement.DataManager;
import cz.johnslovakia.gameapi.serverManagement.IGame;
import cz.johnslovakia.gameapi.serverManagement.gameData.GameDataManager;
import cz.johnslovakia.gameapi.task.Task;
import cz.johnslovakia.gameapi.users.PlayerManager;
import cz.johnslovakia.gameapi.game.team.TeamManager;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.utils.Utils;
import cz.johnslovakia.gameapi.utils.Logger;
import cz.johnslovakia.gameapi.worldManagement.WorldManager;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.World;
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

        game.finishSetup(success -> {
            if (!success){
                Logger.log("Game (" + game.getID() + ") setup failed.", Logger.LogType.INFO);
            }else{
                games.add(game);
                Logger.log("Added Game " + game.getID(), Logger.LogType.INFO);
            }
        });

        /*if (games.size() > 1 && Minigame.getInstance().getDatabase() != null){
            //TODO: dát jinde
            Bukkit.getScheduler().runTaskAsynchronously(Minigame.getInstance().getPlugin(), task -> Minigame.getInstance().getMinigameTable().createNewColumn(Type.VARCHAR128, "game"));
        }*/
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
                    game = partyMember.getGame();
                }
            }
        }
        if (game != null) {
            if (gamePlayer.getGame() != null) {
                gamePlayer.getGame().quitPlayer(player);
            }
            game.joinPlayer(player);
            return;
        }else if (DataManager.getInstance() != null){
            if (DataManager.getInstance().getMinigame(Minigame.getInstance().getName()).isPresent()) {
                IGame bestArena = DataManager.getInstance().getMinigame(Minigame.getInstance().getName()).get().getBestServer();
                if ((DataManager.getInstance().getServerDataMySQL() != null || DataManager.getInstance().getServerDataRedis() != null) && bestArena != null) {
                    bestArena.sendPlayerToServer(gamePlayer);
                    return;
                }
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
        game.setState(GameState.RESTARTING);

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
            }.runTaskLater(Minigame.getInstance().getPlugin(), 80L);
            return;
        }

        //TODO: zkontrolovat
        KitManager.removeKitManager(game);


        Game newGame = Minigame.getInstance().setupGame(game.getName());

        //TODO: udělat lépe
        game.setName("ToRemove-" + game.getID());

        List<Player> players = new ArrayList<>();
        if (!game.getParticipants().isEmpty()) {
            game.getParticipants().forEach(gamePlayer -> {
                if (gamePlayer.getOnlinePlayer().isOnline()) {
                    players.add(gamePlayer.getOnlinePlayer());
                }else{
                    PlayerManager.removeGamePlayer(gamePlayer.getOnlinePlayer());
                }
            });
        }


        GameResetEvent ev = new GameResetEvent(game, newGame);
        Bukkit.getPluginManager().callEvent(ev);

        if (!players.isEmpty()) {
            for (Player player : players) {
                MessageManager.get(player, "chat.finding_new_game").send();
                Bukkit.getScheduler().runTaskLater(Minigame.getInstance().getPlugin(), task -> {
                    PlayerManager.removeGamePlayer(player);
                    GameManager.newArena(player, true);
                }, 20L);
            }
        }


        Bukkit.getScheduler().runTaskLater(Minigame.getInstance().getPlugin(), task -> {
            try {
                WorldManager.unload(game.getCurrentMap().getWorld());
            }catch (Exception e){
                e.printStackTrace();
            }
            games.remove(game);
        }, 70L);
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