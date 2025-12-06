package cz.johnslovakia.gameapi.modules.kits;

import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.events.KitActiveEvent;
import cz.johnslovakia.gameapi.events.KitGiveContentEvent;
import cz.johnslovakia.gameapi.modules.messages.MessageModule;
import cz.johnslovakia.gameapi.events.KitSelectEvent;
import cz.johnslovakia.gameapi.modules.game.GameInstance;
import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.resources.Resource;
import cz.johnslovakia.gameapi.modules.resources.ResourcesModule;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.GamePlayerState;
import cz.johnslovakia.gameapi.users.PlayerData;
import cz.johnslovakia.gameapi.utils.GameUtils;
import cz.johnslovakia.gameapi.utils.StringUtils;

import lombok.Getter;
import lombok.Setter;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;

@Getter
public class Kit implements Listener{

    @Setter
    private KitManager kitManager;
    private final String name;
    private final ItemStack icon;
    private final int price;

    @Setter
    private KitContent content;

    private boolean giveAfterDead = false;

    public Kit(String name, ItemStack icon, int price) {
        this.name = name;
        this.icon = icon;
        this.icon.setAmount(1);
        this.price = price;
    }

    public void addItem(ItemStack... items){
        if (content == null) content = new KitContent();

        for (ItemStack item : items){
            content.addItem(item);
        }
    }

    public Kit setGiveAfterDead(boolean giveAfterDead) {
        this.giveAfterDead = giveAfterDead;

        return this;
    }

    public void activate(GamePlayer gamePlayer) {
        KitManager kitManager = KitManager.getKitManager(gamePlayer.getGame());
        ResourcesModule resourcesModule = ModuleManager.getModule(ResourcesModule.class);

        Player player = gamePlayer.getOnlinePlayer();
        Resource resource = kitManager.getResource();
        resourcesModule.getPlayerBalance(player, resource).thenAccept(balance -> {
            if (gamePlayer.getGameSession().getState().equals(GamePlayerState.DISCONNECTED)){
                return;
            }


            KitActiveEvent ev = new KitActiveEvent(gamePlayer, this);
            Bukkit.getPluginManager().callEvent(ev);
            if (ev.isCancelled()) return;

            giveContent(gamePlayer);


            if ((gamePlayer.getPlayerData().getPurchasedKitsThisGame() != null && gamePlayer.getPlayerData().getPurchasedKitsThisGame().contains(this))
                    || getPrice() == 0 || kitManager.getDefaultKit().equals(this)){
                return;
            }


            if (kitManager.hasKitPermission(gamePlayer, this)) {
                ModuleManager.getModule(MessageModule.class).get(player, "chat.kit.activated_vip")
                        .replace("%kit%", getName())
                        .replace("%saved%", StringUtils.betterNumberFormat(getPrice()))
                        .replace("%economy_name%", resource.getDisplayName())
                        .send();
            }else{
                ModuleManager.getModule(MessageModule.class).get(player, "chat.kit.activated")
                        .replace("%kit%", getName())
                        .replace("%price%", StringUtils.betterNumberFormat(getPrice()))
                        .replace("%economy_name%", resource.getDisplayName())
                        .send();
                ModuleManager.getModule(MessageModule.class).get(player, "chat.current_balance")
                        .replace("%kit%", getName())
                        .replace("%balance%", StringUtils.betterNumberFormat(balance))
                        .replace("%economy_name%", resource.getDisplayName())
                        .send();
                new BukkitRunnable(){
                    @Override
                    public void run() {
                        resourcesModule.withdraw(gamePlayer.getOnlinePlayer(), resource, getPrice());
                    }
                }.runTaskAsynchronously(Minigame.getInstance().getPlugin());
            }

            gamePlayer.getPlayerData().addPurchasedKitThisGame(this);
        });
    }

    public void unselect(GamePlayer gamePlayer) {
        unselect(gamePlayer, true);
    }

    public void unselect(GamePlayer gamePlayer, boolean message) {
        KitManager kitManager = KitManager.getKitManager(gamePlayer.getGame());
        gamePlayer.getGamePlayer().getGameSession().setSelectedKit(kitManager.getDefaultKit() != null ? kitManager.getDefaultKit() : null);

        if (message) {
            ModuleManager.getModule(MessageModule.class).get(gamePlayer, "chat.kit.unselected")
                    .replace("%kit%", getName())
                    .send();
        }
    }

