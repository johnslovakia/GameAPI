package cz.johnslovakia.gameapi.guis;

import cz.johnslovakia.gameapi.game.map.GameMap;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.utils.ItemBuilder;
import me.zort.containr.Component;
import me.zort.containr.Element;
import me.zort.containr.GUI;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class VotingInventory {

    private static ItemStack getEditedItem(GamePlayer gamePlayer, GameMap map){
        String name = map.getName().substring(0, 1).toUpperCase() + map.getName().substring(1);

        ItemBuilder item = new ItemBuilder((map.getIcon() != null ? map.getIcon() : new ItemStack(Material.MAP)), (map.getVotes() != 0 ? map.getVotes() : 1));
        item.setName((gamePlayer.getPlayerData().getVotesForMaps().size() >= getPlayersFreeVotes(gamePlayer) ? "§c§l" + name : "§a§l" + name));
        item.hideAllFlags();
        item.removeLore();
        item.addLoreLine("");
        MessageManager.get(gamePlayer, "inventory.map.votes")
                .replace("%votes%", "" + map.getVotes())
                .addToItemLore(item);
        if (map.getAuthors() != null) {
            MessageManager.get(gamePlayer, "inventory.map.creators")
                    .replace("%creators%", map.getAuthors())
                    .addToItemLore(item);
        }
        if (!gamePlayer.getPlayerData().getGame().getMapManager().isVoting()){
            item.addLoreLine("");
            MessageManager.get(gamePlayer, "inventory.map.vote_ended")
                    .addToItemLore(item);
        }else if (gamePlayer.getPlayerData().getVotesForMaps().size() < getPlayersFreeVotes(gamePlayer)) {
            item.addLoreLine("");
            MessageManager.get(gamePlayer, "inventory.map.vote")
                    .addToItemLore(item);
        }else{
            item.addLoreLine("");
            MessageManager.get(gamePlayer, "inventory.map.voted")
                    .addToItemLore(item);
        }
        return item.toItemStack();
    }

    public static void openGUI(GamePlayer gamePlayer){
        GUI inventory = Component.gui()
                .title("Perks")
                .rows(2)
                .prepare((gui, player) -> {
                    ItemBuilder close = new ItemBuilder(Material.MAP);
                    close.setCustomModelData(1010);
                    close.hideAllFlags();
                    close.setName(MessageManager.get(player, "inventory.item.close")
                            .getTranslated());

                    ItemBuilder info = new ItemBuilder(Material.MAP);
                    info.setCustomModelData(1010);
                    info.hideAllFlags();
                    info.setName(MessageManager.get(player, "inventory.info_item.voting_inventory.name")
                            .getTranslated());
                    info.setLore(MessageManager.get(player, "inventory.info_item.voting_inventory.lore").getTranslated());

                    gui.appendElement(0, Component.element(close.toItemStack()).addClick(i -> gui.close(player)).build());
                    gui.appendElement(8, Component.element(info.toItemStack()).build());

                    gui.setContainer(10, Component.staticContainer()
                            .size(7, 1)
                            .init(container -> {

                                List<GameMap> maps = gamePlayer.getPlayerData().getGame().getMapManager().getMaps();
                                Collections.shuffle(maps);

                                for (GameMap map : maps.subList(0, Math.min(7, maps.size()))){
                                    if (!map.isIngame()){
                                        continue;
                                    }

                                    Element element = Component.element(getEditedItem(gamePlayer, map)).addClick(i -> {
                                        map.voteForMap(player);
                                        if (getPlayersFreeVotes(gamePlayer) == 0) {
                                            player.closeInventory();
                                        }
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
