package cz.johnslovakia.gameapi.game;

import com.cryptomorin.xseries.XMaterial;
import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerManager;
import cz.johnslovakia.gameapi.utils.GameUtil;
import cz.johnslovakia.gameapi.utils.ItemBuilder;
import cz.johnslovakia.gameapi.utils.inventoryBuilder.InventoryManager;
import cz.johnslovakia.gameapi.utils.inventoryBuilder.Item;
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

        if (GameManager.getGames().size() > 1) {
            Item playAgain = new Item(new ItemBuilder(XMaterial.PAPER.parseMaterial()).hideAllFlags().toItemStack(),
                    1, "inventory.play_again",
                    new Consumer<PlayerInteractEvent>() {
                        @Override
                        public void accept(PlayerInteractEvent e) {
                            GameManager.newArena(e.getPlayer(), false);
                        }
                    });
            im.registerItem(playAgain);
            im2.registerItem(playAgain);
        }

        /*Item teamSelector = new Item(new ItemBuilder(XMaterial.WHITE_WOOL.parseMaterial()).hideAllFlags().toItemStack(),
                3,
                "Item.team_selector",
                e -> GameAPI.getInstance().getMinigame().getInventories().openTeamSelectorInventory(e.getPlayer()));

        Item settings = new Item(new ItemBuilder(XMaterial.COMPARATOR.parseMaterial()).hideAllFlags().toItemStack(),
                7,
                "inventory.spectator.settings",
                e -> GameAPI.getInstance().getMinigame().getInventories().openSettingsInventory(e.getPlayer()));

        Item alivePlayers = new Item(new ItemBuilder(Material.COMPASS).hideAllFlags().toItemStack(),
                4,
                "inventory.spectator.teleporter",
                e -> GameAPI.getInstance().getMinigame().getInventories().openTeleporterInventory(e.getPlayer()));*/
        //TODO: dodělat inventáře


        im.setHoldItemSlot(4);
        //im.registerItem(settings);
        //im.registerItem(alivePlayers);
        itemManager = im;


        im2.setHoldItemSlot(4);
        //im2.registerItem(teamSelector);
        //im2.registerItem(settings);
        //im2.registerItem(alivePlayers);
        withTeamSelectorItemManager = im2;
    }


    //TODO: dát jinam
    public void viewPlayerInventory(Player player, Player target){
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);
        PlayerInventory pInv = target.getInventory();
        Inventory inv = Bukkit.createInventory(null, 54, "§7" + target.getName() + " §7inventory!");

        ItemStack gray = new ItemBuilder(XMaterial.GRAY_STAINED_GLASS.parseMaterial()).setName(" ").hideAllFlags().toItemStack();

        int invSlot = 9;
        for (int slot = 0; slot <= 26; slot++) {
            inv.setItem(slot, pInv.getItem(invSlot));
            invSlot++;
        }

        int invSlot2 = 0;
        for (int slot = 27; slot <= 35; slot++) {
            inv.setItem(slot, pInv.getItem(invSlot2));
            invSlot2++;
        }

        for (int slot = 36; slot <= 44; slot++){
            inv.setItem(slot, gray);
        }

        if (pInv.getHelmet() != null){
            inv.setItem(45, pInv.getHelmet());
        }
        if (pInv.getChestplate() != null){
            inv.setItem(46, pInv.getChestplate());
        }
        if (pInv.getLeggings() != null){
            inv.setItem(47, pInv.getLeggings());
        }
        if (pInv.getBoots() != null){
            inv.setItem(48, pInv.getBoots());
        }

        ItemBuilder inf = new ItemBuilder(Material.BOOK);
        inf.setName((PlayerManager.getGamePlayer(target).getPlayerData().getTeam() != null ? PlayerManager.getGamePlayer(target).getPlayerData().getTeam().getChatColor() : "§r§b") + target.getName());
        inf.setLore(MessageManager.get(player, "inventory.player_inventory.health")
                .replace("%health%", "" + (int) target.getHealth())
                .replace("%max_health%", "" + (int) GameAPI.getInstance().getVersionSupport().getMaxPlayerHealth(target)).getTranslated());
        MessageManager.get(player, "inventory.player_inventory.food")
                .replace("%food%", "" + target.getFoodLevel())
                .addToItemLore(inf);
        MessageManager.get(player, "inventory.player_inventory.experience")
                .replace("%experience%", "" + target.getLevel())
                .addToItemLore(inf);
        if (GameAPI.getInstance().getKitManager() != null) {
            MessageManager.get(player, "inventory.teleporter.kit")
                    .replace("%kit%", (PlayerManager.getGamePlayer(target).getPlayerData().getKit()) != null ? PlayerManager.getGamePlayer(target).getPlayerData().getKit().getName() : MessageManager.get(player, "word.none_kit").getTranslated())
                    .addToItemLore(inf);
        }

        inf.addLoreLine("");
        MessageManager.get(player, "inventory.player_inventory.effects")
                .addToItemLore(inf);
        for(PotionEffect effect : target.getPlayer().getActivePotionEffects()){
            inf.addLoreLine(" §7" + effect.getType().getName().toLowerCase() + " " + (effect.getAmplifier() + 1) + " (" + GameUtil.getDurationString(effect.getDuration() / 20) + "§7)");
        }

        inv.setItem(53, inf.toItemStack());

        player.openInventory(inv);
    }

    public InventoryManager getInventoryManager() {
        return itemManager;
    }

    public InventoryManager getWithTeamSelectorInventoryManager(){
        return withTeamSelectorItemManager;
    }
}