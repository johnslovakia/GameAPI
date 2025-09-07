package cz.johnslovakia.gameapi.game;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.MinigameSettings;
import cz.johnslovakia.gameapi.guis.ProfileInventory;
import cz.johnslovakia.gameapi.guis.QuestInventory;
import cz.johnslovakia.gameapi.levelSystem.LevelManager;
import cz.johnslovakia.gameapi.levelSystem.LevelProgress;
import cz.johnslovakia.gameapi.messages.Message;
import cz.johnslovakia.gameapi.serverManagement.DataManager;
import cz.johnslovakia.gameapi.serverManagement.gameData.GameDataManager;
import cz.johnslovakia.gameapi.users.resources.Resource;
import cz.johnslovakia.gameapi.users.resources.ResourcesManager;
import cz.johnslovakia.gameapi.events.*;
import cz.johnslovakia.gameapi.game.map.GameMap;
import cz.johnslovakia.gameapi.game.map.MapVotesComparator;
import cz.johnslovakia.gameapi.game.team.GameTeam;
import cz.johnslovakia.gameapi.game.map.MapManager;
import cz.johnslovakia.gameapi.task.tasks.EndCountdown;
import cz.johnslovakia.gameapi.task.tasks.StartCountdown;
import cz.johnslovakia.gameapi.users.*;
import cz.johnslovakia.gameapi.game.team.TeamManager;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.task.Task;
import cz.johnslovakia.gameapi.users.friends.FriendsInterface;
import cz.johnslovakia.gameapi.users.parties.PartyInterface;
import cz.johnslovakia.gameapi.utils.*;
import cz.johnslovakia.gameapi.utils.chatHead.ChatHeadAPI;
import cz.johnslovakia.gameapi.utils.inventoryBuilder.InventoryManager;
import cz.johnslovakia.gameapi.utils.inventoryBuilder.Item;
import cz.johnslovakia.gameapi.utils.rewards.unclaimed.UnclaimedReward;
import cz.johnslovakia.gameapi.worldManagement.WorldManager;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.function.Consumer;

@Getter
public class Game {
    private final Game game = Game.this;

    @Setter
    private String name;
    private String ID;
    private GameState state = GameState.LOADING;
    @Setter
    private Task runningMainTask;
    @Setter
    private LobbyManager lobbyManager;
    @Setter
    private SpectatorManager spectatorManager;
    @Setter
    private boolean firstGameKill = true;

    @Setter
    private MapManager mapManager;
    @Setter
    private TeamManager teamManager;
    private GameDataManager serverDataManager;
    @Setter
    private StartingProcessHandler startingProcessHandler = new StartingProcessHandler() {
        @Override
        public Game getGame() {
            return game;
        }
    };

    private final List<GamePlayer> participants = new ArrayList<>();
    private List<Block> placedBlocks;
    private final Map<String, Object> metadata = new WeakHashMap<>();
    private boolean automaticStart = false;

    public Game(String name) {
        this.name = name;

        this.spectatorManager = new SpectatorManager();

        while (this.ID == null || GameManager.isDuplicate(this.ID)){
            this.ID = StringUtils.randomString(6, true, true, false);
        }
        GameManager.addID(this.ID);
    }

    public void finishSetup(Consumer<Boolean> callback){
        getSpectatorManager().loadItemManager();

        if (getSettings().isChooseRandomMap()){
            selectRandomMap();

            new BukkitRunnable() {
                int attempts = 0;

                @Override
                public void run() {
                    if (getCurrentMap().getWorld() != null) {
                        state = GameState.WAITING;
                        callback.accept(true);
                        this.cancel();
                        return;
                    }

                    if (++attempts >= 10) {
                        Logger.log("Game: The world for the selected map " + getCurrentMap().getName() + "  is missing or not loaded!", Logger.LogType.ERROR);
                        callback.accept(false);
                        this.cancel();
                    }
                }
            }.runTaskTimer(Minigame.getInstance().getPlugin(), 20L, 20L);
        }else {
            Bukkit.getScheduler().runTaskLater(Minigame.getInstance().getPlugin(), task -> {
                state = GameState.WAITING;
                callback.accept(true);
            }, 10L);
        }

        Minigame minigame = Minigame.getInstance();
        if (DataManager.getInstance() != null){
            if (minigame.getProperties() != null) {
                if (!minigame.getProperties().isEmpty()) {
                    serverDataManager = new GameDataManager(this, minigame.getProperties());
                    serverDataManager.updateGame();
                    return;
                }
            }
            serverDataManager = new GameDataManager(this);
            serverDataManager.updateGame();
        }
    }

