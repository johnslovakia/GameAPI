package cz.johnslovakia.gameapi.users;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.Shared;
import cz.johnslovakia.gameapi.modules.game.GameInstance;
import cz.johnslovakia.gameapi.modules.game.GameService;
import cz.johnslovakia.gameapi.modules.game.GameState;
import cz.johnslovakia.gameapi.modules.game.Winner;
import cz.johnslovakia.gameapi.modules.game.map.Area;
import cz.johnslovakia.gameapi.modules.game.map.GameMap;
import cz.johnslovakia.gameapi.modules.game.session.GameSessionModule;
import cz.johnslovakia.gameapi.modules.game.session.PlayerGameSession;
import cz.johnslovakia.gameapi.modules.game.team.GameTeam;
import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.messages.Message;
import cz.johnslovakia.gameapi.modules.messages.MessageModule;
import cz.johnslovakia.gameapi.users.friends.FriendsInterface;
import cz.johnslovakia.gameapi.users.parties.PartiesHook;
import cz.johnslovakia.gameapi.users.parties.PartyInterface;

import cz.johnslovakia.gameapi.users.parties.FriendSystemHook;
import cz.johnslovakia.gameapi.users.parties.PartyAndFriendsHook;

import cz.johnslovakia.gameapi.utils.GameUtils;
import cz.johnslovakia.gameapi.utils.Logger;
import lombok.Getter;
import lombok.Setter;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

@Getter @Setter
public class GamePlayer extends Winner implements PlayerIdentity {

    private OfflinePlayer offlinePlayer;
    private FriendsInterface friends = EmptyFriendsInterface.INSTANCE;
    private PartyInterface party = EmptyPartyInterface.INSTANCE;

    private PlayerData playerData;
    private String gameID;

    private PlayerLifecycleState lifecycleState = PlayerLifecycleState.FULL;

    public GamePlayer(OfflinePlayer offlinePlayer){
        super(WinnerType.PLAYER);
        this.offlinePlayer = offlinePlayer;
        this.playerData = new PlayerData(this);

        hookFriendsAndParty();
    }

    public void cleanUpHeavyData(){
        if (lifecycleState.equals(PlayerLifecycleState.LIGHTWEIGHT)) return;

        playerData = null;
        //gameID = null;
        getMetadata().remove((PlayerIdentity) this);
    }

    public PlayerData getPlayerData() {
        if (playerData == null) {
            Logger.log("Recreating PlayerData for " + getName() + " (was in LIGHTWEIGHT state)", Logger.LogType.INFO);
            this.playerData = new PlayerData(this);
            lifecycleState = PlayerLifecycleState.FULL;
        }
        return playerData;
    }

    public String getPrefix(){
        World world = getOnlinePlayer().getWorld();
        GameAPI plugin = GameAPI.getInstance();

        String prefix = "";
        if (plugin.getVaultChat() != null) {
            String g = plugin.getVaultPerms().getPrimaryGroup(Bukkit.getPlayer(getOnlinePlayer().getName()));
            prefix = plugin.getVaultChat().getGroupPrefix(world, g);
            if (!prefix.endsWith(" ")){
                prefix = prefix + " ";
            }
        }
        return prefix;
    }

    public GameInstance getGame() {
        return ModuleManager.getModule(GameService.class)
                .getGameByID(gameID)
                .orElse(null);
    }

    public PlayerGameSession getGameSession(){
        if (!isInGame()) return null;
        return getGame().getModule(GameSessionModule.class).getPlayerSession(this);
    }

    public void hookFriendsAndParty(){
        if (Bukkit.getServer().getPluginManager().getPlugin("Parties") != null) {
            if (Bukkit.getServer().getPluginManager().getPlugin("Parties").isEnabled()) {
                this.party = new PartiesHook(this);
            }
        }else if (Bukkit.getServer().getPluginManager().getPlugin("PartyAndFriends") != null) {
            if (Bukkit.getServer().getPluginManager().getPlugin("PartyAndFriends").isEnabled()) {
                this.party = new PartyAndFriendsHook(this);
                this.friends = new cz.johnslovakia.gameapi.users.friends.PartyAndFriendsHook(this);
            }
        }else if (Bukkit.getServer().getPluginManager().getPlugin("FriendSystem-Spigot-API") != null) {
            if (Bukkit.getServer().getPluginManager().getPlugin("FriendSystem-Spigot-API").isEnabled()) {
                this.party = new FriendSystemHook(this);
                this.friends = new cz.johnslovakia.gameapi.users.friends.FriendSystemHook(this);
            }
        }
    }

    public Message createMessage(String translationKey){
        return ModuleManager.getModule(MessageModule.class).get(this, translationKey);
    }

