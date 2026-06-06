package cz.johnslovakia.gameapi.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class CollisionManager {

    private static final String NO_COLLISION_TEAM = "gameapi_nocollide";

    private static Team getOrCreateTeam() {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = scoreboard.getTeam(NO_COLLISION_TEAM);
        if (team == null) {
            team = scoreboard.registerNewTeam(NO_COLLISION_TEAM);
            team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
        }
        return team;
    }

    public static void disableCollision(Player player) {
        getOrCreateTeam().addPlayer(player);
    }

    public static void enableCollision(Player player) {
        Team team = Bukkit.getScoreboardManager().getMainScoreboard().getTeam(NO_COLLISION_TEAM);
        if (team != null && team.hasPlayer(player)) {
            team.removePlayer(player);
        }
    }

    public static void cleanup(Player player) {
        enableCollision(player);
    }
}