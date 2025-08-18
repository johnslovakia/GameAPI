package cz.johnslovakia.gameapi.guis;

import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerData;
import cz.johnslovakia.gameapi.users.resources.Resource;
import cz.johnslovakia.gameapi.utils.ItemBuilder;
import cz.johnslovakia.gameapi.utils.StringUtils;

import me.zort.containr.Component;
import me.zort.containr.Element;
import me.zort.containr.GUI;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

public class ConfirmInventory {

    private final GamePlayer gamePlayer;
    private final Consumer<GamePlayer> confirmAction;
    private final Consumer<GamePlayer> cancelAction;

    private String description_key;
    private ItemStack item;
    private Resource resource;
    private int price;

    public ConfirmInventory(GamePlayer gamePlayer, String description_key, Consumer<GamePlayer> confirmAction, Consumer<GamePlayer> cancelAction) {
        this.gamePlayer = gamePlayer;
        this.description_key = description_key;
        this.confirmAction = confirmAction;
        this.cancelAction = cancelAction;
    }

    public ConfirmInventory(GamePlayer gamePlayer, ItemStack item, Resource resource, int price, Consumer<GamePlayer> confirmAction, Consumer<GamePlayer> cancelAction) {
        this.gamePlayer = gamePlayer;
        this.item = item;
        this.resource = resource;
        this.price = price;
        this.confirmAction = confirmAction;
        this.cancelAction = cancelAction;
    }

    public void openGUI(){
        boolean purchaseThisItemInventory = item != null;


        GUI inventory = Component.gui()
                .title(net.kyori.adventure.text.Component.text("§f七七七七七七七七" + (purchaseThisItemInventory ? "" : "")).font(Key.key("jsplugins", "guis")))
                .rows(!purchaseThisItemInventory ? 3 : 5)
                .prepare((gui, player) -> {
                    ItemBuilder confirm = new ItemBuilder(Material.ECHO_SHARD);
                    confirm.setCustomModelData(1010);
                    confirm.hideAllFlags();
                    confirm.setName(" ");
                    if (!purchaseThisItemInventory) {
                        MessageManager.get(player, description_key)
                                .addToItemLore(confirm);
                    }else{
                        PlayerData data = gamePlayer.getPlayerData();
                        int balance = data.getBalance(resource);

                        MessageManager.get(gamePlayer, "inventory.confirm_inventory.price")
                                .replace("%balance%", "§a" + StringUtils.betterNumberFormat(balance))
                                .replace("%price%", StringUtils.betterNumberFormat(price))
                                .replace("%economy_name%", resource.getDisplayName())
                                .addToItemLore(confirm);
                    }
                    confirm.addLoreLine("");
                    MessageManager.get(player, "inventory.confirm_inventory.confirm")
                            .addToItemLore(confirm);

                    ItemBuilder cancel = new ItemBuilder(Material.ECHO_SHARD);
                    cancel.setCustomModelData(1010);
                    cancel.hideAllFlags();
                    cancel.setName(" ");
                    MessageManager.get(player, description_key)
                            .addToItemLore(cancel);
                    cancel.addLoreLine("");
                    MessageManager.get(player, "inventory.confirm_inventory.cancel")
                            .addToItemLore(cancel);

                    if (purchaseThisItemInventory){
                        gui.appendElement(10, Component.element(item).build());
                    }


                    gui.setContainer(!purchaseThisItemInventory ? 10 : 28, Component.staticContainer()
                            .size(3, 3)
                            .init(container -> {
                                Element element = Component.element(confirm.toItemStack()).addClick(i -> {
                                    confirmAction.accept(gamePlayer);
                                }).build();
                                container.appendElement(element);
                            }).build());

                    gui.setContainer(!purchaseThisItemInventory ? 14 : 32, Component.staticContainer()
                            .size(3, 3)
                            .init(container -> {
                                Element element = Component.element(cancel.toItemStack()).addClick(i -> {
                                    cancelAction.accept(gamePlayer);
                                }).build();
                                container.appendElement(element);
                            }).build());

                }).build();
        inventory.open(gamePlayer.getOnlinePlayer());

    }
}
