package cz.johnslovakia.gameapi.game;

import com.cryptomorin.xseries.XMaterial;
import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.game.kit.KitManager;
import cz.johnslovakia.gameapi.guis.ProfileInventory;
import cz.johnslovakia.gameapi.guis.QuestInventory;
import cz.johnslovakia.gameapi.guis.TeleporterInventory;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.serverManagement.DataManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerManager;
import cz.johnslovakia.gameapi.utils.Utils;
import cz.johnslovakia.gameapi.utils.ItemBuilder;
import cz.johnslovakia.gameapi.utils.inventoryBuilder.InventoryManager;
import cz.johnslovakia.gameapi.utils.inventoryBuilder.Item;
import cz.johnslovakia.gameapi.utils.rewards.unclaimed.UnclaimedReward;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;

import java.util.function.Consumer;

public class SpectatorManager {

    private InventoryManager itemManager;
    private InventoryManager withTeamSelectorItemManager;

    public void loadItemManager(){
        //TODO: co ty názvy?
        InventoryManager im = new InventoryManager("Spectator");
        InventoryManager im2 = new InventoryManager("Spectator");

        if (GameManager.getGames().size() > 1 || (DataManager.getInstance() != null)) {
            Item playAgain = new Item(new ItemBuilder(XMaterial.PAPER.parseMaterial()).hideAllFlags().toItemStack(),
                    1, "item.play_again", event -> GameManager.newArena(event.getPlayer(), false));
            im.registerItem(playAgain);
            im2.registerItem(playAgain);
        }

        /*Item teamSelector = new Item(new ItemBuilder(XMaterial.WHITE_WOOL.parseMaterial()).hideAllFlags().toItemStack(),
                3,
                "Item.team_selector",
                e -> Minigame.getInstance().getInventories().openTeamSelectorInventory(e.getPlayer()));

        Item settings = new Item(new ItemBuilder(XMaterial.COMPARATOR.parseMaterial()).hideAllFlags().toItemStack(),
                7,
                "item.settings",
                e -> Minigame.getInstance().getInventories().openSettingsInventory(e.getPlayer()));*/

        Item alivePlayers = new Item(new ItemBuilder(Material.COMPASS).hideAllFlags().toItemStack(),
                3,
                "item.teleporter",
                e -> TeleporterInventory.openGUI(PlayerManager.getGamePlayer(e.getPlayer())));
        //TODO: dodělat inventáře

        Item playerMenu = new Item(new ItemBuilder(Material.ECHO_SHARD).setCustomModelData(1025).hideAllFlags().toItemStack(), 7, "Item.player_menu", e -> ProfileInventory.openGUI(PlayerManager.getGamePlayer(e.getPlayer())));
        playerMenu.setBlinking(gamePlayer -> !gamePlayer.getPlayerData().getUnclaimedRewards(UnclaimedReward.Type.DAILYMETER).isEmpty()
                || !gamePlayer.getPlayerData().getUnclaimedRewards(UnclaimedReward.Type.LEVELUP).isEmpty()
                || !gamePlayer.getPlayerData().getUnclaimedRewards(UnclaimedReward.Type.QUEST).isEmpty());
        playerMenu.setBlinkingItemCustomModelData(1026);

        Item quests = new Item(new ItemBuilder(Material.BOOK).hideAllFlags().toItemStack(), 5, "Item.quests", e -> QuestInventory.openGUI(PlayerManager.getGamePlayer(e.getPlayer())));
        quests.setBlinking(gamePlayer -> !gamePlayer.getPlayerData().getUnclaimedRewards(UnclaimedReward.Type.QUEST).isEmpty());
        quests.setBlinkingItemCustomModelData(1010);


        im.setHoldItemSlot(4);
        //im.registerItem(settings);
        im.registerItem(alivePlayers, playerMenu, quests);
        itemManager = im;


        im2.setHoldItemSlot(4);
        //im2.registerItem(teamSelector);
        //im2.registerItem(settings);
        im2.registerItem(alivePlayers, playerMenu, quests);
        withTeamSelectorItemManager = im2;
    }

    public InventoryManager getInventoryManager() {
        return itemManager;
    }

    public InventoryManager getWithTeamSelectorInventoryManager(){
        return withTeamSelectorItemManager;
    }
}