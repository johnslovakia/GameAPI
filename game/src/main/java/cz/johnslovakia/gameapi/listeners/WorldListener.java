package cz.johnslovakia.gameapi.listeners;

import org.bukkit.GameRule;
import org.bukkit.GameRules;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldInitEvent;

public class WorldListener implements Listener {

    @EventHandler(priority= EventPriority.HIGHEST)
    public void onWorldInit(WorldInitEvent e) {
        e.getWorld().setAutoSave(false);
        e.getWorld().setGameRule(GameRules.SHOW_ADVANCEMENT_MESSAGES, false);
        e.getWorld().setGameRule(GameRules.LOCATOR_BAR, false);
        e.getWorld().setGameRule(GameRules.IMMEDIATE_RESPAWN, true);
        e.getWorld().setKeepSpawnInMemory(false);
    }
}
