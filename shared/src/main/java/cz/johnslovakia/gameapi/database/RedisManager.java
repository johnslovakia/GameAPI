package cz.johnslovakia.gameapi.database;

import lombok.Getter;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

import java.util.HashSet;
import java.util.Set;

@Getter
public class RedisManager {

    public final JedisPool pool;

    public RedisManager(String host, int port, String password, String username) {
        GenericObjectPoolConfig<Jedis> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(20);
        poolConfig.setMaxIdle(10);
        poolConfig.setMinIdle(5);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);

        if (password != null && !password.isEmpty()) {
            this.pool = new JedisPool(poolConfig, host, port, 2000, password);
        } else {
            this.pool = new JedisPool(poolConfig, host, port, 2000);
        }
        if (username != null) {
            pool.getResource().auth(username, password);
        }else{
            pool.getResource().auth(password);
        }
    }

    public RedisManager(String host, int port) {
        this(host, port, null, null);
    }

    public void setDatabase(int database){
        pool.getResource().select(database);
    }

    public String get(String key) {
        try (Jedis jedis = pool.getResource()) {
            return jedis.get(key);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void set(String key, String value, int expireSeconds) {
        try (Jedis jedis = pool.getResource()) {
            jedis.setex(key, expireSeconds, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void set(String key, String value) {
        try (Jedis jedis = pool.getResource()) {
            jedis.set(key, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void delete(String key) {
        try (Jedis jedis = pool.getResource()) {
            jedis.del(key);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Set<String> scanKeys(String pattern) {
        Set<String> keys = new HashSet<>();
        try (Jedis jedis = pool.getResource()) {
            String cursor = "0";
            ScanParams scanParams = new ScanParams().match(pattern).count(100);

            do {
                ScanResult<String> scanResult = jedis.scan(cursor, scanParams);
                keys.addAll(scanResult.getResult());
                cursor = scanResult.getCursor();
            } while (!cursor.equals("0"));

        } catch (Exception e) {
            e.printStackTrace();
        }
        return keys;
    }

    public boolean exists(String key) {
        try (Jedis jedis = pool.getResource()) {
            return jedis.exists(key);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void close() {
        if (pool != null && !pool.isClosed()) {
            pool.close();
        }
    }
}