    public void sendMessage(String translationKey){
        ModuleManager.getModule(MessageModule.class).get(this, translationKey).send();
    }

    public boolean isRespawning(){
        if (getGame().getSettings().isUseTeams() && getGame().getSettings().isEnabledRespawning()){
            return !getGameSession().getTeam().isDead();
        }else{
            return getGame().getSettings().isEnabledRespawning();
        }
    }

    public void setSpectator(boolean spectator){
        setSpectator(spectator, false);
    }

    public void setSpectator(boolean spectator, boolean teamSelector){
        GameInstance game = getGame();

        if (spectator){
            getGameSession().setState(GamePlayerState.SPECTATOR);

            getOnlinePlayer().setGameMode(GameMode.ADVENTURE);
            getOnlinePlayer().setAllowFlight(true);
            getOnlinePlayer().setFlying(true);
            getOnlinePlayer().addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0));

            for (GamePlayer alivePlayer : game.getPlayers()) {
                alivePlayer.getOnlinePlayer().hidePlayer(Shared.getInstance().getPlugin(), getOnlinePlayer());
            }
            for (GamePlayer otherSpectator : game.getSpectators()) {
                otherSpectator.getOnlinePlayer().hidePlayer(Shared.getInstance().getPlugin(), getOnlinePlayer());
            }

            GameMap currentMap = game.getCurrentMap();
            if (currentMap != null){
                currentMap.getPlayerToLocation().remove(this);
            }

            if (game.getState().equals(GameState.INGAME)) {
                if (teamSelector) {
                    getGame().getSpectatorManager().getWithTeamSelectorInventoryManager().give(this);
                } else {
                    getGame().getSpectatorManager().getInventoryManager().give(this);
                }
            }

            ModuleManager.getModule(MessageModule.class).get(this, "title.spectator")
                    .send();
        }else{
            getGameSession().setState(GamePlayerState.PLAYER);

            for (GamePlayer gamePlayer : game.getPlayers()){
                if (!gamePlayer.equals(this)){
                    gamePlayer.getOnlinePlayer().showPlayer(Shared.getInstance().getPlugin(), getOnlinePlayer());
                }
            }
            for (GamePlayer otherSpectator : game.getSpectators()) {
                otherSpectator.getOnlinePlayer().hidePlayer(Shared.getInstance().getPlugin(), getOnlinePlayer());
            }

            if (getGame().getSpectatorManager().getInventoryManager().getPlayers().contains(getOnlinePlayer())){
                getGame().getSpectatorManager().getInventoryManager().unloadInventory(this);
            }
            if (getGame().getSpectatorManager().getWithTeamSelectorInventoryManager().getPlayers().contains(getOnlinePlayer())){
                getGame().getSpectatorManager().getWithTeamSelectorInventoryManager().unloadInventory(this);
            }

            getOnlinePlayer().setAllowFlight(false);
            getOnlinePlayer().setFlying(false);
            for (PotionEffect potionEffect : getOnlinePlayer().getActivePotionEffects()){
                getOnlinePlayer().removePotionEffect(potionEffect.getType());
            }
        }
    }


    public void resetAttributes(){
        getOnlinePlayer().getInventory().clear();
        getOnlinePlayer().getAttribute(Attribute.MAX_HEALTH).setBaseValue(20D);
        getOnlinePlayer().setHealth(20);
        getOnlinePlayer().setFoodLevel(20);
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

    @Override
    public UUID getUniqueId() {
        return getOfflinePlayer().getUniqueId();
    }

    @Override
    public String getName() {
        return getOfflinePlayer().getName();
    }

    public Player getOnlinePlayer(){
        return offlinePlayer.getPlayer();
    }

    public boolean isOnline(){
        return getOnlinePlayer() != null && (getGameSession() != null && !getGameSession().getState().equals(GamePlayerState.DISCONNECTED));
    }

    public boolean isSpectator() {
        return getGameSession().getState().equals(GamePlayerState.SPECTATOR);
    }

    public boolean hasKit(){
        return getGameSession().getSelectedKit() != null;
    }

    public boolean isInGame(){
        return getGame() != null;
    }

    public OfflinePlayer getOfflinePlayer() {
        return offlinePlayer;
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
    public FriendsInterface getFriends() {
        return this.friends;
    }

    @Override
    public PartyInterface getParty() {
        return this.party;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GamePlayer that)) return false;
        return getUniqueId().equals(that.getUniqueId());
    }

    @Override
    public int hashCode() {
        return getUniqueId().hashCode();
    }


    enum PlayerLifecycleState {
        FULL,
        LIGHTWEIGHT,
        //DESTROYED
    }
}