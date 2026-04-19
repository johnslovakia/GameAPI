package cz.johnslovakia.gameapi.modules.game.handlers;

import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.MinigameSettings;
import cz.johnslovakia.gameapi.events.GameJoinEvent;
import cz.johnslovakia.gameapi.events.GamePlayerDeathEvent;
import cz.johnslovakia.gameapi.events.GameQuitEvent;
import cz.johnslovakia.gameapi.listeners.PVPListener;
import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.game.GameInstance;
import cz.johnslovakia.gameapi.modules.game.GameService;
import cz.johnslovakia.gameapi.modules.game.GameState;
import cz.johnslovakia.gameapi.modules.game.lobby.LobbyModule;
import cz.johnslovakia.gameapi.modules.game.map.GameMap;
import cz.johnslovakia.gameapi.modules.game.map.MapModule;
import cz.johnslovakia.gameapi.modules.game.session.GameSessionModule;
import cz.johnslovakia.gameapi.modules.game.session.PlayerGameSession;
import cz.johnslovakia.gameapi.modules.game.task.TaskModule;
import cz.johnslovakia.gameapi.modules.levels.LevelModule;
import cz.johnslovakia.gameapi.modules.messages.MessageModule;
import cz.johnslovakia.gameapi.modules.serverManagement.gameData.GameDataManager;
import cz.johnslovakia.gameapi.modules.game.task.Task;
import cz.johnslovakia.gameapi.modules.game.task.tasks.StartCountdown;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.GamePlayerState;
import cz.johnslovakia.gameapi.users.PlayerManager;
import cz.johnslovakia.gameapi.utils.GameUtils;
import cz.johnslovakia.gameapi.utils.Logger;
import cz.johnslovakia.gameapi.utils.PlayerBossBar;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PlayerJoinQuitHandler {

    private final GameInstance gameInstance;

    public PlayerJoinQuitHandler(GameInstance gameInstance) {
        this.gameInstance = gameInstance;
    }

    public void joinPlayer(Player player){
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);
        MinigameSettings settings = gameInstance.getSettings();

        PlayerGameSession existingSession = gameInstance.getModule(GameSessionModule.class).getPlayerSession(gamePlayer);
        GamePlayerState previousState = existingSession != null ? existingSession.getState() : GamePlayerState.UNKNOWN;

        if (gamePlayer.getGame() != null && !gamePlayer.getGame().equals(gameInstance)){
            gamePlayer.getGame().quitPlayer(player);
        }

        boolean isRejoin = gameInstance.getState().equals(GameState.INGAME)
                && previousState.equals(GamePlayerState.DISCONNECTED)
                && gameInstance.getSettings().isEnabledReJoin();

        PlayerGameSession session;
        if (isRejoin) {
            session = existingSession;
        } else {
            GameSessionModule gameSessionModule = gameInstance.getModule(GameSessionModule.class);
            if (gameInstance.getState().equals(GameState.INGAME)) {
                if (existingSession != null && existingSession.getTeam() != null) {
                    existingSession.getTeam().quitPlayer(gamePlayer);
                }
                gameSessionModule.removePlayerSession(gamePlayer);
            }
            session = gameSessionModule.createPlayerSession(gamePlayer);
        }

        List<GamePlayer> participants = gameInstance.getParticipants();

        if (gameInstance.getState().equals(GameState.WAITING)
                || gameInstance.getState().equals(GameState.STARTING)){
            if (participants.size() >= settings.getMaxPlayers()) {
                if (player.hasPermission("game.joinfullserver")){
                    boolean joined = false;
                    for (int i = (participants.size() - 1); i > 0; i--){
                        Player kickPlayer = participants.get(i).getOnlinePlayer();
                        if (kickPlayer.hasPermission("game.joinfullserver")){
                            continue;
                        }

                        ModuleManager.getModule(MessageModule.class).getMessage(kickPlayer, "server.kicked_because_reserved_slot")
                                .send();
                        GameUtils.sendToLobby(kickPlayer, false);
                        joined = true;
                        break;
                    }

                    if (!joined){
                        ModuleManager.getModule(MessageModule.class).getMessage(player, "vip.full.slots")
                                .send();
                        GameUtils.sendToLobby(player, false);
                        return;
                    }
                }else {
                    ModuleManager.getModule(MessageModule.class).getMessage(player, "server.full")
                            .send();
                    GameUtils.sendToLobby(player, false);
                    return;
                }
            }

            gamePlayer.setGameID(gameInstance.getID());
            player.setDisplayName("§r" + player.getName());
            GameUtils.setTeamNameTag(gamePlayer.getOnlinePlayer(), gameInstance.getName().replace(" ", "") + gameInstance.getID(), ChatColor.WHITE);

            Location lobbyLocation = gameInstance.getModule(LobbyModule.class).getLobbyLocation().getLocation();
            player.teleport(lobbyLocation);
            Bukkit.getScheduler().runTaskLater(Minigame.getInstance().getPlugin(), task -> {
                if (player.getLocation().getWorld() != lobbyLocation.getWorld()){
                    player.teleport(lobbyLocation);
                }
                player.setGameMode(GameMode.ADVENTURE);
            }, 2L);

            participants.add(gamePlayer);
            session.setState(GamePlayerState.PLAYER);
            Bukkit.getScheduler().runTaskAsynchronously(Minigame.getInstance().getPlugin(), task -> gamePlayer.getPlayerData().loadKits());

            LevelModule levelModule = ModuleManager.getModule(LevelModule.class);
            if (levelModule != null){
                int players = gameInstance.getParticipants().size();
                levelModule.loadPlayerData(gamePlayer).thenAccept(data -> {
                    if (data == null) return;

                    Bukkit.getScheduler().runTask(Minigame.getInstance().getPlugin(), task -> {
                        sendPlayerJoinMessage(gamePlayer, players);
                        player.playerListName(data.getLevelEvolution().getIcon().append(Component.text(" §r" + player.getName()).font(Key.key("minecraft", "default"))));

                    });
                });
            }else{
                sendPlayerJoinMessage(gamePlayer, gameInstance.getParticipants().size());
                player.playerListName(Component.text(" §r" + player.getName()));
            }

            GameDataManager<GameInstance> gameDataManager = gameInstance.getServerDataManager();
            if (gameDataManager != null) {
                gameDataManager.updateGame();
            }

            if (gameInstance.getState().equals(GameState.WAITING)) {
                gameInstance.updateWaitingForPlayersBossBar();
            }else{
                if (participants.size() == settings.getReducedPlayers()) {
                    if (gameInstance.getRunningMainTask().getCounter() > settings.getReducedTime()) {
                        gameInstance.getRunningMainTask().setCounter(settings.getReducedTime());
                        ModuleManager.getModule(MessageModule.class).getMessage(participants, "chat.time_reduced")
                                .send();
                    }
                }
            }

            GameUtils.hideAndShowPlayers(gameInstance, gamePlayer.getOnlinePlayer());

            gamePlayer.resetAttributes();
            player.getInventory().clear();
            if (gameInstance.getModule(LobbyModule.class).getInventoryManager() != null){
                gameInstance.getModule(LobbyModule.class).getInventoryManager().give(gamePlayer);
            }else{
                player.sendMessage("§cAn error occurred while loading the waiting lobby inventory.");
            }

            if (gameInstance.getPlayers().size() >= settings.getMinPlayers() && !gameInstance.getState().equals(GameState.STARTING)){
                gameInstance.setState(GameState.STARTING);
                gameInstance.setAutomaticStart(true);
                Task task = gameInstance.getModule(TaskModule.class).addTask(new Task(gameInstance, "StartCountdown", gameInstance.getSettings().getStartingTime(), new StartCountdown(), Minigame.getInstance().getPlugin()));
                task.setAsMainTask();
            }

            GameJoinEvent ev = new GameJoinEvent(gameInstance, gamePlayer, GameJoinEvent.JoinType.LOBBY);
            Bukkit.getPluginManager().callEvent(ev);
            return;
        } else if (isRejoin) {
            if (!participants.contains(gamePlayer)) participants.add(gamePlayer);
            gamePlayer.setGameID(gameInstance.getID());

            Location rejoinLocation = (Location) gamePlayer.getMetadata().get("death_location");
            if (rejoinLocation == null) {
                rejoinLocation = gameInstance.getCurrentMap().getPlayerToLocation(gamePlayer);
            }
            if (rejoinLocation == null && session.getTeam() != null) {
                rejoinLocation = session.getTeam().getSpawn();
            }

            if (rejoinLocation != null) {
                final Location finalRejoinLocation = rejoinLocation;

                Bukkit.getScheduler().runTaskAsynchronously(Minigame.getInstance().getPlugin(), task -> gamePlayer.getPlayerData().loadKits());

                gamePlayer.setSpectator(false);
                gameInstance.getGameStartHandler().preparePlayer(gamePlayer);
                session.setState(GamePlayerState.PLAYER);

                LevelModule levelModule = ModuleManager.getModule(LevelModule.class);
                Runnable afterLevelLoad = () -> {
                    if (Minigame.getInstance().getSettings().isUseTeams() && session.getTeam() != null) {
                        session.getTeam().rejoin(gamePlayer);
                    }
                };

                if (levelModule != null) {
                    levelModule.loadPlayerData(gamePlayer).thenAccept(data -> {
                        Bukkit.getScheduler().runTask(Minigame.getInstance().getPlugin(), afterLevelLoad);
                    });
                } else {
                    afterLevelLoad.run();
                }

                GameJoinEvent ev = new GameJoinEvent(gameInstance, gamePlayer, GameJoinEvent.JoinType.REJOIN);
                Bukkit.getPluginManager().callEvent(ev);

                player.teleport(finalRejoinLocation);
                return;
            } else {
                Logger.log("Could not find rejoin location for " + player.getName(), Logger.LogType.WARNING);
                player.sendMessage("§cAn error occurred, you are being sent to the lobby");
            }
        } else if (gameInstance.getState().equals(GameState.INGAME) && settings.isEnabledJoiningAfterStart()){
            player.teleport(GameUtils.getNonRespawnLocation(gameInstance));
            if (gameInstance.getPlayers().size() < settings.getMaxPlayers()) {
                participants.add(gamePlayer);
                gamePlayer.setGameID(gameInstance.getID());
                gamePlayer.getGameSession().setState(GamePlayerState.PLAYER);

                Bukkit.getScheduler().runTaskAsynchronously(Minigame.getInstance().getPlugin(), task -> {
                    gamePlayer.getPlayerData().loadKits();

                    Bukkit.getScheduler().runTask(Minigame.getInstance().getPlugin(), task2 -> {
                        LevelModule levelModule = ModuleManager.getModule(LevelModule.class);
                        if (levelModule != null){
                            levelModule.loadPlayerData(gamePlayer).thenAccept(data ->
                                    Bukkit.getScheduler().runTask(Minigame.getInstance().getPlugin(), () ->
                                            gameInstance.getGameStartHandler().preparePlayer(gamePlayer)
                                    ));
                        }else{
                            gameInstance.getGameStartHandler().preparePlayer(gamePlayer);
                        }
                    });
                });

                GameJoinEvent ev = new GameJoinEvent(gameInstance, gamePlayer, GameJoinEvent.JoinType.JOIN_AFTER_START);
                Bukkit.getPluginManager().callEvent(ev);
                return;
            }else{
                player.setGameMode(GameMode.SPECTATOR);
                ModuleManager.getModule(MessageModule.class).getMessage(gamePlayer, "chat.join_failed.full_game.game_in_progress")
                        .send();
            }
        } else if (gameInstance.getState().equals(GameState.INGAME)) {
            participants.add(gamePlayer);
            gamePlayer.setGameID(gameInstance.getID());
            gamePlayer.setSpectator(true);

            LevelModule levelModule = ModuleManager.getModule(LevelModule.class);
            if (levelModule != null) {
                levelModule.loadPlayerData(gamePlayer);
            }

            GameJoinEvent ev = new GameJoinEvent(gameInstance, gamePlayer, GameJoinEvent.JoinType.SPECTATOR);
            Bukkit.getPluginManager().callEvent(ev);
            return;
        }

        GameUtils.sendToLobby(player);
    }

    public void sendPlayerJoinMessage(GamePlayer gamePlayer, int players){
        ModuleManager.getModule(MessageModule.class).getMessage(gameInstance.getParticipants(), "chat.join")
                .replace("%prefix%", gamePlayer.getPrefix())
                .replaceWithComponent("%player%", gp -> {
                    boolean highlight = gp.getFriends().isFriendWith(gamePlayer)
                            || gp.getParty().getAllOnlinePlayers().contains(gamePlayer);

                    Component icon = Component.empty();
                    LevelModule lvl = ModuleManager.getModule(LevelModule.class);
                    if (lvl != null) icon = lvl.getPlayerData(gamePlayer).getLevelEvolution().getIcon();

                    return icon
                            .appendSpace()
                            .append(Component.text(
                                    highlight ? "§6" : "§f" + gamePlayer.getOnlinePlayer().getName())
                            ).font(Key.key("minecraft", "default"));
                })
                .replace("%players%", "" + players)
                .replace("%max_players%", "" + gameInstance.getSettings().getMaxPlayers())
                .add("Ẅ", gp -> gp.getFriends().isFriendWith(gamePlayer))
                .add("ẅ", gp -> gp.getParty().getAllOnlinePlayers().contains(gamePlayer))
                .send();
    }

    public void spectateGame(Player player){
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);

        if (gamePlayer.getGame() != null){
            gamePlayer.getGame().quitPlayer(player);
        }

        gameInstance.getModule(GameSessionModule.class).createPlayerSession(gamePlayer);

        gameInstance.getParticipants().add(gamePlayer);
        gamePlayer.setGameID(gameInstance.getID());
        gamePlayer.setSpectator(true);
        GameUtils.hideAndShowPlayers(gameInstance, player);

        player.teleport(GameUtils.getNonRespawnLocation(gameInstance));

        LevelModule levelModule = ModuleManager.getModule(LevelModule.class);
        if (levelModule != null) {
            levelModule.loadPlayerData(gamePlayer);
        }

        GameJoinEvent ev = new GameJoinEvent(gameInstance, gamePlayer, GameJoinEvent.JoinType.SPECTATOR);
        Bukkit.getPluginManager().callEvent(ev);
    }

    public void quitPlayer(Player player){
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);
        PlayerGameSession session = gameInstance.getModule(GameSessionModule.class).getPlayerSession(gamePlayer);

        GameQuitEvent ev = new GameQuitEvent(gameInstance, gamePlayer);
        Bukkit.getPluginManager().callEvent(ev);

        PlayerBossBar.removeBossBar(player.getUniqueId());

        GameDataManager<GameInstance> gameDataManager = gameInstance.getServerDataManager();
        if (gameDataManager != null) {
            gameDataManager.updateGame();
        }

        gameInstance.getParticipants().remove(gamePlayer);

        if (gameInstance.getState().equals(GameState.WAITING) || gameInstance.getState().equals(GameState.STARTING)){
            ModuleManager.getModule(MessageModule.class).getMessage(gameInstance.getParticipants(), "chat.quit")
                    .replace("%prefix%", gamePlayer.getPrefix())
                    .replaceWithComponent("%player%", gp -> {
                        boolean highlight = gp.getFriends().isFriendWith(gamePlayer)
                                || gp.getParty().getAllOnlinePlayers().contains(gamePlayer);

                        Component icon = Component.empty();
                        LevelModule lvl = ModuleManager.getModule(LevelModule.class);
                        if (lvl != null && lvl.getPlayerData(gamePlayer) != null) {
                            icon = lvl.getPlayerData(gamePlayer).getLevelEvolution().getIcon();
                        }

                        return icon
                                .appendSpace()
                                .append(Component.text(
                                        (highlight ? "§6" : "§f") + gamePlayer.getOnlinePlayer().getName())
                                ).font(Key.key("minecraft", "default"));
                    })
                    .replace("%players%", String.valueOf(gameInstance.getPlayers().size()))
                    .replace("%max_players%", "" + gameInstance.getSettings().getMaxPlayers())
                    .add("", gp -> gp.getFriends().isFriendWith(gamePlayer))
                    .add("", gp -> gp.getParty().getAllOnlinePlayers().contains(gamePlayer))
                    .send();

            gameInstance.getModule(MapModule.class).removePlayerVotes(gamePlayer);
            if (session.getTeam() != null) {
                session.getTeam().quitPlayer(gamePlayer);
            }
            session.setState(GamePlayerState.DISCONNECTED);

            if (gameInstance.getState().equals(GameState.STARTING)) {
                gameInstance.checkArenaFullness();
            }else if (gameInstance.getState().equals(GameState.WAITING)){
                gameInstance.updateWaitingForPlayersBossBar();
            }
        } else if (gameInstance.getState() == GameState.INGAME){
            if (!gamePlayer.isSpectator()){
                if (gameInstance.getSettings().isEnabledReJoin()) {
                    gamePlayer.getMetadata().put("rejoin_inventory", player.getInventory().getContents().clone());
                    gamePlayer.getMetadata().put("rejoin_armor", player.getInventory().getArmorContents().clone());
                    gamePlayer.getMetadata().put("rejoin_offhand", player.getInventory().getItemInOffHand().clone());
                }

                session.setState(GamePlayerState.DISCONNECTED);

                boolean hasKiller = PVPListener.hasLastDamager(gamePlayer) && (System.currentTimeMillis() - PVPListener.getLastDamager(gamePlayer).getMs()) <= PVPListener.KILL_CREDIT_TIMEOUT_MS;
                GamePlayer killerGamePlayer = hasKiller ? PVPListener.getLastDamager(gamePlayer).getLastDamager() : null;
                List<GamePlayer> assists = PVPListener.getAssists(gamePlayer, killerGamePlayer);

                GamePlayerDeathEvent deathEvent = new GamePlayerDeathEvent(gameInstance, killerGamePlayer, gamePlayer, assists, null, new ArrayList<>());
                Bukkit.getPluginManager().callEvent(deathEvent);

                PVPListener.cleanupPlayer(gamePlayer);
            }else if (!Minigame.getInstance().getSettings().isEnabledReJoin()){
                GameMap currentMap = gameInstance.getCurrentMap();
                if (currentMap != null){
                    currentMap.getPlayerToLocation().remove(gamePlayer);
                }
            }

            if (gameInstance.getPlayers().isEmpty()){
                gameInstance.endGame(null);
            }
        }
        Bukkit.getScheduler().runTaskLater(Minigame.getInstance().getPlugin(), task -> {
            if (gamePlayer.isOnline()) return;
            gamePlayer.setGameID(null);
        }, 10L);
    }
}