package cz.johnslovakia.gameapi.guis;

import cz.johnslovakia.gameapi.modules.game.map.GameMap;
import cz.johnslovakia.gameapi.modules.game.map.MapModule;
import cz.johnslovakia.gameapi.modules.messages.MessageModule;
import cz.johnslovakia.gameapi.users.GamePlayer;

import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.utils.ItemBuilder;
import me.zort.containr.Component;
import me.zort.containr.Element;
import me.zort.containr.GUI;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;

public class VotingInventory {

    private static ItemStack getEditedItem(GamePlayer gamePlayer, GameMap map){
        MapModule mapModule = gamePlayer.getGame().getModule(MapModule.class);
        MessageModule messageModule = ModuleManager.getModule(MessageModule.class);
        String name = map.getName().substring(0, 1).toUpperCase() + map.getName().substring(1);
        int votes = mapModule.getTotalPlayerVotes(gamePlayer);

        ItemBuilder item = new ItemBuilder((map.getIcon() != null ? map.getIcon() : new ItemStack(Material.MAP)), (map.getVotes() != 0 ? map.getVotes() : 1));
        item.setName((votes >= getPlayersFreeVotes(gamePlayer) ? "§c§l" + name : "§a§l" + name));
        item.hideAllFlags();
        item.removeLore();
        item.addLoreLine("");
        messageModule.get(gamePlayer, "inventory.map.votes")
                .replace("%votes%", "" + map.getVotes())
                .addToItemLore(item);
        if (map.getAuthors() != null) {
            messageModule.get(gamePlayer, "inventory.map.creators")
                    .replace("%creators%", map.getAuthors())
                    .addToItemLore(item);
        }
        if (!mapModule.isVoting()){
            item.addLoreLine("");
            messageModule.get(gamePlayer, "inventory.map.vote_ended")
                    .addToItemLore(item);
        }else if (votes < getPlayersFreeVotes(gamePlayer)) {
            item.addLoreLine("");
            if (getPlayersFreeVotes(gamePlayer) - votes > 1){
                messageModule.get(gamePlayer, "inventory.map.vote.more_votes")
                        .replace("%votes_left%", "" + (getPlayersFreeVotes(gamePlayer) - votes))
                        .addToItemLore(item);
            }else {
                messageModule.get(gamePlayer, "inventory.map.vote")
                        .addToItemLore(item);
            }
        }else{
            item.addLoreLine("");
            messageModule.get(gamePlayer, "inventory.map.voted")
                    .addToItemLore(item);
        }
        return item.toItemStack();
    }

    public static void openGUI(GamePlayer gamePlayer){
        GUI inventory = Component.gui()
                .title(net.kyori.adventure.text.Component.text("§f七七七七七七七七ㆽ").font(Key.key("jsplugins", "guis")))
                .rows(2)
                .prepare((gui, player) -> {
                    MessageModule messageModule = ModuleManager.getModule(MessageModule.class);

                    ItemBuilder close = new ItemBuilder(Material.ECHO_SHARD);
                    close.setCustomModelData(1017);
                    close.hideAllFlags();
                    close.setName(messageModule.get(player, "inventory.item.close")
                            .getTranslated());

                    ItemBuilder info = new ItemBuilder(Material.ECHO_SHARD);
                    info.setCustomModelData(1018);
                    info.hideAllFlags();
                    info.setName(messageModule.get(player, "inventory.info_item.voting_inventory.name")
                            .getTranslated());
                    info.setLore(messageModule.get(player, "inventory.info_item.voting_inventory.lore").getTranslated());

                    gui.appendElement(0, Component.element(close.toItemStack()).addClick(i -> {
                        gui.close(player);
                        player.playSound(player, Sound.UI_BUTTON_CLICK, 1F, 1F);
                    }).build());
                    gui.appendElement(8, Component.element(info.toItemStack()).build());

                    gui.setContainer(9, Component.staticContainer()
                            .size(9, 1)
                            .init(container -> {

                                List<GameMap> maps = gamePlayer.getGame().getModule(MapModule.class).getMaps();
                                Collections.shuffle(maps);

                                for (GameMap map : maps.subList(0, Math.min(7, maps.size()))){
                                    if (!map.isIngame()){
                                        continue;
                                    }

                                    Element element = Component.element(getEditedItem(gamePlayer, map)).addClick(i -> {
                                        map.voteForMap(player);
                                        openGUI(gamePlayer);
                                    }).build();

                                    container.appendElement(element);
                                }
                            }).build());



                }).build();
        inventory.open(gamePlayer.getOnlinePlayer());
    }


    private static int getPlayersFreeVotes(GamePlayer gamePlayer){
        int votes = 1;
        if (gamePlayer.getOnlinePlayer().hasPermission("vip.2votes")){
            votes = 2;
        }else if (gamePlayer.getOnlinePlayer().hasPermission("vip.3votes")){
            votes = 3;
        }
        return votes;
    }
}
