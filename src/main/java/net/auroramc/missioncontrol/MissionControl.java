/*
 * Copyright (c) 2021 AuroraMC Ltd. All Rights Reserved.
 */

package net.auroramc.missioncontrol;

import com.mattmalec.pterodactyl4j.application.entities.ApplicationServer;
import jline.console.ConsoleReader;
import net.auroramc.core.api.backend.communication.ServerCommunicationUtils;
import net.auroramc.missioncontrol.backend.util.Game;
import net.auroramc.missioncontrol.backend.managers.DatabaseManager;
import net.auroramc.missioncontrol.backend.managers.HaProxyManager;
import net.auroramc.missioncontrol.backend.managers.JenkinsManager;
import net.auroramc.missioncontrol.backend.managers.PanelManager;
import net.auroramc.missioncontrol.commands.*;
import net.auroramc.missioncontrol.entities.ProxyInfo;
import net.auroramc.missioncontrol.entities.ServerInfo;
import net.auroramc.proxy.api.backend.communication.ProxyCommunicationUtils;
import net.md_5.bungee.log.MissionControlLogger;
import net.md_5.bungee.log.LoggingOutputStream;
import org.fusesource.jansi.AnsiConsole;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

public class MissionControl {

    private static ConsoleReader consoleReader;
    private static Logger logger;
    private static DatabaseManager dbManager;
    private static PanelManager panelManager;
    private static JenkinsManager jenkinsManager;
    private static HaProxyManager proxyManager;

    private static Map<ServerInfo.Network, Map<String, ServerInfo>> servers;
    private static Map<UUID, ProxyInfo> proxies;
    private static Map<String, Command> commands;

