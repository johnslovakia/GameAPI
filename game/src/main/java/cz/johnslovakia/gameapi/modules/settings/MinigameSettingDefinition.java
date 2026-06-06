package cz.johnslovakia.gameapi.modules.settings;

import lombok.Getter;
import org.bukkit.Material;

@Getter
public final class MinigameSettingDefinition {

    public enum Type { INT, BOOL }

    private final String key;
    private final Type type;
    private final Material icon;
    private final String name, description;

    private final int defaultInt, min, max;
    private final boolean defaultBool;

    private MinigameSettingDefinition(IntBuilder b) {
        this.key = b.key;
        this.type = Type.INT;
        this.icon = b.icon;
        this.name = b.name;
        this.description = b.description;
        this.defaultInt = b.defaultValue;
        this.min = b.min;
        this.max = b.max;
        this.defaultBool = false;
    }

    private MinigameSettingDefinition(BoolBuilder b) {
        this.key = b.key;
        this.type = Type.BOOL;
        this.icon = b.icon;
        this.name = b.name;
        this.description = b.description;
        this.defaultBool = b.defaultValue;
        this.defaultInt = 0;
        this.min = 0;
        this.max = 0;
    }

    public static IntBuilder intSetting(String key) {
        return new IntBuilder(key);
    }

    public static BoolBuilder boolSetting(String key) {
        return new BoolBuilder(key);
    }

    public boolean getDefaultBool() {
        return defaultBool;
    }

    public static final class IntBuilder {
        private final String key;
        private Material icon = Material.PAPER;
        private String name = "Setting";
        private String description = "";
        private int defaultValue = 0;
        private int min = 1;
        private int max = Integer.MAX_VALUE;

        private IntBuilder(String key) { this.key = key; }

        public IntBuilder icon(Material icon) {
            this.icon = icon;
            return this;

        }

        public IntBuilder name(String name) {
            this.name = name;
            return this;
        }

        public IntBuilder description(String desc) {
            this.description = desc;
            return this;
        }

        public IntBuilder defaultValue(int val) {
            this.defaultValue = val;
            return this;
        }

        public IntBuilder range(int min, int max) {
            this.min = min;
            this.max = max;
            return this;
        }

        public MinigameSettingDefinition build() {
            return new MinigameSettingDefinition(this);
        }
    }

    public static final class BoolBuilder {
        private final String key;
        private Material icon = Material.LEVER;
        private String name = "Toggle";
        private String description = "";
        private boolean defaultValue = false;

        private BoolBuilder(String key) { this.key = key; }

        public BoolBuilder icon(Material icon) {
            this.icon = icon;
            return this;
        }

        public BoolBuilder name(String name) {
            this.name = name;
            return this;
        }

        public BoolBuilder description(String desc) {
            this.description = desc;
            return this;
        }

        public BoolBuilder defaultValue(boolean val) {
            this.defaultValue = val;
            return this;
        }

        public MinigameSettingDefinition build() {
            return new MinigameSettingDefinition(this);
        }
    }
}
