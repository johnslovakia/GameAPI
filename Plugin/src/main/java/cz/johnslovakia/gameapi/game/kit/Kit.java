package cz.johnslovakia.gameapi.game.kit;

import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.events.KitActiveEvent;
import cz.johnslovakia.gameapi.events.KitGiveContentEvent;
import cz.johnslovakia.gameapi.users.resources.Resource;
import cz.johnslovakia.gameapi.events.KitSelectEvent;
import cz.johnslovakia.gameapi.game.Game;
import cz.johnslovakia.gameapi.game.GameState;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.GamePlayerType;
import cz.johnslovakia.gameapi.users.PlayerData;
import cz.johnslovakia.gameapi.utils.StringUtils;
import cz.johnslovakia.gameapi.utils.Utils;

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

import java.util.Arrays;
import java.util.Objects;

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

        Player player = gamePlayer.getOnlinePlayer();
        Resource resource = kitManager.getResource();
        PlayerData data = gamePlayer.getPlayerData();
        int balance = data.getBalance(resource);


        if (gamePlayer.getType().equals(GamePlayerType.DISCONNECTED)){
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
            MessageManager.get(player, "chat.kit.activated_vip")
                    .replace("%kit%", getName())
                    .replace("%saved%", StringUtils.betterNumberFormat(getPrice()))
                    .replace("%economy_name%", resource.getDisplayName())
                    .send();
        }else{
            MessageManager.get(player, "chat.kit.activated")
                    .replace("%kit%", getName())
                    .replace("%price%", StringUtils.betterNumberFormat(getPrice()))
                    .replace("%economy_name%", resource.getDisplayName())
                    .send();
            MessageManager.get(player, "chat.current_balance")
                    .replace("%kit%", getName())
                    .replace("%balance%", StringUtils.betterNumberFormat(balance))
                    .replace("%economy_name%", resource.getDisplayName())
                    .send();
            new BukkitRunnable(){
                @Override
                public void run() {
                    gamePlayer.getPlayerData().withdraw(resource, getPrice());
                }
            }.runTaskAsynchronously(Minigame.getInstance().getPlugin());
        }

        gamePlayer.getPlayerData().addPurchasedKitThisGame(this);
    }

    public void unselect(GamePlayer gamePlayer) {
        unselect(gamePlayer, true);
    }

    public void unselect(GamePlayer gamePlayer, boolean message) {
        KitManager kitManager = KitManager.getKitManager(gamePlayer.getGame());
        gamePlayer.setKit((kitManager.getDefaultKit() != null ? kitManager.getDefaultKit() : null));

        if (message) {
            MessageManager.get(gamePlayer, "chat.kit.unselected")
                    .replace("%kit%", getName())
                    .send();
        }
    }

    public void select(GamePlayer gamePlayer) {
        KitManager kitManager = KitManager.getKitManager(gamePlayer.getGame());

        Player player = gamePlayer.getOnlinePlayer();
        Resource resource = kitManager.getResource();
        int balance = gamePlayer.getPlayerData().getBalance(resource);


        if ((kitManager.getDefaultKit() != null && kitManager.getDefaultKit().equals(this)) || balance >= getPrice() || kitManager.hasKitPermission(gamePlayer, this) || (gamePlayer.getPlayerData().getPurchasedKitsThisGame() != null && gamePlayer.getPlayerData().getPurchasedKitsThisGame().contains(this))) {
            if (gamePlayer.hasKit() ){
                if (gamePlayer.getKit().equals(this)){
                    if (!kitManager.getDefaultKit().equals(this))
                        MessageManager.get(gamePlayer, "chat.kit.already_selected")
                            .send();
                    return;
                }
            }
            if (kitManager.getDefaultKit() != this) {
                MessageManager.get(player, "chat.kit.selected")
                        .replace("%kit%", getName())
                        .send();
                if (!(player.hasPermission("kits.free") || getPrice() == 0 || kitManager.hasKitPermission(gamePlayer, this))) {
                    MessageManager.get(player, "chat.kit.balance_deducted")
                            .replace("%economy_name%", kitManager.getResource().getName())
                            .send();
                }
            }

            gamePlayer.setKit(this);

            KitSelectEvent ev = new KitSelectEvent(gamePlayer, this);
            Bukkit.getPluginManager().callEvent(ev);
        } else {
            if (kitManager.isPurchaseKitForever()){
                return;
            }
            MessageManager.get(player, "chat.dont_have_enough")
                    .replace("%need_more%", "" + StringUtils.betterNumberFormat((getPrice() - balance)))
                    .replace("%economy_name%", resource.getDisplayName())
                    .send();
        }
    }

    public void giveContent(GamePlayer gamePlayer){
        Player player = gamePlayer.getOnlinePlayer();
        PlayerData data = gamePlayer.getPlayerData();
        Game game = gamePlayer.getGame();

        PlayerInventory playerInventory = player.getInventory();
        Inventory kitInventory = data.getKitInventory(this);
        if (game.isPreparation()) {
            playerInventory.clear();
            playerInventory.setContents(kitInventory.getContents());
        }else{
            for (ItemStack itemStack : kitInventory.getContents()) {
                if (itemStack != null && itemStack.getType() != Material.AIR) {
                    if (itemStack.getType().toString().toLowerCase().contains("helmet")){
                        if (playerInventory.getHelmet() == null) playerInventory.setItem(39, itemStack);
                    }else if (itemStack.getType().toString().toLowerCase().contains("chestplate")){
                        if (playerInventory.getChestplate() == null) playerInventory.setItem(38, itemStack);
                    }else if (itemStack.getType().toString().toLowerCase().contains("leggings")){
                        if (playerInventory.getLeggings() == null) playerInventory.setItem(37, itemStack);
                    }else if (itemStack.getType().toString().toLowerCase().contains("boots")){
                        if (playerInventory.getBoots() == null) playerInventory.setItem(36, itemStack);
                    }else {
                        playerInventory.addItem(itemStack);
                    }
                }
            }
        }


        Utils.colorizeArmor(gamePlayer);

        KitGiveContentEvent ev = new KitGiveContentEvent(gamePlayer, this);
        Bukkit.getPluginManager().callEvent(ev);
    }

    public String getTranslationKey(){
        return "perk." + getName().toLowerCase().replace(" ", "_");
    }
}