    public static void main(String[] args) throws Exception {
        System.setProperty( "library.jansi.version", "BungeeCord" );

        AnsiConsole.systemInstall();
        try {
            consoleReader = new ConsoleReader();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        consoleReader.setExpandEvents( false );

        logger = new MissionControlLogger( "Mission Control", "missioncontrol.log", consoleReader );
        System.setErr( new PrintStream( new LoggingOutputStream( logger, Level.SEVERE ), true ) );
        System.setOut( new PrintStream( new LoggingOutputStream( logger, Level.INFO ), true ) );
        Thread.currentThread().setName("Main Thread");
        logger.info("Starting AuroraMC Mission Control...");
        commands = new HashMap<>();

        Preferences prefs = Preferences.userNodeForPackage(MissionControl.class);
        String mysqlHost = prefs.get("mysqlHost", null);
        String mysqlPort = prefs.get("mysqlPort", null);
        String mysqlDb = prefs.get("mysqlDb", null);
        String mysqlUsername = prefs.get("mysqlUsername", null);
        String mysqlPassword = prefs.get("mysqlPassword", null);
        String mysqlServerUsername = prefs.get("mysqlServerUsername", null);
        String mysqlServerPassword = prefs.get("mysqlServerPassword", null);
        String redisHost = prefs.get("redisHost", null);
        String redisAuth = prefs.get("redisAuth", null);
        String ciBaseURL = prefs.get("ciBaseURL", null);
        String ciAPIKey = prefs.get("ciAPIKey", null);
        String panelBaseURL = prefs.get("panelBaseURL", null);
        String panelAPIKey = prefs.get("panelAPIKey", null);
        String panelUserAPIKey = prefs.get("panelUserAPIKey", null);
        String loadBalancerBaseURL = prefs.get("loadBalancerBaseURL", null);
        String loadBalancerAuth = prefs.get("loadBalancerAuth", null);

        if (mysqlHost == null || mysqlPort == null || mysqlDb == null || mysqlUsername == null || mysqlPassword == null || redisHost == null || redisAuth == null || ciBaseURL == null || panelBaseURL == null || loadBalancerBaseURL == null || ciAPIKey == null || panelAPIKey == null || loadBalancerAuth == null || panelUserAPIKey == null) {
            logger.info("\n" +
                    "===================================================\n" +
                    "AuroraMC Mission Control First Time Setup\n" +
                    "\n" +
                    "Welcome to the AuroraMC  Mission Control first time setup.\n" +
                    "To start Mission Control properly, we need some information\n" +
                    "about the network.\n" +
                    "\n" +
                    "To start off, what is the MySQL database host?\n");
            mysqlHost = consoleReader.readLine(">");
            logger.info("Now, we need the MySQL database port?\n");
            mysqlPort = consoleReader.readLine(">");
            logger.info("Now, we need the MySQL database Database name?\n");
            mysqlDb = consoleReader.readLine(">");
            logger.info("Now, we need the MySQL database username to be used by Mission Control?\n");
            mysqlUsername = consoleReader.readLine(">");
            logger.info("Now, we need the MySQL database password to be used by Mission Control?\n");
            mysqlPassword = consoleReader.readLine(">");
            logger.info("Now, we need the MySQL database username to be used by servers?\n");
            mysqlServerUsername = consoleReader.readLine(">");
            logger.info("Now, we need the MySQL database password to be used by servers?\n");
            mysqlServerPassword = consoleReader.readLine(">");
            logger.info("Now, we need the Redis database host?\n");
            redisHost = consoleReader.readLine(">");
            logger.info("Now, we need the Redis database password?\n");
            redisAuth = consoleReader.readLine(">");
            logger.info("Now, we need the base URL for the Jenkins API?\n");
            ciBaseURL = consoleReader.readLine(">");
            logger.info("Now, we need the API key for the Jenkins API?\n");
            ciAPIKey = consoleReader.readLine(">");
            logger.info("Now, we need the base URL for the Pterodactyl API?\n");
            panelBaseURL = consoleReader.readLine(">");
            logger.info("Now, we need the Application API key for the Pterodactyl API?\n");
            panelAPIKey = consoleReader.readLine(">");
            logger.info("Now, we need the User API key for the Pterodactyl API?\n");
            panelUserAPIKey = consoleReader.readLine(">");
            logger.info("Now, we need the base URL for the HaProxy API?\n");
            loadBalancerBaseURL = consoleReader.readLine(">");
            logger.info("Now, we need the password for the HaProxy API?\n");
            loadBalancerAuth = consoleReader.readLine(">");
            logger.info("That's now everything! First time setup is complete!\n" +
                    "If the details need to change, you can use the Mission Control\n" +
                    "admin panel to modify them!\n" +
                    "===================================================");
            prefs.put("mysqlHost", mysqlHost);
            prefs.put("mysqlPort", mysqlPort);
            prefs.put("mysqlDb", mysqlDb);
            prefs.put("mysqlUsername", mysqlUsername);
            prefs.put("mysqlPassword", mysqlPassword);
            prefs.put("mysqlServerUsername", mysqlServerUsername);
            prefs.put("mysqlServerPassword", mysqlServerPassword);
            prefs.put("redisHost", redisHost);
            prefs.put("redisAuth", redisAuth);
            prefs.put("ciBaseURL", ciBaseURL);
            prefs.put("ciAPIKey", ciAPIKey);
            prefs.put("panelBaseURL", panelBaseURL);
            prefs.put("panelAPIKey", panelAPIKey);
            prefs.put("panelUserAPIKey", panelUserAPIKey);
            prefs.put("loadBalancerBaseURL", loadBalancerBaseURL);
            prefs.put("loadBalancerAuth", loadBalancerAuth);
        }

        logger.info("Registering commands...");
        registerCommand(new CommandAlpha());
        registerCommand(new CommandGame());
        registerCommand(new CommandHelp());
        registerCommand(new CommandServerManager());
        registerCommand(new CommandUpdate());
        registerCommand(new CommandExit());

        dbManager = new DatabaseManager(mysqlHost, mysqlPort, mysqlDb, mysqlUsername, mysqlPassword, redisHost, redisAuth);

        proxyManager = new HaProxyManager(loadBalancerBaseURL, loadBalancerAuth);
        panelManager = new PanelManager(panelBaseURL, panelAPIKey, panelUserAPIKey, ciAPIKey, mysqlHost, mysqlPort, mysqlDb, mysqlServerUsername, mysqlServerPassword, redisHost, redisAuth);
        jenkinsManager = new JenkinsManager(ciBaseURL, ciAPIKey);

        logger.info("Loading current server/connection node configuration...");

        servers = dbManager.getAllServers();
        proxies = dbManager.getAllConnectionNodes();

        logger.info("Checking HaProxy and Pterodactyl configuration for mismatches...");

        List<ApplicationServer> panelServers = panelManager.getAllServers();
        List<ApplicationServer> panelServersCopy = new ArrayList<>(panelServers);

        Map<ServerInfo.Network, Set<String>> serverNameSets = new HashMap<>();
        for (ServerInfo.Network network : ServerInfo.Network.values()) {
           serverNameSets.put(network, new HashSet<>(servers.get(network).keySet()));
        }
        Set<UUID> proxyNames = new HashSet<>(proxies.keySet());


        outer:
        for (ApplicationServer server : panelServers) {
            for (ServerInfo.Network network : ServerInfo.Network.values()) {
                if (servers.get(network).containsKey(server.getName().replace("-" + network.name(), ""))) {
                    panelServersCopy.remove(server);
                    serverNameSets.get(network).remove(server.getName().replace("-" + network.name(), ""));
                    continue outer;
                }
            }
            if (server.getName().matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}-[A-Z]{4,5}$")) {
                if (proxies.containsKey(UUID.fromString(server.getName().substring(0, 37)))) {
                    panelServersCopy.remove(server);
                    proxyNames.remove(UUID.fromString(server.getName().substring(0, 37)));
                }
            }
        }

        int totalServers = 0;
        for (ServerInfo.Network network : ServerInfo.Network.values()) {
            totalServers += serverNameSets.get(network).size();
        }

        if (panelServersCopy.size() > 0 || totalServers > 0 || proxyNames.size() > 0) {
            logger.warning("Pterodactyl mismatch found, updating panel servers...");
            for (ApplicationServer server : panelServersCopy) {
                logger.info("Deleting server " + server.getName() + " from the panel.");
                panelManager.deleteServer(server);
            }

            for (ServerInfo.Network network : ServerInfo.Network.values()) {
                for (String server : serverNameSets.get(network)) {
                    logger.info("Creating server " + server + " on the panel for network " + network.name() + ".");
                    panelManager.createServer(servers.get(network).get(server), Game.valueOf(servers.get(network).get(server).getServerType().getString("game").toUpperCase(Locale.ROOT)).getMemoryAllocation());
                }
            }

            for (UUID proxy : proxyNames) {
                logger.info("Creating Proxy " + proxy.toString() + " on the panel.");
                panelManager.createProxy(proxies.get(proxy));
            }
        } else {
            logger.info("No Pterodactyl mismatch found.");
        }

        checkMissingProxies(proxyManager.getBackendServers(ServerInfo.Network.MAIN), ServerInfo.Network.MAIN);
        checkMissingProxies(proxyManager.getBackendServers(ServerInfo.Network.ALPHA), ServerInfo.Network.ALPHA);
        checkMissingProxies(proxyManager.getBackendServers(ServerInfo.Network.TEST), ServerInfo.Network.TEST);


        logger.info("Starting server/proxy messaging protocol listeners...");

        ServerCommunicationUtils.init();
        ProxyCommunicationUtils.init();

        logger.info("Server/proxy messaging protocol listeners successfully started.");
        logger.info("AuroraMC Mission Control successfully started. Handing off to the network manager...");
        NetworkManager.handoff();
    }

