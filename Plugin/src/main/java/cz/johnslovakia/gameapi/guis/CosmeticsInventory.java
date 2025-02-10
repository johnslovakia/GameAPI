package cz.johnslovakia.gameapi.guis;

import com.cryptomorin.xseries.XEnchantment;
import com.cryptomorin.xseries.XMaterial;
import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.game.cosmetics.Cosmetic;
import cz.johnslovakia.gameapi.game.cosmetics.CosmeticRarityComparator;
import cz.johnslovakia.gameapi.game.cosmetics.CosmeticsCategory;
import cz.johnslovakia.gameapi.game.cosmetics.CosmeticsManager;
import cz.johnslovakia.gameapi.game.kit.KitManager;
import cz.johnslovakia.gameapi.game.perk.Perk;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerData;
import cz.johnslovakia.gameapi.users.PlayerManager;
import cz.johnslovakia.gameapi.users.quests.PlayerQuestData;
import cz.johnslovakia.gameapi.users.quests.Quest;
import cz.johnslovakia.gameapi.users.quests.QuestType;
import cz.johnslovakia.gameapi.users.resources.Resource;
import cz.johnslovakia.gameapi.utils.ItemBuilder;
import cz.johnslovakia.gameapi.utils.Sounds;
import cz.johnslovakia.gameapi.utils.StringUtils;
import cz.johnslovakia.gameapi.utils.Utils;
import cz.johnslovakia.gameapi.utils.rewards.RewardItem;
import me.zort.containr.Component;
import me.zort.containr.Element;
import me.zort.containr.GUI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class CosmeticsInventory implements Listener {

    private static ItemStack getEditedItem(GamePlayer gamePlayer, Cosmetic cosmetic){
        PlayerData data = gamePlayer.getPlayerData();
        CosmeticsManager manager = cosmetic.getCategory().getManager();
        int balance = data.getBalance(manager.getEconomy());

        ItemBuilder item = new ItemBuilder(cosmetic.getIcon()/*cosmetic.hasPlayer(gamePlayer) ? cosmetic.getIcon() : new ItemBuilder(Material.ECHO_SHARD).setCustomModelData(1021).toItemStack()*/);
        if (!cosmetic.hasPlayer(gamePlayer)){
            item.setName((balance >= cosmetic.getPrice() ? "§a" + cosmetic.getName() : "§c" + cosmetic.getName()));
        }else{
            item.setName("§a§l" + cosmetic.getName());
        }
        item.removeLore();
        if (MessageManager.get(gamePlayer, cosmetic.getRarity().getTranslateKey()).getTranslated().length() == 1){
            item.addLoreLine("§f" + MessageManager.get(gamePlayer, cosmetic.getRarity().getTranslateKey()).getTranslated());
        }
        item.addLoreLine("§8" + cosmetic.getCategory().getName());
        item.removeEnchantment(XEnchantment.DAMAGE_ALL.getEnchant());


        item.addLoreLine("");
        if (MessageManager.get(gamePlayer, cosmetic.getRarity().getTranslateKey()).getTranslated().length() > 1) {
            MessageManager.get(gamePlayer, "inventory.cosmetics.rarity")
                    .replace("%rarity%", cosmetic.getRarity().getColor() + MessageManager.get(gamePlayer, cosmetic.getRarity().getTranslateKey()).getTranslated())
                    .addToItemLore(item);
        }
        if (!cosmetic.hasPlayer(gamePlayer)) {
            MessageManager.get(gamePlayer, "inventory.cosmetics.price")
                    .replace("%balance%", (balance >= cosmetic.getPrice() ? "§a" : "§c") + StringUtils.betterNumberFormat(balance))
                    .replace("%price%", StringUtils.betterNumberFormat(cosmetic.getPrice()))
                    .replace("%economy_name%", manager.getEconomy().getName())
                    .addToItemLore(item);
        }else{
            if (gamePlayer.getOnlinePlayer().hasPermission("cosmetics.free")){
                MessageManager.get(gamePlayer, "inventory.kit.saved")
                        .replace("%price%", "" + cosmetic.getPrice())
                        .replace("%economy_name%", manager.getEconomy().getName())
                        .addToItemLore(item);
            }else{
                MessageManager.get(gamePlayer, "inventory.kit.purchased_for")
                        .replace("%price%", StringUtils.betterNumberFormat(cosmetic.getPrice()))
                        .replace("%economy_name%", manager.getEconomy().getName())
                        .addToItemLore(item);
            }
        }
        item.addLoreLine("");

        /*if (cosmetic.getLoreKey() != null && MessageManager.existMessage(cosmetic.getLoreKey())) {
            MessageManager.get(gamePlayer, cosmetic.getLoreKey())
                    .addToItemLore(item);
            item.addLoreLine("");
        }*/

        if (manager.getSelectedCosmetic(gamePlayer, cosmetic.getCategory()) != null &&
                manager.getSelectedCosmetic(gamePlayer, cosmetic.getCategory()).equals(cosmetic)) {
            item.addEnchant(XEnchantment.DAMAGE_ALL.getEnchant(), 1);
            MessageManager.get(gamePlayer, "inventory.cosmetics.selected")
                    .addToItemLore(item);
        } else if (cosmetic.hasPlayer(gamePlayer)) {
            MessageManager.get(gamePlayer, "inventory.cosmetics.select")
                    .addToItemLore(item);
        } else if (balance <= cosmetic.getPrice()) {
            MessageManager.get(gamePlayer, "inventory.cosmetics.dont_have_enough")
                    .replace("%economy_name%", manager.getEconomy().getName())
                    .addToItemLore(item);
        } else {
            MessageManager.get(gamePlayer, "inventory.cosmetics.purchase")
                    .addToItemLore(item);
        }

        item.hideAllFlags();

        if (cosmetic.getPreviewConsumer() != null){
            MessageManager.get(gamePlayer, "inventory.cosmetics.preview")
                    .addToItemLore(item);
        }

        return item.toItemStack();
    }

    public static void openGUI(GamePlayer gamePlayer){
        openCategory(gamePlayer, (gamePlayer.getMetadata().containsKey("last_opened_cosmetic_category") ? (CosmeticsCategory) gamePlayer.getMetadata().get("last_opened_cosmetic_category") : GameAPI.getInstance().getCosmeticsManager().getCategories().get(0)));
    }

    public static void openCategory(GamePlayer gamePlayer, CosmeticsCategory category){
        int balance = gamePlayer.getPlayerData().getBalance(category.getManager().getEconomy());

        char ch = switch (category.getName()) {
            case "Kill Messages" -> 'Ẑ';
            case "Kill Sounds" -> 'Ẏ';
            case "Kill Effects" -> 'ẏ';
            case "Projectile Trails" -> 'ẑ';
            case "Hats" -> 'ẗ';
            default -> '-';
        };


        GUI inventory = Component.gui()
                .title("§f七七七七七七七七" + ch)
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
                    info.setName(MessageManager.get(player, "inventory.info_item.cosmetics_inventory.name")
                            .getTranslated());
                    info.setLore(MessageManager.get(player, "inventory.info_item.cosmetics_inventory.lore").getTranslated());

                    gui.appendElement(0, Component.element(close.toItemStack()).addClick(i -> {
                        gui.close(player);
                        player.playSound(player, Sounds.CLICK.bukkitSound(), 1F, 1F);
                    }).build());
                    gui.appendElement(8, Component.element(info.toItemStack()).build());

                    gui.setContainer(9, Component.staticContainer()
                            .size(9, 1)
                            .init(container -> {
                                for (CosmeticsCategory category2 : category.getManager().getCategories()){
                                    ItemBuilder categoryItem = new ItemBuilder(category2.getIcon());
                                    categoryItem.setName("§a" + category2.getName());
                                    categoryItem.removeLore();
                                    if (category.equals(category2)) categoryItem.addEnchant(XEnchantment.DAMAGE_ALL.getEnchant(), 1);
                                    categoryItem.hideAllFlags();

                                    categoryItem.addLoreLine("");
                                    MessageManager.get(gamePlayer, "inventory.cosmetics.sorting").addToItemLore(categoryItem);
                                    categoryItem.addLoreLine("");
                                    MessageManager.get(player, "inventory.cosmetics.click_to_view")
                                            .addToItemLore(categoryItem);

                                    Element element = Component.element(categoryItem.toItemStack()).addClick(i -> {
                                        openCategory(gamePlayer, category2);
                                        player.playSound(player.getLocation(), Sounds.CLICK.bukkitSound(), 10.0F, 10.0F);
                                    }).build();

                                    container.appendElement(element);
                                }
                            }).build());

                    List<Cosmetic> cosmetics = category.getCosmetics();
                    if (cosmetics == null || cosmetics.isEmpty()) return;

                    /*Cosmetic selectedCosmetic = category.getSelectedCosmetic(gamePlayer);
                    if (selectedCosmetic != null) {
                        cosmetics.sort(Comparator.comparing((Cosmetic c) -> !c.equals(selectedCosmetic))
                                .thenComparing(new CosmeticRarityComparator()));
                    } else {
                        cosmetics.sort(new CosmeticRarityComparator());
                    }*/
                    //cosmetics.sort(new CosmeticRarityComparator());

                    cosmetics.sort(Comparator.comparing((Cosmetic c) -> c.hasPlayer(gamePlayer), Comparator.reverseOrder())
                            .thenComparing(new CosmeticRarityComparator()));

                    gui.setContainer(18, Component.staticContainer()
                            .size(9, 4)
                            .init(container -> {
                                for (Cosmetic cosmetic : cosmetics){
                                    Element element = Component.element(getEditedItem(gamePlayer, cosmetic)).addClick(i -> {
                                        if (i.getClickType().isLeftClick()) {
                                            if (cosmetic.hasSelected(gamePlayer)) {
                                                //player.closeInventory();
                                                player.playSound(player.getLocation(), Sounds.ANVIL_BREAK.bukkitSound(), 10.0F, 10.0F);
                                                MessageManager.get(player, "chat.cosmetics.already_selected")
                                                        .replace("%cosmetic", cosmetic.getName())
                                                        .send();
                                            } else if (cosmetic.hasPlayer(gamePlayer)) {
                                                player.playSound(player.getLocation(), Sounds.LEVEL_UP.bukkitSound(), 10.0F, 10.0F);
                                                cosmetic.select(gamePlayer);
                                            } else if (balance <= cosmetic.getPrice()) {
                                                player.closeInventory();
                                                player.playSound(player.getLocation(), Sounds.ANVIL_BREAK.bukkitSound(), 10.0F, 10.0F);
                                                MessageManager.get(player, "chat.dont_have_enough")
                                                        .replace("%need_more%", "" + (cosmetic.getPrice() - balance))
                                                        .replace("%economy_name%", category.getManager().getEconomy().getName())
                                                        .send();
                                                return;
                                            } else {
                                                player.closeInventory();
                                                player.playSound(player.getLocation(), Sounds.CLICK.bukkitSound(), 20.0F, 20.0F);
                                                cosmetic.purchase(gamePlayer);
                                            }
                                            openCategory(gamePlayer, category);
                                        }else if (i.getClickType().isRightClick() && cosmetic.getPreviewConsumer() != null){
                                            cosmetic.getPreviewConsumer().accept(gamePlayer);
                                            if (!cosmetic.hasPlayer(gamePlayer)){
                                                Objects.requireNonNull(i.getElement().item(player)).setType(cosmetic.getIcon().getType());
                                            }
                                        }
                                    }).build();

                                    container.appendElement(element);
                                }
                            }).build());

                }).build();
        inventory.open(gamePlayer.getOnlinePlayer());
        gamePlayer.getMetadata().put("last_opened_cosmetic_category", category);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);
        ItemStack item = event.getCurrentItem();

        if (event.getClickedInventory() == null || !player.getOpenInventory().getTitle().contains("ㆹ")){
            return;
        }
        if (item == null){
            return;
        }
        event.setCancelled(true);

        if (event.getSlot() == 45){
            player.playSound(player.getLocation(), Sounds.CLICK.bukkitSound(), 1F, 1F);
            TeleporterInventory.openGUI(gamePlayer);
        }
    }
}
