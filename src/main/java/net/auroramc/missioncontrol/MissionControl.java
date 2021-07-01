package net.auroramc.missioncontrol;

import com.mattmalec.pterodactyl4j.application.entities.ApplicationServer;
import net.auroramc.core.api.backend.communication.ServerCommunicationUtils;
import net.auroramc.missioncontrol.backend.*;
import net.auroramc.missioncontrol.entities.ProxyInfo;
import net.auroramc.missioncontrol.entities.ServerInfo;
import net.auroramc.proxy.api.backend.communication.ProxyCommunicationUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;
import java.util.prefs.Preferences;

public class MissionControl {

    private static final Logger logger = Logger.getLogger(MissionControl.class);
    private static DatabaseManager dbManager;
    private static PanelManager panelManager;
    private static JenkinsManager jenkinsManager;
    private static HaProxyManager proxyManager;

    private static Map<String, ServerInfo> servers;
    private static Map<UUID, ProxyInfo> proxies;

    public static void main(String[] args) {
        Thread.currentThread().setName("Main Thread");
        logger.info("Starting AuroraMC Mission Control...");

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
        String loadBalancerBaseURL = prefs.get("loadBalancerBaseURL", null);
        String loadBalancerAuth = prefs.get("loadBalancerAuth", null);

        if (mysqlHost == null || mysqlPort == null || mysqlDb == null || mysqlUsername == null || mysqlPassword == null || redisHost == null || redisAuth == null || ciBaseURL == null || panelBaseURL == null || loadBalancerBaseURL == null || ciAPIKey == null || panelAPIKey == null || loadBalancerAuth == null) {
            Scanner scanner = new Scanner(System.in);
            logger.info("\n" +
                    "===================================================\n" +
                    "AuroraMC Mission Control First Time Setup\n" +
                    "\n" +
                    "Welcome to the AuroraMC  Mission Control first time setup.\n" +
                    "To start Mission Control properly, we need some information\n" +
                    "about the network.\n" +
                    "\n" +
                    "To start off, what is the MySQL database host?\n");
            mysqlHost = scanner.nextLine();
            logger.info("Now, we need the MySQL database port?\n");
            mysqlPort = scanner.nextLine();
            logger.info("Now, we need the MySQL database Database name?\n");
            mysqlDb = scanner.nextLine();
            logger.info("Now, we need the MySQL database username to be used by Mission Control?\n");
            mysqlUsername = scanner.nextLine();
            logger.info("Now, we need the MySQL database password to be used by Mission Control?\n");
            mysqlPassword = scanner.nextLine();
            logger.info("Now, we need the MySQL database username to be used by servers?\n");
            mysqlServerUsername = scanner.nextLine();
            logger.info("Now, we need the MySQL database password to be used by servers?\n");
            mysqlServerPassword = scanner.nextLine();
            logger.info("Now, we need the Redis database host?\n");
            redisHost = scanner.nextLine();
            logger.info("Now, we need the Redis database password?\n");
            redisAuth = scanner.nextLine();
            logger.info("Now, we need the base URL for the Jenkins API?\n");
            ciBaseURL = scanner.nextLine();
            logger.info("Now, we need the API key for the Jenkins API?\n");
            ciAPIKey = scanner.nextLine();
            logger.info("Now, we need the base URL for the Pterodactyl API?\n");
            panelBaseURL = scanner.nextLine();
            logger.info("Now, we need the API key for the Pterodactyl API?\n");
            panelAPIKey = scanner.nextLine();
            logger.info("Now, we need the base URL for the HaProxy API?\n");
            loadBalancerBaseURL = scanner.nextLine();
            logger.info("Now, we need the password for the HaProxy API?\n");
            loadBalancerAuth = scanner.nextLine();
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
            prefs.put("loadBalancerBaseURL", loadBalancerBaseURL);
            prefs.put("loadBalancerAuth", loadBalancerAuth);
        }

        dbManager = new DatabaseManager(mysqlHost, mysqlPort, mysqlDb, mysqlUsername, mysqlPassword, redisHost, redisAuth);

        proxyManager = new HaProxyManager(loadBalancerBaseURL, loadBalancerAuth);
        panelManager = new PanelManager(panelBaseURL, panelAPIKey, ciAPIKey, mysqlHost, mysqlPort, mysqlDb, mysqlServerUsername, mysqlServerPassword, redisHost, redisAuth);
        jenkinsManager = new JenkinsManager(ciBaseURL, ciAPIKey);

        logger.info("Loading current server/connection node configuration...");

        servers = dbManager.getAllServers();
        proxies = dbManager.getAllConnectionNodes();

        logger.info("Checking HaProxy and Pterodactyl configuration for mismatches...");

        List<ApplicationServer> panelServers = panelManager.getAllServers();
        List<ApplicationServer> panelServersCopy = new ArrayList<>(panelServers);

        Set<String> serverNames = new HashSet<>(servers.keySet());
        Set<UUID> proxyNames = new HashSet<>(proxies.keySet());

        for (ApplicationServer server : panelServers) {
            if (servers.containsKey(server.getName())) {
                panelServersCopy.remove(server);
                serverNames.remove(server.getName());
            } else if (server.getName().matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")) {
                if (proxies.containsKey(UUID.fromString(server.getName()))) {
                    panelServersCopy.remove(server);
                    proxyNames.remove(UUID.fromString(server.getName()));
                }
            }
        }

        if (panelServersCopy.size() > 0 || serverNames.size() > 0 || proxyNames.size() > 0) {
            logger.warn("Pterodactyl mismatch found, updating panel servers...");
            for (ApplicationServer server : panelServersCopy) {
                logger.info("Deleting server " + server.getName() + " from the panel.");
                panelManager.deleteServer(server);
            }

            for (String server : serverNames) {
                logger.info("Creating server " + server + " on the panel.");
                panelManager.createServer(servers.get(server), MemoryAllocation.valueOf(servers.get(server).getServerType().getString("type").toUpperCase(Locale.ROOT)));
            }

            for (UUID proxy : proxyNames) {
                logger.info("Creating Proxy " + proxy.toString() + " on the panel.");
                panelManager.createProxy(proxies.get(proxy));
            }
        } else {
            logger.info("No Pterodactyl mismatch found.");
        }

        JSONObject object = proxyManager.getBackendServers();
        if (object != null) {
            proxyNames = new HashSet<>(proxies.keySet());
            JSONArray array = object.getJSONArray("data");
            List<Object> copy = new ArrayList<>(array.toList());
            for (Object o : array) {
                JSONObject ob = (JSONObject) o;
                if (proxies.containsKey(UUID.fromString(ob.getString("name")))) {
                    proxyNames.remove(UUID.fromString(ob.getString("name")));
                    copy.remove(o);
                }
            }

            if (copy.size() > 0 || proxyNames.size() > 0) {
                logger.warn("HaProxy mismatch found, updating servers...");
                for (Object o : copy) {
                    JSONObject ob = (JSONObject) o;
                    logger.info("Removing proxy " + ob.getString("name") + " in HaProxy.");
                    proxyManager.removeServer(ob.getString("name"));
                }

                for (UUID uuid : proxyNames) {
                    logger.info("Creating proxy " + uuid.toString() + " for HaProxy.");
                    proxyManager.addServer(proxies.get(uuid));
                }
            } else {
                logger.warn("No HaProxy mismatch found.");
            }
        } else {
            logger.warn("There was an issue contacting the HaProxy Data Plane API.");
        }

        logger.info("Starting server/proxy messaging protocol listeners...");

        ServerCommunicationUtils.init();
        ProxyCommunicationUtils.init();

        logger.info("Server/proxy messaging protocol listeners successfully started.");
        logger.info("AuroraMC Mission Control successfully started. Handing off to the network manager...");
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

    public static Map<String, ServerInfo> getServers() {
        return servers;
    }

    public static Map<UUID, ProxyInfo> getProxies() {
        return proxies;
    }
}
