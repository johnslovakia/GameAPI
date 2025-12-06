package cz.johnslovakia.gameapi.modules.game.map;

import cz.johnslovakia.gameapi.modules.game.GameInstance;
import cz.johnslovakia.gameapi.modules.game.GameState;
import cz.johnslovakia.gameapi.modules.game.team.GameTeam;
import cz.johnslovakia.gameapi.modules.game.team.TeamModule;
import cz.johnslovakia.gameapi.modules.messages.MessageModule;
import cz.johnslovakia.gameapi.modules.serverManagement.gameData.GameDataManager;
import cz.johnslovakia.gameapi.users.PlayerManager;
import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.utils.Logger;

import cz.johnslovakia.gameapi.worldManagement.WorldManager;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

@Getter
public class GameMap {

    @Setter
    private GameInstance game;
    private final String name, authors;
    @Setter
    public MapLocation spectatorSpawn;
    public Area mainArea;
    @Setter
    private ItemStack icon;
    @Setter
    private AreaSettings settings;


    @Setter
    private World world;
    @Setter
    private int votes = 0;

    @Getter
    private boolean winned = false;
    @Setter
    private boolean played = false;
    @Setter
    private boolean ingame = true;

    private final List<MapLocation> spawns = new ArrayList<>();
    private final Map<GamePlayer, Location> playerToLocation = new HashMap<>();
    private final List<Area> areas = new ArrayList<>();
    private final HashMap<String, Object> metadata = new HashMap<>();

    public GameMap(GameInstance game, String name, String authors) {
        this.game = game;
        this.name = name.replaceAll("_", " ");
        this.authors = authors;
        this.settings = new AreaSettings();
    }

    public GameMap setMainArea(Area mainArea) {
        this.mainArea = mainArea;
        mainArea.setBorder(true);
        return this;
    }

    public void registerArea(Area... areas){
        this.areas.addAll(Arrays.asList(areas));
    }

    public Area getArea(String name) {
        for (Area area : this.areas) {
            if (area.getName().equals(name)) return area;
        }
        return null;
    }

    public void addSpawn(MapLocation... spawns){
        this.spawns.addAll(Arrays.asList(spawns));
    }

    public void voteForMap(Player player) {
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);

        int votes = 1;
        if (player.hasPermission("vip.2votes")){
            votes = 2;
        }else if (player.hasPermission("vip.3votes")){
            votes = 3;
        }

        if (!game.getModule(MapModule.class).isEnabledVoting() || !(game.getState().equals(GameState.WAITING) || game.getState().equals(GameState.STARTING))){
            ModuleManager.getModule(MessageModule.class).get(player, "chat.map.vote_ended")
                    .send();
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_BREAK, 20.0F, 20.0F);
            return;
        }


        if (game.getModule(MapModule.class).getTotalPlayerVotes(gamePlayer) >= votes) {
            ModuleManager.getModule(MessageModule.class).get(player, "chat.map.no_more_votes")
                    .send();
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_BREAK, 20.0F, 20.0F);
            return;
        }

        game.getModule(MapModule.class).addPlayerVote(gamePlayer, this);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 20.0F, 20.0F);
        addVote();

        ModuleManager.getModule(MessageModule.class).get(player, "chat.map.vote")
                .replace("%map%", getName())
                .replace("%votes%", "" + getVotes())
                .send();
    }

    public Location getPlayerToLocation(GamePlayer gamePlayer){
        if (playerToLocation.containsKey(gamePlayer)){
            Location oldLoc = playerToLocation.get(gamePlayer);
            return new Location(world, oldLoc.getX(), oldLoc.getY(), oldLoc.getZ(), oldLoc.getYaw(), oldLoc.getPitch());
        }
        return null;
    }

    public Location getSpawn(String id){
        Location finalLocation = null;
        for (MapLocation spawn : getSpawns()) {
            if (spawn.getId().equals(id)) {
                finalLocation = spawn.getLocation();
            }
        }
        if (finalLocation == null) Logger.log("Something went wrong when trying to get the spawn " + id + ". The following message is for Developers: Location 'finalLocation' is null!", Logger.LogType.ERROR);
        return finalLocation;
    }

    public List<MapLocation> getSpawns(String id){
        return getSpawns().stream().filter(spawn -> spawn.getId().equals(id)).toList();
    }

    public void teleport() {
        if (game.getSettings().isUseTeams()) {
            for (GameTeam gameTeam : game.getModule(TeamModule.class).getTeams().values()) {
                int spawn = 0;
                for (GamePlayer gamePlayer : gameTeam.getMembers()) {
                    if (!gamePlayer.isOnline()) {
                        continue;
                    }

                    Player player = gamePlayer.getOnlinePlayer();

                    try {
                        List<MapLocation> locations = getSpawns(gamePlayer.getGameSession().getTeam().getName());
                        if (locations.isEmpty()) {
                            continue;
                        }

                        int index = Math.min(spawn, locations.size() - 1);
                        Location location = locations.get(index).getLocation();
                        if (location == null) {
                            continue;
                        }

                        player.teleport(location);
                        getPlayerToLocation().put(gamePlayer, location);
                    } catch (Exception ex) {
                        Logger.log("Something went wrong when teleporting " + player.getName() + " to map spawn! Team:" + gamePlayer.getGameSession().getTeam().getName() + " GameID:" + game.getID(), Logger.LogType.ERROR);
                        ex.fillInStackTrace();
                    }

                    spawn++;
                }
            }
        }else {
            int spawn = 0;
            for (GamePlayer gamePlayer : game.getPlayers()) {
                if (!gamePlayer.isOnline()) {
                    return;
                }
                Player player = gamePlayer.getOnlinePlayer();

                try {
                    String s = spawns.get(Math.min(spawn, spawns.size() - 1)).getId();
                    Location location = getSpawn(s);
                    if (location == null) {
                        continue;
                    }
                    player.teleport(location);
                    getPlayerToLocation().put(gamePlayer, location);
                } catch (Exception ex) {
                    Logger.log("Something went wrong when teleporting " + player.getName() + " to map spawn! GameID:" + game.getID(), Logger.LogType.ERROR);
                    ex.fillInStackTrace();
                }
                spawn++;
            }
        }
    }

    public void setWinned(boolean winned){
        this.winned = winned;
        GameDataManager<GameInstance> gameDataManager = game.getServerDataManager();
        if (gameDataManager != null) {
            gameDataManager.updateGame();
        }
        if (winned){
            setGame(game);

            if (settings.isLoadWorldWithGameAPI()) {
                WorldManager.loadArenaWorld(this, game);
            }
        }/*else{
            if (getWorld() != null && settings.isLoadWorldWithGameAPI()) {
                try {
                    WorldManager.unload(this);
                }catch (Exception e){

                    throw new RuntimeException(e);
                }
            }
        }*/
    }

    public boolean isPlaying(){
        return game.getState().equals(GameState.INGAME) && isWinned() && !isPlayed();
    }

    public void addVote(){
        votes++;
    }

    public void removeVote(){
        votes--;
    }
}
