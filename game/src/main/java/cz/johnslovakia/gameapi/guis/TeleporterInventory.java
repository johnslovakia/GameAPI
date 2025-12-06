package cz.johnslovakia.gameapi.guis;

import cz.johnslovakia.gameapi.modules.kits.Kit;
import cz.johnslovakia.gameapi.modules.kits.KitManager;
import cz.johnslovakia.gameapi.users.GamePlayer;

import cz.johnslovakia.gameapi.modules.game.team.GameTeam;
import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.messages.MessageModule;
import cz.johnslovakia.gameapi.utils.ItemBuilder;

import cz.johnslovakia.gameapi.utils.Utils;
import me.zort.containr.Component;
import me.zort.containr.Element;
import me.zort.containr.GUI;

import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class TeleporterInventory {

    public static void openGUI(GamePlayer gamePlayer){
        GUI inventory = Component.gui()
                .title(net.kyori.adventure.text.Component.text("§f七七七七七七七七ㆿ").font(Key.key("jsplugins", "guis")))
                .rows(6)
                .prepare((gui, player) -> {
                    ItemBuilder close = new ItemBuilder(Material.ECHO_SHARD);
                    close.setCustomModelData(1017);
                    close.hideAllFlags();
                    close.setName(ModuleManager.getModule(MessageModule.class).get(player, "inventory.item.close")
                            .getTranslated());

                    ItemBuilder info = new ItemBuilder(Material.ECHO_SHARD);
                    info.setCustomModelData(1018);
                    info.hideAllFlags();
                    info.setName(ModuleManager.getModule(MessageModule.class).get(player, "inventory.info_item.teleporter.name")
                            .getTranslated());
                    info.setLore(ModuleManager.getModule(MessageModule.class).get(player, "inventory.info_item.teleporter.lore").getTranslated());

                    gui.appendElement(0, Component.element(close.toItemStack()).addClick(i -> {
                        gui.close(player);
                        player.playSound(player, Sound.UI_BUTTON_CLICK, 1F, 1F);
                    }).build());
                    gui.appendElement(8, Component.element(info.toItemStack()).build());


                    gui.setContainer(9, Component.staticContainer()
                            .size(9, 5)
                            .init(container -> {
                                for (GamePlayer aliveGamePlayer : gamePlayer.getGame().getPlayers()){
                                    Element element = Component.element(getAlivePlayerItemStack(gamePlayer, aliveGamePlayer)).addClick(i -> {
                                        if (i.getClickType().isLeftClick()) {
                                            player.teleport((!(aliveGamePlayer.isSpectator()) ? aliveGamePlayer.getOnlinePlayer().getLocation() : gamePlayer.getGame().getCurrentMap().getSpectatorSpawn().getLocation()));
                                        } else if (i.getClickType().isRightClick()){
                                            ViewPlayerInventory.openGUI(gamePlayer, aliveGamePlayer);
                                        }
                                    }).build();

                                    container.appendElement(element);
                                }
                            }).build());

                }).build();

        inventory.open(gamePlayer.getOnlinePlayer());
    }

    public static ItemStack getAlivePlayerItemStack(GamePlayer forGamePlayer, GamePlayer aliveGamePlayer){
        Player alivePlayer = aliveGamePlayer.getOnlinePlayer();
        GameTeam team = aliveGamePlayer.getGameSession().getTeam();

        KitManager kitManager = KitManager.getKitManager(forGamePlayer.getGame());

        ItemBuilder alivePlayerItem = new ItemBuilder(Utils.getPlayerHead(aliveGamePlayer.getOnlinePlayer()));
        alivePlayerItem.hideAllFlags();
        alivePlayerItem.removeLore();
        alivePlayerItem.setName(alivePlayer.getName()).setName("§a§l" + alivePlayer.getName());

        if (team != null) {
            ModuleManager.getModule(MessageModule.class).get(forGamePlayer, "inventory.teleporter.team")
                    .replace("%team%", team.getChatColor() + team.getName())
                    .addToItemLore(alivePlayerItem);
        }
        alivePlayerItem.addLoreLine("");
        ModuleManager.getModule(MessageModule.class).get(forGamePlayer, "inventory.teleporter.health")
                .replace("%health%", "" + (int) alivePlayer.getHealth())
                .replace("%max_health%", "" + (int) alivePlayer.getHealthScale())
                .addToItemLore(alivePlayerItem);
        ModuleManager.getModule(MessageModule.class).get(forGamePlayer, "inventory.teleporter.food")
                .replace("%food%", "" + alivePlayer.getFoodLevel())
                .addToItemLore(alivePlayerItem);
        alivePlayerItem.addLoreLine("");
        if (kitManager != null) {
            Kit kit = aliveGamePlayer.getGamePlayer().getGameSession().getSelectedKit();

            ModuleManager.getModule(MessageModule.class).get(forGamePlayer, "inventory.teleporter.kit")
                    .replace("%kit%", (kit != null ? net.kyori.adventure.text.Component.text(kit.getName()) : ModuleManager.getModule(MessageModule.class).get(forGamePlayer, "word.none_kit").getTranslated()))
                    .addToItemLore(alivePlayerItem);
            alivePlayerItem.addLoreLine("");
        }
        ModuleManager.getModule(MessageModule.class).get(forGamePlayer, "inventory.teleporter.left_click")
                .addToItemLore(alivePlayerItem);
        ModuleManager.getModule(MessageModule.class).get(forGamePlayer, "inventory.teleporter.right_click")
                .addToItemLore(alivePlayerItem);

        return alivePlayerItem.toItemStack();
    }
}
