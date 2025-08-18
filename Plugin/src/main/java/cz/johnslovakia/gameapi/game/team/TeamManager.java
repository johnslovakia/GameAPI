package cz.johnslovakia.gameapi.game.team;

import cz.johnslovakia.gameapi.game.Game;
import lombok.Getter;

import java.util.*;

@Getter
public class TeamManager {

    private final Game game;

    private final Map<Integer, GameTeam> teamPlacement = new HashMap<>();
    private List<GameTeam> teams = new ArrayList<>();
    private HashMap<String, List<TeamScore>> scores;// = new HashMap<>();

    public TeamManager(Game game) {
        this.game = game;
    }

    public TeamManager(Game game, List<GameTeam> teams) {
        this.game = game;
        this.teams = teams;
    }

    public void registerTeam(GameTeam... team){
        teams.addAll(Arrays.asList(team));
    }

    public boolean getTeamAllowEnter(GameTeam team) {
        int tolerance = 1;

        int totalPlayers = 0;
        for (GameTeam t : teams) {
            totalPlayers += t.getAllMembers().size();
        }

        double averagePlayers = (double) totalPlayers / teams.size();
        int maxAllowedSize = (int) Math.ceil(averagePlayers + tolerance);

        return team.getAllMembers().size() < maxAllowedSize;
    }

    public GameTeam getSmallestTeam() {
        GameTeam smallestTeam = null;
        for (GameTeam team : teams) {
            if (smallestTeam == null || team.getAllMembers().size() < smallestTeam.getAllMembers().size()) {
                smallestTeam = team;
            }
        }
        return smallestTeam;
    }

    public GameTeam getHighestTeam() {
        GameTeam highest = null;
        for(GameTeam team : teams) {
            if (highest == null || team.getAllMembers().size() > highest.getAllMembers().size())
                highest = team;
        }
        return highest;
    }

    public GameTeam getTeam(String name){
        for (GameTeam team : teams){
            if (team.getName().equals(name)) {
                return team;
            }
        }
        return null;
    }


    public int getTeamsSize(){
        if (teams.isEmpty()){
            return 4;
        }else{
            return teams.size();
        }
    }

    public List<TeamScore> getScoresByName(String name) {
        if (scores == null){
            return Collections.emptyList();
        }

        if (scores.containsKey(name)) {
            return scores.get(name);
        }
        return null;
    }

    public List<TeamScore> getScoresByTeam(GameTeam team) {
        if (scores == null){
            return Collections.emptyList();
        }

        List<TeamScore> teamScores = new ArrayList<>();
        for (List<TeamScore> ts : scores.values()) {
            for (TeamScore ts2 : ts) {
                if (ts2.getTeam().equals(team)) {
                    teamScores.add(ts2);
                }
            }
        }
        return teamScores;
    }

    public void registerNewScore(String name) {
        if (scores == null){
            scores = new HashMap<>();
        }

        if (scores.containsKey(name)) return;
        List<TeamScore> sc = new ArrayList<>();
        for (GameTeam t : teams) {
            TeamScore ts = new TeamScore(t, name);
            sc.add(ts);
        }
        scores.put(name, sc);
    }
}