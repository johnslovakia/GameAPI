package cz.johnslovakia.gameapi.users;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.game.Game;
import cz.johnslovakia.gameapi.game.GameState;
import cz.johnslovakia.gameapi.game.Winner;
import cz.johnslovakia.gameapi.game.kit.Kit;
import cz.johnslovakia.gameapi.game.map.Area;
import cz.johnslovakia.gameapi.game.map.GameMap;
import cz.johnslovakia.gameapi.game.team.GameTeam;
import cz.johnslovakia.gameapi.messages.Language;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.users.friends.FriendsInterface;
import cz.johnslovakia.gameapi.users.parties.PartiesHook;
import cz.johnslovakia.gameapi.users.parties.PartyInterface;

import fr.mrmicky.fastboard.FastBoard;

import lombok.Getter;
import lombok.Setter;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Getter @Setter
public class GamePlayer extends Winner {

    private OfflinePlayer player;
    private GamePlayerType type;

    private Game game;
    private GameTeam team;
    private Kit kit;

    private boolean enabledPVP = true;
    private boolean enabledMovement = true;
    private boolean limited = false;

    private final PlayerData playerData;
    @Deprecated
    private FastBoard scoreboard;

    private FriendsInterface friends;
    private PartyInterface party;


    private HashMap<String, Object> metadata = new HashMap<>();

    public GamePlayer(OfflinePlayer offlinePlayer){
        super(WinnerType.PLAYER);
        this.player = offlinePlayer;
        this.type = GamePlayerType.PLAYER;
        this.playerData = new PlayerData(this);

        hookFriendsAndParty();
    }

    public Language getLanguage() {
        return getPlayerData().getLanguage();
    }

    public void hookFriendsAndParty(){
        if (Bukkit.getServer().getPluginManager().getPlugin("Parties") != null) {
            if (Bukkit.getServer().getPluginManager().getPlugin("Parties").isEnabled()) {
                this.party = new PartiesHook(this);
            }
        }else if (Bukkit.getServer().getPluginManager().getPlugin("PartyAndFriends") != null) {
            if (Bukkit.getServer().getPluginManager().getPlugin("PartyAndFriends").isEnabled()) {
                this.party = new cz.johnslovakia.gameapi.users.parties.PartyAndFriendsHook(this);
                this.friends = new cz.johnslovakia.gameapi.users.friends.PartyAndFriendsHook(this);
            }
        }else if (Bukkit.getServer().getPluginManager().getPlugin("FriendSystem-Spigot-API") != null) {
            if (Bukkit.getServer().getPluginManager().getPlugin("FriendSystem-Spigot-API").isEnabled()) {
                this.party = new cz.johnslovakia.gameapi.users.parties.FriendSystemHook(this);
                this.friends = new cz.johnslovakia.gameapi.users.friends.FriendSystemHook(this);
            }
        }else{
            this.party = new PartyInterface() {
                @Override
                public boolean isInParty() {
                    return false;
                }

                @Override
                public List<GamePlayer> getAllOnlinePlayers() {
                    return List.of();
                }

                @Override
                public GamePlayer getLeader() {
                    return null;
                }
            };
            this.friends = new FriendsInterface() {
                @Override
                public boolean hasFriends() {
                    return false;
                }

                @Override
                public List<GamePlayer> getAllOnlinePlayers() {
                    return List.of();
                }

                @Override
                public boolean isFriendWith(GamePlayer gamePlayer) {
                    return false;
                }
            };
        }
    }


    public boolean isRespawning(){
        if (getGame().getSettings().useTeams() && getGame().getSettings().isEnabledRespawning()){
            GameTeam team = getTeam();

            return !team.isDead();
        }else{
            return getGame().getSettings().isEnabledRespawning();
        }
    }

    public void setSpectator(boolean spectator){
        setSpectator(spectator, false);
    }

    public void setSpectator(boolean spectator, boolean teamSelector){
        Game game = getGame();

        if (spectator){
            setType(GamePlayerType.SPECTATOR);

            getOnlinePlayer().setGameMode(GameMode.ADVENTURE);
            getOnlinePlayer().setAllowFlight(true);
            getOnlinePlayer().setFlying(true);
            getOnlinePlayer().addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0));

            for (GamePlayer alivePlayer : game.getPlayers()) {
                GameAPI.getInstance().getVersionSupport().hidePlayer(Minigame.getInstance().getPlugin(), alivePlayer.getOnlinePlayer(), getOnlinePlayer());
            }
            for (GamePlayer otherSpectator : game.getSpectators()) {
                GameAPI.getInstance().getVersionSupport().showPlayer(Minigame.getInstance().getPlugin(), otherSpectator.getOnlinePlayer(), getOnlinePlayer());
            }

