package cz.johnslovakia.gameapi.modules.stats;

import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.messages.MessageModule;
import cz.johnslovakia.gameapi.users.PlayerIdentity;
import cz.johnslovakia.gameapi.users.PlayerIdentityRegistry;
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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerStatsHologram implements Listener {

    private static final double PERIOD_SUB_Y_OFFSET = 0.45;

    private final StatsModule statsModule;
    private final Map<PlayerIdentity, TextDisplay> displayMap = new ConcurrentHashMap<>();
    private final Map<PlayerIdentity, TextDisplay> periodSubDisplayMap = new ConcurrentHashMap<>();
    private final Map<PlayerIdentity, Interaction> clickZoneMap = new ConcurrentHashMap<>();
    private final Map<PlayerIdentity, Location> locationMap = new ConcurrentHashMap<>();
    private final Map<PlayerIdentity, StatPeriod> playerPeriod = new ConcurrentHashMap<>();

    public PlayerStatsHologram(StatsModule statsModule) {
        this.statsModule = statsModule;
        Bukkit.getPluginManager().registerEvents(this, Minigame.getInstance().getPlugin());
    }

    public void create(PlayerIdentity playerIdentity, Location location) {
        remove(playerIdentity);
        locationMap.put(playerIdentity, location);
        playerPeriod.putIfAbsent(playerIdentity, statsModule.getDefaultDisplayPeriod());
        draw(playerIdentity);
    }

    public void remove(PlayerIdentity playerIdentity) {
        TextDisplay display = displayMap.remove(playerIdentity);
        if (display != null && !display.isDead()) display.remove();

        TextDisplay periodSub = periodSubDisplayMap.remove(playerIdentity);
        if (periodSub != null && !periodSub.isDead()) periodSub.remove();

        Interaction clickZone = clickZoneMap.remove(playerIdentity);
        if (clickZone != null && !clickZone.isDead()) clickZone.remove();

        locationMap.remove(playerIdentity);
        playerPeriod.remove(playerIdentity);
    }

    public void removeAll() {
        for (PlayerIdentity identity : List.copyOf(displayMap.keySet())) remove(identity);
        displayMap.clear();
        periodSubDisplayMap.clear();
        clickZoneMap.clear();
        locationMap.clear();
        playerPeriod.clear();
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent e) {
        if (!(e.getRightClicked() instanceof Interaction clicked)) return;

        Player player = e.getPlayer();
        PlayerIdentity identity = PlayerIdentityRegistry.get(player);
        if (identity == null) return;

        Interaction clickZone = clickZoneMap.get(identity);
        if (clickZone == null || !clickZone.equals(clicked)) return;

        e.setCancelled(true);

        StatPeriod current = playerPeriod.getOrDefault(identity, statsModule.getDefaultDisplayPeriod());
        playerPeriod.put(identity, current.next());

        TextDisplay old = displayMap.remove(identity);
        if (old != null && !old.isDead()) old.remove();

        TextDisplay oldSub = periodSubDisplayMap.remove(identity);
        if (oldSub != null && !oldSub.isDead()) oldSub.remove();

        draw(identity);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
    }

    private void draw(PlayerIdentity playerIdentity) {
        Location location = locationMap.get(playerIdentity);
        if (location == null) return;

        StatPeriod period = playerPeriod.getOrDefault(playerIdentity, statsModule.getDefaultDisplayPeriod());
        List<Stat> stats = statsModule.getStats().stream().filter(Stat::isShowToPlayer).toList();

        Component periodName = ModuleManager.getModule(MessageModule.class)
                .get(playerIdentity, period.getTranslationKey()).getTranslated();

        Component title = ModuleManager.getModule(MessageModule.class)
                .get(playerIdentity, "hologram.player_stats")
                .replace("%minigame_name%", Minigame.getInstance().getName())
                .replace("%period_name%", periodName)
                .getTranslated();

        int maxLength = stats.stream()
                .mapToInt(stat -> {
                    Component name = stat.getTranslated(playerIdentity);
                    Component score = Component.text(statsModule.getPlayerStat(playerIdentity, stat.getName(), period));
                    return getLength(name) + getLength(score);
                })
                .max().orElse(0);

        maxLength = Math.max(maxLength + (stats.size() > 7 ? 20 : 10), getLength(title));

        int finalMaxLength = maxLength;

        TextDisplay display = location.getWorld().spawn(location, TextDisplay.class, entity -> {
            entity.text(buildMainComponent(playerIdentity, title, stats, period, finalMaxLength));
            entity.setBillboard(statsModule.isFixedBillboardDisplay() ? Display.Billboard.FIXED : Display.Billboard.VERTICAL);
            entity.setVisibleByDefault(false);
            entity.setPersistent(false);
        });

        playerIdentity.getOnlinePlayer().showEntity(Minigame.getInstance().getPlugin(), display);
        displayMap.put(playerIdentity, display);

        Component periodSubContent = ModuleManager.getModule(MessageModule.class)
                .get(playerIdentity, "hologram.player_stats.click_period")
                .replace("%period_name%", periodName)
                .getTranslated();

        Location subLoc = location.clone().subtract(0, 0.6, 0);

        TextDisplay periodSub = subLoc.getWorld().spawn(subLoc, TextDisplay.class, entity -> {
            entity.text(periodSubContent);
            entity.setBillboard(statsModule.isFixedBillboardDisplay() ? Display.Billboard.FIXED : Display.Billboard.VERTICAL);
            entity.setVisibleByDefault(false);
            entity.setPersistent(false);
        });

        playerIdentity.getOnlinePlayer().showEntity(Minigame.getInstance().getPlugin(), periodSub);
        periodSubDisplayMap.put(playerIdentity, periodSub);

        if (!clickZoneMap.containsKey(playerIdentity)) {
            Interaction clickZone = subLoc.getWorld().spawn(subLoc, Interaction.class, entity -> {
                entity.setInteractionWidth(2.0f);
                entity.setInteractionHeight(0.5f);
                entity.setVisibleByDefault(false);
                entity.setPersistent(false);
            });
            playerIdentity.getOnlinePlayer().showEntity(Minigame.getInstance().getPlugin(), clickZone);
            clickZoneMap.put(playerIdentity, clickZone);
        }
    }

    private Component buildMainComponent(PlayerIdentity playerIdentity, Component title, List<Stat> stats, StatPeriod period, int maxLength) {
        Component component = title.appendNewline();

        for (int i = 0; i < stats.size(); i++) {
            Stat stat = stats.get(i);
            int value = statsModule.getPlayerStat(playerIdentity, stat.getName(), period);

            int dotCharWidth = StringUtils.DefaultFontInfo.getDefaultFontInfo('.').getLength();
            int dotCount = Math.max(0,
                    (maxLength - getLength(stat.getTranslated(playerIdentity)) - getLength(Component.text(value))) / dotCharWidth
            );

            component = component.append(
                    ModuleManager.getModule(MessageModule.class).get(playerIdentity, "hologram.player_stats.score_line")
                            .replace("%stat_name%", stat.getTranslated(playerIdentity))
                            .replace("%value%", StringUtils.betterNumberFormat(value))
                            .replace("%space_pad%", Component.text(StringUtils.calculateNegativeSpaces(dotCount)))
                            .getTranslated()
            );

            if (i < stats.size() - 1) component = component.appendNewline();
        }

        return component;
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
}