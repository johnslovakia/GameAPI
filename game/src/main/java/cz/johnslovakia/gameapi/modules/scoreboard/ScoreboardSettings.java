package cz.johnslovakia.gameapi.modules.scoreboard;

import lombok.Getter;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

@Getter
public final class ScoreboardSettings {

    private final String resourceDirectory;
    private final String fileSuffix;
    private final String section;
    private final String defaultTitle;
    private final long updateDelayTicks;
    private final long updatePeriodTicks;
    private final boolean autoCreate;
    private final boolean updateOnGameEvents;
    private final boolean copyResourceFiles;
    private final boolean usePlaceholderAPI;
    private final boolean registerDefaultPlaceholders;
    private final boolean registerDefaultTriggers;
    private final boolean periodicUpdates;
    private final Map<String, ScoreboardPlaceholder> placeholders;
    private final List<ScoreboardUpdateTrigger<?>> triggers;

    private ScoreboardSettings(Builder builder) {
        this.resourceDirectory = builder.resourceDirectory;
        this.fileSuffix = builder.fileSuffix;
        this.section = builder.section;
        this.defaultTitle = builder.defaultTitle;
        this.updateDelayTicks = builder.updateDelayTicks;
        this.updatePeriodTicks = builder.updatePeriodTicks;
        this.autoCreate = builder.autoCreate;
        this.updateOnGameEvents = builder.updateOnGameEvents;
        this.copyResourceFiles = builder.copyResourceFiles;
        this.usePlaceholderAPI = builder.usePlaceholderAPI;
        this.registerDefaultPlaceholders = builder.registerDefaultPlaceholders;
        this.registerDefaultTriggers = builder.registerDefaultTriggers;
        this.periodicUpdates = builder.periodicUpdates;
        this.placeholders = Collections.unmodifiableMap(new LinkedHashMap<>(builder.placeholders));
        this.triggers = Collections.unmodifiableList(new ArrayList<>(builder.triggers));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static ScoreboardSettings defaults() {
        return builder().build();
    }

    public static final class Builder {

        private String resourceDirectory = "languages";
        private String fileSuffix = "_scoreboard.yml";
        private String section = "scoreboard";
        private String defaultTitle;
        private long updateDelayTicks = 20L;
        private long updatePeriodTicks = 20L;
        private boolean autoCreate = true;
        private boolean updateOnGameEvents = true;
        private boolean copyResourceFiles = true;
        private boolean usePlaceholderAPI = true;
        private boolean registerDefaultPlaceholders = true;
        private boolean registerDefaultTriggers = true;
        private boolean periodicUpdates = false;
        private final Map<String, ScoreboardPlaceholder> placeholders = new LinkedHashMap<>();
        private final List<ScoreboardUpdateTrigger<?>> triggers = new ArrayList<>();

        private Builder() {
        }

        public Builder resourceDirectory(String resourceDirectory) {
            this.resourceDirectory = cleanPath(resourceDirectory);
            return this;
        }

        public Builder fileSuffix(String fileSuffix) {
            this.fileSuffix = Objects.requireNonNull(fileSuffix, "fileSuffix");
            return this;
        }

        public Builder section(String section) {
            this.section = Objects.requireNonNull(section, "section");
            return this;
        }

        public Builder defaultTitle(String defaultTitle) {
            this.defaultTitle = defaultTitle;
            return this;
        }

        public Builder updateDelayTicks(long updateDelayTicks) {
            this.updateDelayTicks = Math.max(0L, updateDelayTicks);
            return this;
        }

        public Builder updatePeriodTicks(long updatePeriodTicks) {
            this.updatePeriodTicks = Math.max(1L, updatePeriodTicks);
            return this;
        }

        public Builder autoCreate(boolean autoCreate) {
            this.autoCreate = autoCreate;
            return this;
        }

        public Builder updateOnGameEvents(boolean updateOnGameEvents) {
            this.updateOnGameEvents = updateOnGameEvents;
            return this;
        }

        public Builder copyResourceFiles(boolean copyResourceFiles) {
            this.copyResourceFiles = copyResourceFiles;
            return this;
        }

        public Builder usePlaceholderAPI(boolean usePlaceholderAPI) {
            this.usePlaceholderAPI = usePlaceholderAPI;
            return this;
        }

        public Builder registerDefaultPlaceholders(boolean registerDefaultPlaceholders) {
            this.registerDefaultPlaceholders = registerDefaultPlaceholders;
            return this;
        }

        public Builder registerDefaultTriggers(boolean registerDefaultTriggers) {
            this.registerDefaultTriggers = registerDefaultTriggers;
            return this;
        }

        public Builder periodicUpdates(boolean periodicUpdates) {
            this.periodicUpdates = periodicUpdates;
            return this;
        }

        public Builder placeholder(String placeholder, ScoreboardPlaceholder replacer) {
            placeholders.put(normalizePlaceholder(placeholder), Objects.requireNonNull(replacer, "replacer"));
            return this;
        }

        public Builder trigger(ScoreboardUpdateTrigger<?> trigger) {
            triggers.add(Objects.requireNonNull(trigger, "trigger"));
            return this;
        }

        public Builder triggers(List<ScoreboardUpdateTrigger<?>> triggers) {
            triggers.forEach(this::trigger);
            return this;
        }

        public Builder placeholders(Map<String, ScoreboardPlaceholder> placeholders) {
            placeholders.forEach(this::placeholder);
            return this;
        }

        public Builder placeholders(Consumer<Builder> consumer) {
            consumer.accept(this);
            return this;
        }

        public ScoreboardSettings build() {
            return new ScoreboardSettings(this);
        }

        private static String cleanPath(String path) {
            String cleaned = Objects.requireNonNull(path, "resourceDirectory").replace("\\", "/");
            while (cleaned.startsWith("/")) {
                cleaned = cleaned.substring(1);
            }
            while (cleaned.endsWith("/")) {
                cleaned = cleaned.substring(0, cleaned.length() - 1);
            }
            return cleaned;
        }

        private static String normalizePlaceholder(String placeholder) {
            String normalized = Objects.requireNonNull(placeholder, "placeholder").trim();
            if (normalized.startsWith("%") && normalized.endsWith("%") && normalized.length() > 1) {
                normalized = normalized.substring(1, normalized.length() - 1);
            }
            return normalized.toLowerCase(Locale.ROOT);
        }
    }
}
