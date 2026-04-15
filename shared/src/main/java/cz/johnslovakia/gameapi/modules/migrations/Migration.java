package cz.johnslovakia.gameapi.modules.migrations;

/**
 * Represents a versioned migration action that runs once after a plugin update.
 *
 * <p>Each migration has a unique {@link #getId() id} (e.g. {@code "1.2.0_reset_death_message"})
 * and a {@link MigrationScope scope} that determines where the completion state is stored.
 * Once executed, it will not run again on the same server or database (depending on scope).</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * migrationManager.register(new Migration("1.3.0_reset_chat_death", MigrationScope.SERVER) {
 *     @Override
 *     public void run() {
 *         ModuleManager.getModule(MessageModule.class).removeMessageFromFiles("chat.death");
 *     }
 * });
 * }</pre>
 */
public abstract class Migration {

    private final String id;
    private final MigrationScope scope;

    /**
     * @param id    Unique identifier for this migration (e.g. {@code "1.2.0_reset_death_message"}).
     * @param scope Where the completion state should be stored.
     */
    protected Migration(String id, MigrationScope scope) {
        this.id = id;
        this.scope = scope;
    }

    /**
     * Returns the unique identifier of this migration.
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the scope that determines how completion is tracked.
     */
    public MigrationScope getScope() {
        return scope;
    }

    /**
     * The action to execute. Called exactly once per scope, then marked as completed.
     * Implementations should not throw unless they want the migration to be retried on next startup.
     */
    public abstract void run();
}
