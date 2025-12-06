package cz.johnslovakia.gameapi.utils;

import cz.johnslovakia.gameapi.modules.game.GameInstance;
import cz.johnslovakia.gameapi.modules.game.team.GameTeam;
import cz.johnslovakia.gameapi.modules.game.team.TeamJoinCause;
import cz.johnslovakia.gameapi.modules.game.team.TeamModule;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerIdentity;

import java.util.ArrayList;
import java.util.List;

public class PartyUtils {

    public static void assignRemainingPartyMembers(List<PlayerIdentity> members){
        List<PlayerIdentity> leftedAlone = new ArrayList<>();

        for (PlayerIdentity identity : members) {
            if (identity instanceof GamePlayer gamePlayer) {
                GameInstance game = gamePlayer.getGame();
                GameTeam team = game.getModule(TeamModule.class).getSmallestTeam();

                if (team.getMembers().size() < game.getSettings().getMaxTeamPlayers()) {
                    team.joinPlayer(gamePlayer, TeamJoinCause.PARTY_OTHER_TEAM);
                } else {
                    leftedAlone.add(gamePlayer);
                }

                if (!leftedAlone.isEmpty()) {
                    assignRemainingPartyMembers(leftedAlone);
                }
            }
        }
    }
}
