package cz.johnslovakia.gameapi.modules.game.team;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.modules.game.GameInstance;
import cz.johnslovakia.gameapi.modules.game.GameState;
import cz.johnslovakia.gameapi.modules.game.Winner;
import cz.johnslovakia.gameapi.modules.game.map.GameMap;
import cz.johnslovakia.gameapi.modules.game.session.PlayerGameSession;
import cz.johnslovakia.gameapi.modules.levels.LevelModule;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.GamePlayerState;
import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.messages.MessageModule;
import cz.johnslovakia.gameapi.users.PlayerIdentity;
import cz.johnslovakia.gameapi.utils.GameUtils;
import cz.johnslovakia.gameapi.utils.PartyUtils;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Getter
public class GameTeam extends Winner implements Comparable<GameTeam>{

    private final GameInstance game;
    private final TeamColor teamColor;

    @Getter(AccessLevel.PACKAGE)
    private final List<GamePlayer> members = new ArrayList<>();
    private final HashMap<String, Object> metadata = new HashMap<>();

    @Setter
    private boolean dead = true;

    public GameTeam(GameInstance game, TeamColor teamColor){
        super(WinnerType.TEAM);
        this.game = game;
        this.teamColor = teamColor;
    }

    public String getName() {
        return teamColor.formattedName();
    }

    public boolean isFull(){
        return getAllMembers().size() >= game.getSettings().getMaxTeamPlayers();
    }

    public boolean joinPlayer(GamePlayer gamePlayer, TeamJoinCause cause){
        Player player = gamePlayer.getOnlinePlayer();
        PlayerGameSession gameSession = gamePlayer.getGameSession();
        GameInstance game = gamePlayer.getGame();

        if (game != null) {
            if (game.getState() == GameState.WAITING
                    || game.getState() == GameState.STARTING
                    || game.getSettings().isEnabledJoiningAfterStart()
                    || cause.equals(TeamJoinCause.REJOIN)) {

                if (!getAllMembers().contains(gamePlayer)) {
                    if (game.getState().equals(GameState.INGAME)) {
                        if (isDead()) {
                            ModuleManager.getModule(MessageModule.class).get(gamePlayer, "chat.team.dead")
                                    .send();
                            quitPlayer(gamePlayer);
                            return false;
                        }
                    }
                    if(isFull()){
                        ModuleManager.getModule(MessageModule.class).get(gamePlayer, "chat.team.full")
                                .send();
                        return false;
                    }
                    if (!game.getModule(TeamModule.class).getTeamAllowEnter(this)) {
                        ModuleManager.getModule(MessageModule.class).get(gamePlayer, "chat.team.balancing.cant_join")
                                .send();
                        return false;
                    }


                    if (gameSession.getTeam() != null) {
                        gameSession.getTeam().quitPlayer(gamePlayer);
                    }

                    getAllMembers().add(gamePlayer);
                    gameSession.setTeam(this);

                    if (cause.equals(TeamJoinCause.INDIVIDUAL) || cause.equals(TeamJoinCause.AUTO)) {
                        if (gamePlayer.getParty().isInParty()) {
                            List<PlayerIdentity> leftedAlone = new ArrayList<>();
                            for (PlayerIdentity party : gamePlayer.getParty().getAllOnlinePlayers()) {
                                if (getMembers().size() < game.getSettings().getMaxTeamPlayers()) {
                                    joinPlayer((GamePlayer) party, TeamJoinCause.PARTY);
                                } else {
                                    leftedAlone.add(party);
                                }
                            }
                            if (!leftedAlone.isEmpty()) {
                                PartyUtils.assignRemainingPartyMembers(leftedAlone);
                            }
                        }
                    }


                    if (game.getState() == GameState.WAITING
                            || game.getState() == GameState.STARTING){
                        ItemStack[] contents = player.getInventory().getContents();
                        for (ItemStack item : contents) {
                            if (item == null) continue;

                            if (item.getType().toString().toLowerCase().contains("banner")) {
                                BannerMeta bannerMeta = (BannerMeta) item.getItemMeta();
                                bannerMeta.addPattern(new Pattern(getDyeColor(), PatternType.BASE));
                                item.setItemMeta(bannerMeta);
                                break;
                            }else if (item.getType().toString().toLowerCase().contains("wool")) {
                                Material colorfulWool = Material.valueOf(getDyeColor().name() + "_WOOL");
                                if (colorfulWool != null){
                                    item.setType(colorfulWool);
                                    break;
                                }
                            }
                        }
                    }

                    GameUtils.setTeamNameTag(player, getName() + "_" + game.getID(), getChatColor());
                    LevelModule levelModule = ModuleManager.getModule(LevelModule.class);
                    Component levelIcon = Component.empty();
                    if (levelModule != null && levelModule.getPlayerData(gamePlayer) != null) {
                        levelIcon = levelModule.getPlayerData(gamePlayer).getLevelEvolution().getIcon();
                    }

                    player.playerListName(levelIcon
                            .append(Component.text(" " + player.getName())
                                .font(Key.key("minecraft", "default"))
                                    .color(getTeamColor().getTextColor())));
                    player.setDisplayName(getChatColor() + player.getName());

                    if (!cause.equals(TeamJoinCause.AUTO)) {
                        ModuleManager.getModule(MessageModule.class).get(gamePlayer, "chat.team.join")
                                .replace("%team%", getChatColor() + getName())
                                .send();
                    }

                    if (cause.equals(TeamJoinCause.PARTY)){
                        ModuleManager.getModule(MessageModule.class).get(gamePlayer, "chat.party.team_join")
                                .send();
                    }else if (cause.equals(TeamJoinCause.PARTY_OTHER_TEAM)){
                        ModuleManager.getModule(MessageModule.class).get(gamePlayer, "chat.party.other_team_join")
                                .send();
                    }
                    return true;
                } else {
                    ModuleManager.getModule(MessageModule.class).get(gamePlayer, "chat.team.already_joined")
                            .send();
                }
            }else{
                ModuleManager.getModule(MessageModule.class).get(gamePlayer, "chat.not_allowed")
                        .send();
            }
            return false;
        }
        return false;
    }

