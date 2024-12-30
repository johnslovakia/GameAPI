package cz.johnslovakia.gameapi.game;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.MinigameSettings;
import cz.johnslovakia.gameapi.serverManagement.gameData.GameDataManager;
import cz.johnslovakia.gameapi.users.resources.Resource;
import cz.johnslovakia.gameapi.users.resources.ResourcesManager;
import cz.johnslovakia.gameapi.events.*;
import cz.johnslovakia.gameapi.game.map.GameMap;
import cz.johnslovakia.gameapi.game.map.MapVotesComparator;
import cz.johnslovakia.gameapi.game.team.GameTeam;
import cz.johnslovakia.gameapi.game.team.TeamJoinCause;
import cz.johnslovakia.gameapi.game.map.MapManager;
import cz.johnslovakia.gameapi.task.tasks.EndCountdown;
import cz.johnslovakia.gameapi.task.tasks.StartCountdown;
import cz.johnslovakia.gameapi.users.*;
import cz.johnslovakia.gameapi.game.team.TeamManager;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.task.Task;
import cz.johnslovakia.gameapi.task.tasks.GameCountdown;
import cz.johnslovakia.gameapi.task.tasks.PreparationCountdown;
import cz.johnslovakia.gameapi.users.friends.FriendsInterface;
import cz.johnslovakia.gameapi.users.parties.PartyInterface;
import cz.johnslovakia.gameapi.users.stats.StatsHolograms;
import cz.johnslovakia.gameapi.utils.*;
import cz.johnslovakia.gameapi.utils.chatHead.ChatHeadAPI;
import cz.johnslovakia.gameapi.utils.inventoryBuilder.InventoryManager;
import cz.johnslovakia.gameapi.utils.inventoryBuilder.Item;
import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.boss.KeyedBossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.function.Consumer;

@Getter
public class Game {

    private final String name;
    private String ID;
    private GameState state = GameState.LOADING;
    @Setter
    private Task runningMainTask;
    private final Location lobbyPoint;
    @Setter
    private InventoryManager lobbyInventory;
    @Setter
    private SpectatorManager spectatorManager;
    @Setter
    private boolean firstGameKill = true;

    private Winner winner;
    @Setter
    private MapManager mapManager;
    private GameDataManager serverDataManager;

    private final List<GamePlayer> participants = new ArrayList<>();
    private final List<Block> placedBlocks = new ArrayList<>();
    private final HashMap<String, Object> metadata = new HashMap<>();

    public Game(String name, InventoryManager lobbyInventory, Location lobbyPoint) {
        this.name = name;
        this.lobbyInventory = lobbyInventory;
        this.lobbyPoint = lobbyPoint;

        this.spectatorManager = new SpectatorManager();

        while (this.ID == null || GameManager.isDuplicate(this.ID)){
            this.ID = StringUtils.randomString(6, true, true, false);
        }

    }

