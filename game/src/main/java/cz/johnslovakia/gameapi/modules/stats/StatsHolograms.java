package cz.johnslovakia.gameapi.modules.stats;

import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.Shared;
import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.messages.MessageModule;
import cz.johnslovakia.gameapi.users.PlayerData;
import cz.johnslovakia.gameapi.users.PlayerIdentity;
import cz.johnslovakia.gameapi.utils.StringUtils;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.TextDisplay;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatsHolograms {

    private final StatsModule statsModule;

    //TODO: zkontrolovat k ram x....
    private final Map<PlayerIdentity, TextDisplay> textDisplayMap = new HashMap<>();

    public StatsHolograms(StatsModule statsModule) {
        this.statsModule = statsModule;
    }

    public void createPlayerStatisticsHologram(PlayerIdentity playerIdentity, Location location){
        List<Stat> stats = statsModule.getStats().stream().filter(Stat::isShowToPlayer).toList();

        Component title = ModuleManager.getModule(MessageModule.class).get(playerIdentity, "hologram.lifetime_stats")
                .replace("%minigame_name%", Minigame.getInstance().getName()).getTranslated();
        //Component title = Component.text("§a" + Minigame.getInstance().getName() + " §7- §fYour statistics");

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
           Component component = title
                    .appendNewline();

            for (int i = 0; i < stats.size(); i++) {
                Stat stat = stats.get(i);
                int playerStat = statsModule.getPlayerStat(playerIdentity, stat.getName());

                int dotCharWidth = StringUtils.DefaultFontInfo.getDefaultFontInfo('.').getLength();
                int dotCount = Math.max(0,
                        (finalMaxLength
                                - getLength(stat.getTranslated(playerIdentity))
                                - getLength(Component.text(playerStat)))
                                / dotCharWidth
                );

                if (ModuleManager.getModule(MessageModule.class).existMessage("hologram.lifetime_stats.score_line")) {
                    component = component.append(
                            ModuleManager.getModule(MessageModule.class).get(playerIdentity, "hologram.lifetime_stats.score_line")
                                    .replace("%stat_name%", stat.getTranslated(playerIdentity))
                                    .replace("%value%", String.valueOf(playerStat))
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


            entity.text(component);
            entity.setBillboard(Display.Billboard.FIXED);
            entity.setVisibleByDefault(false);
            entity.setPersistent(false);
        });

        playerIdentity.getOnlinePlayer().showEntity(Minigame.getInstance().getPlugin(), display);
        textDisplayMap.put(playerIdentity, display);
    }

    private int getLength(Component component){
        if (component == null)
            return 0;

        String text = ChatColor.stripColor(LegacyComponentSerializer.legacySection().serialize(component));

        double textWidth = 0;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            textWidth += StringUtils.DefaultFontInfo.getDefaultFontInfo(ch).getLength();

            if (ch != ' ' && i < text.length() - 1 && text.charAt(i + 1) != ' ') {
                textWidth += 1;
            }
        }
        return (int) textWidth;
    }

    public void showTOPStatisticHologram(Location location, PlayerIdentity playerIdentity) {
        //TODO: dodělat, ?optimalizovat at se to nedotazuje pro každého hráče

        /*GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);
        statsModule manager = Minigame.getInstance().getstatsModule();
        List<Stat> statList = manager.getStats();

        Stat oldStat = gamePlayer.getMetadata().get("top_stats_hologram_stat") != null ? (Stat) gamePlayer.getMetadata().get("top_stats_hologram_stat") : null;
        Stat nextStat;
        if (oldStat != null){
            int currentIndex = statList.indexOf(oldStat);
            nextStat = (currentIndex + 1 < statList.size()) ? statList.get(currentIndex + 1) : statList.get(0);
        }else{
            nextStat = statList.get(0);
        }


        TextDisplay display = location.getWorld().spawn(location, TextDisplay.class, entity -> {
            Component title = ModuleManager.getModule(MessageModule.class).get(gamePlayer, "hologram.top_stats").replace("%stat%", nextStat.getTranslated(gamePlayer)).getTranslated();
            Component component = title.appendNewline();

            component = component.append(ModuleManager.getModule(MessageModule.class).get(gamePlayer, "hologram.top_stats").replace("%stat%", nextStat.getTranslated(gamePlayer)).getTranslated()
                    .appendNewline());

            List<Map.Entry<String, Integer>> topMap = Minigame.getInstance().getstatsModule().getTable().topStats(nextStat.getName(), 10).entrySet()
                    .stream()
                    .sorted((o1, o2) -> -Integer.compare(o1.getValue(), o2.getValue()))
                    .limit(10)
                    .toList();

            for (int i = 0; i <= 9; i++){
                if (i < topMap.size()) {
                    Map.Entry<String, Integer> map = topMap.get(i);

                    if (ModuleManager.getModule(MessageModule.class).existMessage("hologram.top_stats.score_line")){
                        component = component.append(ModuleManager.getModule(MessageModule.class).get(player, "hologram.top_stats.score_line")
                                .replace("%stat_name%", nextStat.getTranslated(gamePlayer))
                                .replace("%value%", "" + map.getValue())
                                .replace("%player%", map.getKey())
                                .replace("%position%", "" + (i + 1)).getTranslated());
                    }else {
                        component = component.append(Component.text("§f" + (i + 1) + ". §a" + map.getKey() + " §8- §a" + map.getValue() + " ").append(nextStat.getTranslated(gamePlayer)));
                    }
                }else{
                    component = component.append(Component.text("§f" + (i + 1) + ". §8-"));
                }
                component = component.append(Component.newline());
            }

            component = component.append(Component.newline());
            component = component.append(ModuleManager.getModule(MessageModule.class).get(gamePlayer, "hologram.next_page").getTranslated());

            entity.text(component);
            entity.setVisibleByDefault(false);
            entity.setPersistent(false);
        });

        player.showEntity(Minigame.getInstance().getPlugin(), display);
        gamePlayer.getMetadata().put("top_stats_hologram_stat", nextStat);
        */
    }


    public void remove(PlayerIdentity playerIdentity){
        if (textDisplayMap.containsKey(playerIdentity)){
            TextDisplay textDisplay = (TextDisplay) textDisplayMap.get(playerIdentity);
            textDisplay.remove();
            textDisplayMap.remove(playerIdentity);
        }
        /*if (config.getLocation("topStatsHologram") != null) {
            if (DHAPI.getHologram("topStats_" + player.getName()) != null) {
                DHAPI.removeHologram("topStats_" + player.getName());
            }
        }*/
    }
}