    public void rejoin(GamePlayer gamePlayer){
        Player player = gamePlayer.getOnlinePlayer();

        GameUtils.setTeamNameTag(player, getName() + "_" + game.getID(), getChatColor());
        LevelModule levelModule = ModuleManager.getModule(LevelModule.class);
        Component levelIcon = Component.empty();
        if (levelModule != null && levelModule.getPlayerData(gamePlayer) != null) {
            levelIcon = levelModule.getPlayerData(gamePlayer).getLevelEvolution().getIcon();
        }

        player.playerListName(levelIcon
                .append(Component.text(" " + player.getName())
                        .font(Key.key("minecraft", "default"))
                        .color(getTeamColor().getTextColor())));
        player.setDisplayName(getChatColor() + player.getName());
    }

    public void quitPlayer(GamePlayer gamePlayer){
        if (!isMember(gamePlayer)) {
            return;
        }
        getAllMembers().remove(gamePlayer);
    }

    public boolean isMember(GamePlayer gamePlayer){
        return getAllMembers().contains(gamePlayer);
    }

    public Location getSpawn(){
        GameMap map = game.getCurrentMap();
        return map.getSpawn(getName());
    }

    public ChatColor getChatColor() {
        return teamColor.getChatColor();
    }

    public DyeColor getDyeColor() {
        return teamColor.getDyeColor();
    }

    public Color getColor() {
        return teamColor.getColor();
    }

    public List<GamePlayer> getAllMembers() {
        return members;
    }

    public List<GamePlayer> getAliveMembers() {
        return getAllMembers().stream().filter(gamePlayer -> gamePlayer.getGameSession() != null && gamePlayer.getGameSession().getState().equals(GamePlayerState.PLAYER)).toList();
    }

    public List<GamePlayer> getOnlineMembers() {
        return getAllMembers().stream().filter(GamePlayer::isOnline).toList();
    }

    public List<GamePlayer> getDisconnectedMembers() {
        return getAllMembers().stream().filter(gamePlayer -> gamePlayer.getGameSession().getState().equals(GamePlayerState.DISCONNECTED)).toList();
    }

    public TeamScore getScoreByName(String name) {
        for (TeamScore ts : game.getModule(TeamModule.class).getScoresByTeam(this)) {
            if (ts.getScoreName().equalsIgnoreCase(name)) {
                return ts;
            }
        }
        return null;
    }

    @Override
    public int compareTo(GameTeam o) {
        return 0;
    }
}