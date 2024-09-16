package cz.johnslovakia.gameapi.game.perk;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.economy.Economy;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerData;
import cz.johnslovakia.gameapi.utils.Sounds;
import cz.johnslovakia.gameapi.utils.StringUtils;
import cz.johnslovakia.gameapi.utils.eTrigger.Trigger;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Set;

public interface Perk {

    String getName();
    ItemStack getIcon();
    PerkType getType();
    List<PerkLevel> getLevels();
    Set<Trigger<?>> getTriggers();

    default void purchase(GamePlayer gamePlayer) {
        Player player = gamePlayer.getOnlinePlayer();
        PlayerData playerData = gamePlayer.getPlayerData();

        Economy economy = GameAPI.getInstance().getPerkManager().getEconomy();
        PerkLevel nextLevel = GameAPI.getInstance().getPerkManager().getNextPlayerPerkLevel(gamePlayer, this);

        Integer balance = economy.getEconomyInterface().getBalance(gamePlayer);

        if (nextLevel != null){
            Integer nextLevelPrice = nextLevel.price();

            if (balance >= nextLevelPrice){
                MessageManager.get(player, "chat.perk.purchase")
                        .replace("%economy_name%", economy.getName())
                        .replace("%price%", "" + nextLevelPrice)
                        .replace("%perk%", getName() + " " + StringUtils.numeral(nextLevel.level()))
                        .send();

                gamePlayer.getPlayerData().setPerkLevel(this, playerData.getPerkLevel(this).level() + 1);


                player.playSound(player.getLocation(), Sounds.CLICK.bukkitSound(), 10.0F, 10.0F);
                new BukkitRunnable(){
                    @Override
                    public void run() {
                        gamePlayer.getPlayerData().withdraw(economy, nextLevelPrice);
                    }
                }.runTaskAsynchronously(GameAPI.getInstance());
            }else{
                MessageManager.get(player, "chat.perk.dont_have_enough")
                        .replace("%economy_name%", economy.getName())
                        .replace("%need_more%", "" + (nextLevelPrice - balance))
                        .send();
                player.playSound(player.getLocation(), Sounds.ANVIL_BREAK.bukkitSound(), 10.0F, 10.0F);
            }
        }else{
            MessageManager.get(player, "chat.perk.max_level")
                    .send();
            player.playSound(player.getLocation(), Sounds.ANVIL_BREAK.bukkitSound(), 10.0F, 10.0F);
        }
    }
}
