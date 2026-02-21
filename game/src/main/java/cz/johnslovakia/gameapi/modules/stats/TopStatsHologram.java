package cz.johnslovakia.gameapi.modules.stats;

import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.messages.MessageModule;
import cz.johnslovakia.gameapi.users.PlayerIdentity;
import cz.johnslovakia.gameapi.users.PlayerIdentityRegistry;
import cz.johnslovakia.gameapi.utils.StringUtils;

import lombok.Getter;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Display;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class TopStatsHologram implements Listener {

    private static final int TOP_SIZE = 10;

    private final StatsModule statsModule;

    private final Map<PlayerIdentity, TextDisplay> displayMap = new HashMap<>();
    private final Map<PlayerIdentity, Interaction> clickZoneMap = new HashMap<>();
    private final Map<PlayerIdentity, Location> locationMap = new HashMap<>();
    private final Map<PlayerIdentity, Integer> playerStatIndex = new HashMap<>();

    public TopStatsHologram(StatsModule statsModule) {
        this.statsModule = statsModule;
        Bukkit.getPluginManager().registerEvents(this, Minigame.getInstance().getPlugin());
    }

    public void createTopStatisticsHologram(PlayerIdentity playerIdentity, Location location) {
        remove(playerIdentity);
        locationMap.put(playerIdentity, location);
        playerStatIndex.putIfAbsent(playerIdentity, 0);
        fetchAndDraw(playerIdentity);
    }

    public void remove(PlayerIdentity playerIdentity) {
        TextDisplay display = displayMap.remove(playerIdentity);
        if (display != null && !display.isDead()) display.remove();

        Interaction clickZone = clickZoneMap.remove(playerIdentity);
        if (clickZone != null && !clickZone.isDead()) clickZone.remove();

        locationMap.remove(playerIdentity);
        playerStatIndex.remove(playerIdentity);
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent e) {
        if (!(e.getRightClicked() instanceof Interaction clickedEntity)) return;

        Player player           = e.getPlayer();
        PlayerIdentity identity = PlayerIdentityRegistry.get(player);

        Interaction clickZone = clickZoneMap.get(identity);
        if (clickZone == null || !clickZone.equals(clickedEntity)) return;

        e.setCancelled(true);

        List<Stat> stats = statsModule.getStats();
        if (stats.isEmpty()) return;

        int current = playerStatIndex.getOrDefault(identity, 0);
        playerStatIndex.put(identity, (current + 1) % stats.size());

        TextDisplay old = displayMap.remove(identity);
        if (old != null && !old.isDead()) old.remove();

        fetchAndDraw(identity);

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
    }

    private void fetchAndDraw(PlayerIdentity playerIdentity) {
        List<Stat> stats = statsModule.getStats();
        if (stats.isEmpty()) return;

        int currentIndex = playerStatIndex.getOrDefault(playerIdentity, 0);
        Stat currentStat = stats.get(currentIndex);

        CompletableFuture.supplyAsync(() -> {
            Map<String, Integer> topPlayers = statsModule.getStatsTable()
                    .topStats(currentStat.getName().replace(" ", "_"), TOP_SIZE);
            int playerRank      = getPlayerRank(playerIdentity.getName(), currentStat.getName(), topPlayers);
            int playerStatValue = statsModule.getPlayerStat(playerIdentity, currentStat.getName());
            return new Object[]{ topPlayers, playerRank, playerStatValue };

        }).thenAccept(results -> {
            @SuppressWarnings("unchecked")
            Map<String, Integer> topPlayers = (Map<String, Integer>) results[0];
            int playerRank      = (int) results[1];
            int playerStatValue = (int) results[2];

            Bukkit.getScheduler().runTask(Minigame.getInstance().getPlugin(), () -> {
                if (playerIdentity.getOnlinePlayer() == null) return;
                spawnDisplay(playerIdentity, currentStat, topPlayers,
                        playerRank, playerStatValue, currentIndex, stats.size());
            });
        });
    }

    private void spawnDisplay(PlayerIdentity playerIdentity, Stat stat,
                              Map<String, Integer> topPlayers, int playerRank,
                              int playerStatValue, int currentIndex, int totalStats) {
        Location location = locationMap.get(playerIdentity);
        if (location == null) return;

        Component content = buildComponent(playerIdentity, stat, topPlayers,
                playerRank, playerStatValue, currentIndex, totalStats);

        TextDisplay display = location.getWorld().spawn(location, TextDisplay.class, entity -> {
            entity.text(content);
            entity.setBillboard(Display.Billboard.FIXED);
            entity.setVisibleByDefault(false);
            entity.setPersistent(false);
            entity.setSeeThrough(false);
            entity.setViewRange(0.5f);
            entity.setLineWidth(10000);
        });

        if (!clickZoneMap.containsKey(playerIdentity)) {
            Interaction clickZone = location.getWorld().spawn(
                    location.clone().add(0, -1.0, 0),
                    Interaction.class,
                    entity -> {
                        entity.setInteractionWidth(2.5f);
                        entity.setInteractionHeight(4.0f);
                        entity.setVisibleByDefault(false);
                        entity.setPersistent(false);
                    }
            );
            clickZoneMap.put(playerIdentity, clickZone);
            playerIdentity.getOnlinePlayer().showEntity(Minigame.getInstance().getPlugin(), clickZone);
        }

        playerIdentity.getOnlinePlayer().showEntity(Minigame.getInstance().getPlugin(), display);
        displayMap.put(playerIdentity, display);
    }

    private Component buildComponent(PlayerIdentity playerIdentity, Stat stat,
                                     Map<String, Integer> topPlayers, int playerRank,
                                     int playerStatValue, int currentIndex, int totalStats) {
        Component title     = getTitle(playerIdentity, stat);
        int maxLineWidth    = computeMaxLineWidth(topPlayers);
        Component component = title.appendNewline();

        int position = 1;
        for (Map.Entry<String, Integer> entry : topPlayers.entrySet()) {
            component = component.appendNewline()
                    .append(buildRankLine(entry.getKey(), entry.getValue(), position,
                            maxLineWidth, playerRank == position));
            position++;
        }
        while (position <= TOP_SIZE) {
            component = component.appendNewline().append(buildEmptyRankLine(position, maxLineWidth));
            position++;
        }

        boolean playerInTop = topPlayers.containsKey(playerIdentity.getName());
        if (!playerInTop && playerRank > 0) {
            component = component.appendNewline()
                    .append(buildRankLine(playerIdentity.getName(), playerStatValue,
                            playerRank, maxLineWidth, true));
        }

        if (ModuleManager.getModule(MessageModule.class).existMessage(playerIdentity, "hologram.leaderboard.click_to_cycle")) {
            component = component.appendNewline().appendNewline()
                    .append(ModuleManager.getModule(MessageModule.class)
                            .get(playerIdentity, "hologram.leaderboard.click_to_cycle")
                            .replace("%currentIndex%", String.valueOf(currentIndex + 1))
                            .replace("%totalStats%", String.valueOf(totalStats))
                            .getTranslated());
        }

        return component;
    }

    private Component getTitle(PlayerIdentity playerIdentity, Stat stat) {
        MessageModule msg = ModuleManager.getModule(MessageModule.class);
        if (msg.existMessage(playerIdentity, "hologram.leaderboard.title")) {
            return msg.get(playerIdentity, "hologram.leaderboard.title")
                    .replace("%minigame_name%", Minigame.getInstance().getName())
                    .replace("%stat_name%", stat.getTranslated(playerIdentity))
                    .getTranslated();
        }
        return Component.text("§a" + Minigame.getInstance().getName() + " §7- ")
                .append(stat.getTranslated(playerIdentity));
    }

    private Component buildRankLine(String playerName, int value, int position,
                                    int maxWidth, boolean isPlayerRow) {
        String color        = isPlayerRow ? "§e" : "§f";
        Component rankComp  = Component.text(color + position + ". ");
        Component nameComp  = Component.text("§f" + playerName);
        Component valueComp = Component.text("§f" + StringUtils.betterNumberFormat(value));

        int dotCharWidth = StringUtils.DefaultFontInfo.getDefaultFontInfo('.').getLength();
        int dotCount = Math.max(0,
                (maxWidth - getLength(rankComp) - getLength(nameComp) - getLength(valueComp)) / dotCharWidth);

        return Component.empty()
                .append(rankComp)
                .append(nameComp)
                .append(Component.text(StringUtils.calculateNegativeSpaces(dotCount))
                        .font(Key.key("jsplugins", "gameapi")))
                .append(Component.text(" "))
                .append(valueComp);
    }

    private Component buildEmptyRankLine(int position, int maxWidth) {
        Component rankComp  = Component.text("§7" + position + ". ");
        Component emptyComp = Component.text("§8-");
        int dotCharWidth = StringUtils.DefaultFontInfo.getDefaultFontInfo('.').getLength();
        int dotCount = Math.max(0, (maxWidth - getLength(rankComp) - getLength(emptyComp)) / dotCharWidth);

        return Component.empty()
                .append(rankComp).append(emptyComp)
                .append(Component.text(StringUtils.calculateNegativeSpaces(dotCount))
                        .font(Key.key("jsplugins", "gameapi")))
                .append(Component.text(" "));
    }

    private int computeMaxLineWidth(Map<String, Integer> topPlayers) {
        int max = 0, pos = 1;
        for (Map.Entry<String, Integer> entry : topPlayers.entrySet()) {
            Component line = Component.text(pos + ". " + entry.getKey() + "  " +
                    StringUtils.betterNumberFormat(entry.getValue()));
            max = Math.max(max, getLength(line));
            if (++pos > TOP_SIZE) break;
        }
        return Math.max(max, 142);
    }

    private int getLength(Component component) {
        if (component == null) return 0;
        String text = ChatColor.stripColor(
                LegacyComponentSerializer.legacySection().serialize(component));
        double width = 0;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            width += StringUtils.DefaultFontInfo.getDefaultFontInfo(ch).getLength();
            if (ch != ' ' && i < text.length() - 1 && text.charAt(i + 1) != ' ') width += 1;
        }
        return (int) width;
    }

    private int getPlayerRank(String playerName, String statName, Map<String, Integer> topPlayers) {
        int pos = 1;
        for (String name : topPlayers.keySet()) {
            if (name.equalsIgnoreCase(playerName)) return pos;
            pos++;
        }
        int playerValue = statsModule.getStatsTable().getStat(playerName, statName);
        if (playerValue <= 0) return -1;
        Map<String, Integer> all = statsModule.getStatsTable()
                .topStats(statName.replace(" ", "_"), Integer.MAX_VALUE);
        int rank = 1;
        for (int value : all.values()) {
            if (value > playerValue) rank++;
        }
        return rank;
    }
}