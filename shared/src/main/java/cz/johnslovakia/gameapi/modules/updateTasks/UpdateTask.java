package cz.johnslovakia.gameapi.modules.updateTasks;

/**
 * Represents a versioned migration action that runs once after a plugin update.
 *
 * <p>Each update task has a unique {@link #getId() id} (e.g. {@code "1.2.0_reset_death_message"})
 * and a {@link Scope scope} that determines where the completion state is stored.
 * Once executed, it will not run again on the same server or database (depending on scope).</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * updateTaskModule.register(new UpdateTask("1.3.0_reset_chat_death", UpdateTask.Scope.SERVER) {
 *     @Override
 *     public void run() {
 *         ModuleManager.getModule(MessageModule.class).removeMessageFromFiles("chat.death");
 *     }
 * });
 * }</pre>
 */
public abstract class UpdateTask {

    private final String id;
    private final Scope scope;

    /**
     * @param id    Unique identifier for this update task (e.g. {@code "1.2.0_reset_death_message"}).
     * @param scope Where the completion state should be stored.
     */
    protected UpdateTask(String id, Scope scope) {
        this.id = id;
        this.scope = scope;
    }

    /**
     * Returns the unique identifier of this update task.
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the scope that determines how completion is tracked.
     */
    public Scope getScope() {
        return scope;
    }

    /**
     * The action to execute. Called exactly once per scope, then marked as completed.
     * Implementations should not throw unless they want the migration to be retried on next startup.
     */
    public abstract void run();


    public enum Scope {

        /**
         * Tracked per-server in a local file. Each server runs the migration independently.
         * Use this for actions like resetting or deleting language file entries.
         */
        SERVER,

        /**
         * Tracked globally in the database via {@link cz.johnslovakia.gameapi.database.JSConfigs}.
         * Runs once across all servers sharing the same database.
         * Use this for database schema changes or data migrations.
         */
        DATABASE
    }
}
