package cz.johnslovakia.gameapi.modules.game;

import cz.johnslovakia.gameapi.Shared;
import cz.johnslovakia.gameapi.modules.game.gameData.implementations.*;
import cz.johnslovakia.gameapi.modules.game.handlers.GameEndHandler;
import cz.johnslovakia.gameapi.modules.game.handlers.GameStartHandler;
import cz.johnslovakia.gameapi.modules.game.handlers.PlayerJoinQuitHandler;
import cz.johnslovakia.gameapi.modules.game.map.GameMap;
import cz.johnslovakia.gameapi.modules.game.map.MapModule;
import cz.johnslovakia.gameapi.modules.game.map.MapVotesComparator;
import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.MinigameSettings;
import cz.johnslovakia.gameapi.events.GameStateChangeEvent;
import cz.johnslovakia.gameapi.modules.game.session.GameSessionModule;
import cz.johnslovakia.gameapi.modules.game.task.TaskModule;
import cz.johnslovakia.gameapi.modules.messages.Message;
import cz.johnslovakia.gameapi.modules.messages.MessageModule;
import cz.johnslovakia.gameapi.modules.serverManagement.DataManager;
import cz.johnslovakia.gameapi.modules.serverManagement.gameData.GameDataManager;
import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.game.task.Task;
import cz.johnslovakia.gameapi.utils.*;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.GamePlayerState;

import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.function.Consumer;

@Getter
public class GameInstance {
    private final GameInstance game = GameInstance.this;

    @Setter
    private String name;
    private String ID;
    private GameState state = GameState.LOADING;
    @Setter
    private Task runningMainTask;
    @Setter
    private SpectatorManager spectatorManager;
    @Setter
    private boolean firstGameKill = true;

    private GameDataManager<GameInstance> serverDataManager;

    private final List<GamePlayer> participants = new ArrayList<>();
    private List<Block> placedBlocks;
    private final Map<String, Object> metadata = new HashMap<>();
    private final List<Placement<?>> placements = new ArrayList<>();
    @Setter
    private boolean automaticStart = false;

    private final GameEndHandler gameEndHandler;
    private final GameStartHandler gameStartHandler;
    private final PlayerJoinQuitHandler playerJoinQuitHandler;

    private final Map<Class<? extends GameModule>, GameModule> modules = new HashMap<>();

    public GameInstance(String name) {
        this.name = name;

        this.spectatorManager = new SpectatorManager();

        GameService gameService = ModuleManager.getModule(GameService.class);
        while (this.ID == null || gameService.isDuplicate(this.ID)){
            this.ID = StringUtils.randomString(6, true, true, false);
        }
        gameService.addID(this.ID);

        registerModule(new GameSessionModule());
        registerModule(new TaskModule());

        this.gameEndHandler = new GameEndHandler(this);
        this.gameStartHandler = new GameStartHandler(this);
        this.playerJoinQuitHandler = new PlayerJoinQuitHandler(this);
    }

    public void joinPlayer(Player player){
        getPlayerJoinQuitHandler().joinPlayer(player);
    }

    public void quitPlayer(Player player){
        getPlayerJoinQuitHandler().quitPlayer(player);
    }

    public void endGame(Winner winner){
        getGameEndHandler().endGame(winner);
    }

    public <T extends GameModule> T registerModule(T module) {
        Class<? extends GameModule> clazz = module.getClass();

        if (modules.containsKey(clazz)) {
            throw new IllegalArgumentException("Module already registered: " + clazz.getSimpleName());
        }

        module.setGame(this);

        modules.put(clazz, module);
        module.initialize();

        if (module instanceof Listener listener)
            Bukkit.getPluginManager().registerEvents(listener, Shared.getInstance().getPlugin());
        return module;
    }

