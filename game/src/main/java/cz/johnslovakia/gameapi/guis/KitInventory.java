package cz.johnslovakia.gameapi.guis;

import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.modules.game.GameInstance;
import cz.johnslovakia.gameapi.modules.game.GameState;
import cz.johnslovakia.gameapi.modules.kits.Kit;
import cz.johnslovakia.gameapi.modules.kits.KitManager;
import cz.johnslovakia.gameapi.modules.messages.MessageModule;
import cz.johnslovakia.gameapi.modules.resources.Resource;
import cz.johnslovakia.gameapi.modules.resources.ResourcesModule;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerData;

import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.utils.ItemBuilder;
import cz.johnslovakia.gameapi.utils.StringUtils;
import me.zort.containr.Component;
import me.zort.containr.Element;
import me.zort.containr.GUI;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class KitInventory implements Listener {

    public static void openKitInventory(GamePlayer gamePlayer) {
        KitManager kitManager = KitManager.getKitManager(gamePlayer.getGame());
        Resource resource = kitManager.getResource();

        int size = kitManager.getKits().size();
        char sizeChar = (size <= 9 ? 'ㆺ' : 'ẙ'/*'Ẕ'*/);

        ModuleManager.getModule(ResourcesModule.class).getPlayerBalance(gamePlayer, resource).thenAccept(balance -> {
            GUI inventory = Component.gui()
                    .title(net.kyori.adventure.text.Component.text("§f七七七七七七七七" + sizeChar).font(Key.key("jsplugins", "guis")))
                    .rows(size <= 9 ? 3 : 4)
                    .prepare((gui, player) -> {
                        PlayerData playerData = gamePlayer.getPlayerData();
                        GameInstance game = gamePlayer.getGame();
                        Kit selectedKit = gamePlayer.getGameSession().getSelectedKit();

                        ItemBuilder close = new ItemBuilder(Material.ECHO_SHARD);
                        close.setCustomModelData(1017);
                        close.hideAllFlags();
                        close.setName(ModuleManager.getModule(MessageModule.class).get(player, "inventory.item.close")
                                .getTranslated());

                        ItemBuilder info = new ItemBuilder(Material.ECHO_SHARD);
                        info.setCustomModelData(1018);
                        info.hideAllFlags();
                        info.setName(ModuleManager.getModule(MessageModule.class).get(player, "inventory.info_item.kit_inventory.name")
                                .getTranslated());
                        info.setLore(ModuleManager.getModule(MessageModule.class).get(player, "inventory.info_item.kit_inventory.lore").getTranslated());
                        if (kitManager.getGameMap() != null){
                            info.addLoreLine("");
                            info.addLoreLine("§8These kits are map-specific and not global.");
                        }


                        ItemBuilder reset = new ItemBuilder(Material.MAP);
                        reset.setCustomModelData(1010);
                        reset.hideAllFlags();
                        reset.setName(ModuleManager.getModule(MessageModule.class).get(player, "inventory.kit.reset")
                                .getTranslated());
                        reset.removeLore();
                        if (!game.getState().equals(GameState.INGAME)) {
                            ModuleManager.getModule(MessageModule.class).get(player, "inventory.kit.reset_lore")
                                    .addToItemLore(reset);
                        }else{
                            ModuleManager.getModule(MessageModule.class).get(player, "inventory.kit.cant_reset_lore")
                                    .addToItemLore(reset);
                        }



                        gui.appendElement(0, Component.element(close.toItemStack()).addClick(i -> {
                            gui.close(player);
                            player.playSound(player, Sound.UI_BUTTON_CLICK, 1F, 1F);
                        }).build());
                        gui.appendElement(8, Component.element(info.toItemStack()).build());

                        gui.setContainer((size <= 9 ? 20 : 29), Component.staticContainer()
                                .size(5, 1)
                                .init(container -> {
                                    Element spacerElement = Component.element(reset.toItemStack()).addClick(i -> {
                                        if(game.getState().equals(GameState.INGAME)) return;

                                        if (selectedKit != null) {
                                            selectedKit.unselect(gamePlayer, false);
                                            if (kitManager.getDefaultKit() != null){
                                                kitManager.getDefaultKit().select(gamePlayer);
                                            }
                                        }

                                        player.closeInventory();
                                        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_BREAK, 10.0F, 10.0F);
                                        ModuleManager.getModule(MessageModule.class).get(player, "chat.kit.reset")
                                                .send();
                                    }).build();
                                    container.fillElement(spacerElement);
                                }).build());

                        List<Kit> kits = kitManager.getKits();
                        kits.sort(Comparator.comparingInt((Kit kit) -> kit.equals(kitManager.getDefaultKit()) ? 0 : 1)
                                .thenComparing(Kit::getName));

                        gui.setContainer(9, Component.staticContainer()
                                .size(9, (size <= 9 ? 1 : 2))
                                .init(container -> {
                                    for (Kit kit : kits) {
                                        //if (kitManager.getDefaultKit() != null && kitManager.getDefaultKit().equals(kit))
                                        //continue;

                                        String translateKey = "kit." + kit.getName().toLowerCase().replace(" ", "_");

                                        ItemBuilder item = new ItemBuilder(kit.getIcon());
                                        item.removeEnchantment(Enchantment.INFINITY);
                                        item.setName((balance >= kit.getPrice() || kit.getPrice() == 0 || kitManager.hasKitPermission(gamePlayer, kit) ? "§a§l" + org.apache.commons.lang3.StringUtils.capitalize(kit.getName().replace("_", " ")) : "§c§l" + kit.getName()));
                                        item.removeLore();
                                        if (kitManager.getGameMap() != null) {
                                            item.addLoreLine(ModuleManager.getModule(MessageModule.class).get(gamePlayer, "inventory.kit.map")
                                                    .replace("%map%", org.apache.commons.lang3.StringUtils.capitalize(org.apache.commons.lang3.StringUtils.capitalize(kitManager.getGameMap().replace("_", " "))))
                                                    .getTranslated());
                                        }
                                        item.addLoreLine("");
                                        if (ModuleManager.getModule(MessageModule.class).existMessage(translateKey)) {
                                            ModuleManager.getModule(MessageModule.class).get(player, translateKey)
                                                    .addToItemLore(item);
                                            item.addLoreLine("");
                                        }else{
                                            if (kit.getContent() != null) {
                                                if (kit.getContent().getContents() != null) {
                                                    for (ItemStack kitItem : Arrays.stream(kit.getContent().getContents()).filter(Objects::nonNull).toList()) {
                                                        if (kitItem.getType().isAir()) continue;
                                                        item.addLoreLine("§7" + StringUtils.formatItemStackName(player, kitItem));
                                                    }
                                                    item.addLoreLine("");
                                                }
                                            }
                                        }


                                        if (kit.getPrice() > 0) {
                                            if (player.hasPermission("kits.free")) {
                                                ModuleManager.getModule(MessageModule.class).get(player, "inventory.kit.saved")
                                                        .replace("%price%", "" + StringUtils.betterNumberFormat(kit.getPrice()))
                                                        .replace("%economy_name%", resource.getDisplayName())
                                                        .addToItemLore(item);
                                            } else if ((gamePlayer.getGame().getSettings().isEnabledChangingKitAfterStart() && playerData.getPurchasedKitsThisGame().contains(kit))
                                                    || (kitManager.isPurchaseKitForever() && kitManager.hasKitPermission(gamePlayer, kit))) {
                                                ModuleManager.getModule(MessageModule.class).get(player, "inventory.kit.purchased_for")
                                                        .replace("%price%", StringUtils.betterNumberFormat(kit.getPrice()))
                                                        .replace("%economy_name%", resource.getDisplayName())
                                                        .addToItemLore(item);
                                            } else {
                                                ModuleManager.getModule(MessageModule.class).get(player, "inventory.kit.price")
                                                        .replace("%balance%", (balance >= kit.getPrice() ? "§a" : "§c") + StringUtils.betterNumberFormat(balance))
                                                        .replace("%price%", StringUtils.betterNumberFormat(kit.getPrice()))
                                                        .replace("%economy_name%", resource.getDisplayName())
                                                        .addToItemLore(item);
                                            }
                                            item.addLoreLine("");
                                        }


                                        if (selectedKit != null && selectedKit.equals(kit)) {
                                            item.addEnchant(Enchantment.INFINITY, 1);
                                            ModuleManager.getModule(MessageModule.class).get(player, "inventory.kit.selected")
                                                    .addToItemLore(item);
                                        } else if ((gamePlayer.getGame().getSettings().isEnabledChangingKitAfterStart() && playerData.getPurchasedKitsThisGame().contains(kit))
                                                || (kitManager.isPurchaseKitForever() && kitManager.hasKitPermission(gamePlayer, kit))
                                                || player.hasPermission("kits.free")) {
                                            ModuleManager.getModule(MessageModule.class).get(player, "inventory.kit.select")
                                                    .addToItemLore(item);
                                        } else {
                                            item.addLoreLine(((kit.getPrice() == 0 || kitManager.hasKitPermission(gamePlayer, kit) || balance >= kit.getPrice()) ? ModuleManager.getModule(MessageModule.class).get(player, "inventory.kit.purchase").getTranslated() : ModuleManager.getModule(MessageModule.class).get(player, "inventory.kit.dont_have_enough").replace("%economy_name%", resource.getDisplayName()).getTranslated()));
                                        }


                                        if (Minigame.getInstance().getSettings().isAllowDefaultKitSelection()) {
                                            if (playerData.getDefaultKit() == null) {
                                                if (kitManager.isPurchaseKitForever()) {
                                                    if (kitManager.hasKitPermission(gamePlayer, kit)) {
                                                        ModuleManager.getModule(MessageModule.class).get(player, "inventory.kit.default_kit.select")
                                                                .addToItemLore(item);
                                                    }
                                                } else {
                                                    ModuleManager.getModule(MessageModule.class).get(player, "inventory.kit.default_kit.select")
                                                            .addToItemLore(item);
                                                }
                                            } else if (playerData.getDefaultKit().equals(kit)) {
                                                ModuleManager.getModule(MessageModule.class).get(player, "inventory.kit.default_kit.selected")
                                                        .addToItemLore(item);
                                            } else {
                                                ModuleManager.getModule(MessageModule.class).get(player, "inventory.kit.default_kit.select")
                                                        .addToItemLore(item);
                                            }
                                        }

                                        ModuleManager.getModule(MessageModule.class).get(player, "inventory.kit.edit_inventory.action_info")
                                                .addToItemLore(item);

                                        item.hideAllFlags();


                                        Element spacerElement = Component.element(item.toItemStack()).addClick(i -> {
                                            if (i.getClickType().isLeftClick()) {
                                                if (game.getState() == GameState.STARTING || game.getState() == GameState.WAITING) {
                                                    kit.select(gamePlayer);
                                                } else {
                                                    kit.select(gamePlayer); //bylo false k tomu
                                                    if (selectedKit == kit) {
                                                        kit.activate(gamePlayer);
                                                    }
                                                }
                                                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 20.0F, 20.0F);
                                                player.closeInventory();
                                            } else {
                                                if (i.getClickType().isShiftClick()){
                                                    //KitInventoryEditor.setKitInventory(gamePlayer, kit);
                                                    KitInventoryEditor.setKitInventory(gamePlayer, kit);
                                                    return;
                                                }


                                                if (Minigame.getInstance().getSettings().isAllowDefaultKitSelection()) {
                                                    if (kitManager.isPurchaseKitForever()) {
                                                        if (!kitManager.hasKitPermission(gamePlayer, kit)) {
                                                            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_BREAK, 20.0F, 20.0F);
                                                            ModuleManager.getModule(MessageModule.class).get(player, "chat.kit.default_kit.must_have_purchased")
                                                                    .send();
                                                            player.closeInventory();
                                                            return;
                                                        }
                                                    }
                                                    gamePlayer.getPlayerData().setDefaultKit(kit);
                                                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 20.0F, 20.0F);
                                                    if (game.getState() == GameState.STARTING || game.getState() == GameState.WAITING) {
                                                        kit.select(gamePlayer);
                                                    } else {
                                                        kit.select(gamePlayer); //bylo false k tomu
                                                        if (selectedKit == kit) {
                                                            kit.activate(gamePlayer);
                                                        }
                                                    }
                                                    ModuleManager.getModule(MessageModule.class).get(player, "chat.kit.default_kit.select")
                                                            .replace("%kit%", kit.getName())
                                                            .send();
                                                    player.closeInventory();
                                                }
                                            }
                                        }).build();
                                        container.appendElement(spacerElement);
                                    }
                                }).build());
                    })
                    .build();

            inventory.open(gamePlayer.getOnlinePlayer());
        });
    }
}
