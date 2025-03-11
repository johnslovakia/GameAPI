package cz.johnslovakia.gameapi.game.map;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.game.Game;
import cz.johnslovakia.gameapi.game.GameState;
import cz.johnslovakia.gameapi.game.team.GameTeam;
import cz.johnslovakia.gameapi.game.team.TeamManager;
import cz.johnslovakia.gameapi.users.PlayerManager;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.utils.Logger;
import cz.johnslovakia.gameapi.utils.Sounds;
import cz.johnslovakia.gameapi.worldManagement.WorldManager;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

@Getter
public class GameMap {

    @Setter
    private Game game;
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

    public GameMap(Game game, String name, String authors) {
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

        if (!game.getMapManager().isEnabledVoting() || !(game.getState().equals(GameState.WAITING) || game.getState().equals(GameState.STARTING))){
            MessageManager.get(player, "chat.map.vote_ended")
                    .send();
            player.playSound(player.getLocation(), Sounds.ANVIL_BREAK.bukkitSound(), 20.0F, 20.0F);
            return;
        }


        if (gamePlayer.getPlayerData().getVotesForMaps().size() >= votes) {
            MessageManager.get(player, "chat.map.no_more_votes")
                    .send();
            player.playSound(player.getLocation(), Sounds.ANVIL_BREAK.bukkitSound(), 20.0F, 20.0F);
            return;
        }

        gamePlayer.getPlayerData().addVoteForMap(this);
        player.playSound(player.getLocation(), Sounds.CLICK.bukkitSound(), 20.0F, 20.0F);
        addVote();

        MessageManager.get(player, "chat.map.vote")
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
            for (GameTeam gameTeam : game.getTeamManager().getTeams()){
                int spawn = 0;
                for (GamePlayer gamePlayer : gameTeam.getMembers()){
                    if (!gamePlayer.isOnline()) {
                        return;
                    }
                    Player player = gamePlayer.getOnlinePlayer();

                    try {
                        List<MapLocation> locations = getSpawns(gamePlayer.getPlayerData().getTeam().getName());
                        if (locations.isEmpty()){
                            continue;
                        }
                        Location location = (locations.get(spawn) != null ? locations.get(spawn).getLocation() : locations.get(spawn - 1).getLocation());
                        if (location == null) {
                            continue;
                        }
                        player.teleport(location);
                        getPlayerToLocation().put(gamePlayer, location);
                    } catch (Exception ex) {
                        Logger.log("Something went wrong when teleporting " + player.getName() + " to map spawn! Team:" + gamePlayer.getPlayerData().getTeam().getName() + " GameID:" + game.getID(), Logger.LogType.ERROR);
                        //Logger.log("The following message is for Developers: " + ex.getCause().getMessage(), Logger.LogType.ERROR);
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
                    String s = ((spawns.size() - 1) >= spawn ? spawns.get(spawn).getId() : spawns.get((spawn - 1)).getId());
                    Location location = getSpawn(s);
                    if (location == null) {
                        continue;
                    }
                    player.teleport(location);
                    getPlayerToLocation().put(gamePlayer, location);
                } catch (Exception ex) {
                    Logger.log("Something went wrong when teleporting " + player.getName() + " to map spawn! GameID:" + game.getID(), Logger.LogType.ERROR);
                    //Logger.log("The following message is for Developers: " + ex.getCause().getMessage(), Logger.LogType.ERROR);
                    ex.fillInStackTrace();
                }
                spawn++;
            }
        }
    }

    public void setWinned(boolean winned){
        this.winned = winned;
        if (game.getServerDataManager() != null) {
            game.getServerDataManager().getJSONProperty("Map").update(game, getName());
        }
        if (winned){
            setGame(game);

            if (settings.isLoadWorldWithGameAPI()) {
                WorldManager.loadArenaWorld(this, game);
            }
        }else{
            if (getWorld() != null) {
                try {
                    WorldManager.unload(this, game);
                }catch (Exception e){
                    // This exception cannot be ignored
                    throw new RuntimeException(e);
                }
            }
        }
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
