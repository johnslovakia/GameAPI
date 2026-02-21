package cz.johnslovakia.gameapi.guis;

import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.messages.MessageModule;
import cz.johnslovakia.gameapi.modules.resources.Resource;
import cz.johnslovakia.gameapi.modules.resources.ResourcesModule;
import cz.johnslovakia.gameapi.users.PlayerIdentity;

import cz.johnslovakia.gameapi.utils.ItemBuilder;
import cz.johnslovakia.gameapi.utils.StringUtils;

import lombok.Getter;
import me.zort.containr.Component;
import me.zort.containr.Element;
import me.zort.containr.GUI;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.function.Consumer;

@Getter
public class ConfirmInventory {

    private final PlayerIdentity PlayerIdentity;
    private final Consumer<PlayerIdentity> confirmAction;
    private final Consumer<PlayerIdentity> cancelAction;

    private String description_key;
    private ItemStack item;
    private Map<Resource, Integer> cost;

    public ConfirmInventory(PlayerIdentity PlayerIdentity, String description_key, Consumer<PlayerIdentity> confirmAction, Consumer<PlayerIdentity> cancelAction) {
        this.PlayerIdentity = PlayerIdentity;
        this.description_key = description_key;
        this.confirmAction = confirmAction;
        this.cancelAction = cancelAction;
    }

    public ConfirmInventory(PlayerIdentity PlayerIdentity, ItemStack item, Resource resource, int price, Consumer<PlayerIdentity> confirmAction, Consumer<PlayerIdentity> cancelAction) {

        this.PlayerIdentity = PlayerIdentity;
        this.item = item;
        this.cost = Map.of(resource, price);
        this.confirmAction = confirmAction;
        this.cancelAction = cancelAction;
    }

    public ConfirmInventory(PlayerIdentity PlayerIdentity, ItemStack item, Map<Resource, Integer> cost, Consumer<PlayerIdentity> confirmAction, Consumer<PlayerIdentity> cancelAction) {
        this.PlayerIdentity = PlayerIdentity;
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
                    back.setName(ModuleManager.getModule(MessageModule.class).get(player, "inventory.item.go_back")
                            .getTranslated());

                    ItemBuilder info = new ItemBuilder(Material.ECHO_SHARD);
                    info.setCustomModelData(1018);
                    info.hideAllFlags();
                    info.setName(ModuleManager.getModule(MessageModule.class).get(player, "inventory.info_item.confirm_inventory.name")
                            .getTranslated());
                    info.setLore(ModuleManager.getModule(MessageModule.class).get(player, "inventory.info_item.confirm_inventory.lore").getTranslated());

                    gui.appendElement(0, Component.element(back.toItemStack()).addClick(i -> {
                        cancelAction.accept(PlayerIdentity);
                        player.playSound(player, Sound.UI_BUTTON_CLICK, 1F, 1F);
                    }).build());
                    gui.appendElement(8, Component.element(info.toItemStack()).build());



                    ItemBuilder confirm = new ItemBuilder(Material.MAP);
                    confirm.setCustomModelData(1010);
                    confirm.hideAllFlags();
                    confirm.setName(ModuleManager.getModule(MessageModule.class).get(player, "inventory.confirm_inventory.confirm.name")
                            .getTranslated());
                    confirm.addLoreLine("");
                    if (!purchaseThisItemInventory && description_key != null) {
                        ModuleManager.getModule(MessageModule.class).get(player, description_key)
                                .addToItemLore(confirm);
                        confirm.addLoreLine("");
                    }else{
                        if (cost.size() > 1) {
                            ModuleManager.getModule(MessageModule.class).get(PlayerIdentity, "inventory.confirm_inventory.price")
                                    .replace("%economy_name%", "")
                                    .replace("%price%", "")
                                    .replace("%balance%", "")
                                    .replace("_", "")
                                    .replace("/", "")
                                    .addToItemLore(confirm);
                            for (Map.Entry<Resource, Integer> entry : getCost().entrySet()) {
                                Resource resource = entry.getKey();
                                int cost = entry.getValue();
                                int balance = ModuleManager.getModule(ResourcesModule.class).getPlayerBalanceCached(PlayerIdentity, resource);

                                confirm.addLoreLine(" §f- " + (balance >= cost ? "§a" : "§c") + StringUtils.betterNumberFormat(balance) + "§8/§7" + StringUtils.betterNumberFormat(cost) + " " + resource.getColor() + resource.getDisplayName());
                            }
                        }else{
                            Map.Entry<Resource, Integer> entry = cost.entrySet().iterator().next();
                            Resource resource = entry.getKey();
                            int price = entry.getValue();
                            int balance = ModuleManager.getModule(ResourcesModule.class).getPlayerBalanceCached(PlayerIdentity, resource);

                            ModuleManager.getModule(MessageModule.class).get(PlayerIdentity, "inventory.confirm_inventory.price")
                                    .replace("%balance%", "§a" + StringUtils.betterNumberFormat(balance))
                                    .replace("%price%", StringUtils.betterNumberFormat(price))
                                    .replace("%economy_name%", resource.getDisplayName())
                                    .addToItemLore(confirm);
                        }
                    }
                    confirm.addLoreLine("");
                    ModuleManager.getModule(MessageModule.class).get(player, "inventory.confirm_inventory.click_to_confirm")
                            .addToItemLore(confirm);

                    ItemBuilder cancel = new ItemBuilder(Material.MAP);
                    cancel.setCustomModelData(1010);
                    cancel.hideAllFlags();
                    cancel.setName(ModuleManager.getModule(MessageModule.class).get(player, "inventory.confirm_inventory.cancel.name")
                            .getTranslated());
                    cancel.addLoreLine("");
                    if (!purchaseThisItemInventory && description_key != null) {
                        ModuleManager.getModule(MessageModule.class).get(player, description_key)
                                .addToItemLore(cancel);
                        cancel.addLoreLine("");
                    }
                    ModuleManager.getModule(MessageModule.class).get(player, "inventory.confirm_inventory.click_to_cancel")
                            .addToItemLore(cancel);

                    if (purchaseThisItemInventory){
                        gui.appendElement(4, Component.element(item).build());
                    }


                    gui.setContainer(!purchaseThisItemInventory ? 10 : 10/*28*/, Component.staticContainer()
                            .size(3, 1)
                            .init(container -> {
                                Element element = Component.element(confirm.toItemStack()).addClick(i -> {
                                    confirmAction.accept(PlayerIdentity);
                                }).build();
                                container.fillElement(element);
                            }).build());

                    gui.setContainer(!purchaseThisItemInventory ? 14 : 14 /*32*/, Component.staticContainer()
                            .size(3, 1)
                            .init(container -> {
                                Element element = Component.element(cancel.toItemStack()).addClick(i -> {
                                    cancelAction.accept(PlayerIdentity);
                                }).build();
                                container.fillElement(element);
                            }).build());

                }).build();
        inventory.open(PlayerIdentity.getOnlinePlayer());

    }
}
