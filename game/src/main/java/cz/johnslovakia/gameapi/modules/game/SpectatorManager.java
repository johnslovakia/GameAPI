package cz.johnslovakia.gameapi.modules.game;

import cz.johnslovakia.gameapi.guis.ProfileInventory;
import cz.johnslovakia.gameapi.guis.QuestInventory;
import cz.johnslovakia.gameapi.guis.TeleporterInventory;
import cz.johnslovakia.gameapi.inventoryBuilder.InventoryBuilder;
import cz.johnslovakia.gameapi.inventoryBuilder.Item;
import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.serverManagement.DataManager;
import cz.johnslovakia.gameapi.rewards.unclaimed.UnclaimedRewardType;
import cz.johnslovakia.gameapi.rewards.unclaimed.UnclaimedRewardsModule;
import cz.johnslovakia.gameapi.users.PlayerManager;
import cz.johnslovakia.gameapi.utils.ItemBuilder;

import org.bukkit.Material;

public class SpectatorManager {

    private InventoryBuilder itemManager;
    private InventoryBuilder withTeamSelectorItemManager;

    public void loadItemManager(){
        GameService gameService = ModuleManager.getModule(GameService.class);
        UnclaimedRewardsModule unclaimedRewardsModule = ModuleManager.getModule(UnclaimedRewardsModule.class);

        InventoryBuilder im = new InventoryBuilder("Spectator");
        InventoryBuilder im2 = new InventoryBuilder("Spectator");

        if (gameService.getGames().size() > 1 || (DataManager.getInstance() != null)) {
            Item playAgain = new Item(new ItemBuilder(Material.PAPER).hideAllFlags().toItemStack(),
                    1, "item.play_again", event -> gameService.newArena(event.getPlayer(), false));
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

        Item playerMenu = new Item(new ItemBuilder(Material.ECHO_SHARD).setCustomModelData(1025).hideAllFlags().toItemStack(), 7, "item.player_menu", e -> ProfileInventory.openGUI(PlayerManager.getGamePlayer(e.getPlayer())));
        playerMenu.setBlinking(gamePlayer -> !unclaimedRewardsModule.getPlayerUnclaimedRewardsByType(gamePlayer, UnclaimedRewardType.DAILYMETER).isEmpty()
                || !unclaimedRewardsModule.getPlayerUnclaimedRewardsByType(gamePlayer, UnclaimedRewardType.LEVELUP).isEmpty()
                || !unclaimedRewardsModule.getPlayerUnclaimedRewardsByType(gamePlayer, UnclaimedRewardType.QUEST).isEmpty());
        playerMenu.setBlinkingItemCustomModelData(1026);

        Item quests = new Item(new ItemBuilder(Material.BOOK).hideAllFlags().toItemStack(), 6, "Item.quests", e -> QuestInventory.openGUI(PlayerManager.getGamePlayer(e.getPlayer())));
        quests.setBlinking(gamePlayer -> !unclaimedRewardsModule.getPlayerUnclaimedRewardsByType(gamePlayer, UnclaimedRewardType.QUEST).isEmpty());
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

    public InventoryBuilder getInventoryManager() {
        return itemManager;
    }

    public InventoryBuilder getWithTeamSelectorInventoryManager(){
        return withTeamSelectorItemManager;
    }
}