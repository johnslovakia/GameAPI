package cz.johnslovakia.gameapi.game.kit;

import cz.johnslovakia.gameapi.GameAPI;
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


        if (game.getState() != GameState.INGAME) {
            player.getInventory().clear();
        }

        player.getInventory().setContents(data.getKitInventories().get(this) != null ? data.getKitInventories().get(this).getContents() : getContent().getInventory().getContents());

        if (gamePlayer.getMetadata().get("edited_kit_inventory") != null){
            boolean edited = (boolean) gamePlayer.getMetadata().get("edited_kit_inventory");
            if (edited){
                gamePlayer.getPlayerData().setKitInventory(this, player.getInventory());
                Bukkit.getScheduler().runTaskAsynchronously(GameAPI.getInstance(), task -> data.saveKitInventories());
            }
            gamePlayer.getMetadata().remove("edited_kit_inventory");
        }

        Utils.colorizeArmor(gamePlayer);

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


        if (balance >= getPrice()|| kitManager.hasKitPermission(gamePlayer, this) || gamePlayer.getPlayerData().getPurchasedKitsThisGame().contains(this)) {
            if (gamePlayer.hasKit()){
                if (gamePlayer.getPlayerData().getKit().equals(this)){
                    MessageManager.get(gamePlayer, "chat.kit.already_selected")
                            .send();
                    return;
                }

                gamePlayer.getPlayerData().setKit(null);
            }
            if (kitManager.getDefaultKit() != this) {
                MessageManager.get(player, "chat.kit.selected")
                        .replace("%kit%", getName())
                        .send();
                gamePlayer.getPlayerData().setKit(this);
            }

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
                    player.getInventory().setContents(data.getKitInventories().get(kit) != null ? data.getKitInventories().get(kit).getContents() : getContent().getInventory().getContents());
                    Utils.colorizeArmor(gamePlayer);
                }
            }.runTaskLater(GameAPI.getInstance(), 2L);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof Player player) {
            GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);
            Game game = gamePlayer.getPlayerData().getGame();
            if (game.getRunningMainTask().getId().equalsIgnoreCase("PreparationTask")){
                gamePlayer.getMetadata().put("edited_kit_inventory", true);
            }
        }
    }

    public String getTranslationKey(){
        return "perk." + getName().toLowerCase().replace(" ", "_");
    }
}
