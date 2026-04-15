package cz.johnslovakia.gameapi.modules.migrations;

import cz.johnslovakia.gameapi.Core;
import cz.johnslovakia.gameapi.database.JSConfigs;
import cz.johnslovakia.gameapi.utils.Logger;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages versioned migration actions for a plugin.
 *
 * <p>Register migrations with {@link #register(Migration)} and then call {@link #runAll()}
 * during plugin startup (after all modules are initialized). Each migration runs at most once.</p>
 *
 * <p>{@link MigrationScope#SERVER SERVER} migrations are tracked in
 * {@code plugins/<plugin>/completed_migrations.txt}.
 * {@link MigrationScope#DATABASE DATABASE} migrations are tracked in the {@code jsConfigs}
 * database table.</p>
 */
public class MigrationManager {

    private final JavaPlugin plugin;
    private final List<Migration> migrations = new ArrayList<>();
    private final File completedMigrationsFile;

    public MigrationManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.completedMigrationsFile = new File(plugin.getDataFolder(), "completed_migrations.txt");
    }

    /**
     * Registers a migration to be executed on the next {@link #runAll()} call if not yet completed.
     *
     * @param migration the migration to register
     */
    public void register(Migration migration) {
        migrations.add(migration);
    }

    /**
     * Runs all registered migrations that have not been completed yet,
     * then marks each successful one as completed so it won't run again.
     */
    public void runAll() {
        for (Migration migration : migrations) {
            if (isCompleted(migration)) continue;

            try {
                Logger.log("Running migration: " + migration.getId(), Logger.LogType.INFO);
                migration.run();
                markCompleted(migration);
                Logger.log("Migration completed: " + migration.getId(), Logger.LogType.INFO);
            } catch (Exception e) {
                Logger.log("Migration failed: " + migration.getId() + " — " + e.getMessage(), Logger.LogType.ERROR);
                e.printStackTrace();
            }
        }
    }

    private boolean isCompleted(Migration migration) {
        return switch (migration.getScope()) {
            case SERVER -> isCompletedLocally(migration.getId());
            case DATABASE -> isCompletedInDB(migration.getId());
        };
    }

    private void markCompleted(Migration migration) {
        switch (migration.getScope()) {
            case SERVER -> markCompletedLocally(migration.getId());
            case DATABASE -> markCompletedInDB(migration.getId());
        }
    }

    private boolean isCompletedLocally(String id) {
        if (!completedMigrationsFile.exists()) return false;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(completedMigrationsFile), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().equals(id)) return true;
            }
        } catch (IOException e) {
            Logger.log("Failed to read completed_migrations.txt: " + e.getMessage(), Logger.LogType.WARNING);
        }
        return false;
    }

    private void markCompletedLocally(String id) {
        try {
            if (!completedMigrationsFile.exists()) completedMigrationsFile.createNewFile();
            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(completedMigrationsFile, true), StandardCharsets.UTF_8))) {
                writer.write(id);
                writer.newLine();
            }
        } catch (IOException e) {
            Logger.log("Failed to write completed_migrations.txt: " + e.getMessage(), Logger.LogType.ERROR);
        }
    }

    private boolean isCompletedInDB(String id) {
        if (Core.getInstance() == null || Core.getInstance().getDatabase() == null) return false;
        try {
            String value = new JSConfigs().loadConfig("migration." + id);
            return "done".equals(value);
        } catch (Exception e) {
            Logger.log("Failed to check DB migration status for '" + id + "': " + e.getMessage(), Logger.LogType.WARNING);
            return false;
        }
    }

    private void markCompletedInDB(String id) {
        if (Core.getInstance() == null || Core.getInstance().getDatabase() == null) {
            Logger.log("Cannot mark DB migration '" + id + "' as done: no database connection.", Logger.LogType.WARNING);
            return;
        }
        try {
            new JSConfigs().saveConfig("migration." + id, "done");
        } catch (Exception e) {
            Logger.log("Failed to persist DB migration status for '" + id + "': " + e.getMessage(), Logger.LogType.ERROR);
        }
    }
}
