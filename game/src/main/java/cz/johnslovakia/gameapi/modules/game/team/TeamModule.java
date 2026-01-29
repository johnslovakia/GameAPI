package cz.johnslovakia.gameapi.modules.game.team;

import cz.johnslovakia.gameapi.modules.game.GameModule;
import lombok.Getter;

import java.util.*;

@Getter
public class TeamModule extends GameModule {

    //private Map<Integer, GameTeam> teamPlacement = new HashMap<>();
    private Map<String, GameTeam> teams = new HashMap<>();
    private HashMap<String, List<TeamScore>> scores;// = new HashMap<>();

    @Override
    public void initialize() {

    }

    @Override
    public void terminate() {
        //teamPlacement = null;
        teams = null;
        scores = null;
    }

    public void registerTeam(GameTeam... teams){
        for (GameTeam team : teams) {
            this.teams.put(team.getName(), team);
        }
    }

    public boolean getTeamAllowEnter(GameTeam team) {
        int tolerance = 1;

        int totalPlayers = 0;
        for (GameTeam t : teams.values()) {
            totalPlayers += t.getAllMembers().size();
        }

        double averagePlayers = (double) totalPlayers / teams.size();
        int maxAllowedSize = (int) Math.ceil(averagePlayers + tolerance);

        return team.getAllMembers().size() < maxAllowedSize;
    }

    public GameTeam getSmallestTeam() {
        GameTeam smallestTeam = null;
        for (GameTeam team : teams.values()) {
            if (smallestTeam == null || team.getAllMembers().size() < smallestTeam.getAllMembers().size()) {
                smallestTeam = team;
            }
        }
        return smallestTeam;
    }

    public GameTeam getHighestTeam() {
        GameTeam highest = null;
        for(GameTeam team : teams.values()) {
            if (highest == null || team.getAllMembers().size() > highest.getAllMembers().size())
                highest = team;
        }
        return highest;
    }

    public GameTeam getTeam(String name){
        return teams.get(name);
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
        for (GameTeam t : teams.values()) {
            TeamScore ts = new TeamScore(t, name);
            sc.add(ts);
        }
        scores.put(name, sc);
    }
}