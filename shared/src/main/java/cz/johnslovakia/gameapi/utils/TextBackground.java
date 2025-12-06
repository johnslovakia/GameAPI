package cz.johnslovakia.gameapi.utils;

import lombok.Getter;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class TextBackground {

    @Getter
    public static final Map<Component, Component> backgroundCache = new LinkedHashMap<>() {
        protected boolean removeEldestEntry(Map.Entry eldest) {
            return size() > 20;
        }
    };


    public static Component getTextWithBackgroundBossBar(Component component) {
        String plainText = LegacyComponentSerializer.legacySection().serialize(component);


        Component cached = null;

        Pattern dynamicPattern = Pattern.compile("\\d+:\\d+");
        if (dynamicPattern.matcher(plainText).matches()) {
            for (Map.Entry<Component, Component> entry : backgroundCache.entrySet()) {
                if (dynamicPattern.matcher(LegacyComponentSerializer.legacySection().serialize(entry.getKey())).matches()) {
                    cached = entry.getValue();
                    break;
                }
            }
        } else {
            cached = backgroundCache.get(component);
        }

        if (cached != null)
            return cached.append(component.font(Key.key("jsplugins", "bossbar_offset")).shadowColor(ShadowColor.shadowColor(0)));



        int textWidth = StringUtils.getLength(component);

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
                .font(Key.key("jsplugins", "gameapi"));


        Component finalComponent = backgroundComponent.append(negativeSpaceComponent);
        backgroundCache.put(component, finalComponent);

        return finalComponent.append(component.shadowColor(ShadowColor.shadowColor(0)));
    }

    public static Component getTextWithBackground(Component component) {
        String plainText = LegacyComponentSerializer.legacySection().serialize(component);


        Component cached = null;

        Pattern dynamicPattern = Pattern.compile("\\d+:\\d+");
        if (dynamicPattern.matcher(plainText).matches()) {
            for (Map.Entry<Component, Component> entry : backgroundCache.entrySet()) {
                if (dynamicPattern.matcher(LegacyComponentSerializer.legacySection().serialize(entry.getKey())).matches()) {
                    cached = entry.getValue();
                    break;
                }
            }
        } else {
            cached = backgroundCache.get(component);
        }

        if (cached != null)
            return cached.append(component).shadowColor(ShadowColor.shadowColor(0));



        int textWidth = StringUtils.getLength(component);

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
                .font(Key.key("jsplugins", "gameapi"));

        double actualBackgroundPixelWidth = (numBackgroundChars * BACKGROUND_CHAR_WIDTH) + (2 * EDGE_CHAR_WIDTH);
        double textStartX = (actualBackgroundPixelWidth / 2.0) - (textWidth / 2.0);
        int negativeSpaceValue = (int) -(actualBackgroundPixelWidth - textStartX);

        Component negativeSpaceComponent = Component.text(StringUtils.calculateNegativeSpaces(negativeSpaceValue))
                .font(Key.key("jsplugins", "gameapi"));


        Component finalComponent = backgroundComponent.append(negativeSpaceComponent);
        backgroundCache.put(component, finalComponent);

        return finalComponent.append(component.shadowColor(ShadowColor.shadowColor(0)));
    }
}
