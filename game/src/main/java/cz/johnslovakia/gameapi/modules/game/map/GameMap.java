package cz.johnslovakia.gameapi.modules.game.map;

import cz.johnslovakia.gameapi.modules.game.GameInstance;
import cz.johnslovakia.gameapi.modules.game.GameState;
import cz.johnslovakia.gameapi.modules.game.team.GameTeam;
import cz.johnslovakia.gameapi.modules.game.team.TeamModule;
import cz.johnslovakia.gameapi.modules.messages.Message;
import cz.johnslovakia.gameapi.modules.messages.MessageModule;
import cz.johnslovakia.gameapi.modules.resources.Resource;
import cz.johnslovakia.gameapi.modules.resources.ResourcesModule;
import cz.johnslovakia.gameapi.modules.serverManagement.gameData.GameDataManager;
import cz.johnslovakia.gameapi.users.PlayerManager;
import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.utils.Logger;

import cz.johnslovakia.gameapi.utils.StringUtils;
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
        MessageModule messageModule = ModuleManager.getModule(MessageModule.class);
        MapModule mapModule = game.getModule(MapModule.class);

        if (!mapModule.isEnabledVoting() ||
                !(game.getState().equals(GameState.WAITING) || game.getState().equals(GameState.STARTING))) {
            messageModule.getMessage(player, "chat.map.vote_ended").send();
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_BREAK, 20.0F, 20.0F);
            return;
        }

        int freeVotes = 1;
        if (player.hasPermission("vip.3votes")) {
            freeVotes = 3;
        } else if (player.hasPermission("vip.2votes")) {
            freeVotes = 2;
        }

        int maxVotes = 3;
        int currentVotes = mapModule.getTotalPlayerVotes(gamePlayer);

        if (currentVotes >= maxVotes) {
            messageModule.getMessage(player, "chat.map.no_more_votes").send();
            return;
        }

        ResourcesModule resourcesModule = ModuleManager.getModule(ResourcesModule.class);
        Resource resource = resourcesModule.getResourceByName("Coins");

        int voteCost = getVoteCost(currentVotes, freeVotes);
        boolean paidForVote = voteCost > 0;

        if (paidForVote) {
            int balance = resourcesModule.getPlayerBalanceCached(player, resource);

            if (balance < voteCost) {
                messageModule.getMessage(player, "chat.dont_have_enough")
                        .replace("%need_more%", StringUtils.betterNumberFormat((long) (voteCost - balance)))
                        .replace("%economy_name%", resource.getDisplayName())
                        .send();
                return;
            }

            resourcesModule.withdraw(player, resource, voteCost);
        }

        mapModule.addPlayerVote(gamePlayer, this);

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 20.0F, 20.0F);

        messageModule.getMessage(player, "chat.map.vote")
                .replace("%map%", getName())
                .replace("%votes%", String.valueOf(getVotes()))
                .add(StringUtils.colorizer("§c(- #df1c1c" + StringUtils.betterNumberFormat(voteCost) + " §c" + resource.getDisplayName() + ")"), identity -> paidForVote)
                .send();

        if (paidForVote) {
            messageModule.getMessage(player, "chat.current_balance")
                    .replace("%balance%", StringUtils.betterNumberFormat(resourcesModule.getPlayerBalanceCached(player, resource)))
                    .replace("%economy_name%", resource.getDisplayName())
                    .send();
        }
    }

    private int getVoteCost(int currentVotes, int freeVotes) {
        if (currentVotes < freeVotes) return 0;

        int[] votePrices = {150, 200};
        int priceIndex = currentVotes - freeVotes;
        if (priceIndex >= votePrices.length) return 0;

        return votePrices[priceIndex];
    }

    public int getVotes(){
        return game.getModule(MapModule.class).getTotalVotesForMap(this);
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
                for (GamePlayer gamePlayer : gameTeam.getOnlineMembers()) {
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
        boolean changed = this.winned != winned;
        this.winned = winned;
        GameDataManager<GameInstance> gameDataManager = game.getServerDataManager();
        if (changed && gameDataManager != null) {
            gameDataManager.updateGame();
        }
        if (winned && changed){
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
}
