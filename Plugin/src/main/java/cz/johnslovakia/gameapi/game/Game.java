package cz.johnslovakia.gameapi.game;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.MinigameSettings;
import cz.johnslovakia.gameapi.economy.Economy;
import cz.johnslovakia.gameapi.economy.RewardsManager;
import cz.johnslovakia.gameapi.events.*;
import cz.johnslovakia.gameapi.game.kit.Kit;
import cz.johnslovakia.gameapi.game.map.GameMap;
import cz.johnslovakia.gameapi.game.map.MapVotesComparator;
import cz.johnslovakia.gameapi.game.team.GameTeam;
import cz.johnslovakia.gameapi.game.team.TeamJoinCause;
import cz.johnslovakia.gameapi.game.map.MapManager;
import cz.johnslovakia.gameapi.listeners.PVPListener;
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
import cz.johnslovakia.gameapi.utils.Sounds;
import cz.johnslovakia.gameapi.utils.StringUtils;
import cz.johnslovakia.gameapi.utils.Utils;
import cz.johnslovakia.gameapi.utils.Logger;
import cz.johnslovakia.gameapi.utils.chatHead.ChatHeadAPI;
import cz.johnslovakia.gameapi.utils.inventoryBuilder.InventoryManager;
import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.boss.BossBar;
import org.bukkit.boss.KeyedBossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

@Getter
public class Game {

    private final String name;
    private String ID;
    @Getter @Setter
    private MapManager mapManager;
    private GameState state = GameState.LOADING;
    @Getter @Setter
    private Task runningMainTask;
    private final Location lobbyPoint;
    @Getter @Setter
    private InventoryManager lobbyInventory;
    @Getter @Setter
    private SpectatorManager spectatorManager;
    @Getter @Setter
    private boolean firstGameKill = true;

    private Winner winner;

    private final List<GamePlayer> participants = new ArrayList<>();
    private final List<Block> placedBlocks = new ArrayList<>();
    private final HashMap<String, Object> metadata = new HashMap<>();

    public Game(String name, InventoryManager lobbyInventory, Location lobbyPoint) {
        this.name = name;
        this.lobbyInventory = lobbyInventory;
        this.lobbyPoint = lobbyPoint;

        this.spectatorManager = new SpectatorManager();
        getSpectatorManager().loadItemManager();

        while (this.ID == null || GameManager.isDuplicate(this.ID)){
            this.ID = StringUtils.randomString(6, true, true, false);
        }

        state = GameState.WAITING;
    }


    public void joinPlayer(Player player){
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);
        /*if (!gamePlayer.getType().equals(GamePlayerType.DISCONNECTED)) {
            PlayerManager.removeGamePlayer(player);
            gamePlayer = PlayerManager.getGamePlayer(player);
        }*/

        MinigameSettings settings = getSettings();



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

            Utils.hideAndShowPlayers(gamePlayer);

            MessageManager.get(getParticipants(), "chat.join")
                    .replace("%prefix%", gamePlayer.getPlayerData().getPrefix())
                    .replace("%player%", gp -> (gp.getFriends().isFriendWith(gamePlayer) || gp.getParty().getAllOnlinePlayers().contains(gamePlayer) ? "§6" : "") + player.getName())
                    .replace("%players%", String.valueOf(getPlayers().size()))
                    .add("Ẅ", gp -> gp.getFriends().isFriendWith(gamePlayer))
                    .add("ẅ", gp -> gp.getParty().getAllOnlinePlayers().contains(gamePlayer))
                    .send();


            player.setDisplayName("§r" + player.getName());
            player.setPlayerListName("§r" + player.getName());
            gamePlayer.resetAttributes();
            player.setGameMode(GameMode.ADVENTURE);
            player.teleport(lobbyPoint);

            /*Kit defaultKit = gamePlayer.getPlayerData().getDefaultKit();
            if (defaultKit != null) {
                defaultKit.select(gamePlayer);
            }*/ //v PlayerData.java

            player.getInventory().clear();
            if (getLobbyInventory() != null){
                getLobbyInventory().give(player);
            }