    public void finishSetup(){
        if (getSettings().isChooseRandomMap()){
            selectRandomMap();
        }
        getSpectatorManager().loadItemManager();
        state = GameState.WAITING;


        Minigame minigame = GameAPI.getInstance().getMinigame();
        if (minigame.getDataManager() != null){
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
        String[] dotPatterns = {"", ".", "..", "..."};
        animationTick = (animationTick + 1) % dotPatterns.length;

        String dots = dotPatterns[animationTick];

        for (GamePlayer gamePlayer : getParticipants()){
            BossBar bossBar = (BossBar) gamePlayer.getMetadata().get("bossbar.waiting_for_players");
            if (bossBar == null){
                bossBar = Bukkit.createBossBar("", BarColor.WHITE , BarStyle.SOLID);
                bossBar.setVisible(true);
                bossBar.addPlayer(gamePlayer.getOnlinePlayer());
                gamePlayer.getMetadata().put("bossbar.waiting_for_players", bossBar);
            }


            ChatColor chatColor = ChatColor.WHITE;


            String oldTitle = StringUtils.colorizer(bossBar.getTitle());
            if (!oldTitle.isEmpty()) {
                int oldParticipantsSize = Integer.parseInt(oldTitle.replaceAll("§[0-9a-fA-Fk-or]", "").split("\\(")[1].split("/")[0]);
                chatColor = (oldParticipantsSize != getParticipants().size() ? (getParticipants().size() > oldParticipantsSize ? ChatColor.YELLOW : ChatColor.RED) : ChatColor.WHITE);
            }

            bossBar.setTitle(MessageManager.get(gamePlayer, "bossbar.waiting_for_players")
                    .replace("%online%", "" + chatColor + getParticipants().size())
                    .replace("%required%", "" + getSettings().getMinPlayers())
                    .replace("...", dots)
                    .getFontTextComponentJSON("gameapi:bossbar_offset"));
        }
    }


    public void joinPlayer(Player player){
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);
        /*if (!gamePlayer.getType().equals(GamePlayerType.DISCONNECTED)) {
            PlayerManager.removeGamePlayer(player);
            gamePlayer = PlayerManager.getGamePlayer(player);
        }*/

        MinigameSettings settings = getSettings();

        if (gamePlayer.getPlayerData().getGame() != null){
            gamePlayer.getPlayerData().getGame().quitPlayer(player);
        }


        if (getState().equals(GameState.WAITING)
                || getState().equals(GameState.STARTING)){


            if (getPlayers().size() >= settings.getMaxPlayers()) {
                if (player.hasPermission("game.joinfullserver")){
                    boolean joined = false;
                    for (int i = (getPlayers().size() - 1); i > 0; i--){
                        Player kickPlayer = getPlayers().get(i).getOnlinePlayer();
                        if (kickPlayer.hasPermission("game.joinfullserver")){
                            continue;
                        }

                        MessageManager.get(kickPlayer, "server.kicked_because_reserved_slot")
                                .send();
                        GameManager.newArena(kickPlayer, true);
                        joined = true;
                        break;
                    }

                    if (!joined){
                        MessageManager.get(player, "vip.full.slots")
                                .send();
                        GameManager.newArena(player, true);
                        return;
                    }
                }else {
                    MessageManager.get(player, "server.full")
                            .send();
                    GameManager.newArena(player, true);
                    return;
                }
            }

            getParticipants().add(gamePlayer);
            gamePlayer.getPlayerData().setGame(this);

            if (serverDataManager != null) {
                serverDataManager.getJSONProperty("Players").update(this, getParticipants().size());
            }


            if (getParticipants().size() == 1){
                Bukkit.getScheduler().runTaskTimer(GameAPI.getInstance(), task -> {
                    if (!getState().equals(GameState.WAITING) || getParticipants().isEmpty()){
                        task.cancel();
                        return;
                    }
                    updateWaitingForPlayersBossBar();
                }, 0, 30L);
            }

            Utils.hideAndShowPlayers(gamePlayer);

            MessageManager.get(getParticipants(), "chat.join")
                    .replace("%prefix%", gamePlayer.getPlayerData().getPrefix())
                    .replace("%player%", gp -> (gp.getFriends().isFriendWith(gamePlayer) || gp.getParty().getAllOnlinePlayers().contains(gamePlayer) ? "§6" : "") + player.getName())
                    .replace("%players%", String.valueOf(getPlayers().size()))
                    .replace("%max_players%", "" + getSettings().getMaxPlayers())
                    .add("Ẅ", gp -> gp.getFriends().isFriendWith(gamePlayer))
                    .add("ẅ", gp -> gp.getParty().getAllOnlinePlayers().contains(gamePlayer))
                    .send();


            player.setDisplayName("§r" + player.getName());
            player.setPlayerListName("§r" + player.getName());
            gamePlayer.resetAttributes();
            player.teleport(lobbyPoint);
            Bukkit.getScheduler().runTaskLater(GameAPI.getInstance(), task -> player.setGameMode(GameMode.ADVENTURE), 2L);



            player.getInventory().clear();
            if (getLobbyInventory() != null){
                getLobbyInventory().give(player);
            }


            if (getPlayers().size() == settings.getMinPlayers()){
                setState(GameState.STARTING);
                Task task = new Task(this, "StartCountdown", getSettings().getStartingTime(), new StartCountdown(), GameAPI.getInstance());
                task.setGame(this);
            }

            GameJoinEvent ev = new GameJoinEvent(this, gamePlayer, GameJoinEvent.JoinType.LOBBY);
            Bukkit.getPluginManager().callEvent(ev);
            return;
        }



        if (getState().equals(GameState.INGAME) && gamePlayer.getType().equals(GamePlayerType.DISCONNECTED) && getSettings().isEnabledReJoin()){
            if (getPlayers().size() >= settings.getMaxPlayers()) {

                getParticipants().add(gamePlayer);
                gamePlayer.getPlayerData().setGame(this);

                gamePlayer.setSpectator(false);
                gamePlayer.getPlayerData().getGame().preparePlayer(gamePlayer);
                gamePlayer.setType(GamePlayerType.PLAYER);
                player.teleport(Objects.requireNonNullElse((Location) gamePlayer.getMetadata().get("death_location"), getCurrentMap().getPlayerToLocation(gamePlayer)));


                GameJoinEvent ev = new GameJoinEvent(this, gamePlayer, GameJoinEvent.JoinType.LOBBY);
                Bukkit.getPluginManager().callEvent(ev);
                return;
            }else{
                MessageManager.get(gamePlayer, "chat.join_failed.full_game.rejoin")
                        .send();
            }
        }

        if (getState().equals(GameState.INGAME) && settings.isAllowedJoiningAfterStart()){
            if (getPlayers().size() >= settings.getMaxPlayers()) {

                getParticipants().add(gamePlayer);
                gamePlayer.getPlayerData().setGame(this);

                gamePlayer.getPlayerData().getGame().preparePlayer(gamePlayer);

                GameJoinEvent ev = new GameJoinEvent(this, gamePlayer, GameJoinEvent.JoinType.JOIN_AFTER_START);
                Bukkit.getPluginManager().callEvent(ev);
                return;
            }else{
                MessageManager.get(gamePlayer, "chat.join_failed.full_game.game_in_progress")
                        .send();
            }
        }

        if (getState().equals(GameState.INGAME)) {
            getParticipants().add(gamePlayer);
            gamePlayer.getPlayerData().setGame(this);

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

        BossBar bossBar = gamePlayer.getPlayerData().getCurrentBossBar();
        if (bossBar != null) {
            bossBar.removeAll();
        }

        if (serverDataManager != null) {
            serverDataManager.getJSONProperty("Players").update(this, getParticipants().size());
        }

        new BukkitRunnable(){
            @Override
            public void run() {
                gamePlayer.getPlayerData().saveAll();
            }
        }.runTaskAsynchronously(GameAPI.getInstance());

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
            if (gamePlayer.getPlayerData().getTeam() != null) {
                gamePlayer.getPlayerData().getTeam().quitPlayer(gamePlayer);
            }

            //PlayerManager.removeGamePlayer(player);
            gamePlayer.getPlayerData().flushSomeData();
        }else if (getState() == GameState.INGAME){
            if (!gamePlayer.isSpectator()){
                gamePlayer.setType(GamePlayerType.DISCONNECTED);
                player.damage(GameAPI.getInstance().getVersionSupport().getMaxPlayerHealth(player));

                // boolean killer = PVPListener.containsLastDamager(gamePlayer) && (System.currentTimeMillis() - PVPListener.getLastDamager(gamePlayer).getMs()) <= 12000;
                // GamePlayerDeathEvent deathEvent = new GamePlayerDeathEvent(gamePlayer.getPlayerData().getGame(), (killer ? PVPListener.getLastDamager(gamePlayer).getLastDamager() : null), PlayerManager.getGamePlayer(player), null,null);
                // Bukkit.getPluginManager().callEvent(deathEvent);
            }

            if (getPlayers().isEmpty()){
                endGame(null);
            }else{
                if (getSettings().isEnabledReJoin()){
                    return;
                }
            }
            PlayerManager.removeGamePlayer(player);
        }else{
            PlayerManager.removeGamePlayer(player);
        }


        checkArenaFullness();
    }

