package cz.johnslovakia.gameapi.utils;

import lombok.Getter;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

@Getter
public class PlayerBossBar {
    @Getter
    private static final Map<UUID, PlayerBossBar> bossBarMap = new WeakHashMap<>();

    public static PlayerBossBar getBossBar(UUID uuid){
        return bossBarMap.get(uuid);
    }

    public static PlayerBossBar getOrCreateBossBar(UUID uuid, Component component){
        return bossBarMap.computeIfAbsent(uuid, id -> new PlayerBossBar(id, component));
    }

    public static void removeBossBar(UUID uuid){
        if (!bossBarMap.containsKey(uuid))
            return;

        Player player = Bukkit.getPlayer(uuid);
        if (player != null)
            getBossBar(uuid).getBossBar().removeViewer(player);

        bossBarMap.remove(uuid);
    }

    private final UUID uuid;
    private final BossBar bossBar;

    public PlayerBossBar(UUID uuid, Component component) {
        this.uuid = uuid;

        this.bossBar = BossBar.bossBar(Component.text("boss_bar:" + uuid), 1, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS);
        setName(component);

        Player player = Bukkit.getPlayer(uuid);
        if (player == null)
            return;
        player.showBossBar(getBossBar());

        bossBarMap.put(uuid, this);
    }



    //TODO: mazat cache
    //TODO: možnost vybrat font

    public void setName(Component component){
        Component name = Component.text("")
                .append(TextBackground.getTextWithBackgroundBossBar(component));
        getBossBar().name(name);
    }

    public void setName(Component left, Component center, Component right) {
        boolean hasLeft = left != null;
        boolean hasCenter = center != null;
        boolean hasRight = right != null;


        Component name = Component.empty();


        int spacing = 130;
        int leftSpace = (center != null ? spacing - StringUtils.getLength(left) + 8 : spacing);
        int rightSpace = (center != null ? spacing - StringUtils.getLength(right) + 8: spacing);

        if (hasLeft && hasCenter && hasRight) {
            name = name
                    .append(TextBackground.getTextWithBackgroundBossBar(left))
                    .append(off(leftSpace))
                    .append(TextBackground.getTextWithBackgroundBossBar(center))
                    .append(off(rightSpace))
                    .append(TextBackground.getTextWithBackgroundBossBar(right));
        } else if (hasLeft && hasCenter) {
            name = name
                    .append(off(-(leftSpace + StringUtils.getLength(left) + 8)))
                    .append(TextBackground.getTextWithBackgroundBossBar(left))
                    .append(off(leftSpace))
                    .append(TextBackground.getTextWithBackgroundBossBar(center));
        } else if (hasCenter && hasRight) {
            name = name
                    .append(off(rightSpace + StringUtils.getLength(right) + 8))
                    .append(TextBackground.getTextWithBackgroundBossBar(center))
                    .append(off(rightSpace))
                    .append(TextBackground.getTextWithBackgroundBossBar(right));
        } else if (hasCenter) {
            name = name.append(TextBackground.getTextWithBackgroundBossBar(center));
        }

        getBossBar().name(name);
    }


    private Component off(int px) {
        return Component.text(StringUtils.calculateNegativeSpaces(px)).font(Key.key("jsplugins", "gameapi"));
    }



    /*public Component getBackground(String text) {
        double textWidth = 0;
        for (char ch : text.toCharArray()){
            textWidth += StringUtils.DefaultFontInfo.getDefaultFontInfo(ch).getLength() + 1;
        }


        int backgroundCharCount = (int) Math.ceil(textWidth / 13);
        Component backgroundComponent = Component.text(("\uD8FB\uDF9Cẉ").repeat(backgroundCharCount));

        double size = backgroundCharCount * 13;

        Logger.log(backgroundCharCount + " " + textWidth + " " + size, Logger.LogType.INFO);

        return backgroundComponent
                .shadowColor(ShadowColor.shadowColor(0))
                .append(backgroundComponent)
                .append(Component.text(StringUtils.calculateNegativeSpaces(-((int) size))))
                .font(Key.key("jsplugins", "bossbar_offset"));
    }*/
}
