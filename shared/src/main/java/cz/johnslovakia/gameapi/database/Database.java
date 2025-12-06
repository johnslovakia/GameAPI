package cz.johnslovakia.gameapi.database;

import com.infernalsuite.asp.loaders.mysql.MysqlLoader;
import lombok.Getter;
import me.zort.sqllib.SQLConnectionBuilder;
import me.zort.sqllib.SQLDatabaseConnection;
import me.zort.sqllib.pool.SQLConnectionPool;
import org.bukkit.Bukkit;

import java.sql.SQLException;

@Getter
public class Database {

    //private final SQLDatabaseConnection connection;
    private SQLConnectionPool pool;
    private MysqlLoader aswmLoader;

    private final String sqlURL, host, database, username, password;
    private final int port;
    private final boolean useSSL;

    public Database(String sqlURL, String host, String database, String username, String password, int port, boolean useSSL) {
        this.sqlURL = sqlURL;
        this.host = host;
        this.database = database;
        this.username = username;
        this.password = password;
        this.port = port;
        this.useSSL = useSSL;

        SQLConnectionPool.Options options = new SQLConnectionPool.Options();
        options.setMaxConnections(20); // Connections limit in the pool.
        options.setBorrowObjectTimeout(5000L); // Timeout for borrowing objects from the pool.
        options.setBlockWhenExhausted(true);

        SQLConnectionBuilder template = new SQLConnectionBuilder(host, port, database, username, password);
        pool = new SQLConnectionPool(template, options);
        //this.connection = new SQLConnectionBuilder(host, port, database, username, password).createPool(options).build();
        // //connection.connect();

        if (Bukkit.getPluginManager().getPlugin("SlimeWorldPlugin"/*"SlimeWorldManager"*/) != null) {
            try {
                this.aswmLoader = new MysqlLoader(sqlURL, host, port, database, useSSL, username, password);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public SQLDatabaseConnection getConnection(){
        try (SQLDatabaseConnection resource = pool.getResource()) {
            return resource;
        } catch(SQLException e) {
            throw new RuntimeException(e);
        }
    }
}