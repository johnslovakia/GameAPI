package cz.johnslovakia.gameapi.guis;

import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerData;
import cz.johnslovakia.gameapi.users.resources.Resource;
import cz.johnslovakia.gameapi.utils.ItemBuilder;
import cz.johnslovakia.gameapi.utils.Sounds;
import cz.johnslovakia.gameapi.utils.StringUtils;

import lombok.Getter;
import me.zort.containr.Component;
import me.zort.containr.Element;
import me.zort.containr.GUI;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@Getter
public class ConfirmInventory {

    private final GamePlayer gamePlayer;
    private final Consumer<GamePlayer> confirmAction;
    private final Consumer<GamePlayer> cancelAction;

    private String description_key;
    private ItemStack item;
    private Map<Resource, Integer> cost;

    public ConfirmInventory(GamePlayer gamePlayer, String description_key, Consumer<GamePlayer> confirmAction, Consumer<GamePlayer> cancelAction) {
        this.gamePlayer = gamePlayer;
        this.description_key = description_key;
        this.confirmAction = confirmAction;
        this.cancelAction = cancelAction;
    }

    public ConfirmInventory(GamePlayer gamePlayer, ItemStack item, Resource resource, int price, Consumer<GamePlayer> confirmAction, Consumer<GamePlayer> cancelAction) {

        this.gamePlayer = gamePlayer;
        this.item = item;
        this.cost = Map.of(resource, price);
        this.confirmAction = confirmAction;
        this.cancelAction = cancelAction;
    }

    public ConfirmInventory(GamePlayer gamePlayer, ItemStack item, Map<Resource, Integer> cost, Consumer<GamePlayer> confirmAction, Consumer<GamePlayer> cancelAction) {
        this.gamePlayer = gamePlayer;
        this.item = item;
        this.cost = cost;
        this.confirmAction = confirmAction;
        this.cancelAction = cancelAction;
    }

    public void openGUI(){
        boolean purchaseThisItemInventory = item != null;


        GUI inventory = Component.gui()
                .title(net.kyori.adventure.text.Component.text("§f七七七七七七七七\uE001" + (purchaseThisItemInventory ? "" : "")).font(Key.key("jsplugins", "guis")))
                .rows(!purchaseThisItemInventory ? 2 : 2)
                .prepare((gui, player) -> {
                    ItemBuilder back = new ItemBuilder(Material.ECHO_SHARD);
                    back.setCustomModelData(1016);
                    back.hideAllFlags();
                    back.setName(MessageManager.get(player, "inventory.item.go_back")
                            .getTranslated());

                    ItemBuilder info = new ItemBuilder(Material.ECHO_SHARD);
                    info.setCustomModelData(1018);
                    info.hideAllFlags();
                    info.setName(MessageManager.get(player, "inventory.info_item.confirm_inventory.name")
                            .getTranslated());
                    info.setLore(MessageManager.get(player, "inventory.info_item.confirm_inventory.lore").getTranslated());

                    gui.appendElement(0, Component.element(back.toItemStack()).addClick(i -> {
                        cancelAction.accept(gamePlayer);
                        player.playSound(player, Sounds.CLICK.bukkitSound(), 1F, 1F);
                    }).build());
                    gui.appendElement(8, Component.element(info.toItemStack()).build());



                    ItemBuilder confirm = new ItemBuilder(Material.MAP);
                    confirm.setCustomModelData(1010);
                    confirm.hideAllFlags();
                    confirm.setName(MessageManager.get(player, "inventory.confirm_inventory.confirm.name")
                            .getTranslated());
                    confirm.addLoreLine("");
                    if (!purchaseThisItemInventory && description_key != null) {
                        MessageManager.get(player, description_key)
                                .addToItemLore(confirm);
                        confirm.addLoreLine("");
                    }else{
                        if (cost.size() > 1) {
                            MessageManager.get(gamePlayer, "inventory.confirm_inventory.price")
                                    .replace("%economy_name%", "")
                                    .replace("%price%", "")
                                    .replace("%balance%", "")
                                    .replace("_", "")
                                    .replace("/", "")
                                    .addToItemLore(confirm);
                            for (Map.Entry<Resource, Integer> entry : getCost().entrySet()) {
                                Resource resource = entry.getKey();
                                int cost = entry.getValue();
                                int balance = gamePlayer.getPlayerData().getBalance(resource);

                                confirm.addLoreLine(" §f- " + (balance >= cost ? "§a" : "§c") + StringUtils.betterNumberFormat(balance) + "§8/§7" + StringUtils.betterNumberFormat(cost) + " " + resource.getColor() + resource.getDisplayName());
                            }
                        }else{
                            PlayerData data = gamePlayer.getPlayerData();
                            Map.Entry<Resource, Integer> entry = cost.entrySet().iterator().next();
                            Resource resource = entry.getKey();
                            int price = entry.getValue();
                            int balance = data.getBalance(resource);

                            MessageManager.get(gamePlayer, "inventory.confirm_inventory.price")
                                    .replace("%balance%", "§a" + StringUtils.betterNumberFormat(balance))
                                    .replace("%price%", StringUtils.betterNumberFormat(price))
                                    .replace("%economy_name%", resource.getDisplayName())
                                    .addToItemLore(confirm);
                        }
                    }
                    confirm.addLoreLine("");
                    MessageManager.get(player, "inventory.confirm_inventory.click_to_confirm")
                            .addToItemLore(confirm);

                    ItemBuilder cancel = new ItemBuilder(Material.MAP);
                    cancel.setCustomModelData(1010);
                    cancel.hideAllFlags();
                    cancel.setName(MessageManager.get(player, "inventory.confirm_inventory.cancel.name")
                            .getTranslated());
                    cancel.addLoreLine("");
                    if (!purchaseThisItemInventory && description_key != null) {
                        MessageManager.get(player, description_key)
                                .addToItemLore(cancel);
                        cancel.addLoreLine("");
                    }
                    MessageManager.get(player, "inventory.confirm_inventory.click_to_cancel")
                            .addToItemLore(cancel);

                    if (purchaseThisItemInventory){
                        gui.appendElement(4, Component.element(item).build());
                    }


                    gui.setContainer(!purchaseThisItemInventory ? 10 : 10/*28*/, Component.staticContainer()
                            .size(3, 1)
                            .init(container -> {
                                Element element = Component.element(confirm.toItemStack()).addClick(i -> {
                                    confirmAction.accept(gamePlayer);
                                }).build();
                                container.fillElement(element);
                            }).build());

                    gui.setContainer(!purchaseThisItemInventory ? 14 : 14 /*32*/, Component.staticContainer()
                            .size(3, 1)
                            .init(container -> {
                                Element element = Component.element(cancel.toItemStack()).addClick(i -> {
                                    cancelAction.accept(gamePlayer);
                                }).build();
                                container.fillElement(element);
                            }).build());

                }).build();
        inventory.open(gamePlayer.getOnlinePlayer());

    }
}
