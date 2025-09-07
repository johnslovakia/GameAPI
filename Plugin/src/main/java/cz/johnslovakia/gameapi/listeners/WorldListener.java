package cz.johnslovakia.gameapi.listeners;

import org.bukkit.GameRule;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldInitEvent;

public class WorldListener implements Listener {

    @EventHandler(priority= EventPriority.HIGHEST)
    public void onWorldInit(WorldInitEvent e) {
        e.getWorld().setAutoSave(false);
        e.getWorld().setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        e.getWorld().setGameRule(GameRule.LOCATOR_BAR, false);
        e.getWorld().setKeepSpawnInMemory(false);
    }
}
