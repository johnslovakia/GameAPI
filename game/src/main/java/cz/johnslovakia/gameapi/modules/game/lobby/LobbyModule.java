package cz.johnslovakia.gameapi.modules.game.lobby;

import cz.johnslovakia.gameapi.inventoryBuilder.InventoryBuilder;
import cz.johnslovakia.gameapi.modules.game.GameModule;
import cz.johnslovakia.gameapi.worldManagement.WorldManager;
import lombok.Getter;

@Getter
public class LobbyModule extends GameModule {

    private final LobbyLocation lobbyLocation;
    private final InventoryBuilder inventoryManager;

    public LobbyModule(LobbyLocation lobbyLocation, InventoryBuilder inventoryManager) {
        this.lobbyLocation = lobbyLocation;
        this.inventoryManager = inventoryManager;
    }

    @Override
    public void initialize() {
        WorldManager.cloneWorld(lobbyLocation.getWorldName(), getGame().getID());
    }

    @Override
    public void terminate() {
        inventoryManager.terminate();
    }
}
