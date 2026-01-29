package cz.johnslovakia.gameapi.modules.game.handlers;

import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.events.GamePreparationEvent;
import cz.johnslovakia.gameapi.events.GameStartEvent;
import cz.johnslovakia.gameapi.inventoryBuilder.InventoryBuilder;
import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.game.GameInstance;
import cz.johnslovakia.gameapi.modules.game.GameState;
import cz.johnslovakia.gameapi.modules.game.lobby.LobbyModule;
import cz.johnslovakia.gameapi.modules.game.map.GameMap;
import cz.johnslovakia.gameapi.modules.game.session.PlayerGameSession;
import cz.johnslovakia.gameapi.modules.game.task.TaskModule;
import cz.johnslovakia.gameapi.modules.game.team.GameTeam;
import cz.johnslovakia.gameapi.modules.game.team.TeamJoinCause;
import cz.johnslovakia.gameapi.modules.game.team.TeamModule;
import cz.johnslovakia.gameapi.modules.messages.MessageModule;
import cz.johnslovakia.gameapi.modules.stats.StatsModule;
import cz.johnslovakia.gameapi.modules.game.task.Task;
import cz.johnslovakia.gameapi.modules.game.task.tasks.GameCountdown;
import cz.johnslovakia.gameapi.modules.game.task.tasks.PreparationCountdown;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.GamePlayerState;
import cz.johnslovakia.gameapi.utils.Logger;
import cz.johnslovakia.gameapi.worldManagement.WorldManager;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class GameStartHandler {

    private final GameInstance gameInstance;

    public GameStartHandler(GameInstance gameInstance) {
        this.gameInstance = gameInstance;
    }

    public void startPreparation(){
        Task task = gameInstance.getModule(TaskModule.class).addTask(new Task(gameInstance, "PreparationTask", gameInstance.getSettings().getPreparationTime(), new PreparationCountdown(), Minigame.getInstance().getPlugin()));
        task.setAsMainTask();

        prepareGame();
        gameInstance.getMetadata().put("players_at_start", gameInstance.getPlayers().size());
        gameInstance.setState(GameState.INGAME);

        GamePreparationEvent ev = new GamePreparationEvent(gameInstance);
        Bukkit.getPluginManager().callEvent(ev);


        for (GamePlayer gamePlayer : gameInstance.getPlayers()){
            PlayerGameSession session = gamePlayer.getGameSession();
            session.setEnabledMovement(gameInstance.getSettings().isEnabledMovementInPreparation());
            session.setLimited(true);
        }
    };

    public void prepareGame(){
        if (!gameInstance.getSettings().isChooseRandomMap()) {
            if (gameInstance.nextArena() == null) {
                return;
            }
            gameInstance.nextArena().setWinned(true);
        }


        GameMap playingMap = gameInstance.getCurrentMap();

        if (playingMap.getSettings().isLoadWorldWithGameAPI()) {
            String worldName = playingMap.getName().replaceAll(" ", "_") + "_" + gameInstance.getID();
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                playingMap.setWorld(world);
            } else {
                Logger.log("I haven't had time to load the world!", Logger.LogType.ERROR);
                return;
            }
        }

        if (gameInstance.getSettings().isUseTeams()){
            for (GamePlayer gamePlayer : gameInstance.getParticipants()){
                PlayerGameSession session = gamePlayer.getGameSession();
                if (session.getTeam() == null){
                    GameTeam lowestTeam = gameInstance.getModule(TeamModule.class).getSmallestTeam();
                    lowestTeam.joinPlayer(gamePlayer, TeamJoinCause.AUTO);
                }
            }
        }



        if (gameInstance.getSettings().isEnabledRespawning() && gameInstance.getSettings().isUseTeams()){
            gameInstance.getModule(TeamModule.class).getTeams().values().forEach(team -> team.setDead(false));
        }

        gameInstance.getCurrentMap().getWorld().setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        gameInstance.getCurrentMap().getWorld().setGameRule(GameRule.LOCATOR_BAR, false);

        gameInstance.getCurrentMap().teleport();
        for (GamePlayer gamePlayer : gameInstance.getPlayers()) {
            Player player = gamePlayer.getOnlinePlayer();

            preparePlayer(gamePlayer);
            player.setLevel(0);
            player.setExp(0);

            ModuleManager.getModule(StatsModule.class).getStatsHolograms().remove(gamePlayer);
        }
    }

    public void preparePlayer(GamePlayer gamePlayer){
        PlayerGameSession session = gamePlayer.getGameSession();
        boolean rejoin = session.getState().equals(GamePlayerState.DISCONNECTED);
        Player player = gamePlayer.getOnlinePlayer();

        gamePlayer.resetAttributes();

        InventoryBuilder inventoryManager = InventoryBuilder.getPlayerCurrentInventory(gamePlayer);
        if (inventoryManager != null) {
            inventoryManager.unloadInventory(gamePlayer);
        }

        if (!rejoin) {
            player.sendMessage("");
            if (gameInstance.getSettings().isSendMinigameDescription()) {
                ModuleManager.getModule(MessageModule.class).get(gamePlayer, "chat.description")
                        .replace("%minigame%", Minigame.getInstance().getName())
                        .replace("%description%", ModuleManager.getModule(MessageModule.class).get(gamePlayer, Minigame.getInstance().getDescriptionTranslateKey()).getTranslated())
                        .replace("%map%",
                                ModuleManager.getModule(MessageModule.class).get(gamePlayer, "chat.description.map")
                                        .replace("%map%", gameInstance.getCurrentMap().getName()
                                                .replace("%authors%", gameInstance.getCurrentMap().getAuthors()))
                                        .getTranslated())
                        .replace("%authors%", gameInstance.getCurrentMap().getAuthors())
                        .send();
            }
            if (gameInstance.getSettings().isUseTeams() && gameInstance.getSettings().getMaxTeamPlayers() > 1) {
                player.sendMessage(ModuleManager.getModule(MessageModule.class).get(player, "chat.team_chat").getTranslated());
            }

            if (session.getSelectedKit() != null) {
                session.getSelectedKit().activate(gamePlayer);
            }
            if (gameInstance.getState().equals(GameState.INGAME) && gameInstance.getSettings().isEnabledJoiningAfterStart()) {
                player.teleport(session.getTeam().getSpawn());
            }
        }
    }

    public void startGame(){
        gameInstance.setState(GameState.INGAME);
        gameInstance.getMetadata().put("players_at_start", gameInstance.getPlayers().size());

        if (gameInstance.getSettings().isDefaultGameCountdown()) {
            Bukkit.getScheduler().runTaskLater(Minigame.getInstance().getPlugin(), t -> {
                Task task = gameInstance.getModule(TaskModule.class).addTask(new Task(gameInstance, "GameCountdown", gameInstance.getSettings().getGameTime(), new GameCountdown(), Minigame.getInstance().getPlugin()));
                task.setAsMainTask();

                if (gameInstance.getSettings().getRounds() > 1){
                    task.setRestartCount(gameInstance.getSettings().getRounds() - 1);
                }
            }, 2L);
        }

        if (!gameInstance.getSettings().isUsePreperationTask()){
            gameInstance.getModule(TaskModule.class).cancel("StartCountdown", true);
            prepareGame();
        }else{
            gameInstance.getModule(TaskModule.class).cancel("PreparationTask", true);
        }

        for (GamePlayer gamePlayer : gameInstance.getParticipants()){
            PlayerGameSession session = gamePlayer.getGameSession();
            session.setEnabledMovement(true);
            session.setLimited(false);

        }


        WorldManager.unload(gameInstance.getModule(LobbyModule.class).getLobbyLocation().getWorld());


        GameStartEvent ev = new GameStartEvent(gameInstance);
        Bukkit.getPluginManager().callEvent(ev);
    }
}