    int animationTick = 0;
    public void updateWaitingForPlayersBossBar(){
        if (!getState().equals(GameState.WAITING))
            return;

        for (GamePlayer gamePlayer : getParticipants()){
            ChatColor chatColor = ChatColor.WHITE;

            Message message = MessageManager.get(gamePlayer, "bossbar.waiting_for_players")
                    .replace("%online%", "" + chatColor + getParticipants().size())
                    .replace("%required%", "" + getSettings().getMinPlayers());

            PlayerBossBar playerBossBar = PlayerBossBar.getOrCreateBossBar(gamePlayer.getOnlinePlayer().getUniqueId(), message.getTranslated());

            String oldTitle = StringUtils.colorizer(playerBossBar.getBossBar().name().toString());
            if (!oldTitle.isEmpty()) {
                try{
                    int oldParticipantsSize = Integer.parseInt(oldTitle.replaceAll("§[0-9a-fA-Fk-or]", "").replaceAll(" ", "").split("\\(")[1].split("/")[0]);
                    chatColor = (oldParticipantsSize != getParticipants().size() ? (getParticipants().size() > oldParticipantsSize ? ChatColor.YELLOW : ChatColor.RED) : ChatColor.WHITE);
                    if (chatColor != ChatColor.WHITE)
                        Bukkit.getScheduler().runTaskLater(Minigame.getInstance().getPlugin(), task -> updateWaitingForPlayersBossBar(), 30L);
                }catch (Exception ignored){}
            }


            Component component = message.getTranslated()
                    .font(Key.key("jsplugins", "bossbar_offset"));
            playerBossBar.setName(component);
        }
    }


    public void joinPlayer(Player player){
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);
        MinigameSettings settings = getSettings();

        if (gamePlayer.getGame() != null){
            gamePlayer.getGame().quitPlayer(player);
        }


