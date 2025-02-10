package cz.johnslovakia.gameapi.datastorage;

import cz.johnslovakia.gameapi.utils.Logger;
import lombok.Getter;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

@Getter
public class RedisManager {

    private final JedisPool pool;
    private final String password, username;

    public RedisManager(String host, int port, String password, String username) {
        //this.pool = new JedisPool(host, port);
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        this.pool = new JedisPool(poolConfig, host, port, 2000, password);
        if (username != null) {
            pool.getResource().auth(username, password);
        }else{
            pool.getResource().auth(password);
        }

        this.password = password;
        this.username = username;
    };

    public void setDatabase(int database){
        pool.getResource().select(database);
    }

    public void set(String key, String value) {
        try (Jedis jedis = pool.getResource()) {
            jedis.set(key, value);
        }
    }

    public void set(String key, String value, int ttl) {
        try (Jedis jedis = pool.getResource()) {
            //jedis.auth(username, password);
            jedis.set(key, value);
            jedis.expire(key, ttl);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void close() {
        pool.getResource().close();
        pool.close();
    }
}