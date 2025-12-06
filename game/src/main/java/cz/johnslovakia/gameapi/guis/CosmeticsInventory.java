package cz.johnslovakia.gameapi.guis;

import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.cosmetics.Cosmetic;
import cz.johnslovakia.gameapi.modules.cosmetics.CosmeticRarityComparator;
import cz.johnslovakia.gameapi.modules.cosmetics.CosmeticsCategory;
import cz.johnslovakia.gameapi.modules.cosmetics.CosmeticsModule;
import cz.johnslovakia.gameapi.modules.messages.MessageModule;
import cz.johnslovakia.gameapi.modules.resources.Resource;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerData;
import cz.johnslovakia.gameapi.users.PlayerManager;
import cz.johnslovakia.gameapi.utils.ItemBuilder;

import cz.johnslovakia.gameapi.utils.StringUtils;

import me.zort.containr.Component;
import me.zort.containr.Element;
import me.zort.containr.GUI;

import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.cosmetics.Cosmetic;
import cz.johnslovakia.gameapi.modules.cosmetics.CosmeticRarityComparator;
import cz.johnslovakia.gameapi.modules.cosmetics.CosmeticsCategory;
import cz.johnslovakia.gameapi.modules.cosmetics.CosmeticsModule;
import cz.johnslovakia.gameapi.modules.messages.MessageModule;
import cz.johnslovakia.gameapi.modules.resources.Resource;
import cz.johnslovakia.gameapi.modules.resources.ResourcesModule;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerManager;
import cz.johnslovakia.gameapi.utils.ItemBuilder;

import cz.johnslovakia.gameapi.utils.StringUtils;

