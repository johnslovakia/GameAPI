package cz.johnslovakia.gameapi.guis;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.game.kit.Kit;
import cz.johnslovakia.gameapi.game.kit.KitManager;
import cz.johnslovakia.gameapi.game.team.GameTeam;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerData;
import cz.johnslovakia.gameapi.utils.ItemBuilder;

import cz.johnslovakia.gameapi.utils.Sounds;
import me.zort.containr.Component;
import me.zort.containr.Element;
import me.zort.containr.GUI;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class TeleporterInventory {

    public static void openGUI(GamePlayer gamePlayer){
        PlayerData data = gamePlayer.getPlayerData();

        GUI inventory = Component.gui()
                .title("§f七七七七七七七七ㆿ")
                .rows(6)
                .prepare((gui, player) -> {
                    ItemBuilder close = new ItemBuilder(Material.ECHO_SHARD);
                    close.setCustomModelData(1017);
                    close.hideAllFlags();
                    close.setName(MessageManager.get(player, "inventory.item.close")
                            .getTranslated());

                    ItemBuilder info = new ItemBuilder(Material.ECHO_SHARD);
                    info.setCustomModelData(1018);
                    info.hideAllFlags();
                    info.setName(MessageManager.get(player, "inventory.info_item.teleporter.name")
                            .getTranslated());
                    info.setLore(MessageManager.get(player, "inventory.info_item.teleporter.lore").getTranslated());

                    gui.appendElement(0, Component.element(close.toItemStack()).addClick(i -> {
                        gui.close(player);
                        player.playSound(player, Sounds.CLICK.bukkitSound(), 1F, 1F);
                    }).build());
                    gui.appendElement(8, Component.element(info.toItemStack()).build());


                    gui.setContainer(9, Component.staticContainer()
                            .size(9, 5)
                            .init(container -> {
                                for (GamePlayer aliveGamePlayer : data.getGame().getPlayers()){
                                    Element element = Component.element(getAlivePlayerItemStack(gamePlayer, aliveGamePlayer)).addClick(i -> {
                                        if (i.getClickType().isLeftClick()) {
                                            player.teleport((!(aliveGamePlayer.isSpectator()) ? aliveGamePlayer.getOnlinePlayer().getLocation() : data.getGame().getCurrentMap().getSpectatorSpawn().getLocation()));
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
        GameTeam team = aliveGamePlayer.getPlayerData().getTeam();

        KitManager kitManager = KitManager.getKitManager(forGamePlayer.getPlayerData().getGame());

        ItemBuilder alivePlayerItem = new ItemBuilder(GameAPI.getInstance().getVersionSupport().getPlayerHead(aliveGamePlayer.getOnlinePlayer()));
        alivePlayerItem.hideAllFlags();
        alivePlayerItem.removeLore();
        alivePlayerItem.setName(alivePlayer.getName()).setName("§a§l" + alivePlayer.getName());

        if (team != null) {
            MessageManager.get(forGamePlayer, "inventory.teleporter.team")
                    .replace("%team%", team.getChatColor() + team.getName())
                    .addToItemLore(alivePlayerItem);
        }
        alivePlayerItem.addLoreLine("");
        MessageManager.get(forGamePlayer, "inventory.teleporter.health")
                .replace("%health%", "" + (int) alivePlayer.getHealth())
                .replace("%max_health%", "" + (int) alivePlayer.getHealthScale())
                .addToItemLore(alivePlayerItem);
        MessageManager.get(forGamePlayer, "inventory.teleporter.food")
                .replace("%food%", "" + alivePlayer.getFoodLevel())
                .addToItemLore(alivePlayerItem);
        alivePlayerItem.addLoreLine("");
        if (kitManager != null) {
            Kit kit = aliveGamePlayer.getPlayerData().getKit();

            MessageManager.get(forGamePlayer, "inventory.teleporter.kit")
                    .replace("%kit%", (kit != null ? kit.getName() : MessageManager.get(forGamePlayer, "word.none_kit").getTranslated()))
                    .addToItemLore(alivePlayerItem);
            alivePlayerItem.addLoreLine("");
        }
        MessageManager.get(forGamePlayer, "inventory.teleporter.left_click")
                .addToItemLore(alivePlayerItem);
        MessageManager.get(forGamePlayer, "inventory.teleporter.right_click")
                .addToItemLore(alivePlayerItem);

        return alivePlayerItem.toItemStack();
    }
}
