package cz.johnslovakia.gameapi.modules.game;

import cz.johnslovakia.gameapi.modules.Module;
import cz.johnslovakia.gameapi.modules.kits.KitManager;
import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.events.GameResetEvent;
import cz.johnslovakia.gameapi.events.NewArenaEvent;
import cz.johnslovakia.gameapi.modules.levels.LevelModule;
import cz.johnslovakia.gameapi.modules.messages.MessageModule;
import cz.johnslovakia.gameapi.modules.serverManagement.DataManager;
import cz.johnslovakia.gameapi.modules.serverManagement.IGame;
import cz.johnslovakia.gameapi.users.PlayerIdentityRegistry;
import cz.johnslovakia.gameapi.users.PlayerManager;
import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.utils.GameUtils;
import cz.johnslovakia.gameapi.utils.Logger;
import cz.johnslovakia.gameapi.worldManagement.WorldManager;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

@Getter
@NoArgsConstructor
public class GameService implements Module {

    private Map<String, GameInstance> games = new HashMap<>();
    private List<String> ids = new ArrayList<>();

    @Override
    public void initialize() {

    }

    @Override
    public void terminate() {
        games = null;
        ids = null;
    }

    public void registerGame(GameInstance game){
        game.finishSetup(success -> {
            if (!success){
                Logger.log("Game (" + game.getID() + ") setup failed.", Logger.LogType.INFO);
            }else{
                games.put(game.getID(), game);
                Logger.log("Added Game " + game.getID(), Logger.LogType.INFO);
            }
        });
        /*if (games.size() > 1 && Minigame.getInstance().getDatabase() != null){
            //TODO: dát jinde
            Bukkit.getScheduler().runTaskAsynchronously(Minigame.getInstance().getPlugin(), task -> Minigame.getInstance().getMinigameTable().createNewColumn(Type.VARCHAR128, "game"));
        }*/
    }

    public void newArena(Player player, boolean sendToLobbyIfNoArena) {
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);

        NewArenaEvent ev = new NewArenaEvent(PlayerManager.getGamePlayer(player));
        Bukkit.getPluginManager().callEvent(ev);

        if (ev.isCancelled()) return;

        GameInstance game = getHighestGame();
        if (gamePlayer.getParty().isInParty()){
            for (GamePlayer partyMember : gamePlayer.getParty().getAllOnlinePlayers().stream()
                    .map(p -> (GamePlayer) p)
                    .toList()){
                if (partyMember.isInGame()){
                    game = partyMember.getGame();
                }
            }
        }
        if (game != null) {
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
            ModuleManager.getModule(MessageModule.class).get(gamePlayer, "chat.sending_to_lobby.no_arena_found").send();
            GameUtils.sendToLobby(player, false);
        }else{
            ModuleManager.getModule(MessageModule.class).get(gamePlayer, "chat.no_arena_found").send();
        }
    }

    public GameInstance getHighestGame() {
        GameInstance highest = null;

        if (getGames().isEmpty()) return null;

        for(GameInstance game : getGames().values()) {
            if (game.getState().equals(GameState.INGAME) && !game.getSettings().isEnabledJoiningAfterStart()){
                continue;
            }
            if (game.getPlayers().size() >= game.getSettings().getMaxPlayers()) continue;

            if (highest == null || game.getPlayers().size() > highest.getPlayers().size()) {
                highest = game;
            }
        }

        return highest;
    }

    public void resetGame(GameInstance toResetGame){
        toResetGame.setState(GameState.RESTARTING);

        if (toResetGame.getSettings().isRestartServerAfterEnd()){
            toResetGame.getParticipants().forEach(gp -> newArena(gp.getOnlinePlayer(), true));
            new BukkitRunnable(){
                @Override
                public void run() {
                    if (!Bukkit.getOnlinePlayers().isEmpty()){
                        Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage("§cRestarting server..."));
                    }
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "restart");
                    //Bukkit.shutdown();
                }
            }.runTaskLater(Minigame.getInstance().getPlugin(), 80L);
            return;
        }

        KitManager.removeKitManager(toResetGame);



        String gameName = toResetGame.getName();
        games.remove(toResetGame.getID());

        GameInstance newGame = Minigame.getInstance().setupGame(gameName);


        List<Player> players = new ArrayList<>();
        if (!toResetGame.getParticipants().isEmpty()) {
            toResetGame.getParticipants().forEach(gamePlayer -> {
                if (gamePlayer.getOnlinePlayer().isOnline()) {
                    players.add(gamePlayer.getOnlinePlayer());
                }else{
                    LevelModule levelModule = ModuleManager.getModule(LevelModule.class);
                    if (levelModule != null){
                        levelModule.getCache().remove(gamePlayer);
                    }
                    PlayerIdentityRegistry.unregister(gamePlayer.getUniqueId());
                }
            });
        }


        GameResetEvent ev = new GameResetEvent(toResetGame, newGame);
        Bukkit.getPluginManager().callEvent(ev);

        if (!players.isEmpty()) {
            for (Player player : players) {
                ModuleManager.getModule(MessageModule.class).get(player, "chat.finding_new_game").send();
                Bukkit.getScheduler().runTaskLater(Minigame.getInstance().getPlugin(), task -> {
                    //PlayerManager.removeGamePlayer(player);
                    newArena(player, true);
                }, 25L);
            }
        }


        Bukkit.getScheduler().runTaskLater(Minigame.getInstance().getPlugin(), task -> {
            try {
                WorldManager.unload(toResetGame.getCurrentMap().getWorld());
            }catch (Exception e){
                e.printStackTrace();
            }
            toResetGame.destroyAllModules();
            //games.remove(toResetGame.getID());
        }, 70L);
    }

    public Optional<GameInstance> getGameByID(String id){
        return Optional.ofNullable(games.get(id));
    }

    public Optional<GameInstance> getGameByName(String name){
        return games.values().stream().filter(gameInstance -> gameInstance.getName().equalsIgnoreCase(name)).findAny();
    }

    public void addID(String id){
        if (!ids.contains(id)){
            ids.add(id);
        }
    }

    public boolean isDuplicate(String id){
        return ids.contains(id);
    }
}