import me.zort.containr.Component;
import me.zort.containr.Element;
import me.zort.containr.GUI;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class CosmeticsInventory implements Listener {

    private static ItemStack getEditedItem(GamePlayer gamePlayer, Cosmetic cosmetic) {
        CosmeticsModule cosmeticsModule = ModuleManager.getModule(CosmeticsModule.class);
        MessageModule messageModule = ModuleManager.getModule(MessageModule.class);
        ResourcesModule resourcesModule = ModuleManager.getModule(ResourcesModule.class);

        Map<Resource, Integer> balances = new HashMap<>();
        boolean hasEnough = true;

        for (Map.Entry<Resource, Integer> entry : cosmetic.getCost().entrySet()) {
            Resource resource = entry.getKey();
            int cost = entry.getValue();

            int balance = resourcesModule.getPlayerBalanceCached(gamePlayer, resource);

            balances.put(resource, balance);
            if (balance < cost) {
                hasEnough = false;
            }
        }

        ItemBuilder item = new ItemBuilder(cosmetic.getIcon());
        if (!cosmetic.hasPlayer(gamePlayer)) {
            item.setName((hasEnough ? "§a" + cosmetic.getName() : "§c" + cosmetic.getName()));
        } else {
            item.setName("§a§l" + cosmetic.getName());
        }

        item.removeLore();

        if (PlainTextComponentSerializer.plainText().serialize(messageModule.get(gamePlayer, cosmetic.getRarity().getTranslateKey()).getTranslated()).length() == 1) {
            item.addLoreLine(net.kyori.adventure.text.Component.text("§7").append(messageModule.get(gamePlayer, cosmetic.getRarity().getTranslateKey()).getTranslated()));
        }

        item.addLoreLine("§8" + cosmetic.getCategory().getName());
        item.removeEnchantment(Enchantment.SHARPNESS);

        item.addLoreLine("");

        if (PlainTextComponentSerializer.plainText().serialize(messageModule.get(gamePlayer, cosmetic.getRarity().getTranslateKey()).getTranslated()).length() > 1) {
            messageModule.get(gamePlayer, "inventory.cosmetics.rarity")
                    .replace("%rarity%", messageModule.get(gamePlayer, cosmetic.getRarity().getTranslateKey()).getTranslated())
                    .addToItemLore(item);
        }

        if (!cosmetic.hasPlayer(gamePlayer)) {
            messageModule.get(gamePlayer, "inventory.cosmetics.cost").addToItemLore(item);

            for (Map.Entry<Resource, Integer> entry : cosmetic.getCost().entrySet()) {
                Resource resource = entry.getKey();
                int cost = entry.getValue();
                int balance = balances.get(resource);

                item.addLoreLine(" §f- " + (balance >= cost ? "§a" : "§c") + StringUtils.betterNumberFormat(balance) + "§8/§7" + StringUtils.betterNumberFormat(cost) + " " + resource.getColor() + resource.getDisplayName());
            }
        } else {
            if (gamePlayer.getOnlinePlayer().hasPermission("cosmetics.free")) {
                messageModule.get(gamePlayer, "inventory.cosmetics.saved")
                        .replace("%economy_name%", "")
                        .replace("%price%", "")
                        .replace("_", "")
                        .addToItemLore(item);

                for (Map.Entry<Resource, Integer> entry : cosmetic.getCost().entrySet()) {
                    Resource resource = entry.getKey();
                    int cost = entry.getValue();
                    item.addLoreLine(resource.getColor() + " - " + StringUtils.betterNumberFormat(cost) + " " + resource.getDisplayName());
                }
            } else {
                messageModule.get(gamePlayer, "inventory.kit.purchased_for")
                        .replace("%economy_name%", "")
                        .replace("%price%", "")
                        .addToItemLore(item);

                for (Map.Entry<Resource, Integer> entry : cosmetic.getCost().entrySet()) {
                    Resource resource = entry.getKey();
                    int cost = entry.getValue();
                    item.addLoreLine(resource.getColor() + " - " + StringUtils.betterNumberFormat(cost) + " " + resource.getDisplayName());
                }
            }
        }

        item.addLoreLine("");

        if (cosmeticsModule.getPlayerSelectedCosmetic(gamePlayer, cosmetic.getCategory()) != null &&
                cosmeticsModule.getPlayerSelectedCosmetic(gamePlayer, cosmetic.getCategory()).equals(cosmetic)) {
            item.addEnchant(Enchantment.SHARPNESS, 1);
            messageModule.get(gamePlayer, "inventory.cosmetics.selected").addToItemLore(item);
        } else if (cosmetic.hasPlayer(gamePlayer)) {
            messageModule.get(gamePlayer, "inventory.cosmetics.select").addToItemLore(item);
        } else if (!hasEnough) {
            messageModule.get(gamePlayer, "inventory.cosmetics.dont_have_enough_resources").addToItemLore(item);
        } else {
            messageModule.get(gamePlayer, "inventory.cosmetics.purchase").addToItemLore(item);
        }

        item.hideAllFlags();

        if (cosmetic.getPreviewConsumer() != null) {
            messageModule.get(gamePlayer, "inventory.cosmetics.preview").addToItemLore(item);
        }

        return item.toItemStack();
    }

    public static void openGUI(GamePlayer gamePlayer) {
        openCategory(gamePlayer, (gamePlayer.getMetadata().containsKey("last_opened_cosmetic_category") ? (CosmeticsCategory) gamePlayer.getMetadata().get("last_opened_cosmetic_category") : ModuleManager.getModule(CosmeticsModule.class).getCategories().get(0)));
    }

    public static void openCategory(GamePlayer gamePlayer, CosmeticsCategory category) {
        char ch = switch (category.getName()) {
            case "Kill Messages" -> 'Ẑ';
            case "Kill Sounds" -> 'Ẏ';
            case "Kill Effects" -> 'ẏ';
            case "Projectile Trails" -> 'ẑ';
            case "Hats" -> 'ẗ';
            default -> '-';
        };

        Set<Resource> allResources = new HashSet<>();
        for (Cosmetic cosmetic : category.getCosmetics()) {
            allResources.addAll(cosmetic.getCost().keySet());
        }

        List<CompletableFuture<Void>> preloadFutures = new ArrayList<>();
        for (Resource resource : allResources) {
            preloadFutures.add(resource.getResourceInterface().preload(List.of(gamePlayer)));
        }

        CompletableFuture.allOf(preloadFutures.toArray(new CompletableFuture[0])).thenRun(() -> {
            MessageModule messageModule = ModuleManager.getModule(MessageModule.class);
            ResourcesModule resourcesModule = ModuleManager.getModule(ResourcesModule.class);

            GUI inventory = Component.gui()
                    .title(net.kyori.adventure.text.Component.text("§f七七七七七七七七" + ch).font(Key.key("jsplugins", "guis")))
                    .rows(6)
                    .prepare((gui, player) -> {
                        ItemBuilder back = new ItemBuilder(Material.ECHO_SHARD);
                        back.setCustomModelData(1016);
                        back.hideAllFlags();
                        back.setName(messageModule.get(player, "inventory.item.go_back").getTranslated());

                        ItemBuilder info = new ItemBuilder(Material.ECHO_SHARD);
                        info.setCustomModelData(1018);
                        info.hideAllFlags();
                        info.setName(messageModule.get(player, "inventory.info_item.cosmetics_inventory.name").getTranslated());
                        info.setLore(messageModule.get(player, "inventory.info_item.cosmetics_inventory.lore").getTranslated());

                        gui.appendElement(0, Component.element(back.toItemStack()).addClick(i -> {
                            ProfileInventory.openGUI(gamePlayer);
                            player.playSound(player, Sound.UI_BUTTON_CLICK, 1F, 1F);
                        }).build());

                        gui.appendElement(8, Component.element(info.toItemStack()).build());

                        List<Cosmetic> cosmetics = category.getCosmetics();
                        if (cosmetics == null || cosmetics.isEmpty()) return;

                        cosmetics.sort(Comparator.comparing((Cosmetic c) -> c.hasPlayer(gamePlayer), Comparator.reverseOrder())
                                .thenComparing(new CosmeticRarityComparator()));

                        gui.setContainer(9, Component.staticContainer()
                                .size(9, 5)
                                .init(container -> {
                                    for (Cosmetic cosmetic : cosmetics) {
                                        Element element = Component.element(getEditedItem(gamePlayer, cosmetic)).addClick(i -> {
                                            if (i.getClickType().isLeftClick()) {
                                                if (cosmetic.hasSelected(gamePlayer)) {
                                                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_BREAK, 10.0F, 10.0F);
                                                    messageModule.get(player, "chat.cosmetics.already_selected")
                                                            .replace("%cosmetic", cosmetic.getName())
                                                            .send();
                                                } else if (cosmetic.hasPlayer(gamePlayer)) {
                                                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 10.0F, 10.0F);
                                                    cosmetic.select(gamePlayer);
                                                } else {
                                                    boolean hasEnough = true;
                                                    List<String> missing = new ArrayList<>();

                                                    for (Map.Entry<Resource, Integer> entry : cosmetic.getCost().entrySet()) {
                                                        Resource resource = entry.getKey();
                                                        int cost = entry.getValue();

                                                        Integer playerAmount = resourcesModule.getPlayerBalanceCached(gamePlayer, resource);

                                                        if (playerAmount < cost) {
                                                            hasEnough = false;
                                                            int needMore = cost - playerAmount;
                                                            missing.add(StringUtils.betterNumberFormat(needMore) + " " + resource.getDisplayName());
                                                        }
                                                    }

                                                    if (!hasEnough) {
                                                        if (!missing.isEmpty()) {
                                                            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_BREAK, 10.0F, 10.0F);
                                                            messageModule.get(player, "chat.cosmetic.dont_have_enough")
                                                                    .replace("%resources%", String.join(", ", missing))
                                                                    .send();
                                                            return;
                                                        }
                                                        player.closeInventory();
                                                    } else {
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
                                            } else if (i.getClickType().isRightClick() && cosmetic.getPreviewConsumer() != null) {
                                                cosmetic.getPreviewConsumer().accept(gamePlayer);
                                                if (!cosmetic.hasPlayer(gamePlayer)) {
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
        });
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);
        ItemStack item = event.getCurrentItem();

        if (event.getClickedInventory() == null || !player.getOpenInventory().getTitle().contains("†¹")) {
            return;
        }
        if (item == null) {
            return;
        }
        event.setCancelled(true);

        if (event.getSlot() == 45) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1F, 1F);
            TeleporterInventory.openGUI(gamePlayer);
        }
    }
}