    public void registerModule(GameModule... modules) {
        for (GameModule module : modules) {
            registerModule(module);
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends GameModule> T getModule(Class<T> clazz) {
        return (T) modules.get(clazz);
    }

    public <T extends GameModule> boolean hasModule(Class<T> clazz) {
        return modules.containsKey(clazz);
    }

    public <T extends GameModule> void destroyModule(Class<T> moduleClass) {
        GameModule module = modules.remove(moduleClass);
        if (module != null) {
            if (module instanceof Listener listener)
                HandlerList.unregisterAll(listener);
            module.terminate();
        }
    }

    public void destroyAllModules() {
        for (GameModule module : modules.values()) {
            if (module instanceof Listener listener)
                HandlerList.unregisterAll(listener);
            module.terminate();
        }
        modules.clear();
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
                        setState(GameState.WAITING);
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
            serverDataManager = new GameDataManager<>(Minigame.getInstance().getName(), this, getName(), getSettings().getMaxPlayers());

            serverDataManager.addProperty("GameState", new GameStateValueImple());
            serverDataManager.addProperty("MaxPlayers", new MaxPlayersValueImple());
            serverDataManager.addProperty("Players", new PlayersValueImple());
            serverDataManager.addProperty("Map", new MapValueImple());
            serverDataManager.addProperty("StartingTime", new StartingTimeValueImple());

            if (minigame.getProperties() != null && !minigame.getProperties().isEmpty()) {
                serverDataManager.addProperties(minigame.getProperties());
                return;
            }

            serverDataManager.updateGame();
        }
    }

    int animationTick = 0;
    public void updateWaitingForPlayersBossBar(){
        if (!getState().equals(GameState.WAITING))
            return;

        for (GamePlayer gamePlayer : getParticipants()){
            ChatColor chatColor = ChatColor.WHITE;

            Message message = ModuleManager.getModule(MessageModule.class).get(gamePlayer, "bossbar.waiting_for_players")
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

    public void checkArenaFullness(){
        if (!getState().equals(GameState.STARTING)) return;
        if (!automaticStart && !getParticipants().isEmpty()) return;

        MinigameSettings settings = getSettings();
        boolean notEnough = (getParticipants().isEmpty() || getParticipants().size() < Math.max(settings.getMinPlayers() , 5));
        if (notEnough) {
            setState(GameState.WAITING);

            TaskModule taskModule = getModule(TaskModule.class);
            Optional<Task> countdown = taskModule.getTask("StartCountdown");
            countdown.ifPresent(task -> {
                task.setCounter(settings.getStartingTime());
                taskModule.cancel("StartCountdown", true);
            });


            if (!settings.isChooseRandomMap()) {
                for (GameMap a : getModule(MapModule.class).getMaps()) {
                    if (!a.isIngame()) continue;
                    a.setWinned(false);
                }
                getModule(MapModule.class).setVoting(true);
            }
            ModuleManager.getModule(MessageModule.class).get(getParticipants(), "chat.not_enough_players")
                    .send();
            updateWaitingForPlayersBossBar();
        }
    }

    public final void winMap(){
        MapModule mapModule = getModule(MapModule.class);
        mapModule.setVoting(false);

        List<GameMap> arenas1 = new ArrayList<>(mapModule.getMaps());
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
                    player.getOnlinePlayer().sendMessage(ModuleManager.getModule(MessageModule.class).get(player, "chat.map_won").replace("%map%", a.getName()).replace("%number%", "" + i).getTranslated());
                }
            } else {
                for (GamePlayer player : getPlayers()){
                    player.getOnlinePlayer().playSound(player.getOnlinePlayer(), Sound.UI_BUTTON_CLICK, 1.0F, 1.0F);
                    player.getOnlinePlayer().sendMessage(ModuleManager.getModule(MessageModule.class).get(player, "chat.map_won").replace("%map%", a.getName()).getTranslated());
                }
            }
            if (i == playingArenas) break;
        }
    }

    public void selectRandomMap() {
        MapModule mapModule = getModule(MapModule.class);
        mapModule.setVoting(false);

        List<GameMap> maps = new ArrayList<>(mapModule.getMaps().stream().filter(GameMap::isIngame).toList());
        Collections.shuffle(maps);

        GameMap map = maps.get(0);
        map.setWinned(true);
    }

    public GameMap nextArena() {
        List<GameMap> arenas1 = new ArrayList<>(getModule(MapModule.class).getMaps());
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
        if (getRunningMainTask() == null) return false;;
        return getRunningMainTask().getId().equalsIgnoreCase("PreparationTask");
    }

    public GameMap getCurrentMap() {
        if (getModule(MapModule.class) == null) return null;
        for (GameMap map : getModule(MapModule.class).getMaps()) {
            if (map.isWinned() && !map.isPlayed()) return map;
        }
        return null;
    }

    @Deprecated
    public GameMap getPlayingMap() {
        for (GameMap map : getModule(MapModule.class).getMaps()) {
            if (map.isPlaying()) return map;
        }
        return null;
    }

    public void setState(GameState state) {
        this.state = state;
        if (serverDataManager != null) {
            serverDataManager.updateGame();
        }

        GameStateChangeEvent ev = new GameStateChangeEvent(getGame(), state);
        Bukkit.getPluginManager().callEvent(ev);
    }

    public List<GamePlayer> getPlayers(){
        return participants.stream()
                .filter(gp -> gp.getGameSession().getState().equals(GamePlayerState.PLAYER))
                .toList();
    }

    public List<GamePlayer> getSpectators(){
        return participants.stream()
                .filter(gamePlayer -> gamePlayer.getGameSession().getState().equals(GamePlayerState.SPECTATOR))
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

        GameInstance other = (GameInstance) obj;
        return this.getID() != null && this.getID().equals(other.getID());
    }

    @Override
    public int hashCode() {
        return getID() != null ? getID().hashCode() : 0;
    }
}
