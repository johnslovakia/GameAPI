package cz.johnslovakia.gameapi.modules.scoreboard;

import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.events.GameJoinEvent;
import cz.johnslovakia.gameapi.events.GameQuitEvent;
import cz.johnslovakia.gameapi.events.GameStateChangeEvent;
import cz.johnslovakia.gameapi.events.KitSelectEvent;
import cz.johnslovakia.gameapi.events.PlayerScoreEvent;
import cz.johnslovakia.gameapi.events.TaskEvent;
import cz.johnslovakia.gameapi.modules.Module;
import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.game.GameInstance;
import cz.johnslovakia.gameapi.modules.messages.Language;
import cz.johnslovakia.gameapi.modules.messages.MessageModule;
import cz.johnslovakia.gameapi.modules.scores.Score;
import cz.johnslovakia.gameapi.modules.scores.ScoreModule;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.GamePlayerState;
import cz.johnslovakia.gameapi.users.PlayerIdentity;
import cz.johnslovakia.gameapi.users.PlayerManager;
import cz.johnslovakia.gameapi.utils.Logger;
import cz.johnslovakia.gameapi.utils.StringUtils;
import cz.johnslovakia.gameapi.utils.eTrigger.Mapper;
import cz.johnslovakia.gameapi.utils.eTrigger.Trigger;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScoreboardModule implements Module, Listener {

    private final JavaPlugin plugin;
    private final ScoreboardSettings settings;
    private final Map<UUID, PlayerScoreboard> scoreboards = new HashMap<>();
    private final Map<String, ScoreboardTemplate> templates = new HashMap<>();
    private final Map<String, ScoreboardPlaceholder> defaultPlaceholders = new LinkedHashMap<>();
    private final List<ScoreboardUpdateTrigger<?>> defaultTriggers = new ArrayList<>();
    private final List<Listener> triggerListeners = new ArrayList<>();
    private BukkitTask updateTask;

    public ScoreboardModule(JavaPlugin plugin, ScoreboardSettings settings) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.settings = Objects.requireNonNull(settings, "settings");
        registerDefaultPlaceholders();
        registerDefaultTriggers();
    }

    @Override
    public void initialize() {
        if (settings.isCopyResourceFiles()) {
            copyMissingResourceFiles();
        }
        loadTemplates();

        if (settings.isRegisterDefaultTriggers()) {
            defaultTriggers.forEach(this::registerUpdateTrigger);
        }
        settings.getTriggers().forEach(this::registerUpdateTrigger);

        if (settings.isPeriodicUpdates()) {
            updateTask = new BukkitRunnable() {
                @Override
                public void run() {
                    updateAll();
                }
            }.runTaskTimer(plugin, settings.getUpdateDelayTicks(), settings.getUpdatePeriodTicks());
        }
    }

    @Override
    public void terminate() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
        triggerListeners.forEach(HandlerList::unregisterAll);
        triggerListeners.clear();
        for (PlayerScoreboard scoreboard : new ArrayList<>(scoreboards.values())) {
            scoreboard.delete();
        }
        scoreboards.clear();
        templates.clear();
    }

    public Optional<PlayerScoreboard> getScoreboard(Player player) {
        return Optional.ofNullable(scoreboards.get(player.getUniqueId()));
    }

    public PlayerScoreboard createScoreboard(Player player) {
        PlayerScoreboard existing = scoreboards.get(player.getUniqueId());
        if (existing != null) {
            updateLater(player, 5L);
            return existing;
        }

        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);
        if (!gamePlayer.isInGame()) {
            return null;
        }

        PlayerScoreboard scoreboard = new PlayerScoreboard(this, player, gamePlayer);
        scoreboards.put(player.getUniqueId(), scoreboard);
        scoreboard.update();
        return scoreboard;
    }

    public void removeScoreboard(Player player) {
        PlayerScoreboard scoreboard = scoreboards.remove(player.getUniqueId());
        if (scoreboard != null) {
            scoreboard.delete();
        }
    }

    public void reload() {
        templates.clear();
        if (settings.isCopyResourceFiles()) {
            copyMissingResourceFiles();
        }
        loadTemplates();
        updateAll();
    }

    public void update(Player player) {
        getScoreboard(player).ifPresent(PlayerScoreboard::update);
    }

    public void update(GameInstance game) {
        if (game == null) {
            return;
        }
        for (GamePlayer gamePlayer : game.getParticipants()) {
            Player player = gamePlayer.getOnlinePlayer();
            if (player != null && player.isOnline()) {
                update(player);
            }
        }
    }

    public void update(PlayerIdentity playerIdentity) {
        Player player = playerIdentity != null ? playerIdentity.getOnlinePlayer() : null;
        if (player != null && player.isOnline()) {
            update(player);
        }
    }

    ScoreboardTemplate getTemplate(GamePlayer gamePlayer) {
        String language = getLanguageName(gamePlayer);
        ScoreboardTemplate template = templates.get(language.toLowerCase(Locale.ROOT));
        if (template != null) {
            return template;
        }

        Language defaultLanguage = getDefaultLanguage();
        if (defaultLanguage != null) {
            template = templates.get(defaultLanguage.getName().toLowerCase(Locale.ROOT));
            if (template != null) {
                return template;
            }
        }

        return templates.values().stream().findFirst().orElse(null);
    }

    String getDefaultTitle() {
        if (settings.getDefaultTitle() != null) {
            return settings.getDefaultTitle();
        }
        return "\u00A72\u00A7l" + Minigame.getInstance().getName();
    }

    String render(String value, ScoreboardContext context) {
        if (value == null) {
            return "";
        }

        Matcher matcher = Pattern.compile("%([A-Za-z0-9_.:-]+)%").matcher(value);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String placeholder = matcher.group(1);
            String replacement = resolvePlaceholder(placeholder, context);
            if (replacement == null) {
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group()));
            } else {
                matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
            }
        }
        matcher.appendTail(result);

        String rendered = result.toString();
        if (settings.isUsePlaceholderAPI() && Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null && rendered.contains("%")) {
            rendered = PlaceholderAPI.setPlaceholders(context.getPlayer(), rendered);
        }
        return StringUtils.colorizer(rendered);
    }

    Component renderComponent(String value, ScoreboardContext context) {
        return renderFontTags(render(value, context));
    }

    private Component renderFontTags(String text) {
        Pattern openPattern = Pattern.compile("<font\\s*=\\s*\"([^\"]+)\">", Pattern.CASE_INSENSITIVE);
        String closeTag = "</font>";
        Component result = Component.empty();
        int index = 0;

        while (index < text.length()) {
            Matcher openMatcher = openPattern.matcher(text);
            if (!openMatcher.find(index)) {
                return result.append(legacyComponent(text.substring(index)));
            }

            if (openMatcher.start() > index) {
                result = result.append(legacyComponent(text.substring(index, openMatcher.start())));
            }

            Key font = parseFontKey(openMatcher.group(1));
            int contentStart = openMatcher.end();
            int closeStart = text.toLowerCase(Locale.ROOT).indexOf(closeTag, contentStart);

            if (closeStart == -1) {
                result = result.append(legacyComponent(text.substring(contentStart)).font(font));
                return result;
            }

            result = result.append(legacyComponent(text.substring(contentStart, closeStart)).font(font));
            index = closeStart + closeTag.length();
        }

        return result;
    }

    private Component legacyComponent(String text) {
        if (text.isEmpty()) {
            return Component.empty();
        }
        return LegacyComponentSerializer.builder()
                .hexColors()
                .useUnusualXRepeatedCharacterHexFormat()
                .build()
                .deserialize(text);
    }

    private Key parseFontKey(String rawKey) {
        String key = rawKey.trim();
        if (!key.contains(":")) {
            key = "minecraft:" + key;
        }

        try {
            return Key.key(key);
        } catch (IllegalArgumentException exception) {
            Logger.log("Invalid scoreboard font key '" + rawKey + "': " + exception.getMessage(), Logger.LogType.WARNING);
            return Key.key("minecraft", "default");
        }
    }

    private String resolvePlaceholder(String placeholder, ScoreboardContext context) {
        String key = normalizePlaceholder(placeholder);

        ScoreboardPlaceholder custom = settings.getPlaceholders().get(key);
        if (custom != null) {
            return safeReplace(key, custom, context);
        }

        if (settings.isRegisterDefaultPlaceholders()) {
            ScoreboardPlaceholder defaults = defaultPlaceholders.get(key);
            if (defaults != null) {
                return safeReplace(key, defaults, context);
            }
        }

        if (key.startsWith("score_")) {
            return getScore(context.getGamePlayer(), key.substring("score_".length()));
        }

        if (key.startsWith("metadata_") && context.getGame() != null) {
            Object value = context.getGame().getMetadata().get(key.substring("metadata_".length()));
            return value != null ? String.valueOf(value) : "";
        }

        return null;
    }

    private String safeReplace(String key, ScoreboardPlaceholder placeholder, ScoreboardContext context) {
        try {
            String replacement = placeholder.replace(context);
            return replacement != null ? replacement : "";
        } catch (Exception exception) {
            Logger.log("Scoreboard placeholder %" + key + "% failed: " + exception.getMessage(), Logger.LogType.WARNING);
            return "";
        }
    }

    private void updateAll() {
        for (Map.Entry<UUID, PlayerScoreboard> entry : new ArrayList<>(scoreboards.entrySet())) {
            PlayerScoreboard scoreboard = entry.getValue();
            if (!isValid(scoreboard)) {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player != null) {
                    removeScoreboard(player);
                } else {
                    scoreboards.remove(entry.getKey());
                }
                continue;
            }
            scoreboard.update();
        }
    }

    private boolean isValid(PlayerScoreboard scoreboard) {
        Player player = scoreboard.getPlayer();
        if (player == null || !player.isOnline()) {
            return false;
        }

        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);
        if (!gamePlayer.isInGame() || gamePlayer.getGameSession() == null) {
            return false;
        }
        if (gamePlayer.getGameSession().getState().equals(GamePlayerState.DISCONNECTED)) {
            return false;
        }
        GameInstance game = gamePlayer.getGame();
        return game != null && Objects.equals(game.getID(), scoreboard.getGameId());
    }

    private void updateLater(Player player, long delay) {
        Bukkit.getScheduler().runTaskLater(plugin, task -> update(player), delay);
    }

    private void updateLater(GameInstance game, long delay) {
        Bukkit.getScheduler().runTaskLater(plugin, task -> update(game), delay);
    }

    private void updateLater(PlayerIdentity playerIdentity, long delay) {
        Bukkit.getScheduler().runTaskLater(plugin, task -> update(playerIdentity), delay);
    }

    private void registerUpdateTrigger(ScoreboardUpdateTrigger<?> trigger) {
        Listener listener = new Listener() {};
        triggerListeners.add(listener);
        Bukkit.getPluginManager().registerEvent(
                trigger.getEventClass(),
                listener,
                EventPriority.NORMAL,
                (registeredListener, event) -> onTriggeredUpdate(trigger, event),
                plugin
        );
    }

    private void onTriggeredUpdate(ScoreboardUpdateTrigger<?> trigger, Event event) {
        if (!trigger.validate(event)) {
            return;
        }

        for (PlayerIdentity playerIdentity : trigger.compute(event)) {
            if (playerIdentity == null) {
                continue;
            }

            switch (trigger.getScope()) {
                case PLAYER -> updateLater(playerIdentity, 1L);
                case GAME -> {
                    if (playerIdentity instanceof GamePlayer gamePlayer) {
                        updateLater(gamePlayer.getGame(), 1L);
                    } else {
                        updateLater(playerIdentity, 1L);
                    }
                }
                case ALL -> Bukkit.getScheduler().runTaskLater(plugin, task -> updateAll(), 1L);
            }
        }
    }

    private void copyMissingResourceFiles() {
        File folder = new File(plugin.getDataFolder(), settings.getResourceDirectory());
        if (!folder.exists() && !folder.mkdirs()) {
            Logger.log("Could not create scoreboard folder " + folder.getPath(), Logger.LogType.WARNING);
            return;
        }

        for (String language : discoverLanguageNames(folder)) {
            File target = new File(folder, language + settings.getFileSuffix());
            if (target.exists()) {
                continue;
            }

            String resourcePath = settings.getResourceDirectory() + "/" + language + settings.getFileSuffix();
            try (InputStream inputStream = plugin.getResource(resourcePath)) {
                if (inputStream == null) {
                    continue;
                }
                Files.copy(inputStream, target.toPath());
            } catch (Exception exception) {
                Logger.log("Could not copy scoreboard file " + resourcePath + ": " + exception.getMessage(), Logger.LogType.WARNING);
            }
        }
    }

    private void loadTemplates() {
        File folder = new File(plugin.getDataFolder(), settings.getResourceDirectory());
        File[] files = folder.listFiles((dir, name) -> name.endsWith(settings.getFileSuffix()));
        if (files == null || files.length == 0) {
            Logger.log("No scoreboard files found in " + folder.getPath(), Logger.LogType.WARNING);
            return;
        }

        for (File file : files) {
            String language = file.getName().substring(0, file.getName().length() - settings.getFileSuffix().length());
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            ScoreboardTemplate template = ScoreboardTemplate.load(language, config, settings.getSection());
            templates.put(language.toLowerCase(Locale.ROOT), template);
        }
    }

    private Set<String> discoverLanguageNames(File folder) {
        Set<String> names = new LinkedHashSet<>();
        for (Language language : Language.getLanguages()) {
            names.add(language.getName());
        }

        File[] files = folder.listFiles((dir, name) -> name.endsWith(settings.getFileSuffix()));
        if (files != null) {
            for (File file : files) {
                String name = file.getName().substring(0, file.getName().length() - settings.getFileSuffix().length());
                names.add(name);
            }
        }

        try {
            File jarFile = new File(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
            if (jarFile.isFile()) {
                try (JarFile jar = new JarFile(jarFile)) {
                    String prefix = settings.getResourceDirectory() + "/";
                    jar.stream()
                            .filter(entry -> entry.getName().startsWith(prefix))
                            .filter(entry -> entry.getName().endsWith(settings.getFileSuffix()))
                            .map(entry -> new File(entry.getName()).getName())
                            .map(fileName -> fileName.substring(0, fileName.length() - settings.getFileSuffix().length()))
                            .filter(name -> !name.isBlank())
                            .forEach(names::add);
                }
            }
        } catch (URISyntaxException exception) {
            Logger.log("Could not resolve plugin JAR path for scoreboard auto-detection: " + exception.getMessage(), Logger.LogType.WARNING);
        } catch (Exception exception) {
            Logger.log("Could not auto-scan scoreboard files from plugin JAR: " + exception.getMessage(), Logger.LogType.WARNING);
        }

        return names;
    }

    private String getLanguageName(GamePlayer gamePlayer) {
        MessageModule messageModule = ModuleManager.getModule(MessageModule.class);
        if (messageModule == null) {
            Language defaultLanguage = getDefaultLanguage();
            return defaultLanguage != null ? defaultLanguage.getName() : "english";
        }
        Language language = messageModule.getPlayerLanguage(gamePlayer);
        if (language != null) {
            return language.getName();
        }
        Language defaultLanguage = getDefaultLanguage();
        return defaultLanguage != null ? defaultLanguage.getName() : "english";
    }

    private Language getDefaultLanguage() {
        if (Language.getLanguages().isEmpty()) {
            return null;
        }
        return Language.getDefaultLanguage();
    }

    private void registerDefaultPlaceholders() {
        defaultPlaceholders.put("date", context -> new SimpleDateFormat("MM/dd/yyyy").format(new Date()));
        defaultPlaceholders.put("date_monthday", context -> new SimpleDateFormat("MM/dd/yyyy").format(new Date()));
        defaultPlaceholders.put("date_daymonth", context -> new SimpleDateFormat("dd/MM/yyyy").format(new Date()));
        defaultPlaceholders.put("date_daymonthdots", context -> new SimpleDateFormat("dd. MM. yyyy").format(new Date()));
        defaultPlaceholders.put("game_id", context -> context.getGame() != null ? context.getGame().getID() : "");
        defaultPlaceholders.put("game_name", context -> context.getGame() != null ? context.getGame().getName() : "");
        defaultPlaceholders.put("minigame", context -> Minigame.getInstance().getName());
        defaultPlaceholders.put("minigame_full_name", context -> Minigame.getInstance().getFullName());
        defaultPlaceholders.put("state", context -> context.getGameState() != null ? context.getGameState().name() : "");
        defaultPlaceholders.put("players", context -> context.getGame() != null ? String.valueOf(context.getGame().getPlayers().size()) : "0");
        defaultPlaceholders.put("participants", context -> context.getGame() != null ? String.valueOf(context.getGame().getParticipants().size()) : "0");
        defaultPlaceholders.put("spectators", context -> context.getGame() != null ? String.valueOf(context.getGame().getSpectators().size()) : "0");
        defaultPlaceholders.put("max_players", context -> context.getGame() != null ? String.valueOf(context.getGame().getSettings().getMaxPlayers()) : "0");
        defaultPlaceholders.put("min_players", context -> context.getGame() != null ? String.valueOf(context.getGame().getSettings().getMinPlayers()) : "0");
        defaultPlaceholders.put("required_players", context -> context.getGame() != null ? String.valueOf(context.getGame().getSettings().getMinPlayers()) : "0");
        defaultPlaceholders.put("kit", this::getKit);
        defaultPlaceholders.put("map", context -> context.getGame() != null && context.getGame().getCurrentMap() != null ? context.getGame().getCurrentMap().getName() : "None");
        defaultPlaceholders.put("time", this::getRunningTaskTime);
        defaultPlaceholders.put("remaining", context -> context.getGame() != null ? String.valueOf(context.getGame().getPlayers().size()) : "0");
        defaultPlaceholders.put("alive_enemies", context -> {
            if (context.getGame() == null || context.getGamePlayer() == null) {
                return "0";
            }
            return String.valueOf(context.getGame().getPlayers().stream()
                    .filter(gamePlayer -> !gamePlayer.equals(context.getGamePlayer()))
                    .count());
        });
        defaultPlaceholders.put("players_at_start", context -> context.getGame() != null ? String.valueOf(context.getGame().getMetadata().getOrDefault("players_at_start", 0)) : "0");
        defaultPlaceholders.put("kills", context -> getScore(context.getGamePlayer(), "kill"));
        defaultPlaceholders.put("assists", context -> getScore(context.getGamePlayer(), "assist"));
        defaultPlaceholders.put("deaths", context -> getScore(context.getGamePlayer(), "death"));
    }

    private void registerDefaultTriggers() {
        defaultTriggers.add(ScoreboardUpdateTrigger.game(new Trigger<>(
                GameJoinEvent.class,
                new Mapper.SingleMapper<>(GameJoinEvent::getGamePlayer)
        )));
        defaultTriggers.add(ScoreboardUpdateTrigger.game(new Trigger<>(
                GameQuitEvent.class,
                new Mapper.SingleMapper<>(GameQuitEvent::getGamePlayer)
        )));
        defaultTriggers.add(ScoreboardUpdateTrigger.game(new Trigger<>(
                GameStateChangeEvent.class,
                new Mapper.ListMapper<>(event -> event.getGame().getParticipants().stream()
                        .map(gamePlayer -> (PlayerIdentity) gamePlayer)
                        .toList())
        )));
        defaultTriggers.add(ScoreboardUpdateTrigger.player(new Trigger<>(
                KitSelectEvent.class,
                new Mapper.SingleMapper<>(KitSelectEvent::getGamePlayer)
        )));
        defaultTriggers.add(ScoreboardUpdateTrigger.game(new Trigger<>(
                PlayerScoreEvent.class,
                new Mapper.SingleMapper<>(PlayerScoreEvent::getGamePlayer)
        )));
        defaultTriggers.add(ScoreboardUpdateTrigger.game(new Trigger<>(
                TaskEvent.class,
                new Mapper.ListMapper<>(event -> event.getTask().getGame().getParticipants().stream()
                        .map(gamePlayer -> (PlayerIdentity) gamePlayer)
                        .toList()),
                event -> event.getType() == TaskEvent.Type.TICK
                        && event.getTask().getGame().getRunningMainTask() != null
                        && event.getTask().equals(event.getTask().getGame().getRunningMainTask())
        )));
    }

    private String getKit(ScoreboardContext context) {
        if (context.getGamePlayer() == null || context.getGamePlayer().getGameSession() == null
                || context.getGamePlayer().getGameSession().getSelectedKit() == null) {
            MessageModule messageModule = ModuleManager.getModule(MessageModule.class);
            if (messageModule != null && context.getGamePlayer() != null
                    && messageModule.hasMessage(context.getGamePlayer(), "scoreboard.kit.none")) {
                return context.translate("scoreboard.kit.none");
            }
            if (messageModule != null && context.getGamePlayer() != null
                    && messageModule.hasMessage(context.getGamePlayer(), "word.none_kit")) {
                return context.translate("word.none_kit");
            }
            return "None";
        }
        return context.getGamePlayer().getGameSession().getSelectedKit().getName();
    }

    private String getRunningTaskTime(ScoreboardContext context) {
        if (context.getGame() == null || context.getGame().getRunningMainTask() == null) {
            return "00:00";
        }
        return StringUtils.getDurationString(context.getGame().getRunningMainTask().getCounter());
    }

    private String getScore(GamePlayer gamePlayer, String requestedScore) {
        if (gamePlayer == null || gamePlayer.getGameSession() == null) {
            return "0";
        }

        ScoreModule scoreModule = ModuleManager.getModule(ScoreModule.class);
        if (scoreModule == null) {
            return "0";
        }

        String scoreName = requestedScore.replace("_", " ");
        Optional<Score> score = scoreModule.getScore(scoreName);
        if (score.isEmpty()) {
            score = scoreModule.getScores().values().stream()
                    .filter(s -> s.getName().equalsIgnoreCase(scoreName))
                    .findFirst();
        }
        return score.map(value -> String.valueOf(gamePlayer.getGameSession().getScore(value.getName()))).orElse("0");
    }

    private String normalizePlaceholder(String placeholder) {
        return placeholder.trim().toLowerCase(Locale.ROOT);
    }

    @EventHandler
    public void onGameJoin(GameJoinEvent event) {
        if (settings.isAutoCreate()) {
            Player player = event.getGamePlayer().getOnlinePlayer();
            if (player != null) {
                removeScoreboard(player);
                createScoreboard(player);
            }
        }
        if (settings.isUpdateOnGameEvents() && !settings.isRegisterDefaultTriggers()) {
            updateLater(event.getGame(), 1L);
        }
    }

    @EventHandler
    public void onGameQuit(GameQuitEvent event) {
        Player player = event.getGamePlayer().getOnlinePlayer();
        if (player != null) {
            removeScoreboard(player);
        }
        if (settings.isUpdateOnGameEvents() && !settings.isRegisterDefaultTriggers()) {
            updateLater(event.getGame(), 1L);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        removeScoreboard(event.getPlayer());
    }

    @EventHandler
    public void onGameStateChange(GameStateChangeEvent event) {
        if (settings.isUpdateOnGameEvents() && !settings.isRegisterDefaultTriggers()) {
            updateLater(event.getGame(), 1L);
        }
    }

    @EventHandler
    public void onPlayerScore(PlayerScoreEvent event) {
        if (settings.isUpdateOnGameEvents() && !settings.isRegisterDefaultTriggers()) {
            update(event.getGame());
        }
    }

    @EventHandler
    public void onKitSelect(KitSelectEvent event) {
        Player player = event.getGamePlayer().getOnlinePlayer();
        if (settings.isUpdateOnGameEvents() && !settings.isRegisterDefaultTriggers() && player != null) {
            update(player);
        }
    }

}
