package net.auroramc.missioncontrol.backend.managers;

import net.auroramc.missioncontrol.MissionControl;
import net.auroramc.missioncontrol.backend.MySQLConnectionPool;
import net.auroramc.missioncontrol.entities.ProxyInfo;
import net.auroramc.missioncontrol.entities.ServerInfo;
import org.json.JSONObject;
import redis.clients.jedis.Jedis;
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
                servers.put(set.getString(1), new ServerInfo(set.getString(1), set.getString(2), set.getInt(3), ServerInfo.Network.valueOf(set.getString(4)), set.getBoolean(5), new JSONObject(set.getString(6)), set.getInt(7), set.getInt(8), set.getInt(9), set.getInt(10), set.getInt(11), set.getInt(12)));
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
                proxies.put(UUID.fromString(set.getString(1)), new ProxyInfo(UUID.fromString(set.getString(1)), set.getString(2), set.getInt(3), ServerInfo.Network.valueOf(set.getString(4)), set.getBoolean(5), set.getInt(6), set.getInt(7)));
            }
            return proxies;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void createServer(ServerInfo info) {
        try (Connection connection = mysql.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO servers VALUES (?,?,?,?,?,?,?,?,?,?,?,?)");
            statement.setString(1, info.getName());
            statement.setString(2, info.getIp());
            statement.setInt(3, info.getPort());
            statement.setString(4, info.getNetwork().name());
            statement.setBoolean(5, info.isForced());
            statement.setString(6, info.getServerType().toString());
            statement.setInt(7, info.getProtocolPort());
            statement.setInt(8, info.getBuildNumber());
            statement.setInt(9, info.getLobbyBuildNumber());
            statement.setInt(10, info.getEngineBuildNumber());
            statement.setInt(11, info.getGameBuildNumber());
            statement.setInt(12, info.getBuildBuildNumber());
            statement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteServer(ServerInfo info) {
        try (Connection connection = mysql.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("DELETE FROM servers WHERE servername = ?");
            statement.setString(1, info.getName());
            statement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteNode(ProxyInfo info) {
        try (Connection connection = mysql.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("DELETE FROM proxies WHERE uuid = ?");
            statement.setString(1, info.getUuid().toString());
            statement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void createConnectionNode(ProxyInfo proxyInfo) {
        try (Connection connection = mysql.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO proxies VALUES (?,?,?,?,?,?,?)");
            statement.setString(1, proxyInfo.getUuid().toString());
            statement.setString(2, proxyInfo.getIp());
            statement.setInt(3, proxyInfo.getPort());
            statement.setString(4, proxyInfo.getNetwork().name());
            statement.setBoolean(5, proxyInfo.isForced());
            statement.setInt(6, proxyInfo.getProtocolPort());
            statement.setInt(7, proxyInfo.getBuildNumber());
            statement.execute();
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
                return new ServerInfo(set.getString(1), set.getString(2), set.getInt(3), ServerInfo.Network.valueOf(set.getString(4)), set.getBoolean(5), new JSONObject(set.getString(6)), set.getInt(7), set.getInt(8), set.getInt(9), set.getInt(10), set.getInt(11), set.getInt(12));
            } else {
                return null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public int getCurrentBuildBuildNumber() {
        try (Jedis connection = jedis.getResource()) {
            String buildNumber = connection.get("build.build");
            if (buildNumber != null) {
                return Integer.parseInt(buildNumber);
            } else {
                return -1;
            }
        }
    }

    public int getCurrentCoreBuildNumber() {
        try (Jedis connection = jedis.getResource()) {
            String buildNumber = connection.get("build.core");
            if (buildNumber != null) {
                return Integer.parseInt(buildNumber);
            } else {
                return -1;
            }
        }
    }

    public int getCurrentEngineBuildNumber() {
        try (Jedis connection = jedis.getResource()) {
            String buildNumber = connection.get("build.engine");
            if (buildNumber != null) {
                return Integer.parseInt(buildNumber);
            } else {
                return -1;
            }
        }
    }

    public int getCurrentGameBuildNumber() {
        try (Jedis connection = jedis.getResource()) {
            String buildNumber = connection.get("build.game");
            if (buildNumber != null) {
                return Integer.parseInt(buildNumber);
            } else {
                return -1;
            }
        }
    }

    public int getCurrentLobbyBuildNumber() {
        try (Jedis connection = jedis.getResource()) {
            String buildNumber = connection.get("build.lobby");
            if (buildNumber != null) {
                return Integer.parseInt(buildNumber);
            } else {
                return -1;
            }
        }
    }

    public int getCurrentProxyBuildNumber() {
        try (Jedis connection = jedis.getResource()) {
            String buildNumber = connection.get("build.proxy");
            if (buildNumber != null) {
                return Integer.parseInt(buildNumber);
            } else {
                return -1;
            }
        }
    }

    public void setCurrentBuildBuildNumber(int buildNumber) {
        try (Jedis connection = jedis.getResource()) {
            connection.set("build.build", buildNumber + "");
        }
    }

    public void setCurrentCoreBuildNumber(int buildNumber) {
        try (Jedis connection = jedis.getResource()) {
            connection.set("build.core", buildNumber + "");
        }
    }

    public void setCurrentEngineBuildNumber(int buildNumber) {
        try (Jedis connection = jedis.getResource()) {
            connection.set("build.engine", buildNumber + "");
        }
    }

    public void setCurrentGameBuildNumber(int buildNumber) {
        try (Jedis connection = jedis.getResource()) {
            connection.set("build.game", buildNumber + "");
        }
    }

    public void setCurrentLobbyBuildNumber(int buildNumber) {
        try (Jedis connection = jedis.getResource()) {
            connection.set("build.lobby", buildNumber + "");
        }
    }

    public void setCurrentProxyBuildNumber(int buildNumber) {
        try (Jedis connection = jedis.getResource()) {
            connection.set("build.proxy", buildNumber + "");
        }
    }

}

