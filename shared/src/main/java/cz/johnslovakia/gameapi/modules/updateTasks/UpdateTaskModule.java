package cz.johnslovakia.gameapi.modules.updateTasks;

import cz.johnslovakia.gameapi.Core;
import cz.johnslovakia.gameapi.database.JSConfigs;
import cz.johnslovakia.gameapi.modules.Module;
import cz.johnslovakia.gameapi.utils.Logger;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages versioned task actions for a plugin.
 *
 * <p>{@link UpdateTask.Scope#SERVER SERVER} tasks are tracked in
 * {@code plugins/<plugin>/completed_tasks.txt}.
 * {@link UpdateTask.Scope#DATABASE DATABASE} tasks are tracked in the {@code jsConfigs}
 * database table.</p>
 */
public class UpdateTaskModule implements Module {
    
    private final List<UpdateTask> tasks = new ArrayList<>();
    private final File completedtasksFile;

    public UpdateTaskModule(JavaPlugin plugin) {
        this.completedtasksFile = new File(plugin.getDataFolder(), "completed_tasks.txt");
    }

    @Override
    public void initialize() {
        runAll();
    }

    @Override
    public void terminate() {
        tasks.clear();
    }

    /**
     * Registers a task to be executed on the next {@link #runAll()} call if not yet completed.
     *
     * @param task the task to register
     */
    public void register(UpdateTask task) {
        tasks.add(task);
    }

    /**
     * Runs all registered tasks that have not been completed yet,
     * then marks each successful one as completed so it won't run again.
     */
    public void runAll() {
        for (UpdateTask task : tasks) {
            if (isCompleted(task)) continue;

            try {
                Logger.log("Running update task: " + task.getId(), Logger.LogType.INFO);
                task.run();
                markCompleted(task);
                Logger.log("Update task completed: " + task.getId(), Logger.LogType.INFO);
            } catch (Exception e) {
                Logger.log("Update task failed: " + task.getId() + " — " + e.getMessage(), Logger.LogType.ERROR);
                e.printStackTrace();
            }
        }
    }

    private boolean isCompleted(UpdateTask task) {
        return switch (task.getScope()) {
            case SERVER -> isCompletedLocally(task.getId());
            case DATABASE -> isCompletedInDB(task.getId());
        };
    }

    private void markCompleted(UpdateTask task) {
        switch (task.getScope()) {
            case SERVER -> markCompletedLocally(task.getId());
            case DATABASE -> markCompletedInDB(task.getId());
        }
    }

    private boolean isCompletedLocally(String id) {
        if (!completedtasksFile.exists()) return false;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(completedtasksFile), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().equals(id)) return true;
            }
        } catch (IOException e) {
            Logger.log("Failed to read completed_tasks.txt: " + e.getMessage(), Logger.LogType.WARNING);
        }
        return false;
    }

    private void markCompletedLocally(String id) {
        try {
            if (!completedtasksFile.exists()) completedtasksFile.createNewFile();
            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(completedtasksFile, true), StandardCharsets.UTF_8))) {
                writer.write(id);
                writer.newLine();
            }
        } catch (IOException e) {
            Logger.log("Failed to write completed_tasks.txt: " + e.getMessage(), Logger.LogType.ERROR);
        }
    }

    private boolean isCompletedInDB(String id) {
        if (Core.getInstance() == null || Core.getInstance().getDatabase() == null) return false;
        try {
            String value = new JSConfigs().loadConfig("task." + id);
            return "done".equals(value);
        } catch (Exception e) {
            Logger.log("Failed to check DB task status for '" + id + "': " + e.getMessage(), Logger.LogType.WARNING);
            return false;
        }
    }

    private void markCompletedInDB(String id) {
        if (Core.getInstance() == null || Core.getInstance().getDatabase() == null) {
            Logger.log("Cannot mark DB task '" + id + "' as done: no database connection.", Logger.LogType.WARNING);
            return;
        }
        try {
            new JSConfigs().saveConfig("task." + id, "done");
        } catch (Exception e) {
            Logger.log("Failed to persist DB task status for '" + id + "': " + e.getMessage(), Logger.LogType.ERROR);
        }
    }
}
