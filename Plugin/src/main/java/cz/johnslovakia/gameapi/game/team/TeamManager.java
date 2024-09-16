package cz.johnslovakia.gameapi.game.team;

import cz.johnslovakia.gameapi.game.Game;

import java.util.*;

public class TeamManager {


    //TODO: zmatek... přepsat
    private static Map<Game, List<GameTeam>> teams = new HashMap<>();
    private static HashMap<String, List<TeamScore>> scores = new HashMap<>();

    public static void registerTeam(GameTeam... team){
        for (GameTeam gameTeam : team) {
            if (!teams.containsKey(gameTeam.getGame())) {
                List<GameTeam> list = new ArrayList<>();
                list.add(gameTeam);
                teams.put(gameTeam.getGame(), list);
            }else{
                List<GameTeam> list = teams.get(gameTeam.getGame());
                list.add(gameTeam);
                teams.put(gameTeam.getGame(), list);
            }
        }
    }

    public static boolean getTeamAllowEnter(Game game, GameTeam team) {
        int tolerance = 1;

        int totalPlayers = 0;
        for (GameTeam t : TeamManager.getTeams(game)) {
            totalPlayers += t.getAllMembers().size();
        }

        double averagePlayers = (double) totalPlayers / TeamManager.getTeams(game).size();
        int maxAllowedSize = (int) Math.ceil(averagePlayers + tolerance);

        return team.getAllMembers().size() < maxAllowedSize;
    }

    public static GameTeam getSmallestTeam(Game game) {
        GameTeam smallestTeam = null;
        for (GameTeam team : TeamManager.getTeams(game)) {
            if (smallestTeam == null || team.getAllMembers().size() < smallestTeam.getAllMembers().size()) {
                smallestTeam = team;
            }
        }
        return smallestTeam;
    }

    public static GameTeam getHighestTeam(Game game) {
        GameTeam highest = null;
        for(GameTeam team : TeamManager.getTeams(game)) {
            if (highest == null || team.getAllMembers().size() > highest.getAllMembers().size())
                highest = team;
        }
        return highest;
    }


    public static void resetTeamsAndRegisterForNewGame(Game oldGame, Game newGame){
        if (teams.get(oldGame) == null || teams.isEmpty()){
            return;
        }
        for (GameTeam t : teams.get(oldGame)){
            GameTeam newTeam = new GameTeam(newGame, t.getTeamColor());
            teams.remove(oldGame);
            registerTeam(newTeam);

            List<TeamScore> oldTeamScores = getScoresByTeam(t);

            for (String s : scores.keySet()){
                List<TeamScore> list = scores.get(s);
                list.removeAll(oldTeamScores);

                TeamScore ts = new TeamScore(newTeam, s);
                list.add(ts);

                scores.put(s, list);
            }

        }

        //Collections.sort(teams);
    }


    public static GameTeam getTeam(Game game, String name){
        for (GameTeam team : getTeams(game)){
            if (team.getName().equals(name)) {
                return team;
            }
        }
        return null;
    }

    public static List<GameTeam> getTeams(Game game) {
        return teams.get(game);
        /*List<GameTeam> list = new ArrayList<>();
        for (GameTeam team : teams){
            if (team.getGame().equals(game)){
                list.add(team);
            }
        }
        return list;*/
    }

    public static int getTeamsSize(){
        /*List<TeamColor> uniqueTeams = new ArrayList<>();

        for (GameTeam team : teams) {
            if (!uniqueTeams.contains(team.getTeamColor())) {
                uniqueTeams.add(team.getTeamColor());
            }
        }
        return uniqueTeams.size();*/
        if (teams.isEmpty()){
            return 4;
        }else{
            return teams.get(teams.keySet().stream().toList().get(0)).size();
        }
    }

    public static List<TeamScore> getScoresByName(String name) {
        if (scores.containsKey(name)) {
            return scores.get(name);
        }
        return null;
    }

    public static List<TeamScore> getScoresByTeam(GameTeam team) {
        List<TeamScore> teamScores = new ArrayList<>();
        for (List<TeamScore> ts : scores.values()) {
            for (TeamScore ts2 : ts) {
                if (ts2.getTeam().equals(team)) {
                    teamScores.add(ts2);
                    continue;
                }
            }
        }
        return teamScores;
    }

    public static void registerNewScore(String name) {
        if (scores.containsKey(name)) return;
        List<TeamScore> sc = new ArrayList<>();
        List<GameTeam> allTeams = teams.values().stream()
                .flatMap(Collection::stream)
                .toList();
        for (GameTeam t : allTeams) {
            TeamScore ts = new TeamScore(t, name);
            sc.add(ts);
        }
        scores.put(name, sc);
    }
}