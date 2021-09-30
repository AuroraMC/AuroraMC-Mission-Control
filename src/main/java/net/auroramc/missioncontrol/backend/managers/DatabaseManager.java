package net.auroramc.missioncontrol.backend.managers;

import net.auroramc.missioncontrol.MissionControl;
import net.auroramc.missioncontrol.backend.Game;
import net.auroramc.missioncontrol.backend.MaintenanceMode;
import net.auroramc.missioncontrol.backend.Module;
import net.auroramc.missioncontrol.backend.MySQLConnectionPool;
import net.auroramc.missioncontrol.entities.ProxyInfo;
import net.auroramc.missioncontrol.entities.ServerInfo;
import org.json.JSONObject;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Pipeline;

import java.sql.*;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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

    public Map<ServerInfo.Network, Map<String, ServerInfo>> getAllServers() {
        try (Connection connection = mysql.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM servers");
            ResultSet set = statement.executeQuery();

            Map<ServerInfo.Network, Map<String, ServerInfo>> servers = new HashMap<>();
            servers.put(ServerInfo.Network.MAIN, new HashMap<>());
            servers.put(ServerInfo.Network.TEST, new HashMap<>());
            servers.put(ServerInfo.Network.ALPHA, new HashMap<>());
            while (set.next()) {
                servers.get(ServerInfo.Network.valueOf(set.getString(4))).put(set.getString(1), new ServerInfo(set.getString(1), set.getString(2), set.getInt(3), ServerInfo.Network.valueOf(set.getString(4)), set.getBoolean(5), new JSONObject(set.getString(6)), set.getInt(7), set.getInt(8), set.getInt(9), set.getInt(10), set.getInt(11), set.getInt(12)));
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
            if (info.getLobbyBuildNumber() == -1) {
                statement.setNull(9, Types.INTEGER);
            } else {
                statement.setInt(9, info.getLobbyBuildNumber());
            }
            if (info.getEngineBuildNumber() == -1) {
                statement.setNull(10, Types.INTEGER);
            } else {
                statement.setInt(10, info.getEngineBuildNumber());
            }
            if (info.getGameBuildNumber() == -1) {
                statement.setNull(11, Types.INTEGER);
            } else {
                statement.setInt(11, info.getGameBuildNumber());
            }
            if (info.getBuildBuildNumber() == -1) {
                statement.setNull(12, Types.INTEGER);
            } else {
                statement.setInt(12, info.getBuildBuildNumber());
            }
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
        try (Connection connection = mysql.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("UPDATE servers SET build_buildNumber = ? WHERE build_buildNumber IS NOT NULL");
            statement.setInt(1, buildNumber);
            statement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setCurrentCoreBuildNumber(int buildNumber) {
        try (Jedis connection = jedis.getResource()) {
            connection.set("build.core", buildNumber + "");
        }
        try (Connection connection = mysql.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("UPDATE servers SET buildNumber = ?");
            statement.setInt(1, buildNumber);
            statement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setCurrentEngineBuildNumber(int buildNumber) {
        try (Jedis connection = jedis.getResource()) {
            connection.set("build.engine", buildNumber + "");
        }
        try (Connection connection = mysql.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("UPDATE servers SET engine_buildNumber = ? WHERE servers.engine_buildNumber IS NOT NULL");
            statement.setInt(1, buildNumber);
            statement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setCurrentGameBuildNumber(int buildNumber) {
        try (Jedis connection = jedis.getResource()) {
            connection.set("build.game", buildNumber + "");
        }
        try (Connection connection = mysql.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("UPDATE servers SET game_buildNumber = ? WHERE servers.game_buildNumber IS NOT NULL");
            statement.setInt(1, buildNumber);
            statement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setCurrentLobbyBuildNumber(int buildNumber) {
        try (Jedis connection = jedis.getResource()) {
            connection.set("build.lobby", buildNumber + "");
        }
        try (Connection connection = mysql.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("UPDATE servers SET lobby_buildNumber = ? WHERE servers.lobby_buildNumber IS NOT NULL");
            statement.setInt(1, buildNumber);
            statement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setCurrentProxyBuildNumber(int buildNumber) {
        try (Jedis connection = jedis.getResource()) {
            connection.set("build.proxy", buildNumber + "");
        }
        try (Connection connection = mysql.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("UPDATE proxies SET build_number = ?");
            statement.setInt(1, buildNumber);
            statement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean isGameEnabled(Game game, ServerInfo.Network network) {
        try (Jedis connection = jedis.getResource()) {
            if (!connection.hexists(String.format("missioncontrol.%s", network.name()), String.format("enabled.%s", game.name()))) {
                connection.hset(String.format("missioncontrol.%s", network.name()), String.format("enabled.%s", game.name()), "false");
                return false;
            }
            return Boolean.parseBoolean(connection.hget(String.format("missioncontrol.%s", network.name()), String.format("enabled.%s", game.name())));
        }
    }

    public Map<Game, Boolean> getGameEnabled(ServerInfo.Network network) {
        try (Jedis connection = jedis.getResource()) {
            if (!connection.exists(String.format("missioncontrol.%s", network.name()))) {
                Map<Game, Boolean> enabled = new HashMap<>();
                Pipeline pipeline = connection.pipelined();
                for (Game game : Game.values()) {
                    enabled.put(game, false);
                    pipeline.hset(String.format("missioncontrol.%s", network.name()), String.format("enabled.%s", game.name()), "false");
                }
                pipeline.sync();
                return enabled;
            }
            Map<Game, Boolean> enabled = new HashMap<>();
            for (Game game : Game.values()) {
                if (!connection.hexists(String.format("missioncontrol.%s", network.name()), String.format("enabled.%s", game.name()))) {
                    connection.hset(String.format("missioncontrol.%s", network.name()), String.format("enabled.%s", game.name()), "false");
                    enabled.put(game, false);
                    continue;
                }
                Boolean enable = Boolean.parseBoolean(connection.hget(String.format("missioncontrol.%s", network.name()), String.format("enabled.%s", game.name())));
                enabled.put(game, enable);
            }
            return enabled;
        }
    }

    public void setGameEnabled(Game game, ServerInfo.Network network, boolean enabled) {
        try (Jedis connection = jedis.getResource()) {
            connection.hset(String.format("missioncontrol.%s", network.name()), String.format("enabled.%s", game.name()), enabled + "");
        }
    }

    public boolean isMonitoringEnabled(Game game, ServerInfo.Network network) {
        try (Jedis connection = jedis.getResource()) {
            if (!connection.hexists(String.format("missioncontrol.%s", network.name()), String.format("monitor.%s", game.name()))) {
                connection.hset(String.format("missioncontrol.%s", network.name()), String.format("monitor.%s", game.name()), "false");
                return false;
            }
            return Boolean.parseBoolean(connection.hget(String.format("missioncontrol.%s", network.name()), String.format("monitor.%s", game.name())));
        }
    }

    public Map<Game, Boolean> getMonitoring(ServerInfo.Network network) {
        try (Jedis connection = jedis.getResource()) {
            if (!connection.exists(String.format("missioncontrol.%s", network.name()))) {
                Map<Game, Boolean> monitoring = new HashMap<>();
                Pipeline pipeline = connection.pipelined();
                for (Game game : Game.values()) {
                    monitoring.put(game, false);
                    pipeline.hset(String.format("missioncontrol.%s", network.name()), String.format("monitoring.%s", game.name()), "false");
                }
                pipeline.sync();
                return monitoring;
            }
            Map<Game, Boolean> monitoring = new HashMap<>();
            for (Game game : Game.values()) {
                if (!connection.hexists(String.format("missioncontrol.%s", network.name()), String.format("monitoring.%s", game.name()))) {
                    connection.hset(String.format("missioncontrol.%s", network.name()), String.format("monitoring.%s", game.name()), "false");
                    monitoring.put(game, false);
                    continue;
                }
                Boolean enable = Boolean.parseBoolean(connection.hget(String.format("missioncontrol.%s", network.name()), String.format("monitoring.%s", game.name())));
                monitoring.put(game, enable);
            }
            return monitoring;
        }
    }

    public void setMonitoringEnabled(Game game, ServerInfo.Network network, boolean enabled) {
        try (Jedis connection = jedis.getResource()) {
            connection.hset(String.format("missioncontrol.%s", network.name()), String.format("monitor.%s", game.name()), enabled + "");
        }
    }

    public boolean isAlphaEnabled() {
        try (Jedis connection = jedis.getResource()) {
            if (!connection.hexists("missioncontrol.alpha", "enabled")) {
                connection.hset("missioncontrol.alpha", "enabled", "false");
                return false;
            }
            return Boolean.parseBoolean(connection.hget("missioncontrol.alpha", "enabled"));
        }
    }

    public void setAlphaEnabled(boolean enabled) {
        try (Jedis connection = jedis.getResource()) {
            connection.hset("missioncontrol.alpha", "enabled", enabled + "");
        }
    }

    public Map<Module, String> getBranchMappings() {
        try (Jedis connection = jedis.getResource()) {
            Map<String, String> mappings = connection.hgetAll("missioncontrol.alpha.branches");
            Map<Module, String> branchMappings = new HashMap<>();
            for (Module module : Module.values()) {
                if (mappings.get(module.name()) == null) {
                    setBranchMapping(module, "master");
                }
                branchMappings.put(module, mappings.get(module.name()));
            }
           return branchMappings;
        }
    }

    public void setBranchMapping(Module module, String branch) {
        try (Jedis connection = jedis.getResource()) {
            connection.hset("missioncontrol.alpha.branches", module.name(), branch);
        }
    }

    public Map<Module, Integer> getBuildMappings() {
        try (Jedis connection = jedis.getResource()) {
            Map<String, String> mappings = connection.hgetAll("missioncontrol.alpha.builds");
            Map<Module, Integer> buildMappings = new HashMap<>();
            for (Module module : Module.values()) {
                if (mappings.get(module.name()) == null) {
                    setBuildMapping(module, 1);
                }
                buildMappings.put(module, 1);
            }
            return buildMappings;
        }
    }

    public void setBuildMapping(Module module, int buildNumber) {
        try (Jedis connection = jedis.getResource()) {
            connection.hset("missioncontrol.alpha.builds", module.name(), buildNumber + "");
        }
    }

    public boolean isServerManagerEnabled(ServerInfo.Network network) {
        try (Jedis connection = jedis.getResource()) {
            if (!connection.hexists("missioncontrol.manager.enabled", network.name())) {
                connection.hset("missioncontrol.manager.enabled", network.name(), "false");
                return false;
            }
            return Boolean.parseBoolean(connection.hget("missioncontrol.manager.enabled", network.name()));
        }
    }

    public void setServerManagerEnabled(ServerInfo.Network network, boolean enabled) {
        try (Jedis connection = jedis.getResource()) {
            connection.hset("missioncontrol.manager.enabled", network.name(), enabled + "");
        }
    }

    public Map<ServerInfo.Network, Boolean> getMaintenance() {
        try (Connection connection = mysql.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM proxy_settings");
            Map<ServerInfo.Network, Boolean> maintenance = new HashMap<>();
            ResultSet set = statement.executeQuery();
            while (set.next()) {
                maintenance.put(ServerInfo.Network.valueOf(set.getString(1)), set.getBoolean(2));
            }
            return maintenance;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Map<ServerInfo.Network, MaintenanceMode> getMaintenanceMode() {
        try (Connection connection = mysql.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM proxy_settings");
            Map<ServerInfo.Network, MaintenanceMode> maintenance = new HashMap<>();
            ResultSet set = statement.executeQuery();
            while (set.next()) {
                maintenance.put(ServerInfo.Network.valueOf(set.getString(1)), MaintenanceMode.valueOf(set.getString(5)));
            }
            return maintenance;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Map<ServerInfo.Network, String> getMaintenanceMotd() {
        try (Connection connection = mysql.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM proxy_settings");
            Map<ServerInfo.Network, String> maintenance = new HashMap<>();
            ResultSet set = statement.executeQuery();
            while (set.next()) {
                maintenance.put(ServerInfo.Network.valueOf(set.getString(1)), set.getString(3));
            }
            return maintenance;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Map<ServerInfo.Network, String> getMotd() {
        try (Connection connection = mysql.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM proxy_settings");
            Map<ServerInfo.Network, String> maintenance = new HashMap<>();
            ResultSet set = statement.executeQuery();
            while (set.next()) {
                maintenance.put(ServerInfo.Network.valueOf(set.getString(1)), set.getString(4));
            }
            return maintenance;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void changeMaintenance(ServerInfo.Network network, boolean maintenance) {
        try (Connection connection = mysql.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("UPDATE proxy_settings SET maintenance = ? WHERE network = ?");
            statement.setBoolean(1, maintenance);
            statement.setString(2, network.name());
            boolean set = statement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public void changeMaintenanceMotd(ServerInfo.Network network, String motd) {
        try (Connection connection = mysql.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("UPDATE proxy_settings SET maintenance_motd = ? WHERE network = ?");
            if (motd == null) {
                statement.setNull(1, Types.VARCHAR);
            } else {
                statement.setString(1, motd);
            }
            statement.setString(2, network.name());
            boolean set = statement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void changeMaintenanceMode(ServerInfo.Network network, MaintenanceMode mode) {
        try (Connection connection = mysql.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("UPDATE proxy_settings SET maintenance_mode = ? WHERE network = ?");
            if (mode == null) {
                statement.setNull(1, Types.VARCHAR);
            } else {
                statement.setString(1, mode.name());
            }
            statement.setString(2, network.name());
            boolean set = statement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void changeMotd(ServerInfo.Network network, String motd) {
        try (Connection connection = mysql.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("UPDATE proxy_settings SET motd = ? WHERE network = ?");
            statement.setString(1, motd);
            statement.setString(2, network.name());
            boolean set = statement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void pushPlayerCount(ServerInfo.Network network, int amount) {
        try (Jedis connection = jedis.getResource()) {
            connection.set(String.format("playercount.%s", network.name().toLowerCase()), amount + "");
        }
    }

}
