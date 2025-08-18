package cz.johnslovakia.gameapi.game;

import cz.johnslovakia.gameapi.utils.inventoryBuilder.InventoryManager;
import cz.johnslovakia.gameapi.worldManagement.WorldManager;
import lombok.Getter;

@Getter
public class LobbyManager {

    private final Game game;
    private final LobbyLocation lobbyLocation;
    private final InventoryManager inventoryManager;

    public LobbyManager(Game game, LobbyLocation lobbyLocation, InventoryManager inventoryManager) {
        this.game = game;
        this.lobbyLocation = lobbyLocation;
        this.inventoryManager = inventoryManager;

        WorldManager.cloneWorld(lobbyLocation.getWorldName(), game.getID());
    }
}
