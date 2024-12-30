package cz.johnslovakia.gameapi.game.cosmetics;

import com.mongodb.annotations.Sealed;
import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.users.resources.Resource;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerData;
import cz.johnslovakia.gameapi.utils.Sounds;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.function.Consumer;

@Getter
public class Cosmetic {

    private final String name;
    private final ItemStack icon;
    private final int price;
    private final CosmeticRarity rarity;
    @Setter
    private CosmeticsCategory category;

    private Consumer<GamePlayer> gamePlayerConsumer;
    private Consumer<Location> locationConsumer;
    private Consumer<GamePlayer> selectConsumer;

    public Cosmetic(String name, ItemStack icon, int price, CosmeticRarity rarity) {
        this.name = name;
        this.icon = icon;
        this.price = price;
        this.rarity = rarity;
    }

    public void select(GamePlayer gamePlayer){
        Player player = gamePlayer.getOnlinePlayer();

        if (GameAPI.getInstance().getCosmeticsManager().hasSelected(gamePlayer, this)) {
            MessageManager.get(gamePlayer, "chat.cosmetics.already_selected")
                    .send();
            player.playSound(player.getLocation(), Sounds.VILLAGER_NO.bukkitSound(), 10.0F, 10.0F);
        }

        gamePlayer.getPlayerData().selectCosmetic(this);
        if (getSelectConsumer() != null) getSelectConsumer().accept(gamePlayer);

        MessageManager.get(gamePlayer, "chat.cosmetics.select")
                .replace("%cosmetic%", getName())
                .send();
        player.playSound(player.getLocation(), Sounds.CLICK.bukkitSound(), 10.0F, 10.0F);
    }

    public void purchase(GamePlayer gamePlayer){
        if (GameAPI.getInstance().getCosmeticsManager().hasPlayer(gamePlayer, this)){
            return;
        }

        PlayerData data = gamePlayer.getPlayerData();
        Resource resource = GameAPI.getInstance().getCosmeticsManager().getEconomy();

        data.purchaseCosmetic(this);

        new BukkitRunnable(){
            @Override
            public void run() {
                resource.getResourceInterface().withdraw(gamePlayer, getPrice());
            }
        }.runTaskAsynchronously(GameAPI.getInstance());


        //GameAPI.getInstance().getVaultPerms().playerAdd(player, getPermission());

        MessageManager.get(gamePlayer, "chat.cosmetics.purchase")
                .replace("%cosmetic%", getName())
                .replace("%price%", "" + getPrice())
                .replace("%economy_name%", resource.getName())
                .send();
        //gamePlayer.getOnlinePlayer().playSound(gamePlayer.getOnlinePlayer().getLocation(), Sounds.LEVEL_UP.bukkitSound(), 10.0F, 10.0F); - už u select
        select(gamePlayer);
    }

    public boolean hasPurchased(GamePlayer gamePlayer){
        return gamePlayer.getPlayerData().getPurchasedCosmetics().contains(this);
    }

    public boolean hasSelected(GamePlayer gamePlayer){
        return gamePlayer.getPlayerData().getSelectedCosmetics().containsValue(this);
    }

    public boolean hasPlayer(GamePlayer gamePlayer){
        return hasPurchased(gamePlayer)
                || hasSelected(gamePlayer)
                || gamePlayer.getOnlinePlayer().hasPermission("cosmetics.free");
    }

    public Cosmetic setGamePlayerConsumer(Consumer<GamePlayer> gamePlayerConsumer) {
        this.gamePlayerConsumer = gamePlayerConsumer;
        return this;
    }

    public Cosmetic setLocationConsumer(Consumer<Location> locationConsumer) {
        this.locationConsumer = locationConsumer;
        return this;
    }

    public Cosmetic setSelectConsumer(Consumer<GamePlayer> selectConsumer) {
        this.selectConsumer = selectConsumer;
        return this;
    }
}