            if (getPlayers().size() == settings.getMinPlayers()){
                setState(GameState.STARTING); //TODO: starting
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
                player.teleport(Objects.requireNonNullElse((Location) gamePlayer.getMetadata().get("death_location"), getPlayingMap().getPlayerToLocation(gamePlayer)));


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

        if (getState().equals(GameState.WAITING) || getState().equals(GameState.STARTING)){
            MessageManager.get(getParticipants(), "chat.quit")
                    .replace("%prefix%",  gamePlayer.getPlayerData().getPrefix())
                    .replace("%player%", gp -> (gp.getFriends().isFriendWith(gamePlayer) || gp.getParty().getAllOnlinePlayers().contains(gamePlayer) ? "§6" : "") + player.getName())
                    .replace("%players%", String.valueOf(getPlayers().size()))
                    .add("Ẅ", gp -> gp.getFriends().isFriendWith(gamePlayer))
                    .add("ẅ", gp -> gp.getParty().getAllOnlinePlayers().contains(gamePlayer))
                    .send();

            gamePlayer.getPlayerData().removeVotesForMaps();
            if (gamePlayer.getPlayerData().getTeam() != null) {
                gamePlayer.getPlayerData().getTeam().quitPlayer(gamePlayer);
            }

            //TODO: možná udělat jinak, aby se nemuseli načítat furt data, ale aby se smazali selectnuté kity,... a třeba votes
            PlayerManager.removeGamePlayer(player);
        }else if (getState() == GameState.INGAME || getState() == GameState.PREPARATION){
            if (!gamePlayer.isSpectator()){
                gamePlayer.setType(GamePlayerType.DISCONNECTED);
                player.damage(GameAPI.getInstance().getVersionSupport().getMaxPlayerHealth(player));

                // boolean killer = PVPListener.containsLastDamager(gamePlayer) && (System.currentTimeMillis() - PVPListener.getLastDamager(gamePlayer).getMs()) <= 12000;
                // GamePlayerDeathEvent deathEvent = new GamePlayerDeathEvent(gamePlayer.getPlayerData().getGame(), (killer ? PVPListener.getLastDamager(gamePlayer).getLastDamager() : null), PlayerManager.getGamePlayer(player), null,null);
                // Bukkit.getPluginManager().callEvent(deathEvent);
            }

            if (getPlayers().isEmpty()){
                endGame(null);
            }
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
                        a.setPlaying(false);
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
        setState(GameState.PREPARATION);
        prepareGame();

        GamePreparationEvent ev = new GamePreparationEvent(this);
        Bukkit.getPluginManager().callEvent(ev);


        for (GamePlayer gamePlayer : getPlayers()){
            gamePlayer.setEnabledMovement(false);
        }

        Task.cancel(this, "StartCountdown");
        Task task = new Task(this, "PreparationTask", getSettings().getPreparationTime(), new PreparationCountdown(), GameAPI.getInstance());
        task.setGame(this);
    }

    public void preparePlayer(GamePlayer gamePlayer){
        boolean rejoin = gamePlayer.getType().equals(GamePlayerType.DISCONNECTED);
        Player player = gamePlayer.getOnlinePlayer();

        player.setGameMode(GameMode.SURVIVAL);
        player.setHealth(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
        player.setLevel(0);
        player.setExp(0);
        player.setFireTicks(0);
        player.setAllowFlight(false);
        player.setFlying(false);

        if (!rejoin) {
            player.sendMessage("");
            if (getSettings().sendMinigameDescription()) {
                MessageManager.get(gamePlayer, "chat.description")
                        .replace("%minigame%", GameAPI.getInstance().getMinigame().getMinigameName())
                        .replace("%description%", MessageManager.get(gamePlayer, GameAPI.getInstance().getMinigame().getDescriptionTranslateKey()).getTranslated())
                        .replace("%map%",
                                MessageManager.get(gamePlayer, "chat.description.map")
                                    .replace("%map%", getPlayingMap().getName()
                                    .replace("%authors%", getPlayingMap().getAuthors()))
                                    .getTranslated())
                        .replace("%authors%", getPlayingMap().getAuthors())
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
                //getPlayingMap().teleport(gamePlayer);
                player.teleport(gamePlayer.getPlayerData().getTeam().getSpawn());
            }
        }
    }

    public void prepareGame(){
        if (!getSettings().isChooseRandomMap()) {
            if (this.nextArena() == null) {
                return;
            }
            this.nextArena().setPlaying(true);
        }


        GameMap playingMap = getPlayingMap();

        //TODO: check
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


        getPlayingMap().teleport(this);


        if (getSettings().isEnabledRespawning() && getSettings().useTeams()){
            TeamManager.getTeams(this).forEach(team -> team.setDead(false));
        }

        for (GamePlayer gamePlayer : getPlayers()) {
            preparePlayer(gamePlayer);
        }


        getPlayingMap().getWorld().setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);


        for (GamePlayer gamePlayer : getPlayers()) {
            if (GameAPI.useDecentHolograms()) {
                StatsHolograms.remove(gamePlayer.getOnlinePlayer());
            }
        }
    }

    public void startGame(){
        setState(GameState.INGAME);

        if (getSettings().isDefaultGameCountdown()) {
            Task task = new Task(this, "GameCountdown", getSettings().getGameTime(), new GameCountdown(), GameAPI.getInstance());
            task.setGame(this);
        }

        if (!getSettings().usePreperationTask()){
            Task.cancel(this, "StartCountdown");
            prepareGame();
        }

        GameStartEvent ev = new GameStartEvent(this);
        Bukkit.getPluginManager().callEvent(ev);

        getMetadata().put("players_at_start", getPlayers().size());
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




        Task task = new Task(this, "EndCountdown", (!getParticipants().isEmpty() ? 15 : 5), new EndCountdown(), GameAPI.getInstance());
        task.setGame(this);


        for (GamePlayer gamePlayer : getParticipants()) {
            if (getSettings().teleportPlayersAfterEnd()){
                gamePlayer.getOnlinePlayer().teleport(getLobbyPoint());
            }else{
                gamePlayer.getOnlinePlayer().setAllowFlight(true);
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
                    gamePlayer.getOnlinePlayer().playSound(gamePlayer.getOnlinePlayer(), "custom:victory1", 0.35F, 1.0F);
                } else {
                    MessageManager.get(gamePlayer, "title.winner")
                            .replace("%winner%", (winner.getWinnerType().equals(Winner.WinnerType.PLAYER) ? MessageManager.get(gamePlayer, "winnerType.player") + " " : ((GameTeam) winner).getChatColor()) + winner.getWinnerType().getTranslatedName(gamePlayer) + (winner.getWinnerType().equals(Winner.WinnerType.TEAM) ? " " + MessageManager.get(gamePlayer, "winnerType.team") : ""))
                            .send();
                }
            }


            for (Economy type : GameAPI.getInstance().getMinigame().getEconomies()) {
                if (RewardsManager.getEarned(gamePlayer, type) != 0) {
                    int earned = RewardsManager.getEarned(gamePlayer, type);
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            type.getEconomyInterface().deposit(gamePlayer, earned);
                        }
                    }.runTaskAsynchronously(GameAPI.getInstance());
                }
            }
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
            //TODO: check
            new BukkitRunnable(){
                @Override
                public void run() {
                    try{
                        gamePlayer.getPlayerData().saveAll();
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }.runTaskAsynchronously(GameAPI.getInstance());
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
                    if (winner != null) {
                        MessageManager.get(gp, "chat.winstreak")
                                .replace("%old_winstreak%", "" + oldWinstreaks.get(gp))
                                .replace("%new_winstreak%", (gp.getPlayerData().getStat("Winstreak").getStatScore() == 0 ? "§c" : "§a") + gp.getPlayerData().getStat("Winstreak").getStatScore())
                                .send();

                        gp.getOnlinePlayer().sendMessage("");
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
                            b.append("§7" + score.getDisplayName(true) + ": §a" + score.getScore());
                            i++;
                        }

                        message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, b.create()));
                        gp.getOnlinePlayer().spigot().sendMessage(message);
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
                        .replace("%player%", pos.getOnlinePlayer().getName())
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
                    //b.append(" " + ChatHeadAPI.getInstance().getHeadAsString(partyMember.getOfflinePlayer()) + " §f" + partyMember.getOnlinePlayer().getName() + " §7- " + );
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

