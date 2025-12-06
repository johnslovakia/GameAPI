package cz.johnslovakia.gameapi.modules.game.map;

import cz.johnslovakia.gameapi.events.GamePreparationEvent;
import cz.johnslovakia.gameapi.modules.game.GameModule;
import cz.johnslovakia.gameapi.users.PlayerIdentity;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.*;

@Getter @Setter
public class MapModule extends GameModule implements Listener {

    private List<GameMap> maps = new ArrayList<>();
    private boolean voting = true;

    private Map<PlayerIdentity, Map<GameMap, Integer>> votesForMaps = new HashMap<>();

    @Override
    public void initialize() {

    }

    @Override
    public void terminate() {
        maps = null;
        votesForMaps = null;
    }

    @EventHandler
    public void onGamePreparation(GamePreparationEvent e) {
        if (!e.getGame().equals(getGame())) return; //TODO: udělat lépe

        if (getGame().getSettings().getMaxMapsInGame() > 1){
            getGame().destroyModule(MapModule.class);
        }
    }

    public void addMap(GameMap map){
        if (maps.contains(map)){
            return;
        }
        maps.add(map);
    }


    public void addPlayerVote(PlayerIdentity playerIdentity, GameMap map) {
        votesForMaps.computeIfAbsent(playerIdentity, k -> new HashMap<>())
                .merge(map, 1, Integer::sum);
    }

    public void removePlayerVotes(PlayerIdentity playerIdentity) {
        votesForMaps.remove(playerIdentity);
    }

    public int getPlayerVotesForMap(PlayerIdentity player, GameMap map) {
        Map<GameMap, Integer> playerVotes = votesForMaps.get(player);
        if (playerVotes == null) return 0;
        return playerVotes.getOrDefault(map, 0);
    }

    public Map<GameMap, Integer> getPlayerVotesMap(PlayerIdentity playerIdentity) {
        return votesForMaps.getOrDefault(playerIdentity, Collections.emptyMap());
    }

    public int getTotalPlayerVotes(PlayerIdentity playerIdentity) {
        return getPlayerVotesMap(playerIdentity)
                .values()
                .stream()
                .mapToInt(Integer::intValue)
                .sum();
    }

    public int getTotalVotesForMap(GameMap map) {
        return votesForMaps.values().stream()
                .mapToInt(votes -> votes.getOrDefault(map, 0))
                .sum();
    }

    public GameMap getMostVotedMap() {
        Map<GameMap, Integer> totalVotes = new HashMap<>();

        for (Map<GameMap, Integer> playerVotes : votesForMaps.values()) {
            for (Map.Entry<GameMap, Integer> entry : playerVotes.entrySet()) {
                totalVotes.merge(entry.getKey(), entry.getValue(), Integer::sum);
            }
        }

        return totalVotes.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    public void clearVotes() {
        votesForMaps.clear();
    }

    public boolean isEnabledVoting() {
        return voting;
    }
}
