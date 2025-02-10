package cz.johnslovakia.gameapi.game.cosmetics.defaultCosmetics;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.events.GamePreparationEvent;
import cz.johnslovakia.gameapi.game.cosmetics.Cosmetic;
import cz.johnslovakia.gameapi.game.cosmetics.CosmeticRarity;
import cz.johnslovakia.gameapi.game.cosmetics.CosmeticsCategory;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerManager;
import cz.johnslovakia.gameapi.utils.ItemBuilder;
import cz.johnslovakia.gameapi.utils.Utils;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

public class HatsCategory extends CosmeticsCategory implements Listener {

    public HatsCategory() {
        super("Hats", new ItemBuilder(Material.CARVED_PUMPKIN)
                .setCustomModelData(1).toItemStack());

        FileConfiguration config = GameAPI.getInstance().getMinigame().getPlugin().getConfig();
        
        int LEGENDARY_PRICE = Utils.getPrice(config, "hats.legendary", 18000);
        int EPIC_PRICE = Utils.getPrice(config, "hats.epic", 14000);
        int RARE_PRICE = Utils.getPrice(config, "hats.rare", 10000);
        int UNCOMMON_PRICE = Utils.getPrice(config, "hats.uncommon", 8000);
        int COMMON_PRICE = Utils.getPrice(config, "hats.common", 6000);

        Cosmetic classic = new Cosmetic("Classic", new ItemBuilder(Material.CARVED_PUMPKIN)
                .setCustomModelData(1).toItemStack(), COMMON_PRICE, CosmeticRarity.COMMON)
                .setSelectConsumer(gamePlayer -> {
                    gamePlayer.getOnlinePlayer().getInventory().setHelmet(new ItemBuilder(Material.CARVED_PUMPKIN)
                            .setCustomModelData(1).toItemStack());
                });
        Cosmetic headphones = new Cosmetic("Headphones", new ItemBuilder(Material.CARVED_PUMPKIN)
                .setCustomModelData(2).toItemStack(), COMMON_PRICE, CosmeticRarity.COMMON)
                .setSelectConsumer(gamePlayer -> {
                    gamePlayer.getOnlinePlayer().getInventory().setHelmet(new ItemBuilder(Material.CARVED_PUMPKIN)
                            .setCustomModelData(2).toItemStack());
                });
        Cosmetic straw = new Cosmetic("Straw Hat", new ItemBuilder(Material.CARVED_PUMPKIN)
                .setCustomModelData(3).toItemStack(), UNCOMMON_PRICE, CosmeticRarity.UNCOMMON)
                .setSelectConsumer(gamePlayer -> {
                    gamePlayer.getOnlinePlayer().getInventory().setHelmet(new ItemBuilder(Material.CARVED_PUMPKIN)
                            .setCustomModelData(3).toItemStack());
                });
        Cosmetic miner = new Cosmetic("Miner's Helmet", new ItemBuilder(Material.CARVED_PUMPKIN)
                .setCustomModelData(4).toItemStack(), UNCOMMON_PRICE, CosmeticRarity.UNCOMMON)
                .setSelectConsumer(gamePlayer -> {
                    gamePlayer.getOnlinePlayer().getInventory().setHelmet(new ItemBuilder(Material.CARVED_PUMPKIN)
                            .setCustomModelData(4).toItemStack());
                });
        Cosmetic aureole = new Cosmetic("Aureole", new ItemBuilder(Material.CARVED_PUMPKIN)
                .setCustomModelData(5).toItemStack(), RARE_PRICE, CosmeticRarity.RARE)
                .setSelectConsumer(gamePlayer -> {
                    gamePlayer.getOnlinePlayer().getInventory().setHelmet(new ItemBuilder(Material.CARVED_PUMPKIN)
                            .setCustomModelData(5).toItemStack());
                });
        Cosmetic devilhorns = new Cosmetic("Devil Horns", new ItemBuilder(Material.CARVED_PUMPKIN)
                .setCustomModelData(6).toItemStack(), EPIC_PRICE, CosmeticRarity.EPIC)
                .setSelectConsumer(gamePlayer -> {
                    gamePlayer.getOnlinePlayer().getInventory().setHelmet(new ItemBuilder(Material.CARVED_PUMPKIN)
                            .setCustomModelData(6).toItemStack());
                });
        Cosmetic nosignal = new Cosmetic("No Signal TV", new ItemBuilder(Material.CARVED_PUMPKIN)
                .setCustomModelData(7).toItemStack(), LEGENDARY_PRICE, CosmeticRarity.LEGENDARY)
                .setSelectConsumer(gamePlayer -> {
                    gamePlayer.getOnlinePlayer().getInventory().setHelmet(new ItemBuilder(Material.CARVED_PUMPKIN)
                            .setCustomModelData(7).toItemStack());
                });
        Cosmetic viking = new Cosmetic("Viking's Helmet", new ItemBuilder(Material.CARVED_PUMPKIN)
                .setCustomModelData(8).toItemStack(), RARE_PRICE, CosmeticRarity.RARE)
                .setSelectConsumer(gamePlayer -> {
                    gamePlayer.getOnlinePlayer().getInventory().setHelmet(new ItemBuilder(Material.CARVED_PUMPKIN)
                            .setCustomModelData(8).toItemStack());
                });
        Cosmetic clown = new Cosmetic("Clown Hat", new ItemBuilder(Material.CARVED_PUMPKIN)
                .setCustomModelData(9).toItemStack(), RARE_PRICE, CosmeticRarity.RARE)
                .setSelectConsumer(gamePlayer -> {
                    gamePlayer.getOnlinePlayer().getInventory().setHelmet(new ItemBuilder(Material.CARVED_PUMPKIN)
                            .setCustomModelData(9).toItemStack());
                });
        Cosmetic santa = new Cosmetic("Santa Hat", new ItemBuilder(Material.CARVED_PUMPKIN)
                .setCustomModelData(10).toItemStack(), RARE_PRICE, CosmeticRarity.RARE)
                .setSelectConsumer(gamePlayer -> {
                    gamePlayer.getOnlinePlayer().getInventory().setHelmet(new ItemBuilder(Material.CARVED_PUMPKIN)
                            .setCustomModelData(10).toItemStack());
                });
        Cosmetic witch = new Cosmetic("Witch Hat", new ItemBuilder(Material.CARVED_PUMPKIN)
                .setCustomModelData(11).toItemStack(), EPIC_PRICE, CosmeticRarity.EPIC)
                .setSelectConsumer(gamePlayer -> {
                    gamePlayer.getOnlinePlayer().getInventory().setHelmet(new ItemBuilder(Material.CARVED_PUMPKIN)
                            .setCustomModelData(11).toItemStack());
                });
        Cosmetic pirate = new Cosmetic("Pirate Hat", new ItemBuilder(Material.CARVED_PUMPKIN)
                .setCustomModelData(12).toItemStack(), EPIC_PRICE, CosmeticRarity.EPIC)
                .setSelectConsumer(gamePlayer -> {
                    gamePlayer.getOnlinePlayer().getInventory().setHelmet(new ItemBuilder(Material.CARVED_PUMPKIN)
                            .setCustomModelData(12).toItemStack());
                });


        addCosmetic(classic, headphones, straw, miner, aureole, devilhorns, nosignal, viking, clown, santa, witch, pirate);


        Bukkit.getPluginManager().registerEvents(this, GameAPI.getInstance());
    }


