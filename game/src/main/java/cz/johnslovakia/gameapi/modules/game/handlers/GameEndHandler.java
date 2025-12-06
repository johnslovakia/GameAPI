package cz.johnslovakia.gameapi.modules.game.handlers;

import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.events.GameEndEvent;
import cz.johnslovakia.gameapi.guis.ProfileInventory;
import cz.johnslovakia.gameapi.guis.QuestInventory;
import cz.johnslovakia.gameapi.inventoryBuilder.InventoryBuilder;
import cz.johnslovakia.gameapi.inventoryBuilder.Item;
import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.game.*;
import cz.johnslovakia.gameapi.modules.game.lobby.LobbyModule;
import cz.johnslovakia.gameapi.modules.game.session.GameSessionModule;
import cz.johnslovakia.gameapi.modules.game.session.PlayerGameSession;
import cz.johnslovakia.gameapi.modules.game.task.TaskModule;
import cz.johnslovakia.gameapi.modules.game.team.GameTeam;
import cz.johnslovakia.gameapi.modules.game.team.TeamModule;
import cz.johnslovakia.gameapi.modules.levels.LevelModule;
import cz.johnslovakia.gameapi.modules.levels.PlayerLevelData;
import cz.johnslovakia.gameapi.modules.messages.MessageModule;
import cz.johnslovakia.gameapi.modules.resources.Resource;
import cz.johnslovakia.gameapi.modules.scores.Score;
import cz.johnslovakia.gameapi.modules.scores.ScoreModule;
import cz.johnslovakia.gameapi.modules.serverManagement.DataManager;
import cz.johnslovakia.gameapi.modules.stats.StatsModule;
import cz.johnslovakia.gameapi.rewards.unclaimed.UnclaimedRewardType;
import cz.johnslovakia.gameapi.rewards.unclaimed.UnclaimedRewardsModule;
import cz.johnslovakia.gameapi.modules.game.task.Task;
import cz.johnslovakia.gameapi.modules.game.task.tasks.EndCountdown;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerIdentity;
import cz.johnslovakia.gameapi.users.PlayerManager;
import cz.johnslovakia.gameapi.users.friends.FriendsInterface;
import cz.johnslovakia.gameapi.users.parties.PartyInterface;
import cz.johnslovakia.gameapi.utils.*;
import cz.johnslovakia.gameapi.utils.chatHead.ChatHeadAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class GameEndHandler {

    private final GameInstance gameInstance;

    public GameEndHandler(GameInstance gameInstance) {
        this.gameInstance = gameInstance;
    }

    public void endGame(Winner winner){
        if (winner instanceof GameTeam gameTeam){
            gameInstance.getPlacements().add(new Placement<>(gameTeam, 1));
        }else{
            gameInstance.getPlacements().add(new Placement<>((GamePlayer) winner, 1));
        }

        StatsModule statsModule = ModuleManager.getModule(StatsModule.class);
        UnclaimedRewardsModule unclaimedRewardsModule = ModuleManager.getModule(UnclaimedRewardsModule.class);
        GameService gameService = ModuleManager.getModule(GameService.class);

        gameInstance.setState(GameState.ENDING);
        //Task.cancelAll(gameInstance);
        gameInstance.getModule(TaskModule.class).cancelAll();

        String rankingScore = "kills";
        for (Score score : ModuleManager.getModule(ScoreModule.class).getScores().values()) {
            if (score.isScoreRanking()){
                rankingScore = score.getName();
                break;
            }
        }
        Map<GamePlayer, Integer> ranking =  GameUtils.getTopPlayers(gameInstance, rankingScore, 3);

        GameEndEvent ev = new GameEndEvent(gameInstance, winner, ranking);
        Bukkit.getPluginManager().callEvent(ev);


        Bukkit.getScheduler().runTaskLater(Minigame.getInstance().getPlugin(), t -> {
            Task task = gameInstance.getModule(TaskModule.class).addTask(new Task(gameInstance, "EndCountdown", (!gameInstance.getParticipants().isEmpty() ? 15 : 5), new EndCountdown(), Minigame.getInstance().getPlugin()));
            task.setAsMainTask();
        }, 30L);


        HashMap<GamePlayer, Integer> oldWinstreaks = new HashMap<>();

        for (GamePlayer gamePlayer : gameInstance.getParticipants()) {
            Player player = gamePlayer.getOnlinePlayer();
            PlayerGameSession session = gameInstance.getModule(GameSessionModule.class).getPlayerSession(gamePlayer);

            player.stopSound("jsplugins:ending");
            GameUtils.setTeamNameTag(gamePlayer.getOnlinePlayer(), gameInstance.getName().replace(" ", "") + gameInstance.getID(), ChatColor.WHITE);
            GameUtils.hideAndShowPlayers(gameInstance, player);


            gamePlayer.resetAttributes();
            session.setLimited(false);
            session.setEnabledMovement(true);
            gamePlayer.setSpectator(false);
            player.setGameMode(GameMode.ADVENTURE);


            LevelModule levelManager = ModuleManager.getModule(LevelModule.class);
            if (levelManager != null) {
                PlayerLevelData levelProgress = levelManager.getPlayerData(gamePlayer);
                float xpProgress = (float) levelProgress.getXpOnCurrentLevel() / levelProgress.getLevelRange().neededXP();
                player.setExp(Math.min(xpProgress, 1.0f));
                player.setLevel(levelProgress.getLevel());
            }


            if (gameInstance.getSettings().isTeleportPlayersAfterEnd()){
                player.teleport(gameInstance.getModule(LobbyModule.class).getLobbyLocation().getLocation());
            }else{
                player.setAllowFlight(true);
                player.setFlying(true);
            }


            player.sendMessage("");

            Bukkit.getScheduler().runTaskLater(Minigame.getInstance().getPlugin(), task -> { //kvůli Questům
                if (winner != null) {
                    if ((winner.getWinnerType().equals(Winner.WinnerType.PLAYER) && (winner.equals(gamePlayer)) || (winner.getWinnerType().equals(Winner.WinnerType.TEAM) && gamePlayer.getGameSession().getTeam().equals(winner)))) {
                        ModuleManager.getModule(MessageModule.class).get(gamePlayer, "chat.you_won")
                                .send();
                    } else {
                        ModuleManager.getModule(MessageModule.class).get(gamePlayer, "chat.winner")
                                .replace("%winner%", gp -> (winner.getWinnerType().equals(Winner.WinnerType.PLAYER)
                                        ? ((GamePlayer) winner).getOnlinePlayer().getName()
                                        : ((GameTeam) winner).getChatColor() + ((GameTeam) winner).getName()))
                                .replaceWithComponent("%type%", gp -> winner.getWinnerType().getTranslatedName(gp))
                                .send();
                    }
                } else {
                    ModuleManager.getModule(MessageModule.class).get(gamePlayer, "chat.nobody_won")
                            .send();
                }

                player.sendMessage("");
            }, 3L);



            if (winner != null) {
                if ((winner.getWinnerType().equals(Winner.WinnerType.PLAYER) && (winner.equals(gamePlayer)) || (winner.getWinnerType().equals(Winner.WinnerType.TEAM) && gamePlayer.getGameSession().getTeam().equals(winner)))) {
                    ModuleManager.getModule(MessageModule.class).get(gamePlayer, "title.victory")
                            .send();
                    gamePlayer.getOnlinePlayer().playSound(gamePlayer.getOnlinePlayer(), "jsplugins:victory1", 0.35F, 1F);
                } else {
                    if (winner.getWinnerType().equals(Winner.WinnerType.PLAYER)){
                        ModuleManager.getModule(MessageModule.class).get(gamePlayer, "title.winner")
                                .replace("%winner%",winner.getWinnerType().getTranslatedName(gamePlayer)
                                        + " " + ((GamePlayer) winner).getName())
                                .send();
                    }else if (winner.getWinnerType().equals(Winner.WinnerType.TEAM)){
                        Component component = Component.text(((GameTeam) winner).getChatColor() + "")
                                .append(winner.getWinnerType().getTranslatedName(gamePlayer))
                                .appendSpace()
                                .append(Component.text(((GameTeam)winner).getName()));


                        ModuleManager.getModule(MessageModule.class).get(gamePlayer, "title.winner")
                                .replace("%winner%", component)
                                .send();
                    }
                }
            }


            Location location;
            if (gameInstance.getSettings().isTeleportPlayersAfterEnd()){
                location = gameInstance.getModule(LobbyModule.class).getLobbyLocation().getLocation();
            }else{
                if (gameInstance.getCurrentMap().getMainArea() != null) {
                    location = gameInstance.getCurrentMap().getMainArea().getCenter();
                }else{
                    location = new Location(gameInstance.getCurrentMap().getWorld(), 0, 90, 0);
                }
            }


            new BukkitRunnable(){
                Location newLocation;

                @Override
                public void run() {
                    if (gameService.getGameByID(gameInstance.getID()).isEmpty() || gameInstance.getState() != GameState.ENDING){
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
                    if (gameInstance.getSettings().isUseTeams() && winner != null){
                        color = ((GameTeam) winner).getColor();
                    }
                    Utils.spawnFireworks(newLocation, 1, color, FireworkEffect.Type.BALL);
                }
            }.runTaskTimer(Minigame.getInstance().getPlugin(), 0L, 30L);



            if (gameInstance.getSpectatorManager().getInventoryManager().getPlayers().contains(gamePlayer.getOnlinePlayer())) {
                gameInstance.getSpectatorManager().getInventoryManager().unloadInventory(gamePlayer);
            }

            InventoryBuilder itemManager = new InventoryBuilder("Ending");
            if (gameService.getGames().size() > 1 || (DataManager.getInstance() != null && DataManager.getInstance().getMinigame(Minigame.getInstance().getName()).get().isThereFreeGame())) {
                Item playAgain = new Item(new ItemBuilder(Material.PAPER).hideAllFlags().toItemStack(),
                        1, "item.play_again",
                        new Consumer<PlayerInteractEvent>() {
                            @Override
                            public void accept(PlayerInteractEvent e) {
                                gameService.newArena(e.getPlayer(), false);
                            }
                        });
                itemManager.setHoldItemSlot(1);
                itemManager.registerItem(playAgain);
            }

            Item playerMenu = new Item(new ItemBuilder(Material.ECHO_SHARD).setCustomModelData(1025).hideAllFlags().toItemStack(), 7, "item.player_menu", e -> ProfileInventory.openGUI(PlayerManager.getGamePlayer(e.getPlayer())));
            playerMenu.setBlinking(gamePlayer2 -> !unclaimedRewardsModule.getPlayerUnclaimedRewardsByType(gamePlayer2, UnclaimedRewardType.DAILYMETER).isEmpty()
                    || !unclaimedRewardsModule.getPlayerUnclaimedRewardsByType(gamePlayer, UnclaimedRewardType.LEVELUP).isEmpty()
                    || !unclaimedRewardsModule.getPlayerUnclaimedRewardsByType(gamePlayer, UnclaimedRewardType.QUEST).isEmpty());
            playerMenu.setBlinkingItemCustomModelData(1026);

            Item quests = new Item(new ItemBuilder(Material.BOOK).hideAllFlags().toItemStack(), 5, "Item.quests", e -> QuestInventory.openGUI(PlayerManager.getGamePlayer(e.getPlayer())));
            quests.setBlinking(gamePlayer2 -> !unclaimedRewardsModule.getPlayerUnclaimedRewardsByType(gamePlayer2, UnclaimedRewardType.QUEST).isEmpty());
            quests.setBlinkingItemCustomModelData(1010);

            itemManager.registerItem(playerMenu, quests);

            itemManager.give(gamePlayer);

            oldWinstreaks.put(gamePlayer, statsModule.getPlayerStat(gamePlayer, "Winstreak"));
            gamePlayer.getOnlinePlayer().sendMessage("");
        }

        if (winner != null) {
            if (winner.getWinnerType().equals(Winner.WinnerType.TEAM)) {
                GameTeam wTeam = (GameTeam) winner;
                for (GameTeam lTeam : gameInstance.getModule(TeamModule.class).getTeams().values().stream().filter(team -> team != wTeam).toList()) {
                    lTeam.getMembers().forEach(loser -> statsModule.setPlayerStat(loser, "Winstreak", 0));
                }
                for (GamePlayer gamePlayer : wTeam.getMembers()) {
                    statsModule.increasePlayerStat(gamePlayer, "Winstreak", 1);
                }
            } else {
                statsModule.increasePlayerStat((GamePlayer) winner, "Winstreak", 1);
                for (GamePlayer loser : gameInstance.getParticipants().stream().filter(gp -> gp != winner).toList()) {
                    statsModule.setPlayerStat(loser, "Winstreak", 0);
                }
            }
        }

        for (GamePlayer gamePlayer : gameInstance.getParticipants()){
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
                    PlayerGameSession session = gamePlayer.getGameSession();
                    Player player = gamePlayer.getOnlinePlayer();

                    if (winner != null) {
                        ModuleManager.getModule(MessageModule.class).get(gamePlayer, "chat.winstreak")
                                .replace("%old_winstreak%", "" + oldWinstreaks.get(gamePlayer))
                                .replace("%new_winstreak%", (statsModule.getPlayerStat(gamePlayer, "Winstreak") == 0 ? "§c" : "§a") + statsModule.getPlayerStat(gamePlayer, "Winstreak"))
                                .send();
                    }


                    Map<Score, Integer> stats = session.getScores().entrySet()
                            .stream().filter(entry -> entry.getKey().getLinkedStat() != null)
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                    if (!stats.isEmpty()) {
                        Component message = ModuleManager.getModule(MessageModule.class)
                                .get(gamePlayer, "chat.view_statistic")
                                .getTranslated();

                        List<Component> lines = stats.entrySet().stream()
                                .map(entry -> Component.text()
                                        .append(Component.text(entry.getKey().getDisplayName(gamePlayer) + ": ").color(NamedTextColor.GRAY))
                                        .append(Component.text(entry.getValue()).color(NamedTextColor.GREEN))
                                        .asComponent()
                                )
                                .toList();
                        Component hoverText = Component.join(JoinConfiguration.newlines(), lines);

                        player.sendMessage(message.hoverEvent(hoverText));
                        player.sendMessage(Component.empty());
                    }
                }
            }
        }.runTaskLater(Minigame.getInstance().getPlugin(), 50L);
    }

    public void sendTopPlayers(String rankingScore){
        Map<GamePlayer, Integer> fullRanking =  GameUtils.getTopPlayers(gameInstance, rankingScore, gameInstance.getSettings().getMaxPlayers());
        if (fullRanking.isEmpty()){
            return;
        }

        for (GamePlayer gamePlayer : gameInstance.getParticipants()) {
            Player player = gamePlayer.getOnlinePlayer();
            PlayerGameSession gamePlayerSession = gameInstance.getModule(GameSessionModule.class).getPlayerSession(gamePlayer);

            ModuleManager.getModule(MessageModule.class).get(gamePlayer, "chat.top3players.title").send();

            int i = 1;
            for (GamePlayer pos : fullRanking.keySet()) {
                PlayerGameSession posSession = gameInstance.getModule(GameSessionModule.class).getPlayerSession(pos);

                int position = fullRanking.get(pos);
                ModuleManager.getModule(MessageModule.class).get(gamePlayer, "chat.top3players.position")
                        .replace("%position%", "" + position)
                        .replace("%player_head%", ChatHeadAPI.getInstance().getHeadAsString(pos.getOfflinePlayer()))
                        .replace("%player%", (pos.equals(gamePlayer) ? "§a" : "") + pos.getOnlinePlayer().getName() + "§r")
                        .replace("%score%", "" + posSession.getScore(rankingScore))
                        .replace("%ranking_score_name%", rankingScore)
                        .send();
                i++;
                if (i > 3)
                    break;
            }

            player.sendMessage("");


            ModuleManager.getModule(MessageModule.class).get(gamePlayer, "chat.top3players.your_position")
                    .replace("%position%", "" + (fullRanking.get(gamePlayer) == null ? "§c-" : fullRanking.get(gamePlayer)))
                    .replace("%score%", "" + gamePlayerSession.getScore(rankingScore))
                    .replace("%ranking_score_name%", rankingScore)
                    .send();


            PartyInterface party = gamePlayer.getParty();
            FriendsInterface friends = gamePlayer.getFriends();
            if (party.isInParty() && !party.getAllOnlinePlayers().isEmpty()) {
                Component message = ModuleManager.getModule(MessageModule.class).get(gamePlayer, "chat.top3players.your_party_members_position").getTranslated();

                Component hoverText = Component.empty();
                for (GamePlayer partyMember : fullRanking.keySet().stream().filter(p -> party.getAllOnlinePlayers().contains(p)).toList()) {
                    PlayerGameSession partyMemberSession = gameInstance.getModule(GameSessionModule.class).getPlayerSession(partyMember);

                    hoverText = hoverText.append(ModuleManager.getModule(MessageModule.class).get(gamePlayer, "chat.top3players.position")
                                    .replace("%position%", "" + fullRanking.get(partyMember))
                                    .replace("%player_head%", ChatHeadAPI.getInstance().getHeadAsString(partyMember.getOfflinePlayer()))
                                    .replace("%player%", partyMember.getOnlinePlayer().getName())
                                    .replace("%score%", "" + partyMemberSession.getScore(rankingScore))
                                    .replace("%ranking_score_name%", rankingScore)
                                    .getTranslated())
                            .appendNewline();
                }

                player.sendMessage(message.hoverEvent(hoverText));
            }


            if (friends.hasFriends() && !friends.getAllOnlinePlayers().isEmpty()) {
                List<PlayerIdentity> friendsList = new ArrayList<>(friends.getAllOnlinePlayers()).stream().filter(p -> (!party.isInParty() || party.getAllOnlinePlayers().contains(p))).toList();

                if (!friendsList.isEmpty()) {
                    Component message = ModuleManager.getModule(MessageModule.class).get(gamePlayer, "chat.top3players.friends_position").getTranslated();

                    Component hoverText = Component.empty();
                    for (GamePlayer friend : fullRanking.keySet().stream().filter(friendsList::contains).toList()) {
                        PlayerGameSession friendSession = gameInstance.getModule(GameSessionModule.class).getPlayerSession(friend);

                        hoverText = hoverText.append(ModuleManager.getModule(MessageModule.class).get(gamePlayer, "chat.top3players.position")
                                        .replace("%position%", "" + fullRanking.get(friend))
                                        .replace("%player_head%", ChatHeadAPI.getInstance().getHeadAsString(friend.getOfflinePlayer()))
                                        .replace("%player%", friend.getOnlinePlayer().getName())
                                        .replace("%score%", "" + friendSession.getScore(rankingScore))
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
        for (GamePlayer gamePlayer : gameInstance.getParticipants()){
            PlayerGameSession session = gamePlayer.getGameSession();
            Player player = gamePlayer.getOnlinePlayer();

            ModuleManager.getModule(MessageModule.class).get(gamePlayer, "chat.rewardsummary.title").send();

            if (!session.earnedSomething()){
                ModuleManager.getModule(MessageModule.class).get(gamePlayer, "chat.rewardsummary.no_rewards")
                        .send();
                player.sendMessage("");
            }else{
                gamePlayer.getOnlinePlayer().playSound(player, "jsplugins:gamebonus", 0.8F, 1F);

                for (Map.Entry<Resource, Integer> totalEarnedEntry : session.getTotalEarned().entrySet()){
                    Resource resource = totalEarnedEntry.getKey();

                    Component component = Component.text("  §7• " + (resource.getImgChar() != null ? resource.getImgChar() : "") + " " + resource.getColor() + totalEarnedEntry.getValue() + " §7" + resource.getDisplayName());


                    Collection<Component> lines = session.getEarnedBySource(resource).entrySet().stream()
                            .map(entry -> Component.text()
                                    .append(Component.text("§f" + (entry.getKey().hasPluralName() ? + session.getScore(entry.getKey().getName()) + "x " : "") + entry.getKey().getDisplayName(gamePlayer)))
                                    .append(Component.text(" §7→ "))
                                    .append(Component.text(resource.getColor() + "+" + entry.getValue()))
                                    .asComponent()
                            )
                            .toList();
                    Component hoverComponent = Component.join(JoinConfiguration.newlines(), lines);

                    player.sendMessage(component.hoverEvent(hoverComponent));


                    if (resource.isApplicableBonus()) {
                        List<Integer> percentages = Arrays.asList(100, 75, 50, 45, 40, 35, 30, 25, 20, 15, 10, 5);

                        for (Integer percent : percentages) {
                            if (gamePlayer.getOnlinePlayer().hasPermission("vip.bonus" + percent)){
                                int coins = (int) (((double) totalEarnedEntry.getValue() * (double) percent) / (double) 100);
                                if (coins != 0) {
                                    resource.getResourceInterface().deposit(gamePlayer, coins);
                                    ModuleManager.getModule(MessageModule.class).get(gamePlayer, "chat.rewardsummary.bonus")
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

                /*List<Resource> resourcesWithFirstDailyWinReward = Minigame.getInstance().getEconomies().stream().filter(resource -> resource.getFirstDailyWinReward() != 0).toList();
                if (resourcesWithFirstDailyWinReward.size() > 1 && gamePlayer.getPlayerData().grantDailyFirstWinReward()){
                    player.sendMessage("");
                    ModuleManager.getModule(MessageModule.class).get(gamePlayer, "chat.rewardsummary.first_win_reward")
                            .replace("%minigame%", Minigame.getInstance().getName());
                    for (Resource resource : resourcesWithFirstDailyWinReward){
                        player.sendMessage("   §7↳ " + resource.getColor() + resource.getFirstDailyWinReward() + " " + resource.getDisplayName());
                    }
                }*/
            }
            player.sendMessage("");
        }
    }
}
