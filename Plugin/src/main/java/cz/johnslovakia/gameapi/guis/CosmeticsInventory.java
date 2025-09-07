package cz.johnslovakia.gameapi.guis;

import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.game.cosmetics.Cosmetic;
import cz.johnslovakia.gameapi.game.cosmetics.CosmeticRarityComparator;
import cz.johnslovakia.gameapi.game.cosmetics.CosmeticsCategory;
import cz.johnslovakia.gameapi.game.cosmetics.CosmeticsManager;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerData;
import cz.johnslovakia.gameapi.users.PlayerManager;
import cz.johnslovakia.gameapi.users.resources.Resource;
import cz.johnslovakia.gameapi.utils.ItemBuilder;
import cz.johnslovakia.gameapi.utils.Sounds;
import cz.johnslovakia.gameapi.utils.StringUtils;

import cz.johnslovakia.gameapi.utils.rewards.RewardItem;
import me.zort.containr.Component;
import me.zort.containr.Element;
import me.zort.containr.GUI;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.function.Consumer;

public class CosmeticsInventory implements Listener {

    private static ItemStack getEditedItem(GamePlayer gamePlayer, Cosmetic cosmetic){
        PlayerData data = gamePlayer.getPlayerData();
        CosmeticsManager manager = cosmetic.getCategory().getManager();
        
        boolean hasEnough = true;

        for (Map.Entry<Resource, Integer> entry : cosmetic.getCost().entrySet()) {
            Resource resource = entry.getKey();
            int cost = entry.getValue();
            int playerAmount = data.getBalance(resource);

            if (playerAmount < cost) {
                hasEnough = false;
                break;
            }
        }

        ItemBuilder item = new ItemBuilder(cosmetic.getIcon()/*cosmetic.hasPlayer(gamePlayer) ? cosmetic.getIcon() : new ItemBuilder(Material.ECHO_SHARD).setCustomModelData(1021).toItemStack()*/);
        if (!cosmetic.hasPlayer(gamePlayer)){
            item.setName((hasEnough ? "§a" + cosmetic.getName() : "§c" + cosmetic.getName()));
        }else{
            item.setName("§a§l" + cosmetic.getName());
        }
        item.removeLore();
        if (PlainTextComponentSerializer.plainText().serialize(MessageManager.get(gamePlayer, cosmetic.getRarity().getTranslateKey()).getTranslated()).length() == 1){
            item.addLoreLine(net.kyori.adventure.text.Component.text("§7").append(MessageManager.get(gamePlayer, cosmetic.getRarity().getTranslateKey()).getTranslated()));
        }
        item.addLoreLine("§8" + cosmetic.getCategory().getName());
        item.removeEnchantment(Enchantment.SHARPNESS);


        item.addLoreLine("");
        if (PlainTextComponentSerializer.plainText().serialize(MessageManager.get(gamePlayer, cosmetic.getRarity().getTranslateKey()).getTranslated()).length() > 1) {
            MessageManager.get(gamePlayer, "inventory.cosmetics.rarity")
                    .replace("%rarity%", MessageManager.get(gamePlayer, cosmetic.getRarity().getTranslateKey()).getTranslated())
                    .addToItemLore(item);
        }
        if (!cosmetic.hasPlayer(gamePlayer)) {
            MessageManager.get(gamePlayer, "inventory.cosmetics.cost").addToItemLore(item);
            for (Map.Entry<Resource, Integer> entry : cosmetic.getCost().entrySet()) {
                Resource resource = entry.getKey();
                int cost = entry.getValue();
                int balance = data.getBalance(resource);

                item.addLoreLine(" §f- " + (balance >= cost ? "§a" : "§c") + StringUtils.betterNumberFormat(balance) + "§8/§7" + StringUtils.betterNumberFormat(cost) + " " + resource.getColor() + resource.getDisplayName());
            }
        }else{
            if (gamePlayer.getOnlinePlayer().hasPermission("cosmetics.free")){
                MessageManager.get(gamePlayer, "inventory.cosmetics.saved")
                        .replace("%economy_name%", "")
                        .replace("%price%", "")
                        .replace("_", "")
                        .addToItemLore(item);
                for (Map.Entry<Resource, Integer> entry : cosmetic.getCost().entrySet()) {
                    Resource resource = entry.getKey();
                    int cost = entry.getValue();
                    int balance = data.getBalance(resource);

                    item.addLoreLine(resource.getColor() + " - " + StringUtils.betterNumberFormat(cost) + " " + resource.getDisplayName());}
            }else{
                MessageManager.get(gamePlayer, "inventory.kit.purchased_for")
                        .replace("%economy_name%", "")
                        .replace("%price%", "")
                        .addToItemLore(item);
                for (Map.Entry<Resource, Integer> entry : cosmetic.getCost().entrySet()) {
                    Resource resource = entry.getKey();
                    int cost = entry.getValue();
                    int balance = data.getBalance(resource);

                    item.addLoreLine(resource.getColor() + " - " + StringUtils.betterNumberFormat(cost) + " " + resource.getDisplayName());}
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
            item.addEnchant(Enchantment.SHARPNESS, 1);
            MessageManager.get(gamePlayer, "inventory.cosmetics.selected")
                    .addToItemLore(item);
        } else if (cosmetic.hasPlayer(gamePlayer)) {
            MessageManager.get(gamePlayer, "inventory.cosmetics.select")
                    .addToItemLore(item);
        } else if (!hasEnough) {
            MessageManager.get(gamePlayer, "inventory.cosmetics.dont_have_enough_resources")
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
        openCategory(gamePlayer, (gamePlayer.getMetadata().containsKey("last_opened_cosmetic_category") ? (CosmeticsCategory) gamePlayer.getMetadata().get("last_opened_cosmetic_category") : Minigame.getInstance().getCosmeticsManager().getCategories().get(0)));
    }

    public static void openCategory(GamePlayer gamePlayer, CosmeticsCategory category){
        char ch = switch (category.getName()) {
            case "Kill Messages" -> 'Ẑ';
            case "Kill Sounds" -> 'Ẏ';
            case "Kill Effects" -> 'ẏ';
            case "Projectile Trails" -> 'ẑ';
            case "Hats" -> 'ẗ';
            default -> '-';
        };


        GUI inventory = Component.gui()
                .title(net.kyori.adventure.text.Component.text("§f七七七七七七七七" + ch).font(Key.key("jsplugins", "guis")))
                .rows(6)
                .prepare((gui, player) -> {
                    ItemBuilder back = new ItemBuilder(Material.ECHO_SHARD);
                    back.setCustomModelData(1016);
                    back.hideAllFlags();
                    back.setName(MessageManager.get(player, "inventory.item.go_back")
                            .getTranslated());

                    ItemBuilder info = new ItemBuilder(Material.ECHO_SHARD);
                    info.setCustomModelData(1018);
                    info.hideAllFlags();
                    info.setName(MessageManager.get(player, "inventory.info_item.cosmetics_inventory.name")
                            .getTranslated());
                    info.setLore(MessageManager.get(player, "inventory.info_item.cosmetics_inventory.lore").getTranslated());

                    gui.appendElement(0, Component.element(back.toItemStack()).addClick(i -> {
                        //gui.close(player);
                        ProfileInventory.openGUI(gamePlayer);
                        player.playSound(player, Sounds.CLICK.bukkitSound(), 1F, 1F);
                    }).build());
                    gui.appendElement(8, Component.element(info.toItemStack()).build());

                    /*gui.setContainer(9, Component.staticContainer()
                            .size(9, 1)
                            .init(container -> {
                                for (CosmeticsCategory category2 : category.getManager().getCategories()){
                                    ItemBuilder categoryItem = new ItemBuilder(category2.getIcon());
                                    categoryItem.setName("§a" + category2.getName());
                                    categoryItem.removeLore();
                                    if (category.equals(category2)) categoryItem.addEnchant(Enchantment.SHARPNESS, 1);
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
                            }).build());*/

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

                    gui.setContainer(9, Component.staticContainer()
                            .size(9, 5)
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
                                            } else {
                                                boolean hasEnough = true;
                                                List<String> missing = new ArrayList<>();

                                                for (Map.Entry<Resource, Integer> entry : cosmetic.getCost().entrySet()) {
                                                    Resource resource = entry.getKey();
                                                    int cost = entry.getValue();
                                                    int playerAmount = gamePlayer.getPlayerData().getBalance(resource);

                                                    if (playerAmount < cost) {
                                                        hasEnough = false;
                                                        int needMore = cost - playerAmount;
                                                        missing.add(StringUtils.betterNumberFormat(needMore) + " " + resource.getDisplayName());
                                                    }

                                                }

                                                if (!hasEnough){
                                                    if (!missing.isEmpty()) {
                                                        player.playSound(player.getLocation(), Sounds.ANVIL_BREAK.bukkitSound(), 10.0F, 10.0F);

                                                        MessageManager.get(player, "chat.cosmetic.dont_have_enough")
                                                                .replace("%resources%", String.join(", ", missing))
                                                                .send();
                                                        return;
                                                    }
                                                    player.closeInventory();
                                                }else {
                                                    new ConfirmInventory(gamePlayer, getEditedItem(gamePlayer, cosmetic), cosmetic.getCost(), new Consumer<GamePlayer>() {
                                                        @Override
                                                        public void accept(GamePlayer gamePlayer) {
                                                            cosmetic.purchase(gamePlayer);
                                                            gamePlayer.getOnlinePlayer().closeInventory();
                                                        }
                                                    }, new Consumer<GamePlayer>() {
                                                        @Override
                                                        public void accept(GamePlayer gamePlayer) {
                                                            openCategory(gamePlayer, category);
                                                        }
                                                    }).openGUI();
                                                }
                                                return;
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
