package cz.johnslovakia.gameapi.listeners.cosmetics;

import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.events.GamePlayerDeathEvent;
import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.cosmetics.CosmeticsCategory;
import cz.johnslovakia.gameapi.modules.cosmetics.CosmeticsModule;
import cz.johnslovakia.gameapi.users.GamePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class KillEffectsCategoryListener implements Listener {
    @EventHandler
    public void onGamePlayerDeath(GamePlayerDeathEvent e) {
        CosmeticsModule cosmeticsModule = ModuleManager.getModule(CosmeticsModule.class);
        CosmeticsCategory category = cosmeticsModule.getCategoryByName("Kill Effects");
        GamePlayer killer = e.getKiller();
        if (killer != null && category.getSelectedCosmetic(killer.getOnlinePlayer()) != null){
            category.getSelectedCosmetic(killer.getOnlinePlayer()).getLocationConsumer().accept(e.getGamePlayer().getOnlinePlayer().getLocation());
        }
    }
}
