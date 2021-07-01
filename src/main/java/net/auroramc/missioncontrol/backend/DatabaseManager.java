package net.auroramc.missioncontrol.backend;

import net.auroramc.missioncontrol.MissionControl;
import net.auroramc.missioncontrol.entities.ProxyInfo;
import net.auroramc.missioncontrol.entities.ServerInfo;
import org.json.JSONObject;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.*;

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

    public Map<String, ServerInfo> getAllServers() {
        try (Connection connection = mysql.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM servers");
            ResultSet set = statement.executeQuery();

            Map<String, ServerInfo> servers = new HashMap<>();
            while (set.next()) {
                servers.put(set.getString(1), new ServerInfo(set.getString(1), set.getString(2), set.getInt(3), new JSONObject(set.getString(4)), set.getInt(5), set.getInt(6), set.getInt(7), set.getInt(8), set.getInt(9), set.getInt(10)));
            }
            return servers;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Map<UUID, ProxyInfo> getAllConnectionNodes() {
        try (Connection connection = mysql.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM proxies");
            ResultSet set = statement.executeQuery();

            Map<UUID, ProxyInfo> proxies = new HashMap<>();
            while (set.next()) {
                proxies.put(UUID.fromString(set.getString(1)), new ProxyInfo(UUID.fromString(set.getString(1)), set.getString(2), set.getInt(3), set.getInt(4), set.getInt(5)));
            }
            return proxies;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
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

    public ServerInfo getServerDetailsByName(String name) {
        try (Connection connection = mysql.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM servers WHERE servername = ?");
            statement.setString(1, name);
            ResultSet set = statement.executeQuery();
            if (set.next()) {
                return new ServerInfo(set.getString(1), set.getString(2), set.getInt(3), new JSONObject(set.getString(4)), set.getInt(5), set.getInt(6), set.getInt(7), set.getInt(8), set.getInt(9), set.getInt(10));
            } else {
                return null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

}
