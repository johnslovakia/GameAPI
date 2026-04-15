package cz.johnslovakia.gameapi.modules.migrations;

/**
 * Defines where a migration's completion state is stored.
 */
public enum MigrationScope {

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