    /*@EventHandler
    public void onEntityDeath(PlayerDeathEvent e) {
        //already in PVPListener.java
        e.getDrops().removeIf(drop -> drop.getType().equals(Material.CARVED_PUMPKIN) && drop.hasItemMeta() && drop.getItemMeta().hasCustomModelData());
    }*/

    @EventHandler
    public void onEntityDropItem(PlayerDropItemEvent e) {
        if (e.getItemDrop().getItemStack().getType().equals(Material.CARVED_PUMPKIN) && e.getItemDrop().getItemStack().hasItemMeta() && e.getItemDrop().getItemStack().getItemMeta().hasCustomModelData()){
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent e) {
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(e.getPlayer());
        Cosmetic selected = GameAPI.getInstance().getCosmeticsManager().getSelectedCosmetic(gamePlayer, this);
        if (selected != null){
            selected.getSelectConsumer().accept(gamePlayer);
        }
    }

    @EventHandler
    public void onGamePreparation(GamePreparationEvent e) {
        for (GamePlayer gamePlayer : e.getGame().getParticipants()) {
            Cosmetic selected = GameAPI.getInstance().getCosmeticsManager().getSelectedCosmetic(gamePlayer, this);
            if (selected != null) {
                selected.getSelectConsumer().accept(gamePlayer);
            }
        }
    }

    @EventHandler
    public void onItemClick(InventoryClickEvent e) {
        ItemStack currentItem = e.getCurrentItem();
        if (currentItem != null && currentItem.getType().equals(Material.CARVED_PUMPKIN) && currentItem.hasItemMeta() && currentItem.getItemMeta().hasCustomModelData()){
            e.setCancelled(true);
        }
        ItemStack cursor = e.getCursor();
        if (cursor != null && cursor.getType().equals(Material.CARVED_PUMPKIN) && cursor.hasItemMeta() && cursor.getItemMeta().hasCustomModelData()){
            e.setCancelled(true);
        }
    }
}