            GameMap currentMap = game.getCurrentMap();
            if (currentMap != null){
                currentMap.getPlayerToLocation().remove(this);
            }

            if (game.getState().equals(GameState.INGAME)) {
                if (teamSelector) {
                    getGame().getSpectatorManager().getWithTeamSelectorInventoryManager().give(getOnlinePlayer());
                } else {
                    getGame().getSpectatorManager().getInventoryManager().give(getOnlinePlayer());
                }
            }

            MessageManager.get(this, "title.spectator")
                    .send();
        }else{
            setType(GamePlayerType.PLAYER);

            for (GamePlayer gamePlayer : game.getPlayers()){
                if (!gamePlayer.equals(this)){
                    GameAPI.getInstance().getVersionSupport().showPlayer(Minigame.getInstance().getPlugin(), gamePlayer.getOnlinePlayer(), getOnlinePlayer());
                }
            }
            for (GamePlayer otherSpectator : game.getSpectators()) {
                GameAPI.getInstance().getVersionSupport().hidePlayer(Minigame.getInstance().getPlugin(), otherSpectator.getOnlinePlayer(), getOnlinePlayer());
            }

            if (getGame().getSpectatorManager().getInventoryManager().getPlayers().contains(getOnlinePlayer())){
                getGame().getSpectatorManager().getInventoryManager().unloadInventory(getOnlinePlayer());
            }
            if (getGame().getSpectatorManager().getWithTeamSelectorInventoryManager().getPlayers().contains(getOnlinePlayer())){
                getGame().getSpectatorManager().getWithTeamSelectorInventoryManager().unloadInventory(getOnlinePlayer());
            }

            getOnlinePlayer().setAllowFlight(false);
            getOnlinePlayer().setFlying(false);
            for (PotionEffect potionEffect : getOnlinePlayer().getActivePotionEffects()){
                getOnlinePlayer().removePotionEffect(potionEffect.getType());
            }
        }
    }



    public PlayerScore getScoreByName(String name) {
        for (PlayerScore ts : getPlayerData().getScores()) {
            if (ts.getName().equalsIgnoreCase(name)) {
                return ts;
            }
        }
        return null;
    }

    public void resetAttributes(){
        getOnlinePlayer().getInventory().clear();
        GameAPI.getInstance().getVersionSupport().setMaxPlayerHealth(getOnlinePlayer(), 20D);
        getOnlinePlayer().setHealth(20);
        getOnlinePlayer().setFoodLevel(20);
        //TODO: vymyslet lépe pro level system
        if (getGame().getState().equals(GameState.INGAME) || getGame().isPreparation()) {
            getOnlinePlayer().setLevel(0);
            getOnlinePlayer().setExp(0);
        }
        getOnlinePlayer().setAllowFlight(false);
        getOnlinePlayer().setFlying(false);
        getOnlinePlayer().setFireTicks(0);
        getOnlinePlayer().setGameMode(GameMode.SURVIVAL);
        getOnlinePlayer().setInvulnerable(false);
        for(PotionEffect effect : getOnlinePlayer().getActivePotionEffects()){
            if (effect.getType().equals(PotionEffectType.BLINDNESS)){
                continue;
            }
            getOnlinePlayer().removePotionEffect(effect.getType());
        }
    }

    public GamePlayer getGamePlayer(){
        return this;
    }

    public Player getOnlinePlayer(){
        return player.getPlayer();
    }

    public boolean isOnline(){
        return getOnlinePlayer() != null || getType().equals(GamePlayerType.DISCONNECTED);
    }

    public boolean isSpectator() {
        return type.equals(GamePlayerType.SPECTATOR);
    }

    public boolean hasKit(){
        return getKit() != null;
    }

    public boolean isInGame(){
        return getGame() != null;
    }

    public OfflinePlayer getOfflinePlayer() {
        return player;
    }

    public List<Area> getAreas(){
        if(!isInGame()) return null;
        if(!isOnline()) return null;
        if (getGame().getCurrentMap() == null) return null;

        List<Area> areas = new ArrayList<>();
        for(Area area : getGame().getCurrentMap().getAreas()){
            if(area.isInArea(getOnlinePlayer().getLocation())) {
                areas.add(area);
            }
        }
        return areas;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GamePlayer that)) return false;
        return player.getUniqueId().equals(that.player.getUniqueId());
    }

    @Override
    public int hashCode() {
        return player.getUniqueId().hashCode();
    }
}