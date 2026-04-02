package cz.johnslovakia.gameapi.modules.stats;

import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.messages.MessageModule;
import cz.johnslovakia.gameapi.users.PlayerIdentity;
import cz.johnslovakia.gameapi.utils.StringUtils;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LifetimeStatsHologram {

    private final StatsModule statsModule;
    private final Map<PlayerIdentity, TextDisplay> displayMap = new ConcurrentHashMap<>();

    public LifetimeStatsHologram(StatsModule statsModule) {
        this.statsModule = statsModule;
    }

    public void create(PlayerIdentity playerIdentity, Location location) {
        remove(playerIdentity);

        List<Stat> stats = statsModule.getStats().stream().filter(Stat::isShowToPlayer).toList();

        Component title = ModuleManager.getModule(MessageModule.class).get(playerIdentity, "hologram.lifetime_stats")
                .replace("%minigame_name%", Minigame.getInstance().getName()).getTranslated();

        int maxLength = stats.stream()
                .mapToInt(stat -> {
                    Component name = stat.getTranslated(playerIdentity);
                    Component score = Component.text(statsModule.getPlayerStat(playerIdentity, stat.getName()));
                    return getLength(name) + getLength(score);
                })
                .max()
                .orElse(0);

        maxLength = Math.max(maxLength + (stats.size() > 7 ? 20 : 10), getLength(title));

        int finalMaxLength = maxLength;

        TextDisplay display = location.getWorld().spawn(location, TextDisplay.class, entity -> {
            entity.text(buildComponent(playerIdentity, title, stats, finalMaxLength));
            entity.setBillboard(statsModule.isFixedBillboardDisplay() ? Display.Billboard.FIXED : Display.Billboard.VERTICAL);
            entity.setVisibleByDefault(false);
            entity.setPersistent(false);
        });

        playerIdentity.getOnlinePlayer().showEntity(Minigame.getInstance().getPlugin(), display);
        displayMap.put(playerIdentity, display);
    }

    public void remove(PlayerIdentity playerIdentity) {
        TextDisplay display = displayMap.remove(playerIdentity);
        if (display != null && !display.isDead()) {
            display.remove();
        }
    }

    public void removeAll() {
        for (PlayerIdentity identity : displayMap.keySet()) {
            remove(identity);
        }
        displayMap.clear();
    }

    private Component buildComponent(PlayerIdentity playerIdentity, Component title, List<Stat> stats, int maxLength) {
        Component component = title.appendNewline();

        for (int i = 0; i < stats.size(); i++) {
            Stat stat = stats.get(i);
            int playerStat = statsModule.getPlayerStat(playerIdentity, stat.getName());

            int dotCharWidth = StringUtils.DefaultFontInfo.getDefaultFontInfo('.').getLength();
            int dotCount = Math.max(0,
                    (maxLength
                            - getLength(stat.getTranslated(playerIdentity))
                            - getLength(Component.text(playerStat)))
                            / dotCharWidth
            );

            if (ModuleManager.getModule(MessageModule.class).existMessage("hologram.lifetime_stats.score_line")) {
                component = component.append(
                        ModuleManager.getModule(MessageModule.class).get(playerIdentity, "hologram.lifetime_stats.score_line")
                                .replace("%stat_name%", stat.getTranslated(playerIdentity))
                                .replace("%value%", StringUtils.betterNumberFormat(playerStat))
                                .replace("%space_pad%", Component.text(StringUtils.calculateNegativeSpaces(dotCount)))
                                .getTranslated()
                );
            } else {
                component = component
                        .append(Component.text("§f"))
                        .append(stat.getTranslated(playerIdentity))
                        .append(Component.text(StringUtils.calculateNegativeSpaces(dotCount))
                                .font(Key.key("jsplugins", "gameapi")))
                        .append(Component.text(" §a" + playerStat));
            }

            if (i < stats.size() - 1) {
                component = component.appendNewline();
            }
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
            if (ch != ' ' && i < text.length() - 1 && text.charAt(i + 1) != ' ') {
                width += 1;
            }
        }
        return (int) width;
    }
}