                TextComponent message = new TextComponent(MessageManager.get(gamePlayer, "chat.top3players.friends_position").getTranslated());

                ComponentBuilder b = new ComponentBuilder("");
                for (GamePlayer friend : fullRanking.keySet().stream().filter(friendsList::contains).toList()) {
                    //b.append(" " + ChatHeadAPI.getInstance().getHeadAsString(partyMember.getOfflinePlayer()) + " §f" + partyMember.getOnlinePlayer().getName() + " §7- " + );
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


            player.sendMessage("");
        }
    }

    public void sendRewardSummary(){
        for (GamePlayer gamePlayer : getParticipants()){
            Player player = gamePlayer.getOnlinePlayer();
            player.playSound(player, Sounds.LEVEL_UP.bukkitSound(), 1F, 1F);

            if (!RewardsManager.earnedSomething(gamePlayer)){
                return;
            }

            MessageManager.get(gamePlayer, "chat.rewardsummary.title").send();

            for (Economy type : GameAPI.getInstance().getMinigame().getEconomies()) {
                int earned = RewardsManager.getEarned(gamePlayer, type);
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


                    Economy mainEconomy = GameAPI.getInstance().getMinigame().getEconomies().stream().filter(e -> e.getRank() == 1).toList().get(0);

                    if (mainEconomy != null && mainEconomy == type) {
                        List<Integer> percentages = Arrays.asList(50, 45, 40, 35, 30, 25, 20, 15, 10, 5);

                        for (Integer percent : percentages) {
                            if (gamePlayer.getOnlinePlayer().hasPermission("vip.bonus" + percent)){
                                int coins = (int) (((double) earned * (double) percent) / (double) 100);
                                if (coins != 0) {
                                    mainEconomy.getEconomyInterface().deposit(gamePlayer, coins);
                                    MessageManager.get(gamePlayer, "chat.rewardsummary.bonus")
                                            .replace("%economy_color%", "" + mainEconomy.getChatColor())
                                            .replace("%reward%", "" + coins)
                                            .replace("%economy_name%", mainEconomy.getName())
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

    public GameMap nextArena() {
        List<GameMap> arenas1 = new ArrayList<>(getMapManager().getMaps());
        arenas1.sort(new MapVotesComparator());


        for (GameMap a : arenas1) {
            if (!a.isWinned()) continue;
            if (a.isPlayed()) continue;
            if (a.isPlaying()) {
                a.setPlaying(false);
                a.setPlayed(true);
                continue;
            }
            return a;
        }
        return null;
    }

    public Game setState(GameState state) {
        this.state = state;

        return this;
    }

    public MinigameSettings getSettings(){
        return GameAPI.getInstance().getMinigame().getSettings();
    }

    public GameMap getPlayingMap() {
        for (GameMap a : getMapManager().getMaps()) {
            if (a.isPlaying()) return a;
        }
        return null;
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
