package cz.johnslovakia.gameapi.users.resources;

import cz.johnslovakia.gameapi.users.GamePlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResourcesManager {

    public static List<Resource> resourceList = new ArrayList<>();

    public static void addResource(Resource... resources){
        resourceList.addAll(List.of(resources));
    }

    public static Resource getResourceByName(String name){
        for (Resource resource : getResources()){
            if (resource.getName().equalsIgnoreCase(name)){
                return resource;
            }
        }
        return null;
    }

    public static List<Resource> getResources() {
        return resourceList;
    }

    public static Integer getEarned(GamePlayer gamePlayer, Resource rewardType){
        return gamePlayer.getPlayerData().getScores().stream()
                .mapToInt(score -> score.getEarned(rewardType))
                .sum();
    }

    public static boolean earnedSomething(GamePlayer gamePlayer){
        return gamePlayer.getPlayerData().getScores().stream()
                .anyMatch(score -> score.getEarned().values().stream().anyMatch(v -> v != 0));
    }

    public static Map<Resource, Integer> rewardToHashMap(Resource rewardType, Integer reward){
        Map<Resource, Integer> map = new HashMap<>();
        map.put(rewardType, reward);
        return map;
    }
}