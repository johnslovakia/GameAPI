package cz.johnslovakia.gameapi.modules.stats;

import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.Core;
import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.messages.MessageModule;
import cz.johnslovakia.gameapi.users.PlayerIdentity;
import cz.johnslovakia.gameapi.users.PlayerIdentityRegistry;
import cz.johnslovakia.gameapi.utils.Logger;
import cz.johnslovakia.gameapi.utils.RefreshableCache;
import cz.johnslovakia.gameapi.utils.StringUtils;

import me.zort.sqllib.SQLDatabaseConnection;
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
import org.bukkit.scheduler.BukkitTask;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class TopStatsHologram implements Listener {

    private static final int TOP_SIZE = 10;

    private final StatsModule statsModule;
    private final RefreshableCache<String, Map<String, Integer>> topPlayersCache;

    private final Map<PlayerIdentity, TextDisplay> displayMap = new ConcurrentHashMap<>();
    private final Map<PlayerIdentity, Interaction> clickZoneMap = new ConcurrentHashMap<>();
    private final Map<PlayerIdentity, Location> locationMap = new ConcurrentHashMap<>();
    private final Map<PlayerIdentity, Integer> playerStatIndex = new ConcurrentHashMap<>();
    private final Map<PlayerIdentity, BukkitTask> tickTasks = new ConcurrentHashMap<>();

    public TopStatsHologram(StatsModule statsModule) {
        this.statsModule = statsModule;

        this.topPlayersCache = RefreshableCache.<String, Map<String, Integer>>builder("TopStats")
                .refreshIntervalMinutes(5)
                .autoRefresh(true)
                .debugEnabled(false)
                .build();

        Bukkit.getPluginManager().registerEvents(this, Minigame.getInstance().getPlugin());
    }

    public void registerStats() {
        for (Stat stat : statsModule.getStats()) {
            String cacheKey = stat.getName() + ":" + TOP_SIZE;
            topPlayersCache.register(cacheKey, key -> {
                String[] parts = key.split(":", 2);
                int limit = Integer.parseInt(parts[1]);
                return loadTopPlayersFromDB(parts[0], limit);
            });
        }
    }

    public void shutdown() {
        topPlayersCache.shutdown();
        for (BukkitTask task : tickTasks.values()) task.cancel();
        tickTasks.clear();
        removeAll();
    }

    public void create(PlayerIdentity playerIdentity, Location location) {
        remove(playerIdentity);
        locationMap.put(playerIdentity, location);
        playerStatIndex.putIfAbsent(playerIdentity, 0);
        fetchAndDraw(playerIdentity);
    }

    public void remove(PlayerIdentity playerIdentity) {
        BukkitTask task = tickTasks.remove(playerIdentity);
        if (task != null) task.cancel();

        TextDisplay display = displayMap.remove(playerIdentity);
        if (display != null && !display.isDead()) display.remove();

        Interaction clickZone = clickZoneMap.remove(playerIdentity);
        if (clickZone != null && !clickZone.isDead()) clickZone.remove();

        locationMap.remove(playerIdentity);
        playerStatIndex.remove(playerIdentity);
    }

    public void removeAll() {
        for (PlayerIdentity identity : List.copyOf(displayMap.keySet())) remove(identity);
        displayMap.clear();
        clickZoneMap.clear();
        locationMap.clear();
        playerStatIndex.clear();
        tickTasks.clear();
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent e) {
        if (!(e.getRightClicked() instanceof Interaction clickedEntity)) return;

        Player player = e.getPlayer();
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
        if (currentIndex >= stats.size()) {
            currentIndex = 0;
            playerStatIndex.put(playerIdentity, 0);
        }
        Stat currentStat = stats.get(currentIndex);
        String cacheKey = currentStat.getName() + ":" + TOP_SIZE;

        int finalCurrentIndex = currentIndex;
        CompletableFuture<Map<String, Integer>> topFuture = getTopPlayers(currentStat.getName(), TOP_SIZE);
        CompletableFuture<Integer> rankFuture = getPlayerRankFromDB(playerIdentity.getName(), currentStat.getName());

        CompletableFuture.allOf(topFuture, rankFuture).thenAccept(ignored -> {
            Map<String, Integer> topPlayers = topFuture.join();
            int playerRank = rankFuture.join();
            int playerStatValue = statsModule.getPlayerStat(playerIdentity, currentStat.getName());
            long remainingMs = topPlayersCache.getTimeUntilRefresh(cacheKey)
                    .map(Duration::toMillis).orElse(0L);

            Component content = buildComponent(playerIdentity, currentStat, topPlayers,
                    playerRank, playerStatValue, finalCurrentIndex, stats.size(), remainingMs);

            Bukkit.getScheduler().runTask(Minigame.getInstance().getPlugin(), () -> {
                if (playerIdentity.getOnlinePlayer() == null) return;
                spawnOrUpdate(playerIdentity, content);
            });
        });
    }

    private CompletableFuture<Map<String, Integer>> getTopPlayers(String statName, int limit) {
        String cacheKey = statName + ":" + limit;

        Map<String, Integer> cached = topPlayersCache.getIfPresent(cacheKey);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }

        return CompletableFuture.supplyAsync(() ->
                topPlayersCache.get(cacheKey, () -> loadTopPlayersFromDB(statName, limit))
        );
    }

    private Map<String, Integer> loadTopPlayersFromDB(String statName, int limit) {
        Map<String, Integer> topPlayers = new LinkedHashMap<>();
        String columnName = statName.replace(" ", "_");

        try (SQLDatabaseConnection conn = Core.getInstance().getDatabase().getConnection()) {
            if (conn == null) return topPlayers;

            String query = "SELECT `Nickname`, `" + columnName + "`" +
                    " FROM " + statsModule.getStatsTable().quotedTableName() +
                    " WHERE `" + columnName + "` > 0" +
                    " ORDER BY `" + columnName + "` DESC LIMIT ?";

            try (PreparedStatement stmt = conn.getConnection().prepareStatement(query)) {
                stmt.setInt(1, limit);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        topPlayers.put(rs.getString("Nickname"), rs.getInt(columnName));
                    }
                }
            }
        } catch (Exception e) {
            Logger.log("Failed to load top players: " + e.getMessage(), Logger.LogType.WARNING);
        }

        return topPlayers;
    }

    private void spawnOrUpdate(PlayerIdentity playerIdentity, Component content) {
        TextDisplay existing = displayMap.get(playerIdentity);

        if (existing != null && !existing.isDead()) {
            existing.text(content);
            return;
        }

        if (existing != null) {
            displayMap.remove(playerIdentity);
        }

        Location location = locationMap.get(playerIdentity);
        if (location == null) return;

        TextDisplay display = location.getWorld().spawn(location, TextDisplay.class, entity -> {
            entity.text(content);
            entity.setBillboard(statsModule.isFixedBillboardDisplay() ? Display.Billboard.FIXED : Display.Billboard.VERTICAL);
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

        BukkitTask oldTask = tickTasks.remove(playerIdentity);
        if (oldTask != null) oldTask.cancel();

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(
                Minigame.getInstance().getPlugin(),
                () -> {
                    if (playerIdentity.getOnlinePlayer() == null
                            || !displayMap.containsKey(playerIdentity)) {
                        BukkitTask t = tickTasks.remove(playerIdentity);
                        if (t != null) t.cancel();
                        return;
                    }
                    fetchAndDraw(playerIdentity);
                },
                600L, 600L
        );

        tickTasks.put(playerIdentity, task);
    }

    private Component buildComponent(PlayerIdentity playerIdentity, Stat stat, Map<String, Integer> topPlayers, int playerRank, int playerStatValue, int currentIndex, int totalStats, long remainingMs) {
        Component component = ModuleManager.getModule(MessageModule.class).get(playerIdentity, "hologram.leaderboard.title")
                .replace("%minigame_name%", Minigame.getInstance().getName())
                .replace("%stat_name%", stat.getTranslated(playerIdentity))
                .getTranslated().appendNewline();

        int maxLineWidth = computeMaxLineWidth(topPlayers);

        int position = 1;
        for (Map.Entry<String, Integer> entry : topPlayers.entrySet()) {
            component = component.appendNewline()
                    .append(buildRankLine(entry.getKey(), entry.getValue(), position, maxLineWidth, playerRank == position));
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

        component = component
                .appendNewline().appendNewline()
                .append(StringUtils.colorizerComponent(formatCountdown(playerIdentity, remainingMs)));

        if (ModuleManager.getModule(MessageModule.class).existMessage(playerIdentity, "hologram.leaderboard.click_to_cycle")) {
            component = component.appendNewline()
                    .append(ModuleManager.getModule(MessageModule.class)
                            .get(playerIdentity, "hologram.leaderboard.click_to_cycle")
                            .replace("%currentIndex%", String.valueOf(currentIndex + 1))
                            .replace("%totalStats%", String.valueOf(totalStats))
                            .getTranslated());
        }

        return component;
    }

    private String formatCountdown(PlayerIdentity playerIdentity, long remainingMs) {
        if (remainingMs <= 0) {
            return ModuleManager.getModule(MessageModule.class)
                    .get(playerIdentity, "hologram.leaderboard.refreshing")
                    .getRawTranslated();
        }

        long totalSeconds = remainingMs / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;

        return ModuleManager.getModule(MessageModule.class)
                .get(playerIdentity, "hologram.leaderboard.will_update_in")
                .replace("%m%", minutes > 0 ? String.valueOf(minutes) : "<1")
                .replace("%s%", String.valueOf(seconds))
                .getRawTranslated();
    }

    private Component buildRankLine(String playerName, int value, int position,
                                    int maxWidth, boolean isPlayerRow) {
        String color = isPlayerRow ? "§e" : "§f";

        Component rankComp = Component.text(color + position + ". ");
        Component nameComp = Component.text("§f" + playerName);
        Component valueComp = value >= 0
                ? Component.text("§f" + StringUtils.betterNumberFormat(value))
                : Component.text("§c?");

        int leftWidth = getLength(rankComp) + getLength(nameComp);
        int rightWidth = getLength(valueComp);
        int dotCharWidth = StringUtils.DefaultFontInfo.getDefaultFontInfo('.').getLength();
        int dotCount = Math.max(0, (maxWidth - leftWidth - rightWidth) / dotCharWidth);

        return Component.empty()
                .append(rankComp)
                .append(nameComp)
                .append(Component.text(StringUtils.calculateNegativeSpaces(dotCount))
                        .font(Key.key("jsplugins", "gameapi")))
                .append(Component.text(" "))
                .append(valueComp);
    }

    private Component buildEmptyRankLine(int position, int maxWidth) {
        Component rankComp = Component.text("§7" + position + ". ");
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
            Component line = Component.text(pos + ". " + entry.getKey() + "  " + StringUtils.betterNumberFormat(entry.getValue()));
            max = Math.max(max, getLength(line));
            if (++pos > TOP_SIZE) break;
        }
        return Math.max(max, 150);
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

    private CompletableFuture<Integer> getPlayerRankFromDB(String playerName, String statName) {
        return CompletableFuture.supplyAsync(() ->
                statsModule.getStatsTable().getPlayerRank(playerName, statName)
        );
    }
}