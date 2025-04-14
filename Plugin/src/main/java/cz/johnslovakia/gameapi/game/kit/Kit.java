package cz.johnslovakia.gameapi.game.kit;

import cz.johnslovakia.gameapi.GameAPI;
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
import cz.johnslovakia.gameapi.users.PlayerManager;
import cz.johnslovakia.gameapi.utils.Utils;

import lombok.Getter;
import lombok.Setter;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
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
        icon.setAmount(1);
        this.price = price;

        PluginManager pm = Bukkit.getServer().getPluginManager();
        pm.registerEvents(this, GameAPI.getInstance());
    }

    public void addItem(ItemStack... items){
        if (content == null) content = new KitContent();

        for (ItemStack item : items){
            content.addItem(item);
        }
    }

    public Kit setGiveAfterDead(boolean giveAfterDead) {
        this.giveAfterDead = giveAfterDead;
        if (giveAfterDead){
            PluginManager pm = Bukkit.getServer().getPluginManager();
            pm.registerEvents(this, GameAPI.getInstance());
        }

        return this;
    }

    public void activate(GamePlayer gamePlayer) {
        KitManager kitManager = KitManager.getKitManager(gamePlayer.getPlayerData().getGame());

        Player player = gamePlayer.getOnlinePlayer();
        Game game = gamePlayer.getPlayerData().getGame();
        Resource resource = kitManager.getResource();
        PlayerData data = gamePlayer.getPlayerData();
        int balance = data.getBalance(resource);


        giveContent(gamePlayer);

        if (kitManager.getDefaultKit() != this) {
            if (getPrice() != 0) {
                if (gamePlayer.getPlayerData().getPurchasedKitsThisGame().contains(this) || gamePlayer.getType().equals(GamePlayerType.DISCONNECTED)){
                    return;
                }

                if (!game.getSettings().isEnabledChangingKitAfterStart() && !gamePlayer.getPlayerData().getPurchasedKitsThisGame().contains(this)) {
                    if (kitManager.hasKitPermission(gamePlayer, this)) {
                        MessageManager.get(player, "chat.kit.activated_vip")
                                .replace("%kit%", getName())
                                .replace("%saved%", "" + getPrice())
                                .replace("%economy_name%", resource.getName())
                                .send();
                    }else{
                        MessageManager.get(player, "chat.kit.activated")
                                .replace("%kit%", getName())
                                .replace("%price%", "" + getPrice())
                                .replace("%economy_name%", resource.getName())
                                .send();
                        MessageManager.get(player, "chat.current_balance")
                                .replace("%kit%", getName())
                                .replace("%balance%", "" + balance)
                                .replace("%economy_name%", resource.getName())
                                .send();
                        new BukkitRunnable(){
                            @Override
                            public void run() {
                                gamePlayer.getPlayerData().withdraw(resource, getPrice());
                            }
                        }.runTaskAsynchronously(GameAPI.getInstance());
                    }
                }
            }
            gamePlayer.getPlayerData().addPurchasedKitThisGame(this);
        }

        KitActiveEvent ev = new KitActiveEvent(gamePlayer, this);
        Bukkit.getPluginManager().callEvent(ev);
    }

    public void unselect(GamePlayer gamePlayer) {
        unselect(gamePlayer, true);
    }

    public void unselect(GamePlayer gamePlayer, boolean message) {
        KitManager kitManager = KitManager.getKitManager(gamePlayer.getPlayerData().getGame());
        gamePlayer.getPlayerData().setKit((kitManager.getDefaultKit() != null ? kitManager.getDefaultKit() : null));

        if (message) {
            MessageManager.get(gamePlayer, "chat.kit.unselected")
                    .replace("%kit%", getName())
                    .send();
        }
    }

    public void select(GamePlayer gamePlayer) {
        KitManager kitManager = KitManager.getKitManager(gamePlayer.getPlayerData().getGame());

        Player player = gamePlayer.getOnlinePlayer();
        Resource resource = kitManager.getResource();
        int balance = gamePlayer.getPlayerData().getBalance(resource);


        if ((kitManager.getDefaultKit() != null && kitManager.getDefaultKit().equals(this)) || balance >= getPrice() || kitManager.hasKitPermission(gamePlayer, this) || gamePlayer.getPlayerData().getPurchasedKitsThisGame().contains(this)) {
            if (gamePlayer.hasKit() ){
                if (gamePlayer.getPlayerData().getKit().equals(this)){
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
            }

            gamePlayer.getPlayerData().setKit(this);

            KitSelectEvent ev = new KitSelectEvent(gamePlayer, this);
            Bukkit.getPluginManager().callEvent(ev);
        } else {
            if (kitManager.isPurchaseKitForever()){
                return;
            }
            MessageManager.get(player, "chat.dont_have_enough")
                    .replace("%need_more%", "" + (getPrice() - balance))
                    .replace("%economy_name%", resource.getName())
                    .send();
        }
    }

    public void giveContent(GamePlayer gamePlayer){
        Player player = gamePlayer.getOnlinePlayer();
        PlayerData data = gamePlayer.getPlayerData();
        Game game = gamePlayer.getPlayerData().getGame();

        if (game.getState() != GameState.INGAME) {
            player.getInventory().clear();
        }

        Inventory inventory = data.getKitInventory(this);
        player.getInventory().setContents(inventory.getContents());

        Utils.colorizeArmor(gamePlayer);

        KitGiveContentEvent ev = new KitGiveContentEvent(gamePlayer, this);
        Bukkit.getPluginManager().callEvent(ev);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player player = e.getPlayer();
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);
        PlayerData data = gamePlayer.getPlayerData();
        Game game = data.getGame();
        Kit kit = data.getKit();

        if (game.getState() != GameState.INGAME){
            return;
        }
        if (kit == null || !kit.equals(this)){
            return;
        }

        if (isGiveAfterDead()) {
            new BukkitRunnable(){
                @Override
                public void run() {
                    giveContent(gamePlayer);
                }
            }.runTaskLater(GameAPI.getInstance(), 2L);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof Player player) {
            GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);
            Game game = gamePlayer.getPlayerData().getGame();
            if (game == null)
                return;
            if (game.isPreparation()){
                gamePlayer.getMetadata().put("edited_kit_inventory", true);
            }
        }
    }

    public String getTranslationKey(){
        return "perk." + getName().toLowerCase().replace(" ", "_");
    }
}
