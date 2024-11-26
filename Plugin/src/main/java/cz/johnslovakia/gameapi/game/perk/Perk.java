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
import lombok.Getter;
import lombok.Setter;

@Getter
public class Perk {

    private final String name;
    private final ItemStack icon;
    private final PerkType type;
    private List<PerkLevel> levels;
    @Setter
    private Set<Trigger<?>> triggers;

    public Perk(String name, ItemStack icon, PerkType type) {
        this.name = name;
        this.icon = icon;
        this.type = type;
    }

    public Perk(String name, ItemStack icon, PerkType type, List<PerkLevel> levels) {
        this.name = name;
        this.icon = icon;
        this.type = type;
        this.levels = levels;
    }

    public void addLevel(PerkLevel perkLevel){
        if (!levels.contains(perkLevel)){
            levels.add(perkLevel);
        }
    }

    public void addTrigger(Trigger<?> trigger){
        triggers.add(trigger);
    }

    public void purchase(GamePlayer gamePlayer) {
        Player player = gamePlayer.getOnlinePlayer();
        PlayerData playerData = gamePlayer.getPlayerData();

        Economy economy = GameAPI.getInstance().getPerkManager().getEconomy();
        PerkLevel nextLevel = GameAPI.getInstance().getPerkManager().getNextPlayerPerkLevel(gamePlayer, this);

        Integer balance = economy.getEconomyInterface().getBalance(gamePlayer);

        if (nextLevel != null){
            Integer nextLevelPrice = nextLevel.price();

            if (balance >= nextLevelPrice){
                playerData.setPerkLevel(this, nextLevel.level());

                MessageManager.get(player, "chat.perk.purchase")
                        .replace("%economy_name%", economy.getName())
                        .replace("%price%", "" + nextLevelPrice)
                        .replace("%perk%", getName() + " " + StringUtils.numeral(nextLevel.level()))
                        .send();
                gamePlayer.getOnlinePlayer().playSound(gamePlayer.getOnlinePlayer(), "custom:purchase", 1F, 1.0F);
                new BukkitRunnable(){
                    @Override
                    public void run() {
                        gamePlayer.getPlayerData().withdraw(economy, nextLevelPrice);
                    }
                }.runTaskAsynchronously(GameAPI.getInstance());
            }else{
                MessageManager.get(player, "chat.dont_have_enough")
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

    public String getTranslationKey(){
        return "perk." + getName().toLowerCase().replace(" ", "_");
    }
}
