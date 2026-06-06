package cz.johnslovakia.gameapi.modules.settings;

import lombok.Getter;
import org.bukkit.Material;

@Getter
public final class RewardEventDefinition {

    private final String eventKey;
    private final Material icon;
    private final String label;
    private final String description;

    private RewardEventDefinition(String eventKey, Material icon, String label, String description) {
        this.eventKey = eventKey; this.icon = icon;
        this.label = label; this.description = description;
    }

    public static RewardEventDefinition of(String eventKey, Material icon, String label, String description) {
        return new RewardEventDefinition(eventKey, icon, label, description);
    }

    public boolean isDaily() {
        return eventKey.startsWith("daily.");
    }

    public boolean isStandard() {
        return eventKey.startsWith("standard.");
    }
}