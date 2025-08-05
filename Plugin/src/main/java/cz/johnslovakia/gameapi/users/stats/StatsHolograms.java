package cz.johnslovakia.gameapi.users.stats;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerManager;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.utils.ConfigAPI;
import cz.johnslovakia.gameapi.utils.StringUtils;
import eu.decentsoftware.holograms.api.DHAPI;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;

import java.util.List;
import java.util.Map;

public class StatsHolograms {

    public static void createPlayerStatisticsHologram(Location location, Player player){
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);
        StatsManager manager = Minigame.getInstance().getStatsManager();

        List<Stat> stats = manager.getStats().stream().filter(Stat::isShowToPlayer).toList();

        Component title = MessageManager.get(gamePlayer, "hologram.lifetime_stats")
                .replace("%minigame_name%", Minigame.getInstance().getName()).getTranslated();
        //Component title = Component.text("§a" + Minigame.getInstance().getName() + " §7- §fYour statistics");

        int maxLength = stats.stream()
                .mapToInt(stat -> {
                    Component name = stat.getTranslated(gamePlayer);
                    Component score = Component.text(gamePlayer.getPlayerData().getPlayerStat(stat).getStatScore());
                    return getLength(name) + getLength(score);
                })
                .max()
                .orElse(0);

        maxLength = Math.max(maxLength + (stats.size() > 7 ? 20 : 10), getLength(title));


        int finalMaxLength = maxLength;
        TextDisplay display = location.getWorld().spawn(location, TextDisplay.class, entity -> {
           Component component = title
                    .appendNewline();

            for (Stat stat : stats){
                PlayerStat playerStat = gamePlayer.getPlayerData().getPlayerStat(stat);

                int dotCharWidth = StringUtils.DefaultFontInfo.getDefaultFontInfo('.').getLength();
                int dotCount = Math.max(0, (finalMaxLength - getLength(stat.getTranslated(gamePlayer)) - getLength(Component.text(playerStat.getScore()))) / dotCharWidth);

                if (MessageManager.existMessage("hologram.lifetime_stats.score_line")){
                    component = component.append(MessageManager.get(player, "hologram.lifetime_stats.score_line")
                        .replace("%stat_name%", stat.getTranslated(gamePlayer))
                        .replace("%value%", "" + playerStat.getStatScore())
                        .replace("%space_pad%", Component.text(StringUtils.calculateNegativeSpaces(dotCount)))
                        .getTranslated());
                }else {
                    component = component
                            .append(Component.text("§f"))
                            .append(stat.getTranslated(gamePlayer))
                            .append(Component.text(StringUtils.calculateNegativeSpaces(dotCount))
                                    .font(Key.key("jsplugins", "gameapi")))
                            .append(Component.text( " §a" + playerStat.getStatScore()));
                }

                component = component.appendNewline();
            }


            entity.text(component);

            entity.setBillboard(Display.Billboard.FIXED);
            entity.setVisibleByDefault(false);
            entity.setPersistent(false);
        });

        player.showEntity(Minigame.getInstance().getPlugin(), display);
    }

    private static int getLength(Component component){
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

    private static Component getTextWithBackgroundBackground(Component component) {
        int textWidth = getLength(component);

        final int BACKGROUND_CHAR_WIDTH = 1;
        final int EDGE_CHAR_WIDTH = 1;
        final int PADDING_PIXELS = 4;

        double desiredBackgroundPixelWidth = textWidth + (2 * PADDING_PIXELS) + (2 * EDGE_CHAR_WIDTH);

        int numBackgroundChars = (int) Math.ceil(desiredBackgroundPixelWidth / BACKGROUND_CHAR_WIDTH);
        numBackgroundChars = Math.max(numBackgroundChars, 1);

        Component backgroundComponent = Component.text("Ẋ")
                .append(Component.text("񎾜ẉ".repeat(numBackgroundChars)))
                .append(Component.text("񎾜Ẋ"))
                .shadowColor(ShadowColor.shadowColor(0))
                .font(Key.key("jsplugins", "bossbar_offset"));

        double actualBackgroundPixelWidth = (numBackgroundChars * BACKGROUND_CHAR_WIDTH) + (2 * EDGE_CHAR_WIDTH);
        double textStartX = (actualBackgroundPixelWidth / 2.0) - (textWidth / 2.0);
        int negativeSpaceValue = (int) -(actualBackgroundPixelWidth - textStartX);

        Component negativeSpaceComponent = Component.text(StringUtils.calculateNegativeSpaces(negativeSpaceValue))
                .font(Key.key("jsplugins", "bossbar_offset"));


        return backgroundComponent.append(negativeSpaceComponent).append(component);
    }

    public static void showTOPStatisticHologram(Location location, Player player) {
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);
        StatsManager manager = Minigame.getInstance().getStatsManager();
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
            entity.text(MessageManager.get(gamePlayer, "hologram.top_stats").replace("%stat%", nextStat.getTranslated(gamePlayer)).getTranslated()
                    .appendNewline());

            List<Map.Entry<String, Integer>> topMap = Minigame.getInstance().getStatsManager().getTable().topStats(nextStat.getName(), 10).entrySet()
                    .stream()
                    .sorted((o1, o2) -> -Integer.compare(o1.getValue(), o2.getValue()))
                    .limit(10)
                    .toList();

            for (int i = 0; i <= 9; i++){
                if (i < topMap.size()) {
                    Map.Entry<String, Integer> map = topMap.get(i);

                    if (MessageManager.existMessage("hologram.top_stats.score_line")){
                        entity.text(MessageManager.get(player, "hologram.top_stats.score_line")
                                .replace("%stat_name%", nextStat.getTranslated(gamePlayer))
                                .replace("%value%", "" + map.getValue())
                                .replace("%player%", map.getKey())
                                .replace("%position%", "" + (i + 1)).getTranslated());
                    }else {
                        entity.text(Component.text("§f" + (i + 1) + ". §a" + map.getKey() + " §8- §a" + map.getValue() + " ").append(nextStat.getTranslated(gamePlayer)));
                    }
                }else{
                    entity.text(Component.text("§f" + (i + 1) + ". §8-"));
                }
                entity.text(Component.newline());
            }

            entity.text(Component.newline());
            entity.text(MessageManager.get(gamePlayer, "hologram.next_page").getTranslated());


            entity.setVisibleByDefault(false);
            entity.setPersistent(false);
        });

        player.showEntity(Minigame.getInstance().getPlugin(), display);
        gamePlayer.getMetadata().put("top_stats_hologram_stat", nextStat);
    }


    public static void remove(Player player){
        ConfigAPI config = new ConfigAPI(GameAPI.getInstance().getMinigameDataFolder().toString(), "config.yml", Minigame.getInstance().getPlugin());
        if (config.getLocation("statsHologram") != null) {
            if (DHAPI.getHologram("stats_" + player.getName()) != null) {
                DHAPI.removeHologram("stats_" + player.getName());
            }
        }
        if (config.getLocation("topStatsHologram") != null) {
            if (DHAPI.getHologram("topStats_" + player.getName()) != null) {
                DHAPI.removeHologram("topStats_" + player.getName());
            }
        }
    }
}