    public void checkArenaFullness(){
        if (!getState().equals(GameState.STARTING)) {
            return;
        }

        MinigameSettings settings = getSettings();
        if (getPlayers().isEmpty() || (getPlayers().size() <= settings.getMinPlayers() - 2 && settings.getMinPlayers() >= 5)
                || (settings.getMinPlayers() <= 4 && getPlayers().size() < settings.getMinPlayers())) {
            if (getState() == GameState.STARTING) {
                setState(GameState.WAITING);
                if (Task.getTask(this, "StartCountdown") != null) {
                    Task.getTask(this, "StartCountdown").setCounter(settings.getStartingTime());
                    Task.cancel(this, "StartCountdown");
                }

                for (GameMap a : getMapManager().getMaps()) {
                    if (!a.isIngame()) continue;
                    if (!settings.isChooseRandomMap()) {
                        a.setWinned(false);
                    }
                }
                getMapManager().setVoting(true);
                MessageManager.get(getParticipants(), "chat.not_enough_players")
                        .send();
            }
        }
    }

    public void startPreparation(){
        getMetadata().put("players_at_start", getPlayers().size());
        prepareGame();
        setState(GameState.INGAME);

        GamePreparationEvent ev = new GamePreparationEvent(this);
        Bukkit.getPluginManager().callEvent(ev);


        for (GamePlayer gamePlayer : getPlayers()){
            gamePlayer.setEnabledMovement(getSettings().isEnabledMovementInPreparation());
            gamePlayer.setLimited(true);
        }

        Task.cancel(this, "StartCountdown");
        Task task = new Task(this, "PreparationTask", getSettings().getPreparationTime(), new PreparationCountdown(), GameAPI.getInstance());
        task.setGame(this);
    }

