/*
 * Copyright (c) 2021 AuroraMC Ltd. All Rights Reserved.
 */

package net.auroramc.missioncontrol.backend.managers;

import net.auroramc.missioncontrol.MissionControl;
import net.auroramc.missioncontrol.backend.runnables.StatUpdateRunnable;
import net.auroramc.missioncontrol.backend.util.*;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DatabaseManager {

    private final MySQLConnectionPool mysql;
    private final JedisPool jedis;

    public DatabaseManager(String mysqlHost, String mysqlPort, String mysqlDb, String mysqlUsername, String mysqlPassword, String redisHost, String redisAuth) {
        MissionControl.getLogger().fine("Initialising MySQL and Redis database connection pools...");
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
        MissionControl.getLogger().fine("Database connection pools initialised.");
    }

    public int newUser(UUID uuid, String name) {
        try (Connection connection = mysql.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO auroramc_players(uuid, `name`) VALUES (?, ?);");
            statement.setString(1, uuid.toString());
            statement.setString(2, name);

            statement.execute();

            int id = getAuroraMCID(uuid);
            //Creating records in necessary databases.

            statement = connection.prepareStatement("INSERT INTO ignored(amc_id, users) VALUES (?,'');");
            statement.setInt(1, id);
            statement.execute();

            statement = connection.prepareStatement("INSERT INTO ranks(amc_id, rank) VALUES (?, 0);");
            statement.setInt(1, id);
            statement.execute();

            try (Jedis con = jedis.getResource()) {
                //Setting core stats stuff as otherwise the server core wont be able to load player stats properly.
                Pipeline pipeline = con.pipelined();
                pipeline.hincrBy(String.format("stats.%s.core", uuid), "xpEarned", 0);
                pipeline.hincrBy(String.format("stats.%s.core", uuid), "xpIntoLevel", 0);
                pipeline.hincrBy(String.format("stats.%s.core", uuid), "level", 0);
                pipeline.hincrBy(String.format("stats.%s.core", uuid), "lobbyTimeMs", 0);
                pipeline.hincrBy(String.format("stats.%s.core", uuid), "gameTimeMs", 0);
                pipeline.hset(String.format("stats.%s.core", uuid), "firstJoinTimestamp", System.currentTimeMillis() + "");
                pipeline.hincrBy(String.format("stats.%s.core", uuid), "crownsEarned", 0);
                pipeline.hincrBy(String.format("stats.%s.core", uuid), "ticketsEarned", 0);
                pipeline.hincrBy(String.format("stats.%s.core", uuid), "gamesPlayed", 0);
                pipeline.hincrBy(String.format("stats.%s.core", uuid), "gamesWon", 0);
                pipeline.hincrBy(String.format("stats.%s.core", uuid), "gamesLost", 0);
                pipeline.hincrBy(String.format("bank.%s", uuid), "crowns", 0);
                pipeline.hincrBy(String.format("bank.%s", uuid), "tickets", 0);
                pipeline.hset(String.format("friends.%s", uuid), "visibility", "ALL");
                pipeline.hset(String.format("friends.%s", uuid), "status", "101");
                pipeline.hset(String.format("prefs.%s", uuid), "channel", "ALL");
                pipeline.hset(String.format("prefs.%s", uuid), "friendRequests", "true");
                pipeline.hset(String.format("prefs.%s", uuid), "partyRequests", "true");
                pipeline.hset(String.format("prefs.%s", uuid), "muteInformMode", "DISABLED");
                pipeline.hset(String.format("prefs.%s", uuid), "chatVisibility", "true");
                pipeline.hset(String.format("prefs.%s", uuid), "privateMessageMode", "ALL");
                pipeline.hset(String.format("prefs.%s", uuid), "pingOnPrivateMessage", "true");
                pipeline.hset(String.format("prefs.%s", uuid), "pingOnPartyChat", "true");
                pipeline.hset(String.format("prefs.%s", uuid), "pingOnChatMention", "true");
                pipeline.hset(String.format("prefs.%s", uuid), "hubVisibility", "true");
                pipeline.hset(String.format("prefs.%s", uuid), "hubSpeed", "false");
                pipeline.hset(String.format("prefs.%s", uuid), "hubFlight", "true");
                pipeline.hset(String.format("prefs.%s", uuid), "reportNotifications", "true");
                pipeline.hset(String.format("prefs.%s", uuid), "hubInvisibility", "false");
                pipeline.hset(String.format("prefs.%s", uuid), "ignoreHubKnockback", "false");
                pipeline.hset(String.format("prefs.%s", uuid), "socialMediaNotifications", "false");
                pipeline.hset(String.format("prefs.%s", uuid), "staffLoginNotifications", "false");
                pipeline.hset(String.format("prefs.%s", uuid), "approvalNotifications", "false");
                pipeline.hset(String.format("prefs.%s", uuid), "approvalProcessedNotifications", "false");
                pipeline.hset(String.format("prefs.%s", uuid), "hubForcefield", "false");
                pipeline.hset(String.format("prefs.%s", uuid), "hideDisguiseName", "false");
                pipeline.hset(String.format("prefs.%s", uuid), "pingOnChatMention", "true");
                pipeline.sync();
            }

            return id;

        } catch (SQLException e) {
            e.printStackTrace();
            return -2;
        }
    }

    public int getAuroraMCID(UUID uuid) {
        try (Connection connection = mysql.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("SELECT id FROM auroramc_players WHERE uuid = ?");
            statement.setString(1, uuid.toString());

            ResultSet set = statement.executeQuery();
            if (set.next()) {
                return set.getInt(1);
            } else {
                //NEW USER
                return -1;
            }

        } catch (SQLException e) {
            return -2;
        }
    }

    public void insertPayment(int paymentId, String transactionId, int amcId, double amountPaid, List<String> packages, List<String> crateUUIDs) {
        try (Connection connection = mysql.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM store_payments WHERE transaction_id = ?");
            statement.setInt(1, paymentId);
            ResultSet set = statement.executeQuery();
            if (!set.next()) {
                statement = connection.prepareStatement("INSERT INTO store_payments(payment_id, amc_id, transaction_id, packages_purchased, crate_uuids, status, amount_paid) VALUES (?,?,?,?,?,'PROCESSED',?)");
                statement.setInt(1, paymentId);
                statement.setInt(2, amcId);
                statement.setString(3, transactionId);
                statement.setString(4, String.join(",", packages));
                statement.setString(5, String.join(",", crateUUIDs));
                statement.setDouble(6, amountPaid);
                statement.execute();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void refundPayment(int paymentId) {
        try (Connection connection = mysql.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("UPDATE store_payments SET status = 'REFUNDED' WHERE transaction_id = ?");
            statement.setInt(1, paymentId);
            statement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void chargebackPayment(int paymentId) {
        try (Connection connection = mysql.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("UPDATE store_payments SET status = 'CHARGEDBACK' WHERE transaction_id = ?");
            statement.setInt(1, paymentId);
            statement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean hasActiveSession(UUID uuid) {
        try (Jedis connection = jedis.getResource()) {
            return connection.exists(String.format("server.MAIN.%s", uuid)) && connection.exists(String.format("proxy.MAIN.%s", uuid));
        }
    }

    public synchronized UUID getProxy(UUID uuid) {
        try (Jedis connection = jedis.getResource()) {
            return UUID.fromString(connection.get(String.format("proxy.MAIN.%s", uuid)));
        }
    }

    public boolean setRank(int id, int rank, int oldRank) {
        try (Connection connection = mysql.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("UPDATE ranks SET rank = ? WHERE amc_id = ?");
            statement.setInt(1, rank);
            statement.setLong(2, id);
            boolean success = statement.execute();

            statement = connection.prepareStatement("SELECT discord_id FROM dc_links WHERE amc_id = ?");
            statement.setLong(1, id);

            ResultSet results = statement.executeQuery();
            if (results.next()) {
                //The user has an active discord link, update ranks for discord.
                String discordId = results.getString(1);
                statement = connection.prepareStatement("SELECT * FROM rank_changes WHERE discord_id = ?");
                statement.setString(1, discordId);
                results = statement.executeQuery();

                if (results.next()) {
                    //There are already registered rank/subrank changes in the database. Check to see if a rank update has already occured.
                    if (results.getString(2) != null) {
                        if (results.getString(2).equals(rank + "")) {
                            statement = connection.prepareStatement("DELETE FROM rank_changes WHERE discord_id = ? AND old_rank = ?");
                            statement.setString(1, discordId);
                            statement.setString(2, results.getString(2));
                            statement.execute();
                            return success;
                        }
                        //The first result was a rank change, just update the new_rank column then return.
                        statement = connection.prepareStatement("UPDATE rank_changes SET new_rank = ? WHERE discord_id = ? AND old_rank = ?");
                        statement.setString(1, rank + "");
                        statement.setString(2, discordId);
                        statement.setString(3, results.getString(2));
                        statement.execute();
                        return success;
                    }
                    while (results.next()) {
                        if (results.getString(2) != null) {
                            if (results.getString(2).equals(rank + "")) {
                                statement = connection.prepareStatement("DELETE FROM rank_changes WHERE discord_id = ? AND old_rank = ?");
                                statement.setString(1, discordId);
                                statement.setString(2, results.getString(2));
                                statement.execute();
                                return success;
                            }
                            //The first result was a rank change, just update the new_rank column then return.
                            statement = connection.prepareStatement("UPDATE rank_changes SET new_rank = ? WHERE discord_id = ? AND old_rank = ?");
                            statement.setString(1, rank + "");
                            statement.setString(2, discordId);
                            statement.setString(3, results.getString(2));
                            statement.execute();
                            return success;
                        }
                    }

                    //If not returned by now, its not already in the database, so just insert it.
                    statement = connection.prepareStatement("INSERT INTO rank_changes(discord_id, old_rank, new_rank) VALUES (?,?,?)");
                    statement.setString(1, discordId);
                    statement.setString(2, oldRank + "");
                    statement.setString(3, rank + "");
                    statement.execute();
                } else {
                    //Just insert, it is the only rank update so far.
                    statement = connection.prepareStatement("INSERT INTO rank_changes(discord_id, old_rank, new_rank) VALUES (?,?,?)");
                    statement.setString(1, discordId);
                    statement.setString(2, oldRank + "");
                    statement.setString(3, rank + "");
                    statement.execute();
                }
            }

            return success;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public int getRank(int id) {
        try (Connection connection = mysql.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("SELECT rank FROM `ranks` WHERE amc_id = ?");
            statement.setLong(1, id);

            ResultSet set = statement.executeQuery();
            if (set.next()) {

                return set.getInt(1);
            } else {
                //NEW USER
                return 0;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public void issuePunishment(String code, int amc_id, int ruleID, String extraNotes, int punisherID, long issued, long expire, int status, String uuid) {
        try (Connection connection = mysql.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO punishments(punishment_id,amc_id,rule_id,notes,punisher,issued,expire,status) VALUES (?,?,?,?,?,?,?,?)");
            statement.setString(1, code);
            statement.setInt(2, amc_id);
            statement.setInt(3, ruleID);
            statement.setString(4, extraNotes);
            statement.setInt(5, punisherID);
            statement.setString(6, issued + "");
            statement.setString(7, expire + "");
            statement.setString(8, status + "");

            statement.execute();

            //Insert into redis.
            if (uuid != null) {
                try (Jedis con = jedis.getResource()) {
                    if (con.sismember("bans", uuid)) {
                        //This is a check to ensure that the oldest expiry time remains in the redis, so as not to add a short punishment length to the redis, resulting in bans not applying properly.
                        //This is a super rare case but is possible. Better safe than sorry.
                        if (con.exists(String.format("bans.%s", uuid))) {
                            long ttl = con.ttl(String.format("bans.%s", uuid));

                            if (ttl == -1) {
                                //this means that its a perma ban. do not remove it from the database.
                                return;
                            } else {
                                if (ttl > (int) ((expire-issued)/1000)) {
                                    //The time to live is longer, meaning it will expire after this one would, so do not add this to the redis.
                                    return;
                                }
                            }
                        }
                    } else {
                        con.sadd("bans", uuid);
                    }
                    con.set(String.format("bans.%s", uuid), ruleID + ";" + extraNotes + ";" + status + ";" + issued + ";" + expire + ";" + code);

                    if (expire != -1) {
                        con.expire(String.format("bans.%s", uuid), (int) ((expire-issued)/1000));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void extend(UUID uuid, int days) {
        try (Jedis connection = jedis.getResource()) {
            connection.hincrBy(String.format("plus.%s",uuid), "daysSubscribed", days);
            long expire = (Long.parseLong(connection.hget(String.format("plus.%s", uuid), "expire")) + (((long) days) * 86400000));
            connection.hset(String.format("plus.%s", uuid), "expire", expire + "");
        }
    }

    public long getExpire(UUID uuid) {
        try (Jedis connection = jedis.getResource()) {
            if (connection.hexists(String.format("plus.%s", uuid), "expire")) {
                return Long.parseLong(connection.hget(String.format("plus.%s", uuid), "expire"));
            } else {
                return -1;
            }
        }
    }

    public void newSubscription(UUID uuid, int days) {
        try (Jedis connection = jedis.getResource()) {
            connection.hincrBy(String.format("plus.%s",uuid), "daysSubscribed", days);
            long expire = System.currentTimeMillis() + ((long) days) * 86400000L;
            connection.hset(String.format("plus.%s", uuid), "expire", expire + "");
            connection.hset(String.format("plus.%s", uuid), "streakStart", System.currentTimeMillis() + "");
            connection.hset(String.format("plus.%s", uuid), "streak", "0");
        }
    }

    public boolean hasUnlockedCosmetic(UUID uuid, int id) {
        try (Jedis connection = jedis.getResource()) {
            return connection.sismember(String.format("cosmetics.unlocked.%s", uuid.toString()), id + "");
        }
    }

    public void addCosmetic(UUID uuid, int cosmetic) {
        try (Jedis connection = jedis.getResource()) {
            connection.sadd(String.format("cosmetics.unlocked.%s", uuid.toString()), cosmetic + "");
        }
    }

    public void removeCosmetic(UUID uuid, int cosmetic) {
        try (Jedis connection = jedis.getResource()) {
            connection.srem(String.format("cosmetics.unlocked.%s", uuid.toString()), cosmetic + "");
        }
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
                servers.get(ServerInfo.Network.valueOf(set.getString(4))).put(set.getString(1), new ServerInfo(set.getString(1), set.getString(2), set.getInt(3), ServerInfo.Network.valueOf(set.getString(4)), set.getBoolean(5), new JSONObject(set.getString(6)), set.getInt(7), set.getInt(8), set.getInt(9), set.getInt(10), set.getInt(11), set.getInt(12), set.getInt(13), set.getString(14)));
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
                proxies.put(UUID.fromString(set.getString(1)), new ProxyInfo(UUID.fromString(set.getString(1)), set.getString(2), set.getInt(3), ServerInfo.Network.valueOf(set.getString(4)), set.getBoolean(5), set.getInt(6), set.getInt(7), set.getString(8)));
            }
            return proxies;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void createServer(ServerInfo info) {
        try (Connection connection = mysql.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO servers VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
            statement.setString(1, info.getName());
            statement.setString(2, info.getIp());
            statement.setInt(3, info.getPort());
            statement.setString(4, info.getNetwork().name());
            statement.setBoolean(5, info.isForced());
            statement.setString(6, info.getServerType().toString());
            statement.setInt(7, info.getProtocolPort());
            statement.setInt(8, info.getBuildNumber());
            if (info.getLobbyBuildNumber() == 0) {
                statement.setNull(9, Types.INTEGER);
            } else {
                statement.setInt(9, info.getLobbyBuildNumber());
            }
            if (info.getEngineBuildNumber() == 0) {
                statement.setNull(10, Types.INTEGER);
            } else {
                statement.setInt(10, info.getEngineBuildNumber());
            }
            if (info.getGameBuildNumber() == 0) {
                statement.setNull(11, Types.INTEGER);
            } else {
                statement.setInt(11, info.getGameBuildNumber());
            }
            if (info.getBuildBuildNumber() == 0) {
                statement.setNull(12, Types.INTEGER);
            } else {
                statement.setInt(12, info.getBuildBuildNumber());
            }
            if (info.getDuelsBuildNumber() == 0) {
                statement.setNull(13, Types.INTEGER);
            } else {
                statement.setInt(13, info.getDuelsBuildNumber());
            }
            statement.setString(14, info.getAuthKey());
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
            PreparedStatement statement = connection.prepareStatement("INSERT INTO proxies VALUES (?,?,?,?,?,?,?,?)");
            statement.setString(1, proxyInfo.getUuid().toString());
            statement.setString(2, proxyInfo.getIp());
            statement.setInt(3, proxyInfo.getPort());
            statement.setString(4, proxyInfo.getNetwork().name());
            statement.setBoolean(5, proxyInfo.isForced());
            statement.setInt(6, proxyInfo.getProtocolPort());
            statement.setInt(7, proxyInfo.getBuildNumber());
            statement.setString(8, proxyInfo.getAuthKey());
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
                return new ServerInfo(set.getString(1), set.getString(2), set.getInt(3), ServerInfo.Network.valueOf(set.getString(4)), set.getBoolean(5), new JSONObject(set.getString(6)), set.getInt(7), set.getInt(8), set.getInt(9), set.getInt(10), set.getInt(11), set.getInt(12), set.getInt(13), set.getString(14));
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
                return 0;
            }
        }
    }

    public int getCurrentCoreBuildNumber() {
        try (Jedis connection = jedis.getResource()) {
            String buildNumber = connection.get("build.core");
            if (buildNumber != null) {
                return Integer.parseInt(buildNumber);
            } else {
                return 0;
            }
        }
    }

    public int getCurrentEngineBuildNumber() {
        try (Jedis connection = jedis.getResource()) {
            String buildNumber = connection.get("build.engine");
            if (buildNumber != null) {
                return Integer.parseInt(buildNumber);
            } else {
                return 0;
            }
        }
    }

    public int getCurrentGameBuildNumber() {
        try (Jedis connection = jedis.getResource()) {
            String buildNumber = connection.get("build.game");
            if (buildNumber != null) {
                return Integer.parseInt(buildNumber);
            } else {
                return 0;
            }
        }
    }

    public int getCurrentLobbyBuildNumber() {
        try (Jedis connection = jedis.getResource()) {
            String buildNumber = connection.get("build.lobby");
            if (buildNumber != null) {
                return Integer.parseInt(buildNumber);
            } else {
                return 0;
            }
        }
    }

    public int getCurrentProxyBuildNumber() {
        try (Jedis connection = jedis.getResource()) {
            String buildNumber = connection.get("build.proxy");
            if (buildNumber != null) {
                return Integer.parseInt(buildNumber);
            } else {
                return 0;
            }
        }
    }

    public int getCurrentDuelsBuildNumber() {
        try (Jedis connection = jedis.getResource()) {
            String buildNumber = connection.get("build.duels");
            if (buildNumber != null) {
                return Integer.parseInt(buildNumber);
            } else {
                return 0;
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

    public void setCurrentDuelsBuildNumber(int buildNumber) {
        try (Jedis connection = jedis.getResource()) {
            connection.set("build.duels", buildNumber + "");
        }
        try (Connection connection = mysql.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("UPDATE servers SET duels_buildnumber = ? WHERE servers.duels_buildnumber IS NOT NULL");
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

    public boolean isGameEnabled(ServerType serverType, ServerInfo.Network network) {
        try (Jedis connection = jedis.getResource()) {
            if (!connection.hexists(String.format("missioncontrol.%s", network.name()), String.format("enabled.%s", serverType.name()))) {
                connection.hset(String.format("missioncontrol.%s", network.name()), String.format("enabled.%s", serverType.name()), "false");
                return false;
            }
            return Boolean.parseBoolean(connection.hget(String.format("missioncontrol.%s", network.name()), String.format("enabled.%s", serverType.name())));
        }
    }

    public Map<ServerType, Boolean> getGameEnabled(ServerInfo.Network network) {
        try (Jedis connection = jedis.getResource()) {
            if (!connection.exists(String.format("missioncontrol.%s", network.name()))) {
                Map<ServerType, Boolean> enabled = new HashMap<>();
                Pipeline pipeline = connection.pipelined();
                for (ServerType serverType : ServerType.values()) {
                    enabled.put(serverType, false);
                    pipeline.hset(String.format("missioncontrol.%s", network.name()), String.format("enabled.%s", serverType.name()), "false");
                }
                pipeline.sync();
                return enabled;
            }
            Map<ServerType, Boolean> enabled = new HashMap<>();
            for (ServerType serverType : ServerType.values()) {
                if (!connection.hexists(String.format("missioncontrol.%s", network.name()), String.format("enabled.%s", serverType.name()))) {
                    connection.hset(String.format("missioncontrol.%s", network.name()), String.format("enabled.%s", serverType.name()), "false");
                    enabled.put(serverType, false);
                    continue;
                }
                Boolean enable = Boolean.parseBoolean(connection.hget(String.format("missioncontrol.%s", network.name()), String.format("enabled.%s", serverType.name())));
                enabled.put(serverType, enable);
            }
            return enabled;
        }
    }

    public void setGameEnabled(ServerType serverType, ServerInfo.Network network, boolean enabled) {
        try (Jedis connection = jedis.getResource()) {
            connection.hset(String.format("missioncontrol.%s", network.name()), String.format("enabled.%s", serverType.name()), enabled + "");
        }
    }

    public boolean isMonitoringEnabled(ServerType serverType, ServerInfo.Network network) {
        try (Jedis connection = jedis.getResource()) {
            if (!connection.hexists(String.format("missioncontrol.%s", network.name()), String.format("monitor.%s", serverType.name()))) {
                connection.hset(String.format("missioncontrol.%s", network.name()), String.format("monitor.%s", serverType.name()), "false");
                return false;
            }
            return Boolean.parseBoolean(connection.hget(String.format("missioncontrol.%s", network.name()), String.format("monitor.%s", serverType.name())));
        }
    }

    public Map<ServerType, Boolean> getMonitoring(ServerInfo.Network network) {
        try (Jedis connection = jedis.getResource()) {
            if (!connection.exists(String.format("missioncontrol.%s", network.name()))) {
                Map<ServerType, Boolean> monitoring = new HashMap<>();
                Pipeline pipeline = connection.pipelined();
                for (ServerType serverType : ServerType.values()) {
                    monitoring.put(serverType, false);
                    pipeline.hset(String.format("missioncontrol.%s", network.name()), String.format("monitoring.%s", serverType.name()), "false");
                }
                pipeline.sync();
                return monitoring;
            }
            Map<ServerType, Boolean> monitoring = new HashMap<>();
            for (ServerType serverType : ServerType.values()) {
                if (!connection.hexists(String.format("missioncontrol.%s", network.name()), String.format("monitoring.%s", serverType.name()))) {
                    connection.hset(String.format("missioncontrol.%s", network.name()), String.format("monitoring.%s", serverType.name()), "false");
                    monitoring.put(serverType, false);
                    continue;
                }
                Boolean enable = Boolean.parseBoolean(connection.hget(String.format("missioncontrol.%s", network.name()), String.format("monitoring.%s", serverType.name())));
                monitoring.put(serverType, enable);
            }
            return monitoring;
        }
    }

    public void setMonitoringEnabled(ServerType serverType, ServerInfo.Network network, boolean enabled) {
        try (Jedis connection = jedis.getResource()) {
            connection.hset(String.format("missioncontrol.%s", network.name()), String.format("monitoring.%s", serverType.name()), enabled + "");
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

    public int getPlayersPerGame(Game game, StatUpdateRunnable.StatisticPeriod frequency) {
        try (Jedis connection = jedis.getResource()) {
            String amount = connection.hget(String.format("stat.playerspergame.%s", game.name()), frequency.name());
            if (amount == null) {
                return 0;
            }
            int i = Integer.parseInt(amount);
            connection.hdel(String.format("stat.playerspergame.%s", game.name()), frequency.name());
            return i;
        }
    }

    public int getGamesStarted(Game game, StatUpdateRunnable.StatisticPeriod frequency) {
        try (Jedis connection = jedis.getResource()) {
            String amount = connection.hget(String.format("stat.gamesstarted.%s", game.name()), frequency.name());
            if (amount == null) {
                return 0;
            }
            int i = Integer.parseInt(amount);
            connection.hdel(String.format("stat.gamesstarted.%s", game.name()), frequency.name());
            return i;
        }
    }

    public int getUniquePlayerJoins(StatUpdateRunnable.StatisticPeriod frequency) {
        try (Jedis connection = jedis.getResource()) {
            String amount = connection.hget("stat.uniqueplayerjoins", frequency.name());
            if (amount == null) {
                return 0;
            }
            int i = Integer.parseInt(amount);
            connection.hdel("stat.uniqueplayerjoins", frequency.name());
            return i;
        }
    }

    public int getUniquePlayerTotals() {
        try (Connection connection = mysql.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("SELECT id FROM auroramc_players ORDER BY id DESC LIMIT 1");
            ResultSet set = statement.executeQuery();
            if (set.next()) {
                return set.getInt(1);
            } else {
                return 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public void insertStatistic(Statistic statistic, StatUpdateRunnable.StatisticPeriod period, long timestamp, int value) {
        try (Jedis connection = jedis.getResource()) {
            connection.sadd("stat." + statistic.name() + "." + period, timestamp + ";" + value);
        }
    }

    public void insertStatistic(Statistic statistic, StatUpdateRunnable.StatisticPeriod period, long timestamp, int value, Game game) {
        try (Jedis connection = jedis.getResource()) {
            connection.sadd("stat." + statistic.name() + "." + period, timestamp + ";" + value + ";" + game.name());
        }
    }

    public void insertStatistic(Statistic statistic, StatUpdateRunnable.StatisticPeriod period, long timestamp, int value, ServerType game) {
        try (Jedis connection = jedis.getResource()) {
            connection.sadd("stat." + statistic.name() + "." + period, timestamp + ";" + value + ";" + game.name());
        }
    }

    public void setPanelCode(UUID uuid, String code) {
        try (Jedis connection = jedis.getResource()) {
            connection.set("panel.code." + uuid.toString(), code);
            connection.expire("panel.code." + uuid, 60);
        }
    }

    public void newCrate(UUID uuid, String type, int id) {
        try (Connection connection = mysql.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO crates(uuid, type, amc_id) VALUES (?,?,?)");
            statement.setString(1, uuid.toString());
            statement.setString(2, type);
            statement.setInt(3, id);
            statement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
