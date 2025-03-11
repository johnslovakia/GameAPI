package cz.johnslovakia.gameapi.game.team;

import com.cryptomorin.xseries.XMaterial;
import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.game.Game;
import cz.johnslovakia.gameapi.game.GameState;
import cz.johnslovakia.gameapi.game.Winner;
import cz.johnslovakia.gameapi.game.map.GameMap;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.GamePlayerType;
import cz.johnslovakia.gameapi.users.parties.PartyUtils;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.*;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Getter
public class GameTeam extends Winner implements Comparable<GameTeam>{

    private final Game game;
    private final TeamColor teamColor;
    private Team boardTeam;

    private List<GamePlayer> members = new ArrayList<>();
    private HashMap<String, Object> metadata = new HashMap<>();

    @Setter
    private boolean dead = true;

    public GameTeam(Game game, TeamColor teamColor){
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
        Game game = gamePlayer.getPlayerData().getGame();

        if (game != null) {
            if (game.getState() == GameState.WAITING
                    || game.getState() == GameState.STARTING
                    || game.getSettings().isAllowedJoiningAfterStart()
                    || cause.equals(TeamJoinCause.REJOIN)) {

                if (!getAllMembers().contains(gamePlayer)) {
                    if (game.getState().equals(GameState.INGAME)) {
                        if (isDead()) {
                            MessageManager.get(gamePlayer, "chat.team.dead")
                                    .send();
                            quitPlayer(gamePlayer);
                            return false;
                        }
                    }
                    if(isFull()){
                        MessageManager.get(gamePlayer, "chat.team.full")
                                .send();
                        return false;
                    }
                    if (!game.getTeamManager().getTeamAllowEnter(this)){
                        MessageManager.get(gamePlayer, "chat.team.balancing.cant_join")
                                .send();
                        return false;
                    }


                    if (gamePlayer.getPlayerData().getTeam() != null) {
                        gamePlayer.getPlayerData().getTeam().quitPlayer(gamePlayer);
                    }

                    getAllMembers().add(gamePlayer);
                    gamePlayer.getPlayerData().setTeam(this);

                    if (cause.equals(TeamJoinCause.INDIVIDUAL) || cause.equals(TeamJoinCause.AUTO)) {
                        if (gamePlayer.getParty().isInParty()) {
                            List<GamePlayer> leftedAlone = new ArrayList<>();
                            for (GamePlayer party : gamePlayer.getParty().getAllOnlinePlayers()) {
                                if (getMembers().size() < game.getSettings().getMaxTeamPlayers()) {
                                    joinPlayer(party, TeamJoinCause.PARTY);
                                } else {
                                    leftedAlone.add(party);
                                }
                            }
                            if (!leftedAlone.isEmpty()) {
                                PartyUtils.assignRemainingPartyMembers(leftedAlone);
                            }
                        }
                    }


                    ItemStack[] contents = player.getInventory().getContents();
                    for (ItemStack item : contents) {
                        if (item == null) {
                            continue;
                        }
                        if (item.getType().toString().toLowerCase().contains("banner")) {
                            BannerMeta bannerMeta = (BannerMeta) item.getItemMeta();
                            bannerMeta.addPattern(new Pattern(getDyeColor(), PatternType.BASE));
                            item.setItemMeta(bannerMeta);
                        }else if (item.getType().toString().toLowerCase().contains("wool")) {
                            Material colorfulWool = XMaterial.valueOf(getDyeColor().name() + "_WOOL").parseMaterial();
                            if (colorfulWool != null){
                                item.setType(colorfulWool);
                            }
                        }
                        break;
                    }

                    GameAPI.getInstance().getVersionSupport().setTeamNameTag(player, getName() + "_" + game.getID(), getChatColor());


                    player.setPlayerListName(getChatColor() + player.getName());
                    player.setDisplayName(getChatColor() + player.getName());
                    if (!cause.equals(TeamJoinCause.AUTO)) {
                        MessageManager.get(gamePlayer, "chat.team.join")
                                .replace("%team%", getChatColor() + getName())
                                .send();
                    }

                    if (cause.equals(TeamJoinCause.PARTY)){
                        MessageManager.get(gamePlayer, "chat.party.team_join")
                                .send();
                    }else if (cause.equals(TeamJoinCause.PARTY_OTHER_TEAM)){
                        MessageManager.get(gamePlayer, "chat.party.other_team_join")
                                .send();
                    }
                    return true;
                } else {
                    MessageManager.get(gamePlayer, "chat.team.already_joined")
                            .send();
                }
            }else{
                MessageManager.get(gamePlayer, "chat.not_allowed")
                        .send();
            }
            return false;
        }
        return false;
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
        return getAllMembers().stream().filter(gp -> gp.getType().equals(GamePlayerType.PLAYER)).toList();
    }

    public List<GamePlayer> getDisconnectedMembers() {
        return getAllMembers().stream().filter(gp -> gp.getType().equals(GamePlayerType.DISCONNECTED)).toList();
    }

    public TeamScore getScoreByName(String name) {
        for (TeamScore ts : game.getTeamManager().getScoresByTeam(this)) {
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