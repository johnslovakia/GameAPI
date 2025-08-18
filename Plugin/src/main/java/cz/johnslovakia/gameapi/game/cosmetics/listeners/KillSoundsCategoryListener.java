package cz.johnslovakia.gameapi.game.cosmetics.listeners;

import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.events.GamePlayerDeathEvent;
import cz.johnslovakia.gameapi.game.cosmetics.CosmeticsCategory;
import cz.johnslovakia.gameapi.users.GamePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class KillSoundsCategoryListener implements Listener {

    @EventHandler
    public void onGamePlayerDeath(GamePlayerDeathEvent e) {
        CosmeticsCategory category = Minigame.getInstance().getCosmeticsManager().getCategoryByName("Kill Sounds");
        GamePlayer killer = e.getKiller();
        if (killer != null && category.getSelectedCosmetic(killer) != null){
            category.getSelectedCosmetic(killer).getPreviewConsumer().accept(e.getGamePlayer());
            category.getSelectedCosmetic(killer).getPreviewConsumer().accept(killer);
        }
    }
}
