package cz.johnslovakia.gameapi.users.parties;

import cz.johnslovakia.gameapi.game.Game;
import cz.johnslovakia.gameapi.game.team.GameTeam;
import cz.johnslovakia.gameapi.game.team.TeamJoinCause;
import cz.johnslovakia.gameapi.game.team.TeamManager;
import cz.johnslovakia.gameapi.users.GamePlayer;

import java.util.ArrayList;
import java.util.List;

public class PartyUtils {

    public static void assignRemainingPartyMembers(List<GamePlayer> members){
        List<GamePlayer> leftedAlone = new ArrayList<>();

        for (GamePlayer gamePlayer : members) {
            Game game = gamePlayer.getGame();;
            GameTeam team = game.getTeamManager().getSmallestTeam();

            if (team.getMembers().size() < game.getSettings().getMaxTeamPlayers()) {
                team.joinPlayer(gamePlayer, TeamJoinCause.PARTY_OTHER_TEAM);
            }else{
                leftedAlone.add(gamePlayer);
            }

            if (!leftedAlone.isEmpty()){
                assignRemainingPartyMembers(leftedAlone);
            }
        }
    }
}
