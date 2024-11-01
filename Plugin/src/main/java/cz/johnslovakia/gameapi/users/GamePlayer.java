package cz.johnslovakia.gameapi.users;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.game.Game;
import cz.johnslovakia.gameapi.game.Winner;
import cz.johnslovakia.gameapi.game.map.Area;
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

    private boolean enabledPVP = true;
    private boolean enabledMovement = true;
    private boolean limited = false;

    private PlayerData playerData;
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
        if (getPlayerData().getGame().getSettings().useTeams() && getPlayerData().getGame().getSettings().isEnabledRespawning()){
            GameTeam team = getPlayerData().getTeam();

            return !team.isDead();
        }else{
            return getPlayerData().getGame().getSettings().isEnabledRespawning();
        }
    }

    public void setSpectator(boolean spectator){
        setSpectator(spectator, false);
    }

    public void setSpectator(boolean spectator, boolean teamSelector){
        Game game = getPlayerData().getGame();

        if (spectator){
            setType(GamePlayerType.SPECTATOR);

            getOnlinePlayer().setGameMode(GameMode.ADVENTURE);
            getOnlinePlayer().setAllowFlight(true);
            getOnlinePlayer().setInvulnerable(true);
            getOnlinePlayer().addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0));

            for (GamePlayer alivePlayer : game.getPlayers()) {
                GameAPI.getInstance().getVersionSupport().hidePlayer(GameAPI.getInstance(), alivePlayer.getOnlinePlayer(), getOnlinePlayer());
            }
            for (GamePlayer otherSpectator : game.getSpectators()) {
                GameAPI.getInstance().getVersionSupport().showPlayer(GameAPI.getInstance(), otherSpectator.getOnlinePlayer(), getOnlinePlayer());
            }

            if (teamSelector){
                getPlayerData().getGame().getSpectatorManager().getWithTeamSelectorInventoryManager().give(getOnlinePlayer());
            }else{
                getPlayerData().getGame().getSpectatorManager().getInventoryManager().give(getOnlinePlayer());
            }

            MessageManager.get(this, "title.spectator")
                    .send();
        }else{
            setType(GamePlayerType.PLAYER);

            for (GamePlayer gamePlayer : game.getPlayers()){
                if (!gamePlayer.equals(this)){
                    GameAPI.getInstance().getVersionSupport().showPlayer(GameAPI.getInstance(), gamePlayer.getOnlinePlayer(), getOnlinePlayer());
                }
            }
            for (GamePlayer otherSpectator : game.getSpectators()) {
                GameAPI.getInstance().getVersionSupport().hidePlayer(GameAPI.getInstance(), otherSpectator.getOnlinePlayer(), getOnlinePlayer());
            }

            if (getPlayerData().getGame().getSpectatorManager().getInventoryManager().getPlayers().contains(getOnlinePlayer())){
                getPlayerData().getGame().getSpectatorManager().getInventoryManager().unloadInventory(getOnlinePlayer());
            }
            if (getPlayerData().getGame().getSpectatorManager().getWithTeamSelectorInventoryManager().getPlayers().contains(getOnlinePlayer())){
                getPlayerData().getGame().getSpectatorManager().getWithTeamSelectorInventoryManager().unloadInventory(getOnlinePlayer());
            }

            getOnlinePlayer().setAllowFlight(false);
            getOnlinePlayer().setFlying(false);
            getOnlinePlayer().setInvulnerable(false);
            for (PotionEffect potionEffect : getOnlinePlayer().getActivePotionEffects()){
                getOnlinePlayer().removePotionEffect(potionEffect.getType());
            }
        }
    }



    public PlayerScore getScoreByName(String name) {
        for (PlayerScore ts : PlayerManager.getScoresByPlayer(this)) {
            if (ts.getName().equalsIgnoreCase(name)) {
                return ts;
            }
        }
        return null;
    }

    public GamePlayer getGamePlayer(){
        return this;
    }

    public Player getOnlinePlayer(){
        return player.getPlayer();
    }

    public boolean isOnline(){
        return getOnlinePlayer() != null;
    }

    public boolean isSpectator() {
        return type.equals(GamePlayerType.SPECTATOR);
    }

    public boolean hasKit(){
        return getPlayerData().getKit() != null;
    }

    public boolean isInGame(){
        return getPlayerData().getGame() != null;
    }

    public OfflinePlayer getOfflinePlayer() {
        return player;
    }

    public List<Area> getAreas(){
        if(!isInGame()) return null;
        if(!isOnline()) return null;
        if (getPlayerData().getGame().getPlayingMap() == null) return null;

        List<Area> areas = new ArrayList<>();
        for(Area area : getPlayerData().getGame().getPlayingMap().getAreas()){
            if(area.isInArea(getOnlinePlayer().getLocation())) {
                areas.add(area);
            }
        }
        return areas;
    }
}