    public void preparePlayer(GamePlayer gamePlayer){
        boolean rejoin = gamePlayer.getType().equals(GamePlayerType.DISCONNECTED);
        Player player = gamePlayer.getOnlinePlayer();

        //TODO: jak na rounds... toto spoustět vždy na začátku v startPreparation a jen vynechat tu zprávu...

        gamePlayer.resetAttributes();

        if (!rejoin) {
            player.sendMessage("");
            if (getSettings().sendMinigameDescription()) {
                MessageManager.get(gamePlayer, "chat.description")
                        .replace("%minigame%", GameAPI.getInstance().getMinigame().getName())
                        .replace("%description%", MessageManager.get(gamePlayer, GameAPI.getInstance().getMinigame().getDescriptionTranslateKey()).getTranslated())
                        .replace("%map%",
                                MessageManager.get(gamePlayer, "chat.description.map")
                                    .replace("%map%", getCurrentMap().getName()
                                    .replace("%authors%", getCurrentMap().getAuthors()))
                                    .getTranslated())
                        .replace("%authors%", getCurrentMap().getAuthors())
                        .send();
            }
            if (getSettings().isUseTeams() && getSettings().getMaxTeamPlayers() > 1) {
                player.sendMessage(MessageManager.get(player, "chat.team_chat").getTranslated());
            }

            if (getLobbyInventory() != null) {
                getLobbyInventory().unloadInventory(player);
            }
            player.getInventory().clear();

            if (gamePlayer.getPlayerData().getKit() != null) {
                gamePlayer.getPlayerData().getKit().activate(gamePlayer);
            }
            if (getState().equals(GameState.INGAME) && getSettings().isAllowedJoiningAfterStart()) {
                //getCurrentMap().teleport(gamePlayer);
                player.teleport(gamePlayer.getPlayerData().getTeam().getSpawn());
            }
        }
    }

    public void prepareGame(){
        if (!getSettings().isChooseRandomMap()) {
            if (this.nextArena() == null) {
                return;
            }
            this.nextArena().setWinned(true);
        }


        GameMap playingMap = getCurrentMap();

        if (playingMap.getSettings().isLoadWorldWithGameAPI()) {
            String worldName = playingMap.getName() + "_" + getID();
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                playingMap.setWorld(world);
            } else {
                Logger.log("I haven't had time to load the world!", Logger.LogType.ERROR);
                return;
            }
        }

        if (getSettings().isUseTeams()){
            for (GamePlayer gamePlayer : getParticipants()){
                if (gamePlayer.getPlayerData().getTeam() == null){
                    GameTeam lowestTeam = TeamManager.getSmallestTeam(this);
                    lowestTeam.joinPlayer(gamePlayer, TeamJoinCause.AUTO);
                }
            }
        }



        if (getSettings().isEnabledRespawning() && getSettings().useTeams()){
            TeamManager.getTeams(this).forEach(team -> team.setDead(false));
        }

