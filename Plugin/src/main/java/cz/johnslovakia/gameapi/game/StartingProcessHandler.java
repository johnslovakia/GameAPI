package cz.johnslovakia.gameapi.game;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.events.GamePreparationEvent;
import cz.johnslovakia.gameapi.events.GameStartEvent;
import cz.johnslovakia.gameapi.game.map.GameMap;
import cz.johnslovakia.gameapi.game.team.GameTeam;
import cz.johnslovakia.gameapi.game.team.TeamJoinCause;
import cz.johnslovakia.gameapi.game.team.TeamManager;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.task.Task;
import cz.johnslovakia.gameapi.task.tasks.GameCountdown;
import cz.johnslovakia.gameapi.task.tasks.PreparationCountdown;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.GamePlayerType;
import cz.johnslovakia.gameapi.users.PlayerData;
import cz.johnslovakia.gameapi.users.PlayerManager;
import cz.johnslovakia.gameapi.users.stats.StatsHolograms;
import cz.johnslovakia.gameapi.utils.Logger;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Iterator;
import java.util.UUID;

public interface StartingProcessHandler {


    Game getGame();

    default void startPreparation(){
        prepareGame();
        getGame().setState(GameState.INGAME);

        GamePreparationEvent ev = new GamePreparationEvent(getGame());
        Bukkit.getPluginManager().callEvent(ev);


        for (GamePlayer gamePlayer : getGame().getPlayers()){
            gamePlayer.setEnabledMovement(getGame().getSettings().isEnabledMovementInPreparation());
            gamePlayer.setLimited(true);
        }

        Task task = new Task(getGame(), "PreparationTask", getGame().getSettings().getPreparationTime(), new PreparationCountdown(), GameAPI.getInstance());
        task.setGame(getGame());
    };

    default void prepareGame(){
        if (!getGame().getSettings().isChooseRandomMap()) {
            if (getGame().nextArena() == null) {
                return;
            }
            getGame().nextArena().setWinned(true);
        }


        GameMap playingMap = getGame().getCurrentMap();

        if (playingMap.getSettings().isLoadWorldWithGameAPI()) {
            String worldName = playingMap.getName().replaceAll(" ", "_") + "_" + getGame().getID();
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                playingMap.setWorld(world);
            } else {
                Logger.log("I haven't had time to load the world!", Logger.LogType.ERROR);
                return;
            }
        }

        if (getGame().getSettings().isUseTeams()){
            for (GamePlayer gamePlayer : getGame().getParticipants()){
                if (gamePlayer.getPlayerData().getTeam() == null){
                    GameTeam lowestTeam = getGame().getTeamManager().getSmallestTeam();
                    lowestTeam.joinPlayer(gamePlayer, TeamJoinCause.AUTO);
                }
            }
        }



        if (getGame().getSettings().isEnabledRespawning() && getGame().getSettings().useTeams()){
            getGame().getTeamManager().getTeams().forEach(team -> team.setDead(false));
        }

        getGame().getCurrentMap().getWorld().setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);

        getGame().getCurrentMap().teleport();
        for (GamePlayer gamePlayer : getGame().getPlayers()) {
            preparePlayer(gamePlayer);

            if (GameAPI.getInstance().useDecentHolograms()) {
                StatsHolograms.remove(gamePlayer.getOnlinePlayer());
            }
        }


        Iterator<UUID> iterator = PlayerManager.getPlayerMap().keySet().iterator();
        while (iterator.hasNext()) {
            UUID uuid = iterator.next();
            GamePlayer gamePlayer = PlayerManager.getPlayerMap().get(uuid);
            if (!gamePlayer.isOnline() && gamePlayer.getPlayerData().getGame().equals(this)) {
                iterator.remove();
            }
        }
    };

    default void preparePlayer(GamePlayer gamePlayer){
        boolean rejoin = gamePlayer.getType().equals(GamePlayerType.DISCONNECTED);
        Player player = gamePlayer.getOnlinePlayer();

        gamePlayer.resetAttributes();

        if (!rejoin) {
            player.sendMessage("");
            if (getGame().getSettings().sendMinigameDescription()) {
                MessageManager.get(gamePlayer, "chat.description")
                        .replace("%minigame%", GameAPI.getInstance().getMinigame().getName())
                        .replace("%description%", MessageManager.get(gamePlayer, GameAPI.getInstance().getMinigame().getDescriptionTranslateKey()).getTranslated())
                        .replace("%map%",
                                MessageManager.get(gamePlayer, "chat.description.map")
                                        .replace("%map%", getGame().getCurrentMap().getName()
                                                .replace("%authors%", getGame().getCurrentMap().getAuthors()))
                                        .getTranslated())
                        .replace("%authors%", getGame().getCurrentMap().getAuthors())
                        .send();
            }
            if (getGame().getSettings().isUseTeams() && getGame().getSettings().getMaxTeamPlayers() > 1) {
                player.sendMessage(MessageManager.get(player, "chat.team_chat").getTranslated());
            }

            if (getGame().getLobbyInventory() != null) {
                getGame().getLobbyInventory().unloadInventory(player);
            }
            player.getInventory().clear();

            if (gamePlayer.getPlayerData().getKit() != null) {
                gamePlayer.getPlayerData().getKit().activate(gamePlayer);
            }
            if (getGame().getState().equals(GameState.INGAME) && getGame().getSettings().isAllowedJoiningAfterStart()) {
                player.teleport(gamePlayer.getPlayerData().getTeam().getSpawn());
            }
        }
    };

    default void startGame(){
        getGame().setState(GameState.INGAME);
        getGame().getMetadata().put("players_at_start", getGame().getPlayers().size());

        if (getGame().getSettings().isDefaultGameCountdown()) {
            Bukkit.getScheduler().runTaskLater(GameAPI.getInstance(), t -> {
                Task task = new Task(getGame(), "GameCountdown", getGame().getSettings().getGameTime(), new GameCountdown(), GameAPI.getInstance());
                task.setGame(getGame());

                if (getGame().getSettings().getRounds() > 1){
                    task.setRestartCount(getGame().getSettings().getRounds() - 1);
                }
            }, 2L);
        }

        if (!getGame().getSettings().usePreperationTask()){
            Task.cancel(getGame(), "StartCountdown");
            prepareGame();
        }

        for (GamePlayer gamePlayer : getGame().getParticipants()){
            gamePlayer.setEnabledMovement(true);
            gamePlayer.setLimited(false);

            PlayerData data = gamePlayer.getPlayerData();
            data.getKitInventories().clear();
            data.getVotesForMaps().clear();
        }

        GameStartEvent ev = new GameStartEvent(getGame());
        Bukkit.getPluginManager().callEvent(ev);
    }
}