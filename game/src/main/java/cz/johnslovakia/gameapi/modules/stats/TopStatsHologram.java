package cz.johnslovakia.gameapi.modules.stats;

import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.messages.MessageModule;
import cz.johnslovakia.gameapi.users.PlayerIdentity;
import cz.johnslovakia.gameapi.users.PlayerIdentityRegistry;
import cz.johnslovakia.gameapi.utils.RefreshableCache;
import cz.johnslovakia.gameapi.utils.StringUtils;

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

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class TopStatsHologram implements Listener {

    private static final int TOP_SIZE = 10;

    private final StatsModule statsModule;
    private final RefreshableCache<String, LinkedHashMap<String, Integer>> topPlayersCache;

    private final Map<PlayerIdentity, TextDisplay> displayMap = new ConcurrentHashMap<>();
    private final Map<PlayerIdentity, TextDisplay> statSubDisplayMap = new ConcurrentHashMap<>();
    private final Map<PlayerIdentity, TextDisplay> periodSubDisplayMap = new ConcurrentHashMap<>();
    private final Map<PlayerIdentity, Interaction> statClickZoneMap = new ConcurrentHashMap<>();
    private final Map<PlayerIdentity, Interaction> periodClickZoneMap = new ConcurrentHashMap<>();
    private final Map<PlayerIdentity, Location> locationMap = new ConcurrentHashMap<>();
    private final Map<PlayerIdentity, Integer> playerStatIndex = new ConcurrentHashMap<>();
    private final Map<PlayerIdentity, StatPeriod> playerPeriod = new ConcurrentHashMap<>();
    private final Map<PlayerIdentity, BukkitTask> tickTasks = new ConcurrentHashMap<>();

    public TopStatsHologram(StatsModule statsModule) {
        this.statsModule = statsModule;

        this.topPlayersCache = RefreshableCache.<String, LinkedHashMap<String, Integer>>builder("TopStats")
                .refreshIntervalMinutes(5)
                .autoRefresh(true)
                .debugEnabled(false)
                .build();

        Bukkit.getPluginManager().registerEvents(this, Minigame.getInstance().getPlugin());
    }

    public void registerStats() {
        for (Stat stat : statsModule.getStats()) {
            for (StatPeriod period : StatPeriod.values()) {
                if (!period.equals(StatPeriod.LIFETIME) && stat.getName().equalsIgnoreCase("Winstreak")) continue;
                String cacheKey = cacheKey(stat.getName(), period);
                topPlayersCache.register(cacheKey, key -> {
                    String[] parts = key.split(":", 2);
                    StatPeriod p = StatPeriod.valueOf(parts[1]);
                    return statsModule.getStatsTable().topStats(parts[0], TOP_SIZE, p);
                });
            }
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
        playerPeriod.putIfAbsent(playerIdentity, statsModule.getDefaultDisplayPeriod());
        fetchAndDraw(playerIdentity);
    }

    public void remove(PlayerIdentity playerIdentity) {
        BukkitTask task = tickTasks.remove(playerIdentity);
        if (task != null) task.cancel();

        removeEntity(displayMap.remove(playerIdentity));
        removeEntity(statSubDisplayMap.remove(playerIdentity));
        removeEntity(periodSubDisplayMap.remove(playerIdentity));
        removeEntity(statClickZoneMap.remove(playerIdentity));
        removeEntity(periodClickZoneMap.remove(playerIdentity));

        locationMap.remove(playerIdentity);
        playerStatIndex.remove(playerIdentity);
        playerPeriod.remove(playerIdentity);
    }

    public void removeAll() {
        for (PlayerIdentity identity : List.copyOf(displayMap.keySet())) remove(identity);
        displayMap.clear();
        statSubDisplayMap.clear();
        periodSubDisplayMap.clear();
        statClickZoneMap.clear();
        periodClickZoneMap.clear();
        locationMap.clear();
        playerStatIndex.clear();
        playerPeriod.clear();
        tickTasks.clear();
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent e) {
        if (!(e.getRightClicked() instanceof Interaction clicked)) return;

        Player player = e.getPlayer();
        PlayerIdentity identity = PlayerIdentityRegistry.get(player);
        if (identity == null) return;

        boolean isStatClick = clicked.equals(statClickZoneMap.get(identity));
        boolean isPeriodClick = clicked.equals(periodClickZoneMap.get(identity));
        if (!isStatClick && !isPeriodClick) return;

        e.setCancelled(true);

        if (isStatClick) {
            List<Stat> stats = statsModule.getStats();
            if (stats.isEmpty()) return;
            int current = playerStatIndex.getOrDefault(identity, 0);
            playerStatIndex.put(identity, (current + 1) % stats.size());
        } else {
            playerPeriod.put(identity, playerPeriod.getOrDefault(identity, statsModule.getDefaultDisplayPeriod()).next());
        }

        removeDisplaysOnly(identity);
        fetchAndDraw(identity);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
    }

    private void removeDisplaysOnly(PlayerIdentity playerIdentity) {
        removeEntity(displayMap.remove(playerIdentity));
        removeEntity(statSubDisplayMap.remove(playerIdentity));
        removeEntity(periodSubDisplayMap.remove(playerIdentity));
    }

    private void fetchAndDraw(PlayerIdentity playerIdentity) {
        StatPeriod period = playerPeriod.getOrDefault(playerIdentity, statsModule.getDefaultDisplayPeriod());
        List<Stat> stats = statsModule.getStats()
                .stream()
                .filter(stat -> period.equals(StatPeriod.LIFETIME)
                        || !stat.getName().equalsIgnoreCase("Winstreak"))
                .toList();;
        if (stats.isEmpty()) return;

        int currentIndex = playerStatIndex.getOrDefault(playerIdentity, 0);
        if (currentIndex >= stats.size()) {
            currentIndex = 0;
            playerStatIndex.put(playerIdentity, 0);
        }

        Stat currentStat = stats.get(currentIndex);
        String ck = cacheKey(currentStat.getName(), period);
        int finalIndex = currentIndex;

        CompletableFuture<LinkedHashMap<String, Integer>> topFuture = CompletableFuture.supplyAsync(() -> {
            LinkedHashMap<String, Integer> cached = topPlayersCache.getIfPresent(ck);
            return cached != null ? cached : topPlayersCache.get(ck, () -> statsModule.getStatsTable().topStats(currentStat.getName(), TOP_SIZE, period));
        });

        CompletableFuture<Integer> rankFuture = CompletableFuture.supplyAsync(() ->
                statsModule.getStatsTable().getPlayerRank(playerIdentity.getName(), currentStat.getName(), period));

        CompletableFuture.allOf(topFuture, rankFuture).thenAccept(ignored -> {
            LinkedHashMap<String, Integer> topPlayers = topFuture.join();
            int playerRank = rankFuture.join();
            int playerStatValue = statsModule.getPlayerStat(playerIdentity, currentStat.getName(), period);
            long cacheRemainingMs = topPlayersCache.getTimeUntilRefresh(ck).map(Duration::toMillis).orElse(0L);

            Component mainContent = buildMainComponent(playerIdentity, currentStat, topPlayers, playerRank, playerStatValue, period, cacheRemainingMs);
            Component statSubContent = buildStatSubComponent(playerIdentity, finalIndex, stats.size());
            Component periodSubContent = buildPeriodSubComponent(playerIdentity, period);

            Bukkit.getScheduler().runTask(Minigame.getInstance().getPlugin(), () -> {
                if (playerIdentity.getOnlinePlayer() == null) return;
                spawnOrUpdate(playerIdentity, mainContent, statSubContent, periodSubContent);
            });
        });
    }

    private void spawnOrUpdate(PlayerIdentity playerIdentity, Component mainContent, Component statSubContent, Component periodSubContent) {
        Location location = locationMap.get(playerIdentity);
        if (location == null) return;

        TextDisplay existing = displayMap.get(playerIdentity);
        if (existing != null && !existing.isDead()) {
            existing.text(mainContent);
        } else {
            removeEntity(existing);
            displayMap.remove(playerIdentity);

            TextDisplay display = location.getWorld().spawn(location, TextDisplay.class, entity -> {
                entity.text(mainContent);
                entity.setBillboard(statsModule.isFixedBillboardDisplay() ? Display.Billboard.FIXED : Display.Billboard.VERTICAL);
                entity.setVisibleByDefault(false);
                entity.setPersistent(false);
                entity.setSeeThrough(false);
                entity.setViewRange(0.5f);
                entity.setLineWidth(10000);
            });

            playerIdentity.getOnlinePlayer().showEntity(Minigame.getInstance().getPlugin(), display);
            displayMap.put(playerIdentity, display);
        }

        Location statSubLoc = location.clone().add(-1, -0.6, 0);
        Location periodSubLoc = location.clone().add(1, -0.6, 0);

        spawnOrUpdateSub(playerIdentity, statSubDisplayMap, statClickZoneMap, statSubLoc, statSubContent);
        spawnOrUpdateSub(playerIdentity, periodSubDisplayMap, periodClickZoneMap, periodSubLoc, periodSubContent);

        scheduleTick(playerIdentity);
    }

    private void spawnOrUpdateSub(PlayerIdentity playerIdentity, Map<PlayerIdentity, TextDisplay> subMap, Map<PlayerIdentity, Interaction> clickMap, Location loc, Component content) {
        TextDisplay existing = subMap.get(playerIdentity);
        if (existing != null && !existing.isDead()) {
            existing.text(content);
            return;
        }
        removeEntity(existing);
        subMap.remove(playerIdentity);

        TextDisplay sub = loc.getWorld().spawn(loc, TextDisplay.class, entity -> {
            entity.text(content);
            entity.setBillboard(statsModule.isFixedBillboardDisplay() ? Display.Billboard.FIXED : Display.Billboard.VERTICAL);
            entity.setVisibleByDefault(false);
            entity.setPersistent(false);
            entity.setLineWidth(10000);
        });

        playerIdentity.getOnlinePlayer().showEntity(Minigame.getInstance().getPlugin(), sub);
        subMap.put(playerIdentity, sub);

        if (!clickMap.containsKey(playerIdentity)) {
            Interaction clickZone = loc.getWorld().spawn(loc, Interaction.class, entity -> {
                entity.setInteractionWidth(2.0f);
                entity.setInteractionHeight(0.5f);
                entity.setVisibleByDefault(false);
                entity.setPersistent(false);
            });
            playerIdentity.getOnlinePlayer().showEntity(Minigame.getInstance().getPlugin(), clickZone);
            clickMap.put(playerIdentity, clickZone);
        }
    }

    private void scheduleTick(PlayerIdentity playerIdentity) {
        BukkitTask old = tickTasks.remove(playerIdentity);
        if (old != null) old.cancel();

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(
                Minigame.getInstance().getPlugin(),
                () -> {
                    if (playerIdentity.getOnlinePlayer() == null || !displayMap.containsKey(playerIdentity)) {
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

    private Component buildMainComponent(PlayerIdentity playerIdentity, Stat stat, LinkedHashMap<String, Integer> topPlayers, int playerRank, int playerStatValue, StatPeriod period, long cacheRemainingMs) {
        Component periodName = ModuleManager.getModule(MessageModule.class)
                .getMessage(playerIdentity, period.getTranslationKey()).toComponent();

        Component component = ModuleManager.getModule(MessageModule.class)
                .getMessage(playerIdentity, "hologram.leaderboard.title")
                .replace("%minigame_name%", Minigame.getInstance().getName())
                .replace("%stat_name%", stat.getTranslated(playerIdentity))
                .replace("%period_name%", periodName)
                .replace("§bLifetime", periodName)
                .toComponent()
                .appendNewline();

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
                    .append(buildRankLine(playerIdentity.getName(), playerStatValue, playerRank, maxLineWidth, true));
        }

        component = component.appendNewline().appendNewline()
                .append(StringUtils.colorizerComponent(formatCountdown(playerIdentity, period, cacheRemainingMs)));

        return component;
    }

    private Component buildStatSubComponent(PlayerIdentity playerIdentity, int currentIndex, int totalStats) {
        return ModuleManager.getModule(MessageModule.class)
                .getMessage(playerIdentity, "hologram.leaderboard.click_stat")
                .replace("%currentIndex%", String.valueOf(currentIndex + 1))
                .replace("%totalStats%", String.valueOf(totalStats))
                .toComponent();
    }

    private Component buildPeriodSubComponent(PlayerIdentity playerIdentity, StatPeriod period) {
        Component periodName = ModuleManager.getModule(MessageModule.class)
                .getMessage(playerIdentity, period.getTranslationKey()).toComponent();

        return ModuleManager.getModule(MessageModule.class)
                .getMessage(playerIdentity, "hologram.leaderboard.click_period")
                .replace("%period_name%", periodName)
                .toComponent();
    }

    private String formatCountdown(PlayerIdentity playerIdentity, StatPeriod period, long cacheRemainingMs) {
        //if (period == StatPeriod.LIFETIME) {
            if (cacheRemainingMs <= 0) {
                return ModuleManager.getModule(MessageModule.class)
                        .getMessage(playerIdentity, "hologram.leaderboard.refreshing")
                        .toString();
            }
            long minutes = cacheRemainingMs / 60000;
            return ModuleManager.getModule(MessageModule.class)
                    .getMessage(playerIdentity, "hologram.leaderboard.will_update_in")
                    .replace("%m%", minutes > 0 ? String.valueOf(minutes) : "<1")
                    .replace("%s%", String.valueOf((cacheRemainingMs / 1000) % 60))
                    .toString();
        /*}

        long resetMs = period.getMillisUntilReset();
        if (resetMs <= 0) {
            return ModuleManager.getModule(MessageModule.class)
                    .get(playerIdentity, "hologram.leaderboard.refreshing")
                    .toString();
        }

        long totalSeconds = resetMs / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;

        return ModuleManager.getModule(MessageModule.class)
                .get(playerIdentity, "hologram.leaderboard.resets_in")
                .replace("%h%", hours > 0 ? hours + "h " : "")
                .replace("%m%", minutes + "m")
                .toString();*/
    }

    private Component buildRankLine(String playerName, int value, int position, int maxWidth, boolean isPlayerRow) {
        String color = isPlayerRow ? "§e" : "§f";
        Component rankComp = Component.text(color + position + ". ");
        Component nameComp = Component.text("§f" + playerName);
        Component valueComp = value >= 0
                ? Component.text("§f" + StringUtils.betterNumberFormat(value))
                : Component.text("§c?");

        int leftWidth = getLength(rankComp) + getLength(nameComp);
        int dotCharWidth = StringUtils.DefaultFontInfo.getDefaultFontInfo('.').getLength();
        int dotCount = Math.max(0, (maxWidth - leftWidth - getLength(valueComp)) / dotCharWidth);

        return Component.empty()
                .append(rankComp)
                .append(nameComp)
                .append(Component.text(StringUtils.calculateNegativeSpaces(dotCount)).font(Key.key("jsplugins", "gameapi")))
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
                .append(Component.text(StringUtils.calculateNegativeSpaces(dotCount)).font(Key.key("jsplugins", "gameapi")))
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
        String text = ChatColor.stripColor(LegacyComponentSerializer.legacySection().serialize(component));
        double width = 0;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            width += StringUtils.DefaultFontInfo.getDefaultFontInfo(ch).getLength();
            if (ch != ' ' && i < text.length() - 1 && text.charAt(i + 1) != ' ') width += 1;
        }
        return (int) width;
    }

    private static void removeEntity(org.bukkit.entity.Entity entity) {
        if (entity != null && !entity.isDead()) entity.remove();
    }

    private static String cacheKey(String statName, StatPeriod period) {
        return statName + ":" + period.name();
    }
}