        getCurrentMap().getWorld().setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);

        getCurrentMap().teleport();
        for (GamePlayer gamePlayer : getPlayers()) {
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

    }

    public void startGame(){
        setState(GameState.INGAME);

        if (getSettings().isDefaultGameCountdown()) {
            Task.cancel(this, "PreparationTask");
            Bukkit.getScheduler().runTaskLater(GameAPI.getInstance(), t -> {
                Task task = new Task(this, "GameCountdown", getSettings().getGameTime(), new GameCountdown(), GameAPI.getInstance());
                task.setGame(this);

                if (getSettings().getRounds() > 1){
                    task.setRestartCount(getSettings().getRounds() - 1);
                }
            }, 2L);
        }

        if (!getSettings().usePreperationTask()){
            Task.cancel(this, "StartCountdown");
            prepareGame();
        }

        for (GamePlayer gamePlayer : getParticipants()){
            gamePlayer.setEnabledMovement(true);
            gamePlayer.setLimited(false);
        }

        GameStartEvent ev = new GameStartEvent(this);
        Bukkit.getPluginManager().callEvent(ev);
    }

    public void endGame(Winner winner){
        setState(GameState.ENDING);

        Task.cancelAll(this);

        for (Iterator<KeyedBossBar> it = Bukkit.getBossBars(); it.hasNext();) {
            BossBar bossBar = it.next();
            bossBar.removeAll();
        }



        String rankingScore = "kills";
        for (List<PlayerScore> scoreList : PlayerManager.getPlayerScores().values()) {
            for (PlayerScore score : scoreList) {
                if (score.isScoreRanking()){
                    rankingScore = score.getName();
                }
            }
        }

        Map<GamePlayer, Integer> ranking =  Utils.getTopPlayers(this, rankingScore, 3);

        GameEndEvent ev = new GameEndEvent(this, winner, ranking);
        Bukkit.getPluginManager().callEvent(ev);



        Bukkit.getScheduler().runTaskLater(GameAPI.getInstance(), t -> {
            Task task = new Task(this, "EndCountdown", (!getParticipants().isEmpty() ? 15 : 5), new EndCountdown(), GameAPI.getInstance());
            task.setGame(this);
        }, 30L);


        for (GamePlayer gamePlayer : getParticipants()) {
            gamePlayer.getOnlinePlayer().stopSound("custom:ending");
            GameAPI.getInstance().getVersionSupport().setTeamNameTag(gamePlayer.getOnlinePlayer(), getName().replace(" ", "") + getID(), ChatColor.WHITE);
            Utils.hideAndShowPlayers(gamePlayer);

            if (getSettings().teleportPlayersAfterEnd()){
                gamePlayer.getOnlinePlayer().teleport(getLobbyPoint());
            }else{
                gamePlayer.getOnlinePlayer().setAllowFlight(true);
                gamePlayer.getOnlinePlayer().setFlying(true);
            }

            gamePlayer.getOnlinePlayer().sendMessage("");

            if (winner != null) {
                if ((winner.getWinnerType().equals(Winner.WinnerType.PLAYER) && (winner.equals(gamePlayer)) || (winner.getWinnerType().equals(Winner.WinnerType.TEAM) && gamePlayer.getPlayerData().getTeam().equals(winner)))) {
                    MessageManager.get(gamePlayer, "chat.you_won")
                            .send();
                }else{
                    MessageManager.get(gamePlayer, "chat.winner")
                            .replace("%winner%", gp -> winner.getWinnerType().getTranslatedName(gp) + " " + (winner.getWinnerType().equals(Winner.WinnerType.PLAYER)
                                    ? ((GamePlayer) winner).getOnlinePlayer().getName()
                                    : ((GameTeam) winner).getChatColor() + ((GameTeam) winner).getName()))
                            .send();
                }
            }else{
                MessageManager.get(gamePlayer, "chat.nobody_won")
                        .send();
            }


            if (winner != null) {
                if ((winner.getWinnerType().equals(Winner.WinnerType.PLAYER) && (winner.equals(gamePlayer)) || (winner.getWinnerType().equals(Winner.WinnerType.TEAM) && gamePlayer.getPlayerData().getTeam().equals(winner)))) {
                    MessageManager.get(gamePlayer, "title.victory")
                            .send();
                    gamePlayer.getOnlinePlayer().playSound(gamePlayer.getOnlinePlayer(), "custom:victory1", 0.35F, 1F);
                } else {
                    MessageManager.get(gamePlayer, "title.winner")
                            .replace("%winner%", (winner.getWinnerType().equals(Winner.WinnerType.PLAYER) ? MessageManager.get(gamePlayer, "winnerType.player") + " " : ((GameTeam) winner).getChatColor()) + winner.getWinnerType().getTranslatedName(gamePlayer) + (winner.getWinnerType().equals(Winner.WinnerType.TEAM) ? " " + MessageManager.get(gamePlayer, "winnerType.team") : ""))
                            .send();
                }


                Location location;
                if (getSettings().isTeleportPlayersAfterEnd()){
                    location = getLobbyPoint();
                }else{
                    location = getCurrentMap().getMainArea().getCenter();
                }

                new BukkitRunnable(){
                    Location newLocation;

                    @Override
                    public void run() {
                        if (GameManager.getGameByID(getID()) == null || getState() != GameState.ENDING){
                            this.cancel();
                            return;
                        }

                        newLocation = location.add(Utils.getRandom(-20, 20), 0,  Utils.getRandom(-20, 20));
                        newLocation = newLocation.getWorld().getHighestBlockAt(newLocation).getLocation();
                        newLocation = newLocation.add(0, Utils.getRandom(5, 10), 0);

                        Utils.spawnFireworks(newLocation, 1, Color.WHITE, FireworkEffect.Type.BALL);
                        Color color = Color.YELLOW;
                        if (getSettings().isUseTeams()){
                            color = ((GameTeam) getWinner()).getColor();
                        }
                        Utils.spawnFireworks(newLocation, 1, color, FireworkEffect.Type.BALL);
                    }
                }.runTaskTimer(GameAPI.getInstance(), 0L, 40L);


                new BukkitRunnable(){
                    @Override
                    public void run() {
                        for (GamePlayer gamePlayer : getPlayers()) {
                            Player player = gamePlayer.getOnlinePlayer();

                            if (getSpectatorManager().getInventoryManager().getPlayers().contains(gamePlayer.getOnlinePlayer())) {
                                getSpectatorManager().getInventoryManager().unloadInventory(gamePlayer.getOnlinePlayer());
                            }


                            gamePlayer.resetAttributes();
                            gamePlayer.setLimited(false);
                            gamePlayer.setEnabledMovement(true);
                            gamePlayer.setSpectator(false);
                            player.setGameMode(GameMode.ADVENTURE);
                            gamePlayer.getPlayerData().getCurrentInventory().unloadInventory(player);

                            InventoryManager itemManager = new InventoryManager("Ending");
                            if (GameManager.getGames().size() > 1 || (GameAPI.getInstance().getMinigame().getDataManager() != null && GameAPI.getInstance().getMinigame().getDataManager().isThereFreeGame())) {
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
                            itemManager.give(player);
                        }
                    }
                }.runTaskLater(GameAPI.getInstance(), 2L);
            }


            /*for (Resource type : GameAPI.getInstance().getMinigame().getEconomies()) {
                if (ResourcesManager.getEarned(gamePlayer, type) != 0) {
                    int earned = ResourcesManager.getEarned(gamePlayer, type);
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            type.getResourceInterface().deposit(gamePlayer, earned);
                        }
                    }.runTaskAsynchronously(GameAPI.getInstance());
                }
            }*/
        }


        HashMap<GamePlayer, Integer> oldWinstreaks = new HashMap<>();
        for (GamePlayer gp : getParticipants()) {
            oldWinstreaks.put(gp, gp.getPlayerData().getStat("Winstreak").getStatScore());

            gp.getOnlinePlayer().sendMessage("");
        }

        if (winner != null) {
            if (winner.getWinnerType().equals(Winner.WinnerType.TEAM)) {
                GameTeam wTeam = (GameTeam) winner;
                for (GameTeam lTeam : TeamManager.getTeams(this).stream().filter(team -> team != wTeam).toList()) {
                    lTeam.getMembers().forEach(loser -> loser.getPlayerData().getStat("Winstreak").setStat(0));
                }
                for (GamePlayer gamePlayer : wTeam.getMembers()) {
                    gamePlayer.getPlayerData().getStat("Winstreak").increase();
                }
            } else {
                ((GamePlayer) winner).getPlayerData().getStat("Winstreak").increase();
                for (GamePlayer loser : getParticipants().stream().filter(gp -> gp != winner).toList()) {
                    loser.getPlayerData().getStat("Winstreak").setStat(0);
                }
            }
        }

        for (GamePlayer gamePlayer : getParticipants()){
            Bukkit.getScheduler().runTaskAsynchronously(GameAPI.getInstance(), bukkitTask -> gamePlayer.getPlayerData().saveAll());
        }



        String finalRankingScore = rankingScore;
        new BukkitRunnable(){
            @Override
            public void run() {
                sendTopPlayers(ranking, finalRankingScore);
            }
        }.runTaskLater(GameAPI.getInstance(), 10L);

        new BukkitRunnable(){
            @Override
            public void run() {
                sendRewardSummary();

                for (GamePlayer gp : oldWinstreaks.keySet()) {
                    Player player = gp.getOnlinePlayer();

                    if (winner != null) {
                        player.sendMessage("");
                        MessageManager.get(gp, "chat.winstreak")
                                .replace("%old_winstreak%", "" + oldWinstreaks.get(gp))
                                .replace("%new_winstreak%", (gp.getPlayerData().getStat("Winstreak").getStatScore() == 0 ? "§c" : "§a") + gp.getPlayerData().getStat("Winstreak").getStatScore())
                                .send();
                    }


                    List<PlayerScore> stats = PlayerManager.getScoresByPlayer(gp).stream().filter(s -> s.getScore() != 0 && s.getStat() != null).toList();

                    if (!stats.isEmpty()) {
                        TextComponent message = new TextComponent(MessageManager.get(gp, "chat.view_statistic").getTranslated());

                        ComponentBuilder b = new ComponentBuilder("");
                        int i = 0;
                        for (PlayerScore score : stats) {
                            if (i != 0) {
                                b.append("\n");
                            }
                            b.append("§7" + score.getPluralName() + ": §a" + score.getScore());
                            i++;
                        }

                        message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, b.create()));
                        player.spigot().sendMessage(message);
                        player.sendMessage("");
                    }
                }
            }
        }.runTaskLater(GameAPI.getInstance(), 10L + 40L);
    }

    public void sendTopPlayers(Map<GamePlayer, Integer> ranking, String rankingScore){
        if (ranking.isEmpty()){
            return;
        }

        Map<GamePlayer, Integer> fullRanking =  Utils.getTopPlayers(this, rankingScore, getSettings().getMaxPlayers());

        for (GamePlayer gamePlayer : getParticipants()) {
            Player player = gamePlayer.getOnlinePlayer();

            MessageManager.get(gamePlayer, "chat.top3players.title").send();

            for (GamePlayer pos : ranking.keySet()) {
                int position = ranking.get(pos);
                MessageManager.get(gamePlayer, "chat.top3players.position")
                        .replace("%position%", "" + position)
                        .replace("%player_head%", ChatHeadAPI.getInstance().getHeadAsString(pos.getOfflinePlayer()))
                        .replace("%player%", (pos.equals(gamePlayer) ? "§a" : "") + pos.getOnlinePlayer().getName() + "§r")
                        .replace("%score%", "" + pos.getScoreByName(rankingScore).getScore())
                        .replace("%ranking_score_name%", rankingScore)
                        .send();
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
                TextComponent message = new TextComponent(MessageManager.get(gamePlayer, "chat.top3players.your_party_members_position").getTranslated());

                ComponentBuilder b = new ComponentBuilder("");
                for (GamePlayer partyMember : fullRanking.keySet().stream().filter(p -> party.getAllOnlinePlayers().contains(p)).toList()) {
                    b.append(MessageManager.get(gamePlayer, "chat.top3players.position")
                            .replace("%position%", "" + fullRanking.get(partyMember))
                            .replace("%player_head%", ChatHeadAPI.getInstance().getHeadAsString(partyMember.getOfflinePlayer()))
                            .replace("%player%", partyMember.getOnlinePlayer().getName())
                            .replace("%score%", "" + partyMember.getScoreByName(rankingScore).getScore())
                            .replace("%ranking_score_name%", rankingScore)
                            .getTranslated());
                    b.append("\n");
                }

                message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, b.create()));
                gamePlayer.getOnlinePlayer().spigot().sendMessage(message);
            }


            if (friends.hasFriends() && !friends.getAllOnlinePlayers().isEmpty()) {
                List<GamePlayer> friendsList = new ArrayList<>(friends.getAllOnlinePlayers()).stream().filter(p -> (!party.isInParty() || party.getAllOnlinePlayers().contains(p))).toList();

                if (!friendsList.isEmpty()) {
                    TextComponent message = new TextComponent(MessageManager.get(gamePlayer, "chat.top3players.friends_position").getTranslated());

                    ComponentBuilder b = new ComponentBuilder("");
                    for (GamePlayer friend : fullRanking.keySet().stream().filter(friendsList::contains).toList()) {
                        b.append(MessageManager.get(gamePlayer, "chat.top3players.position")
                                .replace("%position%", "" + fullRanking.get(friend))
                                .replace("%player_head%", ChatHeadAPI.getInstance().getHeadAsString(friend.getOfflinePlayer()))
                                .replace("%player%", friend.getOnlinePlayer().getName())
                                .replace("%score%", "" + friend.getScoreByName(rankingScore).getScore())
                                .replace("%ranking_score_name%", rankingScore)
                                .getTranslated());
                        b.append("\n");
                    }

                    message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, b.create()));
                    gamePlayer.getOnlinePlayer().spigot().sendMessage(message);
                }
            }

            player.sendMessage("");
        }
    }

    public void sendRewardSummary(){
        for (GamePlayer gamePlayer : getParticipants()){
            Player player = gamePlayer.getOnlinePlayer();

            if (!ResourcesManager.earnedSomething(gamePlayer)){
                return;
            }

            gamePlayer.getOnlinePlayer().playSound(gamePlayer.getOnlinePlayer(), "custom:gamebonus", 0.8F, 1F);
            MessageManager.get(gamePlayer, "chat.rewardsummary.title").send();

            for (Resource type : GameAPI.getInstance().getMinigame().getEconomies()) {
                int earned = ResourcesManager.getEarned(gamePlayer, type);
                if (earned > 0) {

                    TextComponent message = new TextComponent("  §7• " + (type.getImg_char() != null ? type.getImg_char() : "") + " " + type.getChatColor() + earned + " §7" + type.getName());
                    ComponentBuilder b = new ComponentBuilder("");

                    List<PlayerScore> scoreList = PlayerManager.getScoresByPlayer(gamePlayer);

                    int i = 0;
                    for (PlayerScore score : scoreList) {
                        if (score.getGamePlayer() == gamePlayer && score.getScore() != 0 && score.getEarned(type) != 0) {
                            b.append(type.getChatColor() + "+" + score.getEarned(type) + ChatColor.GRAY + " (" + (score.getScore() > 1 ? score.getScore() + " " : "") + score.getDisplayName(true)/*(!(score.getDisplayName().endsWith("s")) ? score.getDisplayName() : score.getDisplayName().substring(0, score.getDisplayName().length() - 1))*/ + ")");
                            i++;
                            if (i < scoreList.size() - 1) {
                                b.append("\n");
                            }
                        }
                    }

                    message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, b.create()));
                    gamePlayer.getOnlinePlayer().spigot().sendMessage(message);


                    Resource mainResource = GameAPI.getInstance().getMinigame().getEconomies().stream().filter(e -> e.getRank() == 1).toList().get(0);

                    if (mainResource != null && mainResource == type) {
                        List<Integer> percentages = Arrays.asList(50, 45, 40, 35, 30, 25, 20, 15, 10, 5);

                        for (Integer percent : percentages) {
                            if (gamePlayer.getOnlinePlayer().hasPermission("vip.bonus" + percent)){
                                int coins = (int) (((double) earned * (double) percent) / (double) 100);
                                if (coins != 0) {
                                    mainResource.getResourceInterface().deposit(gamePlayer, coins);
                                    MessageManager.get(gamePlayer, "chat.rewardsummary.bonus")
                                            .replace("%economy_color%", "" + mainResource.getChatColor())
                                            .replace("%reward%", "" + coins)
                                            .replace("%economy_name%", mainResource.getName())
                                            .replace("%bonus%", "" + percent)
                                            .send();
                                    break;
                                }
                            }
                        }
                    }
                }
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
                    player.getOnlinePlayer().sendMessage(MessageManager.get(player, "chat.map_won").replace("%map%", a.getName()).replace("%number%", "" + i).getTranslated());
                }
            } else {
                for (GamePlayer player : getPlayers()){
                    player.getOnlinePlayer().sendMessage(MessageManager.get(player, "chat.map_won").replace("%map%", a.getName()).getTranslated());
                }
            }
            if (i == playingArenas) break;
        }
    }

    public GameMap selectRandomMap() {
        getMapManager().setVoting(false);

        List<GameMap> maps = new ArrayList<>(getMapManager().getMaps().stream().filter(GameMap::isIngame).toList());
        Collections.shuffle(maps);

        GameMap map = maps.get(0);
        map.setWinned(true);
        return map;
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
        return GameAPI.getInstance().getMinigame().getSettings();
    }

    public GameMap get() {
        for (GameMap a : getMapManager().getMaps()) {
            if (a.isPlaying()) return a;
        }
        return null;
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
        if (!containsBlock(block)){
            placedBlocks.add(block);
        }
    }

    public void removeBlock(Block block) {
        placedBlocks.remove(block);
    }

    public boolean containsBlock(Block block) {
        return placedBlocks.contains(block);
    }
}
