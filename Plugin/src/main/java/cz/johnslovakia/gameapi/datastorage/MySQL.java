package cz.johnslovakia.gameapi.datastorage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import cz.johnslovakia.gameapi.utils.Logger;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class MySQL {

    private HikariDataSource source;
    private Connection connection;

    public MySQL(FileConfiguration config, String path){
        HikariConfig hikariConfig = new HikariConfig();

        if (config.get(path) == null){
            Logger.log("There is an error in getting the database from the config.yml configuration file.", Logger.LogType.ERROR);
        }
        if (config.getString(path + ".host") == null){
            Logger.log("You don't have the database set up in the config.yml!", Logger.LogType.ERROR);
            return;
        }
        String sqlUrl = config.getString(path + ".sqlUrl");
        sqlUrl = sqlUrl.replace("{host}", config.getString(path + ".host"));
        sqlUrl = sqlUrl.replace("{port}", String.valueOf(config.get(path + ".port")));
        sqlUrl = sqlUrl.replace("{database}", config.getString(path + ".database"));
        sqlUrl = sqlUrl.replace("{usessl}", String.valueOf(config.get(path + ".usessl")));

        hikariConfig.setJdbcUrl(sqlUrl);
        hikariConfig.setUsername(config.getString(path + ".username"));
        hikariConfig.setPassword(config.getString(path + ".password"));

        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");
        hikariConfig.addDataSourceProperty("useLocalSessionState", "true");
        hikariConfig.addDataSourceProperty("rewriteBatchedStatements", "true");
        hikariConfig.addDataSourceProperty("cacheResultSetMetadata", "true");
        hikariConfig.addDataSourceProperty("cacheServerConfiguration", "true");
        hikariConfig.addDataSourceProperty("elideSetAutoCommits", "true");
        hikariConfig.addDataSourceProperty("maintainTimeStats", "false");

        source = new HikariDataSource(hikariConfig);
        try {
            connection = source.getConnection();
        } catch (SQLException e) {
            Logger.log("Cannot connect to the database!", Logger.LogType.ERROR);
            throw new RuntimeException(e);
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public PreparedStatement getPreparedStatement(String command) throws SQLException {
        return getConnection().prepareStatement(command);
    }
}