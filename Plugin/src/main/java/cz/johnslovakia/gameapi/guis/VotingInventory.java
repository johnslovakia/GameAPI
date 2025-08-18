package cz.johnslovakia.gameapi.guis;

import cz.johnslovakia.gameapi.game.map.GameMap;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.utils.ItemBuilder;
import cz.johnslovakia.gameapi.utils.Sounds;
import me.zort.containr.Component;
import me.zort.containr.Element;
import me.zort.containr.GUI;
import net.kyori.adventure.key.Key;
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
        if (!gamePlayer.getGame().getMapManager().isVoting()){
            item.addLoreLine("");
            MessageManager.get(gamePlayer, "inventory.map.vote_ended")
                    .addToItemLore(item);
        }else if (gamePlayer.getPlayerData().getVotesForMaps().size() < getPlayersFreeVotes(gamePlayer)) {
            item.addLoreLine("");
            if (getPlayersFreeVotes(gamePlayer) - gamePlayer.getPlayerData().getVotesForMaps().size() > 1){
                MessageManager.get(gamePlayer, "inventory.map.vote.more_votes")
                        .replace("%votes_left%", "" + (getPlayersFreeVotes(gamePlayer) - gamePlayer.getPlayerData().getVotesForMaps().size()))
                        .addToItemLore(item);
            }else {
                MessageManager.get(gamePlayer, "inventory.map.vote")
                        .addToItemLore(item);
            }
        }else{
            item.addLoreLine("");
            MessageManager.get(gamePlayer, "inventory.map.voted")
                    .addToItemLore(item);
        }
        return item.toItemStack();
    }

    public static void openGUI(GamePlayer gamePlayer){
        GUI inventory = Component.gui()
                .title(net.kyori.adventure.text.Component.text("§f七七七七七七七七ㆽ").font(Key.key("jsplugins", "guis")))
                .rows(2)
                .prepare((gui, player) -> {
                    ItemBuilder close = new ItemBuilder(Material.ECHO_SHARD);
                    close.setCustomModelData(1017);
                    close.hideAllFlags();
                    close.setName(MessageManager.get(player, "inventory.item.close")
                            .getTranslated());

                    ItemBuilder info = new ItemBuilder(Material.ECHO_SHARD);
                    info.setCustomModelData(1018);
                    info.hideAllFlags();
                    info.setName(MessageManager.get(player, "inventory.info_item.voting_inventory.name")
                            .getTranslated());
                    info.setLore(MessageManager.get(player, "inventory.info_item.voting_inventory.lore").getTranslated());

                    gui.appendElement(0, Component.element(close.toItemStack()).addClick(i -> {
                        gui.close(player);
                        player.playSound(player, Sounds.CLICK.bukkitSound(), 1F, 1F);
                    }).build());
                    gui.appendElement(8, Component.element(info.toItemStack()).build());

                    gui.setContainer(9, Component.staticContainer()
                            .size(9, 1)
                            .init(container -> {

                                List<GameMap> maps = gamePlayer.getGame().getMapManager().getMaps();
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