        List<GamePlayer> participants = getParticipants();
        if (getState().equals(GameState.WAITING)
                || getState().equals(GameState.STARTING)){
            if (participants.size() >= settings.getMaxPlayers()) {
                if (player.hasPermission("game.joinfullserver")){
                    boolean joined = false;
                    for (int i = (participants.size() - 1); i > 0; i--){
                        Player kickPlayer = participants.get(i).getOnlinePlayer();
                        if (kickPlayer.hasPermission("game.joinfullserver")){
                            continue;
                        }

                        MessageManager.get(kickPlayer, "server.kicked_because_reserved_slot")
                                .send();
                        Utils.sendToLobby(kickPlayer, false);
                        joined = true;
                        break;
                    }

                    if (!joined){
                        MessageManager.get(player, "vip.full.slots")
                                .send();
                        Utils.sendToLobby(player, false);
                        return;
                    }
                }else {
                    MessageManager.get(player, "server.full")
                            .send();
                    Utils.sendToLobby(player, false);
                    return;
                }
            }

            player.setDisplayName("§r" + player.getName());
            player.playerListName((Minigame.getInstance().getLevelManager() != null ? gamePlayer.getPlayerData().getLevelProgress().levelEvolution().getIcon(): Component.text("")).append(Component.text(" §r" + player.getName()).font(Key.key("minecraft", "default"))));

            Location lobbyLocation = getLobbyManager().getLobbyLocation().getLocation();
            player.teleport(lobbyLocation);
            Bukkit.getScheduler().runTaskLater(Minigame.getInstance().getPlugin(), task -> {
                //Stávalo se, že to hráče neportlo na lobby
                if (player.getLocation().getWorld() != lobbyLocation.getWorld()){
                    player.teleport(lobbyLocation);
                }
                player.setGameMode(GameMode.ADVENTURE);
                }, 2L);

            participants.add(gamePlayer);
            gamePlayer.setGame(this);
            gamePlayer.setType(GamePlayerType.PLAYER);
            Bukkit.getScheduler().runTaskAsynchronously(Minigame.getInstance().getPlugin(), task -> gamePlayer.getPlayerData().loadKits());

            if (serverDataManager != null) {
                serverDataManager.getJSONProperty("Players").update(this, participants.size());
            }

            if (game.getState().equals(GameState.WAITING)) {
                updateWaitingForPlayersBossBar();
            }else{
                if (participants.size() == settings.getReducedPlayers()) {
                    if (getRunningMainTask().getCounter() > settings.getReducedTime()) {
                        getRunningMainTask().setCounter(settings.getReducedTime());
                        MessageManager.get(participants, "chat.time_reduced")
                                .send();
                    }
                }
            }

            Utils.hideAndShowPlayers(gamePlayer);

            MessageManager.get(participants, "chat.join")
                    .replace("%prefix%", gamePlayer.getPlayerData().getPrefix())
                    .replace("%player%", gp -> (gp.getFriends().isFriendWith(gamePlayer) || gp.getParty().getAllOnlinePlayers().contains(gamePlayer) ? "§6" : "") + player.getName())
                    .replace("%players%", String.valueOf(getPlayers().size()))
                    .replace("%max_players%", "" + getSettings().getMaxPlayers())
                    .add("Ẅ", gp -> gp.getFriends().isFriendWith(gamePlayer))
                    .add("ẅ", gp -> gp.getParty().getAllOnlinePlayers().contains(gamePlayer))
                    .send();


            gamePlayer.resetAttributes();
            player.getInventory().clear();
            if (getLobbyManager().getInventoryManager() != null){
                getLobbyManager().getInventoryManager().give(player);
            }else{
                player.sendMessage("§cAn error occurred while loading the waiting lobby inventory.");
            }


            if (getPlayers().size() >= settings.getMinPlayers() && !getState().equals(GameState.STARTING)){
                setState(GameState.STARTING);
                automaticStart = true;
                Task task = new Task(this, "StartCountdown", getSettings().getStartingTime(), new StartCountdown(), Minigame.getInstance().getPlugin());
                task.setGame(this);
            }

            GameJoinEvent ev = new GameJoinEvent(this, gamePlayer, GameJoinEvent.JoinType.LOBBY);
            Bukkit.getPluginManager().callEvent(ev);
            return;
        }else if (getState().equals(GameState.INGAME) && gamePlayer.getType().equals(GamePlayerType.DISCONNECTED) && getSettings().isEnabledReJoin()){
            if (getPlayers().size() >= settings.getMaxPlayers()) {

                participants.add(gamePlayer);
                gamePlayer.setGame(this);


                //TODO: lepší rejoin
                gamePlayer.setSpectator(false);
                startingProcessHandler.preparePlayer(gamePlayer);
                gamePlayer.setType(GamePlayerType.PLAYER);
                player.teleport(Objects.requireNonNullElse((Location) gamePlayer.getMetadata().get("death_location"), getCurrentMap().getPlayerToLocation(gamePlayer)));


                GameJoinEvent ev = new GameJoinEvent(this, gamePlayer, GameJoinEvent.JoinType.REJOIN);
                Bukkit.getPluginManager().callEvent(ev);
                return;
            }else{
                MessageManager.get(gamePlayer, "chat.join_failed.full_game.rejoin")
                        .send();
            }
        }else if (getState().equals(GameState.INGAME) && settings.isAllowedJoiningAfterStart()){
            if (getPlayers().size() >= settings.getMaxPlayers()) {

                participants.add(gamePlayer);
                gamePlayer.setGame(this);

                startingProcessHandler.preparePlayer(gamePlayer);

                GameJoinEvent ev = new GameJoinEvent(this, gamePlayer, GameJoinEvent.JoinType.JOIN_AFTER_START);
                Bukkit.getPluginManager().callEvent(ev);
                return;
            }else{
                MessageManager.get(gamePlayer, "chat.join_failed.full_game.game_in_progress")
                        .send();
            }
        }else if (getState().equals(GameState.INGAME)) {
            participants.add(gamePlayer);
            gamePlayer.setGame(this);
            gamePlayer.setSpectator(true);

            GameJoinEvent ev = new GameJoinEvent(this, gamePlayer, GameJoinEvent.JoinType.SPECTATOR);
            Bukkit.getPluginManager().callEvent(ev);
            return;
        }
        GameManager.newArena(player, true);
    }

    public void quitPlayer(Player player){
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);

        getParticipants().remove(gamePlayer);

        GameQuitEvent ev = new GameQuitEvent(this, gamePlayer);
        Bukkit.getPluginManager().callEvent(ev);


        PlayerBossBar.removeBossBar(player.getUniqueId());

        if (serverDataManager != null) {
            serverDataManager.getJSONProperty("Players").update(this, getParticipants().size());
        }

        new BukkitRunnable(){
            @Override
            public void run() {
                gamePlayer.getPlayerData().saveAll();
            }
        }.runTaskAsynchronously(Minigame.getInstance().getPlugin());

        if (getState().equals(GameState.WAITING) || getState().equals(GameState.STARTING)){
            MessageManager.get(getParticipants(), "chat.quit")
                    .replace("%prefix%",  gamePlayer.getPlayerData().getPrefix())
                    .replace("%player%", gp -> (gp.getFriends() != null && gp.getFriends().isFriendWith(gamePlayer) || gp.getParty() != null && !gp.getParty().getAllOnlinePlayers().isEmpty() && gp.getParty().getAllOnlinePlayers().contains(gamePlayer) ? "§6" : "") + player.getName())
                    .replace("%players%", String.valueOf(getPlayers().size()))
                    .replace("%max_players%", "" + getSettings().getMaxPlayers())
                    .add("Ẅ", gp -> gp.getFriends().isFriendWith(gamePlayer))
                    .add("ẅ", gp -> gp.getParty().getAllOnlinePlayers().contains(gamePlayer))
                    .send();

            gamePlayer.getPlayerData().removeVotesForMaps();
            if (gamePlayer.getTeam() != null) {
                gamePlayer.getTeam().quitPlayer(gamePlayer);
            }
            gamePlayer.setType(GamePlayerType.DISCONNECTED);
            //gamePlayer.getPlayerData().setGame(null);


            if (getState().equals(GameState.STARTING)) {
                checkArenaFullness();
            }else if (getState().equals(GameState.WAITING)){
                updateWaitingForPlayersBossBar();
            }


            gamePlayer.getPlayerData().flushSomeData();
        }else if (getState() == GameState.INGAME){
            if (!gamePlayer.isSpectator()){
                gamePlayer.setType(GamePlayerType.DISCONNECTED);
                player.damage(GameAPI.getInstance().getVersionSupport().getMaxPlayerHealth(player));

                // boolean killer = PVPListener.containsLastDamager(gamePlayer) && (System.currentTimeMillis() - PVPListener.getLastDamager(gamePlayer).getMs()) <= 12000;
                // GamePlayerDeathEvent deathEvent = new GamePlayerDeathEvent(gamePlayer.getGame(), (killer ? PVPListener.getLastDamager(gamePlayer).getLastDamager() : null), PlayerManager.getGamePlayer(player), null,null);
                // Bukkit.getPluginManager().callEvent(deathEvent);
            }else if (!Minigame.getInstance().getSettings().isEnabledReJoin()){
                GameMap currentMap = game.getCurrentMap();
                if (currentMap != null){
                    currentMap.getPlayerToLocation().remove(gamePlayer);
                }
                PlayerManager.removeGamePlayer(player);
            }

            if (getPlayers().isEmpty()){
                endGame(null);
            }
        }else{
            PlayerManager.removeGamePlayer(player);
        }
    }

    public void checkArenaFullness(){
        if (!getState().equals(GameState.STARTING)) return;
        if (!automaticStart && !getParticipants().isEmpty()) return;

        MinigameSettings settings = getSettings();
        boolean notEnough = (getParticipants().isEmpty() || getParticipants().size() < Math.max(settings.getMinPlayers() , 5));
        if (notEnough) {
            setState(GameState.WAITING);

            Task countdown = Task.getTask(this, "StartCountdown");
            if (countdown != null) {
                countdown.setCounter(settings.getStartingTime());
                Task.cancel(this, "StartCountdown");
            }

            if (!settings.isChooseRandomMap()) {
                for (GameMap a : getMapManager().getMaps()) {
                    if (!a.isIngame()) continue;
                    a.setWinned(false);
                }
                getMapManager().setVoting(true);
            }
            MessageManager.get(getParticipants(), "chat.not_enough_players")
                    .send();
            updateWaitingForPlayersBossBar();
        }
    }

    public void endGame(Winner winner){
        setState(GameState.ENDING);
        Task.cancelAll(this);

        String rankingScore = "kills";
        for (PlayerManager.Score score : PlayerManager.getScores()) {
            if (score.isScoreRanking()){
                rankingScore = score.getName();
                break;
            }
        }
        Map<GamePlayer, Integer> ranking =  Utils.getTopPlayers(this, rankingScore, 3);

        GameEndEvent ev = new GameEndEvent(this, winner, ranking);
        Bukkit.getPluginManager().callEvent(ev);


        Bukkit.getScheduler().runTaskLater(Minigame.getInstance().getPlugin(), t -> {
            Task task = new Task(this, "EndCountdown", (!getParticipants().isEmpty() ? 15 : 5), new EndCountdown(), Minigame.getInstance().getPlugin());
            task.setGame(this);
        }, 30L);


        HashMap<GamePlayer, Integer> oldWinstreaks = new HashMap<>();

        for (GamePlayer gamePlayer : getParticipants()) {
            Player player = gamePlayer.getOnlinePlayer();

            player.stopSound("jsplugins:ending");
            GameAPI.getInstance().getVersionSupport().setTeamNameTag(gamePlayer.getOnlinePlayer(), getName().replace(" ", "") + getID(), ChatColor.WHITE);
            Utils.hideAndShowPlayers(gamePlayer);


            gamePlayer.resetAttributes();
            gamePlayer.setLimited(false);
            gamePlayer.setEnabledMovement(true);
            gamePlayer.setSpectator(false);
            player.setGameMode(GameMode.ADVENTURE);


            LevelManager levelManager = Minigame.getInstance().getLevelManager();
            if (levelManager != null) {
                LevelProgress levelProgress = levelManager.getLevelProgress(gamePlayer);
                float xpProgress = (float) levelProgress.xpOnCurrentLevel() / levelProgress.levelRange().neededXP();
                player.setExp(Math.min(xpProgress, 1.0f));
                player.setLevel(levelProgress.level());
            }


            if (getSettings().teleportPlayersAfterEnd()){
                player.teleport(getLobbyManager().getLobbyLocation().getLocation());
            }else{
                player.setAllowFlight(true);
                player.setFlying(true);
            }


            player.sendMessage("");

            Bukkit.getScheduler().runTaskLater(Minigame.getInstance().getPlugin(), task -> { //kvůli Questům
                if (winner != null) {
                    if ((winner.getWinnerType().equals(Winner.WinnerType.PLAYER) && (winner.equals(gamePlayer)) || (winner.getWinnerType().equals(Winner.WinnerType.TEAM) && gamePlayer.getTeam().equals(winner)))) {
                        MessageManager.get(gamePlayer, "chat.you_won")
                                .send();
                    } else {
                        MessageManager.get(gamePlayer, "chat.winner")
                                .replace("%winner%", gp -> (winner.getWinnerType().equals(Winner.WinnerType.PLAYER)
                                        ? ((GamePlayer) winner).getOnlinePlayer().getName()
                                        : ((GameTeam) winner).getChatColor() + ((GameTeam) winner).getName()))
                                .replaceWithComponent("%type%", gp -> winner.getWinnerType().getTranslatedName(gp))
                                .send();
                    }
                } else {
                    MessageManager.get(gamePlayer, "chat.nobody_won")
                            .send();
                }

                player.sendMessage("");
            }, 3L);



            if (winner != null) {
                if ((winner.getWinnerType().equals(Winner.WinnerType.PLAYER) && (winner.equals(gamePlayer)) || (winner.getWinnerType().equals(Winner.WinnerType.TEAM) && gamePlayer.getTeam().equals(winner)))) {
                    MessageManager.get(gamePlayer, "title.victory")
                            .send();
                    gamePlayer.getOnlinePlayer().playSound(gamePlayer.getOnlinePlayer(), "jsplugins:victory1", 0.35F, 1F);
                } else {
                    if (winner.getWinnerType().equals(Winner.WinnerType.PLAYER)){
                        MessageManager.get(gamePlayer, "title.winner")
                                .replace("%winner%",winner.getWinnerType().getTranslatedName(gamePlayer)
                                        + " " + ((GamePlayer) winner).getPlayer().getName())
                                .send();
                    }else if (winner.getWinnerType().equals(Winner.WinnerType.TEAM)){
                        Component component = Component.text(((GameTeam) winner).getChatColor() + "")
                                .append(winner.getWinnerType().getTranslatedName(gamePlayer))
                                .appendSpace()
                                .append(Component.text(((GameTeam)winner).getName()));
                                
                        
                        MessageManager.get(gamePlayer, "title.winner")
                                .replace("%winner%", component)
                                .send();
                    }
                }
            }


            Location location;
            if (getSettings().isTeleportPlayersAfterEnd()){
                location = getLobbyManager().getLobbyLocation().getLocation();
            }else{
                if (getCurrentMap().getMainArea() != null) {
                    location = getCurrentMap().getMainArea().getCenter();
                }else{
                    location = new Location(getCurrentMap().getWorld(), 0, 90, 0);
                }
            }


            new BukkitRunnable(){
                Location newLocation;

                @Override
                public void run() {
                    if (GameManager.getGameByID(getID()) == null || getState() != GameState.ENDING){
                        this.cancel();
                        return;
                    }

                    newLocation = location.add(RandomUtils.randomInteger(-20, 20), 0,  RandomUtils.randomInteger(-20, 20));
                    if (newLocation.getWorld() == null){
                        return;
                    }
                    newLocation = newLocation.getWorld().getHighestBlockAt(newLocation).getLocation();
                    newLocation = newLocation.add(0, RandomUtils.randomInteger(5, 10), 0);

                    Utils.spawnFireworks(newLocation, 1, Color.WHITE, FireworkEffect.Type.BALL);
                    Color color = Color.YELLOW;
                    if (getSettings().isUseTeams() && winner != null){
                        color = ((GameTeam) winner).getColor();
                    }
                    Utils.spawnFireworks(newLocation, 1, color, FireworkEffect.Type.BALL);
                }
            }.runTaskTimer(Minigame.getInstance().getPlugin(), 0L, 30L);



            if (getSpectatorManager().getInventoryManager().getPlayers().contains(gamePlayer.getOnlinePlayer())) {
                getSpectatorManager().getInventoryManager().unloadInventory(gamePlayer.getOnlinePlayer());
            }

            InventoryManager itemManager = new InventoryManager("Ending");
            if (GameManager.getGames().size() > 1 || (DataManager.getInstance() != null && DataManager.getInstance().getMinigame(Minigame.getInstance().getName()).get().isThereFreeGame())) {
                Item playAgain = new Item(new ItemBuilder(Material.PAPER).hideAllFlags().toItemStack(),
                        1, "item.play_again",
                        new Consumer<PlayerInteractEvent>() {
                            @Override
                            public void accept(PlayerInteractEvent e) {
                                GameManager.newArena(e.getPlayer(), false);
                            }
                        });
                itemManager.setHoldItemSlot(1);
                itemManager.registerItem(playAgain);
            }

            Item playerMenu = new Item(new ItemBuilder(Material.ECHO_SHARD).setCustomModelData(1025).hideAllFlags().toItemStack(), 7, "Item.player_menu", e -> ProfileInventory.openGUI(PlayerManager.getGamePlayer(e.getPlayer())));
            playerMenu.setBlinking(gamePlayer2 -> !gamePlayer2.getPlayerData().getUnclaimedRewards(UnclaimedReward.Type.DAILYMETER).isEmpty()
                    || !gamePlayer.getPlayerData().getUnclaimedRewards(UnclaimedReward.Type.LEVELUP).isEmpty()
                    || !gamePlayer.getPlayerData().getUnclaimedRewards(UnclaimedReward.Type.QUEST).isEmpty());
            playerMenu.setBlinkingItemCustomModelData(1026);

            Item quests = new Item(new ItemBuilder(Material.BOOK).hideAllFlags().toItemStack(), 5, "Item.quests", e -> QuestInventory.openGUI(PlayerManager.getGamePlayer(e.getPlayer())));
            quests.setBlinking(gamePlayer2 -> !gamePlayer2.getPlayerData().getUnclaimedRewards(UnclaimedReward.Type.QUEST).isEmpty());
            quests.setBlinkingItemCustomModelData(1010);

            itemManager.registerItem(playerMenu, quests);

            itemManager.give(player);

            oldWinstreaks.put(gamePlayer, gamePlayer.getPlayerData().getPlayerStat("Winstreak").getStatScore());
            gamePlayer.getOnlinePlayer().sendMessage("");
        }

        if (winner != null) {
            if (winner.getWinnerType().equals(Winner.WinnerType.TEAM)) {
                GameTeam wTeam = (GameTeam) winner;
                for (GameTeam lTeam : getTeamManager().getTeams().stream().filter(team -> team != wTeam).toList()) {
                    lTeam.getMembers().forEach(loser -> loser.getPlayerData().getPlayerStat("Winstreak").setStat(0));
                }
                for (GamePlayer gamePlayer : wTeam.getMembers()) {
                    gamePlayer.getPlayerData().getPlayerStat("Winstreak").increase();
                }
            } else {
                ((GamePlayer) winner).getPlayerData().getPlayerStat("Winstreak").increase();
                for (GamePlayer loser : getParticipants().stream().filter(gp -> gp != winner).toList()) {
                    loser.getPlayerData().getPlayerStat("Winstreak").setStat(0);
                }
            }
        }

        for (GamePlayer gamePlayer : getParticipants()){
            Bukkit.getScheduler().runTaskAsynchronously(Minigame.getInstance().getPlugin(), bukkitTask -> gamePlayer.getPlayerData().saveAll());
        }



        String finalRankingScore = rankingScore;
        new BukkitRunnable(){
            @Override
            public void run() {
                sendTopPlayers(finalRankingScore);
            }
        }.runTaskLater(Minigame.getInstance().getPlugin(), 10L);

        new BukkitRunnable(){
            @Override
            public void run() {
                sendRewardSummary();

                for (GamePlayer gamePlayer : oldWinstreaks.keySet()) {
                    Player player = gamePlayer.getOnlinePlayer();

                    if (winner != null) {
                        MessageManager.get(gamePlayer, "chat.winstreak")
                                .replace("%old_winstreak%", "" + oldWinstreaks.get(gamePlayer))
                                .replace("%new_winstreak%", (gamePlayer.getPlayerData().getPlayerStat("Winstreak").getStatScore() == 0 ? "§c" : "§a") + gamePlayer.getPlayerData().getPlayerStat("Winstreak").getStatScore())
                                .send();
                    }


                    List<PlayerScore> stats = gamePlayer.getPlayerData().getScores().stream().filter(s -> s.getScore() != 0 && s.getStat() != null).toList();

                    if (!stats.isEmpty()) {
                        Component message = MessageManager.get(gamePlayer, "chat.view_statistic").getTranslated();

                        Component hoverText = Component.empty();
                        int i = 0;
                        for (PlayerScore score : stats) {
                            if (i != 0) {
                                hoverText = hoverText.appendNewline();
                            }
                            hoverText = hoverText.append(Component.text("§7" + score.getPluralName() + ": §a" + score.getScore()));
                            i++;
                        }
                        
                        player.sendMessage(message.hoverEvent(hoverText));
                        player.sendMessage("");
                    }
                }
            }
        }.runTaskLater(Minigame.getInstance().getPlugin(), 50L);
    }

    public void sendTopPlayers(String rankingScore){
        Map<GamePlayer, Integer> fullRanking =  Utils.getTopPlayers(this, rankingScore, getSettings().getMaxPlayers());
        if (fullRanking.isEmpty()){
            return;
        }

        for (GamePlayer gamePlayer : getParticipants()) {
            Player player = gamePlayer.getOnlinePlayer();

            MessageManager.get(gamePlayer, "chat.top3players.title").send();

            int i = 1;
            for (GamePlayer pos : fullRanking.keySet()) {
                int position = fullRanking.get(pos);
                MessageManager.get(gamePlayer, "chat.top3players.position")
                        .replace("%position%", "" + position)
                        .replace("%player_head%", ChatHeadAPI.getInstance().getHeadAsString(pos.getOfflinePlayer()))
                        .replace("%player%", (pos.equals(gamePlayer) ? "§a" : "") + pos.getOnlinePlayer().getName() + "§r")
                        .replace("%score%", "" + pos.getScoreByName(rankingScore).getScore())
                        .replace("%ranking_score_name%", rankingScore)
                        .send();
                i++;
                if (i > 3)
                    break;
            }

            player.sendMessage("");


            MessageManager.get(gamePlayer, "chat.top3players.your_position")
                    .replace("%position%", "" + (fullRanking.get(gamePlayer) == null ? "§c-" : fullRanking.get(gamePlayer)))
                    .replace("%score%", "" + gamePlayer.getScoreByName(rankingScore).getScore())
                    .replace("%ranking_score_name%", rankingScore)
                    .send();


            PartyInterface party = gamePlayer.getParty();
            FriendsInterface friends = gamePlayer.getFriends();
            if (party.isInParty() && !party.getAllOnlinePlayers().isEmpty()) {
                Component message = MessageManager.get(gamePlayer, "chat.top3players.your_party_members_position").getTranslated();

                Component hoverText = Component.empty();
                for (GamePlayer partyMember : fullRanking.keySet().stream().filter(p -> party.getAllOnlinePlayers().contains(p)).toList()) {
                    hoverText = hoverText.append(MessageManager.get(gamePlayer, "chat.top3players.position")
                            .replace("%position%", "" + fullRanking.get(partyMember))
                            .replace("%player_head%", ChatHeadAPI.getInstance().getHeadAsString(partyMember.getOfflinePlayer()))
                            .replace("%player%", partyMember.getOnlinePlayer().getName())
                            .replace("%score%", "" + partyMember.getScoreByName(rankingScore).getScore())
                            .replace("%ranking_score_name%", rankingScore)
                            .getTranslated())
                            .appendNewline();
                }
                
                player.sendMessage(message.hoverEvent(hoverText));
            }


            if (friends.hasFriends() && !friends.getAllOnlinePlayers().isEmpty()) {
                List<GamePlayer> friendsList = new ArrayList<>(friends.getAllOnlinePlayers()).stream().filter(p -> (!party.isInParty() || party.getAllOnlinePlayers().contains(p))).toList();

                if (!friendsList.isEmpty()) {
                    Component message = MessageManager.get(gamePlayer, "chat.top3players.friends_position").getTranslated();

                    Component hoverText = Component.empty();
                    for (GamePlayer friend : fullRanking.keySet().stream().filter(friendsList::contains).toList()) {
                        hoverText = hoverText.append(MessageManager.get(gamePlayer, "chat.top3players.position")
                                .replace("%position%", "" + fullRanking.get(friend))
                                .replace("%player_head%", ChatHeadAPI.getInstance().getHeadAsString(friend.getOfflinePlayer()))
                                .replace("%player%", friend.getOnlinePlayer().getName())
                                .replace("%score%", "" + friend.getScoreByName(rankingScore).getScore())
                                .replace("%ranking_score_name%", rankingScore)
                                .getTranslated())
                                .appendNewline();
                    }
                    
                    player.sendMessage(message.hoverEvent(hoverText));
                }
            }

            player.sendMessage("");
        }
    }

    public void sendRewardSummary(){
        for (GamePlayer gamePlayer : getParticipants()){
            Player player = gamePlayer.getOnlinePlayer();

            MessageManager.get(gamePlayer, "chat.rewardsummary.title").send();

            if (!ResourcesManager.earnedSomething(gamePlayer)){
                MessageManager.get(gamePlayer, "chat.rewardsummary.no_rewards")
                        .send();
                player.sendMessage("");
            }else{
                gamePlayer.getOnlinePlayer().playSound(player, "jsplugins:gamebonus", 0.8F, 1F);

                for (Resource resource : ResourcesManager.getResources()) {
                    int earned = ResourcesManager.getEarned(gamePlayer, resource);
                    if (earned > 0) {
                        Component component = Component.text("  §7• " + (resource.getImg_char() != null ? resource.getImg_char() : "") + " " + resource.getColor() + earned + " §7" + resource.getDisplayName());
                        Component hoverComponent = Component.text("");

                        List<PlayerScore> scoreList = gamePlayer.getPlayerData().getScores().stream()
                                .filter(score -> score.getGamePlayer() == gamePlayer && score.getScore() != 0 && score.getEarned(resource) != 0)
                                .sorted((a, b) -> Integer.compare(b.getEarned(resource), a.getEarned(resource)))
                                .toList();

                        int i = 0;
                        for (PlayerScore score : scoreList) {
                            hoverComponent = hoverComponent.append(Component.text(resource.getColor() + "+" + score.getEarned(resource) + ChatColor.GRAY + " (" + (score.getScore() > 1 ? score.getScore() + " " : "") + score.getDisplayName(true) + ")"));
                            if (i < scoreList.size() - 1) {
                                hoverComponent = hoverComponent.appendNewline();
                            }
                            i++;
                        }

                        player.sendMessage(component.hoverEvent(hoverComponent));


                        if (resource.isApplicableBonus()) {
                            List<Integer> percentages = Arrays.asList(50, 45, 40, 35, 30, 25, 20, 15, 10, 5);

                            for (Integer percent : percentages) {
                                if (gamePlayer.getOnlinePlayer().hasPermission("vip.bonus" + percent)){
                                    int coins = (int) (((double) earned * (double) percent) / (double) 100);
                                    if (coins != 0) {
                                        resource.getResourceInterface().deposit(gamePlayer, coins);
                                        MessageManager.get(gamePlayer, "chat.rewardsummary.bonus")
                                                .replace("%economy_color%", "" + resource.getColor())
                                                .replace("%reward%", "" + coins)
                                                .replace("%economy_name%", resource.getColor() + resource.getDisplayName())
                                                .replace("%bonus%", "" + percent)
                                                .send();
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }

                /*List<Resource> resourcesWithFirstDailyWinReward = Minigame.getInstance().getEconomies().stream().filter(resource -> resource.getFirstDailyWinReward() != 0).toList();
                if (resourcesWithFirstDailyWinReward.size() > 1 && gamePlayer.getPlayerData().grantDailyFirstWinReward()){
                    player.sendMessage("");
                    MessageManager.get(gamePlayer, "chat.rewardsummary.first_win_reward")
                            .replace("%minigame%", Minigame.getInstance().getName());
                    for (Resource resource : resourcesWithFirstDailyWinReward){
                        player.sendMessage("   §7↳ " + resource.getColor() + resource.getFirstDailyWinReward() + " " + resource.getDisplayName());
                    }
                }*/
            }
            player.sendMessage("");
        }
    }

    public final void winMap(){
        getMapManager().setVoting(false);

        List<GameMap> arenas1 = new ArrayList<>(getMapManager().getMaps());
        arenas1.sort(new MapVotesComparator());

        int playingArenas = getSettings().getMaxMapsInGame();

        int i = 0;

        for (GameMap a : arenas1) {
            if (!a.isIngame()) continue;
            a.setWinned(true);
            i++;
            if (playingArenas != 1) {
                for (GamePlayer player : getPlayers()){
                    player.getOnlinePlayer().playSound(player.getOnlinePlayer(), Sound.UI_BUTTON_CLICK, 1.0F, 1.0F);
                    player.getOnlinePlayer().sendMessage(MessageManager.get(player, "chat.map_won").replace("%map%", a.getName()).replace("%number%", "" + i).getTranslated());
                }
            } else {
                for (GamePlayer player : getPlayers()){
                    player.getOnlinePlayer().playSound(player.getOnlinePlayer(), Sound.UI_BUTTON_CLICK, 1.0F, 1.0F);
                    player.getOnlinePlayer().sendMessage(MessageManager.get(player, "chat.map_won").replace("%map%", a.getName()).getTranslated());
                }
            }
            if (i == playingArenas) break;
        }
    }

    public void selectRandomMap() {
        getMapManager().setVoting(false);

        List<GameMap> maps = new ArrayList<>(getMapManager().getMaps().stream().filter(GameMap::isIngame).toList());
        Collections.shuffle(maps);

        GameMap map = maps.get(0);
        map.setWinned(true);
    }

    public GameMap nextArena() {
        List<GameMap> arenas1 = new ArrayList<>(getMapManager().getMaps());
        arenas1.sort(new MapVotesComparator());


        for (GameMap a : arenas1) {
            if (!a.isWinned()) continue;
            if (a.isPlayed()) continue;
            if (a.isPlaying()) {
                a.setPlayed(true);
                continue;
            }
            return a;
        }
        return null;
    }

    public MinigameSettings getSettings(){
        return Minigame.getInstance().getSettings();
    }

    public boolean isPreparation(){
        return Task.getTask(this, "PreparationTask") != null;
    }

    public GameMap getCurrentMap() {
        for (GameMap map : getMapManager().getMaps()) {
            if (map.isWinned() && !map.isPlayed()) return map;
        }
        return null;
    }

    @Deprecated
    public GameMap getPlayingMap() {
        for (GameMap map : getMapManager().getMaps()) {
            if (map.isPlaying()) return map;
        }
        return null;
    }

    public void setState(GameState state) {
        this.state = state;
        if (serverDataManager != null) {
            serverDataManager.getJSONProperty("GameState").update(this, state.name());
        }

        GameStateChangeEvent ev = new GameStateChangeEvent(getGame(), state);
        Bukkit.getPluginManager().callEvent(ev);
    }

    public List<GamePlayer> getPlayers(){
        return participants.stream()
                .filter(gp -> gp.getType().equals(GamePlayerType.PLAYER))
                .toList();
    }

    public List<GamePlayer> getSpectators(){
        return participants.stream()
                .filter(gp -> gp.getType().equals(GamePlayerType.SPECTATOR))
                .toList();
    }

    public void addBlock(Block block) {
        if (placedBlocks == null)
            placedBlocks = new ArrayList<>();

        if (!containsBlock(block)){
            placedBlocks.add(block);
        }
    }

    public void removeBlock(Block block) {
        placedBlocks.remove(block);
    }

    public boolean containsBlock(Block block) {
        if (placedBlocks == null) return false;
        return placedBlocks.contains(block);
    }



    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Game other = (Game) obj;
        return this.getID() != null && this.getID().equals(other.getID());
    }

    @Override
    public int hashCode() {
        return getID() != null ? getID().hashCode() : 0;
    }
}
