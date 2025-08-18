package cz.johnslovakia.gameapi.game.cosmetics.listeners;

import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.events.GamePlayerDeathEvent;
import cz.johnslovakia.gameapi.game.cosmetics.CosmeticsCategory;
import cz.johnslovakia.gameapi.users.GamePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class KillEffectsCategoryListener implements Listener {
    @EventHandler
    public void onGamePlayerDeath(GamePlayerDeathEvent e) {
        CosmeticsCategory category = Minigame.getInstance().getCosmeticsManager().getCategoryByName("Kill Effects");
        GamePlayer killer = e.getKiller();
        if (killer != null && category.getSelectedCosmetic(killer) != null){
            category.getSelectedCosmetic(killer).getLocationConsumer().accept(e.getGamePlayer().getOnlinePlayer().getLocation());
        }
    }
}
