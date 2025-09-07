package cz.johnslovakia.gameapi.game.cosmetics;

import com.mongodb.annotations.Sealed;
import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.users.resources.Resource;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerData;
import cz.johnslovakia.gameapi.utils.Sounds;
import cz.johnslovakia.gameapi.utils.StringUtils;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Getter
public class Cosmetic {

    private final String name;
    private final ItemStack icon;
    private final Map<Resource, Integer> cost = new HashMap<>();
    private final CosmeticRarity rarity;
    @Setter
    private CosmeticsCategory category;
    private boolean canBePurchased;

    private Consumer<GamePlayer> gamePlayerConsumer;
    private Consumer<Location> locationConsumer;
    private Consumer<GamePlayer> selectConsumer;
    private Consumer<GamePlayer> previewConsumer;

    public Cosmetic(String name, ItemStack icon, CosmeticRarity rarity) {
        this.name = name;
        this.icon = icon;
        this.rarity = rarity;
    }

    public Cosmetic addCost(Resource resource, int amount){
        cost.put(resource, amount);
        return this;
    }


    public void select(GamePlayer gamePlayer){
        select(gamePlayer, true);
    }

    public void select(GamePlayer gamePlayer, boolean message){
        Player player = gamePlayer.getOnlinePlayer();

        if (!hasPlayer(gamePlayer)) return;

        if (hasSelected(gamePlayer)) {
            if (message) {
                MessageManager.get(gamePlayer, "chat.cosmetics.already_selected")
                        .send();
                player.playSound(player.getLocation(), Sounds.VILLAGER_NO.bukkitSound(), 10.0F, 10.0F);
            }
            return;
        }

        gamePlayer.getPlayerData().selectCosmetic(this);
        if (getSelectConsumer() != null) getSelectConsumer().accept(gamePlayer);

        if (message) {
            MessageManager.get(gamePlayer, "chat.cosmetics.select")
                    .replace("%cosmetic%", getName())
                    .send();
            player.playSound(player.getLocation(), Sounds.CLICK.bukkitSound(), 10.0F, 10.0F);
        }
    }

    public void purchase(GamePlayer gamePlayer){
        if (hasPlayer(gamePlayer)){
            return;
        }

        Player player = gamePlayer.getOnlinePlayer();
        PlayerData data = gamePlayer.getPlayerData();

        List<String> missing = new ArrayList<>();

        for (Map.Entry<Resource, Integer> entry : cost.entrySet()) {
            Resource resource = entry.getKey();
            int cost = entry.getValue();

            int playerAmount = data.getBalance(resource);
            if (playerAmount < cost) {
                int needMore = cost - playerAmount;
                missing.add(StringUtils.betterNumberFormat(needMore) + " " + resource.getDisplayName());
            }
        }

        if (!missing.isEmpty()) {
            player.playSound(player.getLocation(), Sounds.ANVIL_BREAK.bukkitSound(), 10.0F, 10.0F);

            MessageManager.get(player, "chat.cosmetic.dont_have_enough")
                    .replace("%resources%", String.join(", ", missing))
                    .send();
            return;
        }

        data.purchaseCosmetic(this);


        List<String> costStringList = new ArrayList<>();
        for (Map.Entry<Resource, Integer> entry : cost.entrySet()) {
            Resource resource = entry.getKey();
            int cost = entry.getValue();

            costStringList.add(StringUtils.betterNumberFormat(cost) + " " + resource.getDisplayName());

            Bukkit.getScheduler().runTaskAsynchronously(Minigame.getInstance().getPlugin(), task -> {
                resource.getResourceInterface().withdraw(gamePlayer, cost);
            });
        }

        MessageManager.get(gamePlayer, "chat.cosmetics.purchase")
                .replace("%cosmetic%", getName())
                .replace("%resources%", "" + String.join(", ", costStringList))
                .send();


        //GameAPI.getInstance().getVaultPerms().playerAdd(player, getPermission());
        //gamePlayer.getOnlinePlayer().playSound(gamePlayer.getOnlinePlayer().getLocation(), Sounds.CLICK.bukkitSound(), 10.0F, 10.0F); - už u select
        select(gamePlayer);
        Bukkit.getScheduler().runTaskAsynchronously(Minigame.getInstance().getPlugin(), task -> gamePlayer.getPlayerData().saveCosmetics());
    }

    public boolean hasPurchased(GamePlayer gamePlayer){
        if (gamePlayer.getPlayerData().getPurchasedCosmetics() == null)
            return false;
        return gamePlayer.getPlayerData().getPurchasedCosmetics().contains(this);
    }

    public boolean hasSelected(GamePlayer gamePlayer){
        if (gamePlayer.getPlayerData().getSelectedCosmetics() == null)
            return false;
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

    public Cosmetic setAsPurchasable(){
        this.canBePurchased = true;
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

    public Cosmetic setPreviewConsumer(Consumer<GamePlayer> previewConsumer) {
        this.previewConsumer = previewConsumer;
        return this;
    }
}
