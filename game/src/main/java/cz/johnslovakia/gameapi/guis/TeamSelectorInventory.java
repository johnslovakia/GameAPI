package cz.johnslovakia.gameapi.guis;

import cz.johnslovakia.gameapi.modules.game.GameInstance;
import cz.johnslovakia.gameapi.modules.game.team.TeamModule;
import cz.johnslovakia.gameapi.modules.messages.MessageModule;
import cz.johnslovakia.gameapi.users.GamePlayer;

import cz.johnslovakia.gameapi.modules.game.team.GameTeam;
import cz.johnslovakia.gameapi.modules.game.team.TeamJoinCause;
import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.utils.ItemBuilder;
import cz.johnslovakia.gameapi.utils.StringUtils;

import me.zort.containr.Component;
import me.zort.containr.Element;
import me.zort.containr.GUI;

import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.event.Listener;

public class TeamSelectorInventory implements Listener {

    public static void openGUI(GamePlayer gamePlayer) {
        GUI inventory = Component.gui()
                .title(net.kyori.adventure.text.Component.text("§f七七七七七七七七ẝ").font(Key.key("jsplugins", "guis")))
                .rows(3)
                .prepare((gui, player) -> {
                    MessageModule messageModule = ModuleManager.getModule(MessageModule.class);
                    GameInstance game = gamePlayer.getGame();

                    ItemBuilder close = new ItemBuilder(Material.ECHO_SHARD);
                    close.setCustomModelData(1017);
                    close.hideAllFlags();
                    close.setName(messageModule.get(player, "inventory.item.close")
                            .getTranslated());

                    ItemBuilder info = new ItemBuilder(Material.ECHO_SHARD);
                    info.setCustomModelData(1018);
                    info.hideAllFlags();
                    info.setName(messageModule.get(player, "inventory.info_item.team_selector_inventory.name")
                            .getTranslated());
                    info.setLore(messageModule.get(player, "inventory.info_item.team_selector_inventory.lore").getTranslated());


                    ItemBuilder reset = new ItemBuilder(Material.MAP);
                    reset.setCustomModelData(1010);
                    reset.hideAllFlags();
                    reset.setName(messageModule.get(player, "inventory.kit.reset")
                            .getTranslated());
                    reset.removeLore();
                    messageModule.get(player, "inventory.kit.reset_lore")
                            .addToItemLore(reset);



                    gui.appendElement(0, Component.element(close.toItemStack()).addClick(i -> {
                        gui.close(player);
                        player.playSound(player, Sound.UI_BUTTON_CLICK, 1F, 1F);
                    }).build());
                    gui.appendElement(8, Component.element(info.toItemStack()).build());

                    gui.setContainer(9, Component.staticContainer()
                            .size(9, 2)
                            .init(container -> {
                                for (GameTeam team : game.getModule(TeamModule.class).getTeams().values()) {

                                    ItemBuilder item = new ItemBuilder((Material.valueOf(team.getDyeColor().toString().toUpperCase() + "_BANNER") != null ? Material.valueOf(team.getDyeColor().toString().toUpperCase() + "_BANNER") : Material.WHITE_BANNER), (!team.getAliveMembers().isEmpty() ? team.getAliveMembers().size() : 1));//.setWool(team.getDyeColor());
                                    item.toItemStack().setAmount((!team.getAliveMembers().isEmpty() ? team.getAliveMembers().size() : 1));
                                    item.setName("§l" + team.getChatColor() + StringUtils.colorizer(team.getName()));
                                    item.removeLore();
                                    item.addLoreLine("§f" + team.getAliveMembers().size() + "/" + game.getSettings().getMaxTeamPlayers());
                                    if (!team.getAliveMembers().isEmpty()) {
                                        item.addLoreLine("");
                                        for (GamePlayer teamPlayer : team.getAliveMembers()) {
                                            item.addLoreLine(team.getChatColor() + teamPlayer.getOnlinePlayer().getName());
                                        }
                                    }
                                    item.addLoreLine("");
                                    if (team.isMember(gamePlayer)) {
                                        messageModule.get(gamePlayer, "inventory.team.joined")
                                                .addToItemLore(item);
                                    } else if (team.isFull()) {
                                        messageModule.get(gamePlayer, "inventory.team.full")
                                                .addToItemLore(item);
                                    } else if (!team.isMember(gamePlayer)) {
                                        messageModule.get(gamePlayer, "inventory.team.join")
                                                .addToItemLore(item);
                                    }


                                    Element spacerElement = Component.element(item.toItemStack()).addClick(i -> {
                                        team.joinPlayer(gamePlayer, TeamJoinCause.INDIVIDUAL);
                                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 20.0F, 20.0F);
                                        if (game.getModule(TeamModule.class).getTeamAllowEnter(team)) {
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
