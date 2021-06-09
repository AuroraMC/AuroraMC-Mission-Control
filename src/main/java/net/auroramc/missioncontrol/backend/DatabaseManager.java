package net.auroramc.missioncontrol.backend;

import net.auroramc.missioncontrol.MissionControl;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;

public class DatabaseManager {

    private final MySQLConnectionPool mysql;
    private final JedisPool jedis;

    public DatabaseManager(String mysqlHost, String mysqlPort, String mysqlDb, String mysqlUsername, String mysqlPassword, String redisHost, String redisAuth) {
        MissionControl.getLogger().info("Initialising MySQL and Redis database connection pools...");
        //Setting up MySQL connection pool.
        MySQLConnectionPool mysql1;
        try {
            mysql1 = new MySQLConnectionPool(mysqlHost, mysqlPort, mysqlDb, mysqlUsername, mysqlPassword);
        } catch (ClassNotFoundException e) {
            mysql1 = null;
            e.printStackTrace();
        }
        mysql = mysql1;

        //Setting up Redis connection pool.
        final JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(128);
        config.setMaxIdle(128);
        config.setMinIdle(16);
        config.setTestOnBorrow(true);
        config.setTestOnReturn(true);
        config.setTestWhileIdle(true);
        config.setMinEvictableIdleTimeMillis(Duration.ofSeconds(60).toMillis());
        config.setTimeBetweenEvictionRunsMillis(Duration.ofSeconds(30).toMillis());
        config.setNumTestsPerEvictionRun(3);
        config.setBlockWhenExhausted(true);
        jedis = new JedisPool(config, redisHost, 6379, 2000, redisAuth);
        MissionControl.getLogger().info("Database connection pools initialised.");
    }

    public void createServer() {
        try (Connection connection = mysql.getConnection()) {

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void createConnectionNode() {
        try (Connection connection = mysql.getConnection()) {

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