    public void select(GamePlayer gamePlayer) {
        KitManager kitManager = KitManager.getKitManager(gamePlayer.getGame());

        Player player = gamePlayer.getOnlinePlayer();
        Resource resource = kitManager.getResource();

        ModuleManager.getModule(ResourcesModule.class).getPlayerBalance(player, resource).thenAccept(balance -> {
            if ((kitManager.getDefaultKit() != null && kitManager.getDefaultKit().equals(this)) || balance >= getPrice() || kitManager.hasKitPermission(gamePlayer, this) || (gamePlayer.getPlayerData().getPurchasedKitsThisGame() != null && gamePlayer.getPlayerData().getPurchasedKitsThisGame().contains(this))) {
                if (gamePlayer.hasKit() ){
                    if (gamePlayer.getGameSession().getSelectedKit().equals(this)){
                        if (!kitManager.getDefaultKit().equals(this))
                            ModuleManager.getModule(MessageModule.class).get(gamePlayer, "chat.kit.already_selected")
                                    .send();
                        return;
                    }
                }
                if (kitManager.getDefaultKit() != this) {
                    ModuleManager.getModule(MessageModule.class).get(player, "chat.kit.selected")
                            .replace("%kit%", getName())
                            .send();
                    if (!(player.hasPermission("kits.free") || getPrice() == 0 || kitManager.hasKitPermission(gamePlayer, this))) {
                        ModuleManager.getModule(MessageModule.class).get(player, "chat.kit.balance_deducted")
                                .replace("%economy_name%", "" + kitManager.getResource().getName())
                                .send();
                    }
                }

                gamePlayer.getGameSession().setSelectedKit(this);

                KitSelectEvent ev = new KitSelectEvent(gamePlayer, this);
                Bukkit.getPluginManager().callEvent(ev);
            } else {
                if (kitManager.isPurchaseKitForever()){
                    return;
                }
                ModuleManager.getModule(MessageModule.class).get(player, "chat.dont_have_enough")
                        .replace("%need_more%", "" + StringUtils.betterNumberFormat((getPrice() - balance)))
                        .replace("%economy_name%", resource.getDisplayName())
                        .send();
            }
        });
    }

    public void giveContent(GamePlayer gamePlayer){
        Player player = gamePlayer.getOnlinePlayer();
        PlayerData data = gamePlayer.getPlayerData();
        GameInstance game = gamePlayer.getGame();
        PlayerInventory playerInventory = player.getInventory();
        Inventory kitInventory = data.getKitInventory(this);
        if (game.isPreparation()) {
            playerInventory.clear();
            playerInventory.setContents(kitInventory.getContents());
        }else{
            for (ItemStack itemStack : kitInventory.getContents()) {
                if (itemStack != null && !itemStack.getType().equals(Material.AIR)) {
                    if (itemStack.getType().toString().toLowerCase().contains("helmet")){
                        if (playerInventory.getHelmet().getType() == Material.AIR) playerInventory.setHelmet(itemStack);
                    }else if (itemStack.getType().toString().toLowerCase().contains("chestplate")){
                        if (playerInventory.getChestplate().getType() == Material.AIR) playerInventory.setChestplate(itemStack);
                    }else if (itemStack.getType().toString().toLowerCase().contains("leggings")){
                        if (playerInventory.getLeggings().getType() == Material.AIR) playerInventory.setLeggings(itemStack);
                    }else if (itemStack.getType().toString().toLowerCase().contains("boots")){
                        if (playerInventory.getBoots().getType() == Material.AIR) playerInventory.setBoots(itemStack);
                    }else {
                        playerInventory.addItem(itemStack);
                    }
                }
            }
        }


        GameUtils.colorizeArmor(gamePlayer);

        KitGiveContentEvent ev = new KitGiveContentEvent(gamePlayer, this);
        Bukkit.getPluginManager().callEvent(ev);
    }

    public String getTranslationKey(){
        return "perk." + getName().toLowerCase().replace(" ", "_");
    }
}
