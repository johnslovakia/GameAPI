package cz.johnslovakia.gameapi.listeners.cosmetics;

import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.events.GamePlayerDeathEvent;
import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.cosmetics.CosmeticsCategory;
import cz.johnslovakia.gameapi.modules.cosmetics.CosmeticsModule;
import cz.johnslovakia.gameapi.users.GamePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class KillSoundsCategoryListener implements Listener {

    @EventHandler
    public void onGamePlayerDeath(GamePlayerDeathEvent e) {
        CosmeticsModule cosmeticsModule = ModuleManager.getModule(CosmeticsModule.class);
        CosmeticsCategory category = cosmeticsModule.getCategoryByName("Kill Sounds");
        GamePlayer killer = e.getKiller();
        if (killer != null && category.getSelectedCosmetic(killer.getOnlinePlayer()) != null){
            category.getSelectedCosmetic(killer.getOnlinePlayer()).getPreviewConsumer().accept(e.getGamePlayer());
            category.getSelectedCosmetic(killer.getOnlinePlayer()).getPreviewConsumer().accept(killer);
        }
    }
}
