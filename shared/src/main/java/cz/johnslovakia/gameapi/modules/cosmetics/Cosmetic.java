package cz.johnslovakia.gameapi.modules.cosmetics;

import cz.johnslovakia.gameapi.Shared;
import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.messages.MessageModule;
import cz.johnslovakia.gameapi.modules.resources.Resource;
import cz.johnslovakia.gameapi.modules.resources.ResourcesModule;
import cz.johnslovakia.gameapi.users.PlayerIdentity;
import cz.johnslovakia.gameapi.users.PlayerIdentityRegistry;

import cz.johnslovakia.gameapi.utils.StringUtils;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

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

    private Consumer<PlayerIdentity> gamePlayerConsumer;
    private Consumer<Location> locationConsumer;
    private Consumer<PlayerIdentity> selectConsumer;
    private Consumer<PlayerIdentity> previewConsumer;

    public Cosmetic(String name, ItemStack icon, CosmeticRarity rarity) {
        this.name = name;
        this.icon = icon;
        this.rarity = rarity;
    }

    public Cosmetic addCost(Resource resource, int amount){
        cost.put(resource, amount);
        return this;
    }


    public void select(PlayerIdentity playerIdentity){
        select(playerIdentity, true);
    }

    public void select(PlayerIdentity playerIdentity, boolean message){
        if (!hasPlayer(playerIdentity)) return;
        Player player = playerIdentity.getOnlinePlayer();


        if (hasSelected(playerIdentity)) {
            if (message) {
                ModuleManager.getModule(MessageModule.class).get(player, "chat.cosmetics.already_selected")
                        .send();
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 10.0F, 10.0F);
            }
            return;
        }

        ModuleManager.getModule(CosmeticsModule.class).setPlayerSelectedCosmetic(playerIdentity, this);
        if (getSelectConsumer() != null) getSelectConsumer().accept(playerIdentity);

        if (message) {
            ModuleManager.getModule(MessageModule.class).get(player, "chat.cosmetics.select")
                    .replace("%cosmetic%", getName())
                    .send();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 10.0F, 10.0F);
        }
    }

    public void purchase(PlayerIdentity playerIdentity){
        if (hasPlayer(playerIdentity)) return;
        Player player = playerIdentity.getOnlinePlayer();

        List<String> missing = new ArrayList<>();

        for (Map.Entry<Resource, Integer> entry : cost.entrySet()) {
            Resource resource = entry.getKey();
            int cost = entry.getValue();

            ModuleManager.getModule(ResourcesModule.class).getPlayerBalance(player, resource).thenAccept(balance -> {
                if (balance < cost) {
                    int needMore = cost - balance;
                    missing.add(StringUtils.betterNumberFormat(needMore) + " " + resource.getDisplayName());
                }
            });
        }

        if (!missing.isEmpty()) {
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_BREAK, 10.0F, 10.0F);

            ModuleManager.getModule(MessageModule.class).get(player, "chat.cosmetic.dont_have_enough")
                    .replace("%resources%", String.join(", ", missing))
                    .send();
            return;
        }

        ModuleManager.getModule(CosmeticsModule.class).grantCosmeticToPlayer(playerIdentity, this);


        List<String> costStringList = new ArrayList<>();
        for (Map.Entry<Resource, Integer> entry : cost.entrySet()) {
            Resource resource = entry.getKey();
            int cost = entry.getValue();

            costStringList.add(StringUtils.betterNumberFormat(cost) + " " + resource.getDisplayName());

            Bukkit.getScheduler().runTaskAsynchronously(Shared.getInstance().getPlugin(), task -> {
                resource.getResourceInterface().withdraw(PlayerIdentityRegistry.get(player), cost);
            });
        }

        ModuleManager.getModule(MessageModule.class).get(player, "chat.cosmetics.purchase")
                .replace("%cosmetic%", getName())
                .replace("%resources%", "" + String.join(", ", costStringList))
                .send();


        //GameAPI.getInstance().getVaultPerms().playerAdd(player, getPermission());
        //gamePlayer.getOnlinePlayer().playSound(gamePlayer.getOnlinePlayer().getLocation(), Sound.UI_BUTTON_CLICK, 10.0F, 10.0F); - už u select
        select(playerIdentity);
        Bukkit.getScheduler().runTaskAsynchronously(Shared.getInstance().getPlugin(), task -> ModuleManager.getModule(CosmeticsModule.class).savePlayerCosmetics(playerIdentity));
    }

    public boolean hasPurchased(PlayerIdentity playerIdentity){
        List<Cosmetic> purchased = ModuleManager.getModule(CosmeticsModule.class).getPurchasedCosmetics().get(playerIdentity);
        if (purchased == null)
            return false;
        return purchased.contains(this);
    }

    public boolean hasSelected(PlayerIdentity playerIdentity){
        Map<CosmeticsCategory, Cosmetic> selectedCosmetics = ModuleManager.getModule(CosmeticsModule.class).getSelectedCosmetics().get(playerIdentity);
        if (selectedCosmetics == null)
            return false;
        return selectedCosmetics.containsValue(this);
    }

    public boolean hasPlayer(PlayerIdentity playerIdentity){
        return hasPurchased(playerIdentity)
                || hasSelected(playerIdentity)
                || playerIdentity.getOnlinePlayer().hasPermission("cosmetics.free");
    }

    public Cosmetic setGamePlayerConsumer(Consumer<PlayerIdentity> gamePlayerConsumer) {
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

    public Cosmetic setSelectConsumer(Consumer<PlayerIdentity> selectConsumer) {
        this.selectConsumer = selectConsumer;
        return this;
    }

    public Cosmetic setPreviewConsumer(Consumer<PlayerIdentity> previewConsumer) {
        this.previewConsumer = previewConsumer;
        return this;
    }
}