    private static void checkMissingProxies(JSONObject object, ServerInfo.Network network) {
        if (object != null) {
            Set<UUID> proxyNames = proxies.keySet().stream().filter(uuid -> proxies.get(uuid).getNetwork() == network).collect(Collectors.toSet());
            JSONArray array = object.getJSONArray("data");
            List<JSONObject> copy = new ArrayList<>();
            for (Object o : array) {
                JSONObject ob = (JSONObject) o;
                if (proxies.containsKey(UUID.fromString(ob.getString("name")))) {
                    proxyNames.remove(UUID.fromString(ob.getString("name")));
                } else {
                    copy.add(ob);
                }
            }

            if (copy.size() > 0 || proxyNames.size() > 0) {
                logger.warning("HaProxy mismatch found for network " + network.name() + ", updating servers...");
                for (JSONObject ob : copy) {
                    logger.info("Removing proxy " + ob.getString("name") + " in HaProxy for network " + network.name() + ".");
                    proxyManager.removeServer(ob.getString("name"), network);
                }

                for (UUID uuid : proxyNames) {
                    logger.info("Creating proxy " + uuid.toString() + " in HaProxy for network " + network.name() + ".");
                    proxyManager.addServer(proxies.get(uuid));
                }
            } else {
                logger.warning("No HaProxy mismatch found for network " + network.name() + ".");
            }
        } else {
            logger.warning("There was an issue contacting the HaProxy Data Plane API.");
        }
    }

    public static Logger getLogger() {
        return logger;
    }

    public static DatabaseManager getDbManager() {
        return dbManager;
    }

    public static JenkinsManager getJenkinsManager() {
        return jenkinsManager;
    }

    public static HaProxyManager getProxyManager() {
        return proxyManager;
    }

    public static PanelManager getPanelManager() {
        return panelManager;
    }

    public static Map<ServerInfo.Network, Map<String, ServerInfo>> getServers() {
        return servers;
    }

    public static Map<UUID, ProxyInfo> getProxies() {
        return proxies;
    }

    public static Command getCommand(String command) {
        return commands.get(command);
    }

    public static void registerCommand(Command command) {
        commands.put(command.getMainCommand(), command);
        for (String alias : command.getAliases()) {
            commands.put(alias, command);
        }
    }

    public static ConsoleReader getConsoleReader() {
        return consoleReader;
    }
}