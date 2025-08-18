package cz.johnslovakia.gameapi.listeners;

import cz.johnslovakia.gameapi.utils.Sounds;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import eu.decentsoftware.holograms.event.HologramClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class HologramListener implements Listener {


    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent e) {
       if (e.getRightClicked() instanceof TextDisplay textDisplay){
            Player player = e.getPlayer();

        }

    }

    @EventHandler
    public void onHologramClick(HologramClickEvent e) {
        Hologram hologram = e.getHologram();
        Player player = e.getPlayer();

        if (!e.getHologram().getName().equals("topStats_" + player.getName())){
            return;
        }

        int i = hologram.getPlayerPage(player) + 1;
        hologram.show(player, (hologram.getPage(i) != null ? i : 0));
        player.playSound(player, Sounds.CLICK.bukkitSound(), 1F, 1F);
    }
}
