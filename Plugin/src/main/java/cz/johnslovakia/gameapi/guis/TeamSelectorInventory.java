package cz.johnslovakia.gameapi.guis;

import com.cryptomorin.xseries.XMaterial;
import cz.johnslovakia.gameapi.game.Game;
import cz.johnslovakia.gameapi.game.team.GameTeam;
import cz.johnslovakia.gameapi.game.team.TeamJoinCause;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerData;
import cz.johnslovakia.gameapi.utils.ItemBuilder;
import cz.johnslovakia.gameapi.utils.Sounds;
import cz.johnslovakia.gameapi.utils.StringUtils;

import me.zort.containr.Component;
import me.zort.containr.Element;
import me.zort.containr.GUI;

import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.bukkit.event.Listener;

public class TeamSelectorInventory implements Listener {

    public static void openGUI(GamePlayer gamePlayer) {
        GUI inventory = Component.gui()
                .title(net.kyori.adventure.text.Component.text("§f七七七七七七七七ẝ").font(Key.key("jsplugins", "guis")))
                .rows(3)
                .prepare((gui, player) -> {
                    Game game = gamePlayer.getGame();

                    ItemBuilder close = new ItemBuilder(Material.ECHO_SHARD);
                    close.setCustomModelData(1017);
                    close.hideAllFlags();
                    close.setName(MessageManager.get(player, "inventory.item.close")
                            .getTranslated());

                    ItemBuilder info = new ItemBuilder(Material.ECHO_SHARD);
                    info.setCustomModelData(1018);
                    info.hideAllFlags();
                    info.setName(MessageManager.get(player, "inventory.info_item.team_selector_inventory.name")
                            .getTranslated());
                    info.setLore(MessageManager.get(player, "inventory.info_item.team_selector_inventory.lore").getTranslated());


                    ItemBuilder reset = new ItemBuilder(Material.MAP);
                    reset.setCustomModelData(1010);
                    reset.hideAllFlags();
                    reset.setName(MessageManager.get(player, "inventory.kit.reset")
                            .getTranslated());
                    reset.removeLore();
                    MessageManager.get(player, "inventory.kit.reset_lore")
                            .addToItemLore(reset);



                    gui.appendElement(0, Component.element(close.toItemStack()).addClick(i -> {
                        gui.close(player);
                        player.playSound(player, Sounds.CLICK.bukkitSound(), 1F, 1F);
                    }).build());
                    gui.appendElement(8, Component.element(info.toItemStack()).build());

                    gui.setContainer(9, Component.staticContainer()
                            .size(9, 2)
                            .init(container -> {
                                for (GameTeam team : game.getTeamManager().getTeams()) {

                                    ItemBuilder item = new ItemBuilder((XMaterial.valueOf(team.getDyeColor().toString().toUpperCase() + "_BANNER").parseItem() != null ? XMaterial.valueOf(team.getDyeColor().toString().toUpperCase() + "_BANNER").parseItem() : XMaterial.WHITE_BANNER.parseItem()), (!team.getMembers().isEmpty() ? team.getMembers().size() : 1));//.setWool(team.getDyeColor());
                                    item.toItemStack().setAmount((!team.getMembers().isEmpty() ? team.getMembers().size() : 1));
                                    item.setName("§l" + team.getChatColor() + StringUtils.colorizer(team.getName()));
                                    item.removeLore();
                                    item.addLoreLine("§f" + team.getMembers().size() + "/" + game.getSettings().getMaxTeamPlayers());
                                    if (!team.getMembers().isEmpty()) {
                                        item.addLoreLine("");
                                        for (GamePlayer teamPlayer : team.getMembers()) {
                                            item.addLoreLine(team.getChatColor() + teamPlayer.getOnlinePlayer().getName());
                                        }
                                    }
                                    item.addLoreLine("");
                                    if (team.isMember(gamePlayer)) {
                                        MessageManager.get(gamePlayer, "inventory.team.joined")
                                                .addToItemLore(item);
                                    } else if (team.isFull()) {
                                        MessageManager.get(gamePlayer, "inventory.team.full")
                                                .addToItemLore(item);
                                    } else if (!team.isMember(gamePlayer)) {
                                        MessageManager.get(gamePlayer, "inventory.team.join")
                                                .addToItemLore(item);
                                    }


                                    Element spacerElement = Component.element(item.toItemStack()).addClick(i -> {
                                        team.joinPlayer(gamePlayer, TeamJoinCause.INDIVIDUAL);
                                        player.playSound(player.getLocation(), Sounds.CLICK.bukkitSound(), 20.0F, 20.0F);
                                        if (game.getTeamManager().getTeamAllowEnter(team)) {
                                            player.closeInventory();
                                        }
                                    }).build();
                                    container.appendElement(spacerElement);
                                }
                            }).build());
                })
                .build();

        inventory.open(gamePlayer.getOnlinePlayer());
    }
}
