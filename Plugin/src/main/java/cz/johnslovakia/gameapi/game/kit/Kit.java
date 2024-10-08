package cz.johnslovakia.gameapi.game.kit;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.economy.Economy;
import cz.johnslovakia.gameapi.events.KitSelectEvent;
import cz.johnslovakia.gameapi.game.Game;
import cz.johnslovakia.gameapi.game.GameState;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.GamePlayerType;
import cz.johnslovakia.gameapi.users.PlayerData;
import cz.johnslovakia.gameapi.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public interface Kit {

    KitManager getKitManager();
    String getName();
    int getPrice();
    ItemStack getIcon();
    KitContent getContent();

    default void activate(GamePlayer gamePlayer) {
        KitManager kitManager = getKitManager();

        Player player = gamePlayer.getOnlinePlayer();
        Game game = gamePlayer.getPlayerData().getGame();
        Economy economy = kitManager.getEconomy();
        PlayerData data = gamePlayer.getPlayerData();
        int balance = data.getBalance(economy);


        if (game.getState() != GameState.INGAME) {
            player.getInventory().clear();
        }

        player.getInventory().setContents(data.getKitInventories().get(this) != null ? data.getKitInventories().get(this).getContents() : getContent().getInventory().getContents());
        player.getInventory().setArmorContents(getContent().getArmor().toArray(new ItemStack[0]));
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
                                .replace("%economy_name%", economy.getName())
                                .send();
                    }else{
                        MessageManager.get(player, "chat.kit.activated")
                                .replace("%kit%", getName())
                                .replace("%price%", "" + getPrice())
                                .replace("%economy_name%", economy.getName())
                                .send();
                        MessageManager.get(player, "chat.current_balance")
                                .replace("%kit%", getName())
                                .replace("%balance%", "" + balance)
                                .replace("%economy_name%", economy.getName())
                                .send();
                        new BukkitRunnable(){
                            @Override
                            public void run() {
                                gamePlayer.getPlayerData().withdraw(economy, getPrice());
                            }
                        }.runTaskAsynchronously(GameAPI.getInstance());
                    }
                }
            }
            gamePlayer.getPlayerData().addPurchasedKitThisGame(this);
        }
    }

    default void unselect(GamePlayer gamePlayer) {
        gamePlayer.getPlayerData().setKit((getKitManager().getDefaultKit() != null ? getKitManager().getDefaultKit() : null));

        MessageManager.get(gamePlayer, "kit.unselected")
                .send();
    }

    default void select(GamePlayer gamePlayer) {
        KitManager kitManager = getKitManager();

        Player player = gamePlayer.getOnlinePlayer();
        Economy economy = kitManager.getEconomy();
        int balance = gamePlayer.getPlayerData().getBalance(economy);


        if (balance >= getPrice()|| kitManager.hasKitPermission(gamePlayer, this) || gamePlayer.getPlayerData().getPurchasedKitsThisGame().contains(this)) {
            if (gamePlayer.hasKit()){
                gamePlayer.getPlayerData().setKit(null);
            }
            if (kitManager.getDefaultKit() != this) {
                MessageManager.get(player, "kit.selected")
                        .replace("%kit%", getName())
                        .send();
            }

            KitSelectEvent ev = new KitSelectEvent(gamePlayer, this);
            Bukkit.getPluginManager().callEvent(ev);
        } else {
            if (getKitManager().isPurchaseKitForever()){
                return;
            }
            MessageManager.get(player, "kit.dont_have_enough")
                    .replace("%need_more%", "" + (getPrice() - balance))
                    .replace("%economy_name%", economy.getName())
                    .send();
        }
    }
}
