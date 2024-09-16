package cz.johnslovakia.gameapi.game.team;

import cz.johnslovakia.gameapi.events.TeamScoreEvent;
import org.bukkit.Bukkit;

public class TeamScore implements Comparable<TeamScore> {

    private GameTeam team;
    private int score = 0;
    private String scoreName;

    public TeamScore(GameTeam team, String scoreName) {
        this.team = team;
        this.scoreName = scoreName;
    }

    public String getScoreName() {
        return scoreName;
    }

    public GameTeam getTeam() {
        return team;
    }

    public int getScore() {
        return score;
    }


    public void increaseScore() {
        score++;
        TeamScoreEvent event = new TeamScoreEvent(team, this);
        Bukkit.getPluginManager().callEvent(event);
    }

    public void decreaseScore() {
        score--;
        TeamScoreEvent event = new TeamScoreEvent(team, this);
        Bukkit.getPluginManager().callEvent(event);
    }

    public void resetScore() {
        score = 0;
        TeamScoreEvent event = new TeamScoreEvent(team, this);
        Bukkit.getPluginManager().callEvent(event);
    }

    public void addScore(int score) {
        this.score = this.score + score;
        TeamScoreEvent event = new TeamScoreEvent(team, this);
        Bukkit.getPluginManager().callEvent(event);
    }

    public void removeScore(int score) {
        this.score = this.score - score;
        TeamScoreEvent event = new TeamScoreEvent(team, this);
        Bukkit.getPluginManager().callEvent(event);
    }


    @Override
    public int compareTo(TeamScore o) {
        return o.getScore() - this.getScore();
    }
}
