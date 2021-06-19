package net.auroramc.missioncontrol;

import net.auroramc.missioncontrol.backend.DatabaseManager;
import net.auroramc.missioncontrol.backend.HaProxyManager;
import net.auroramc.missioncontrol.backend.JenkinsManager;
import net.auroramc.missioncontrol.backend.PanelManager;
import net.auroramc.missioncontrol.entities.ProxyInfo;
import net.auroramc.missioncontrol.entities.ServerInfo;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.Scanner;
import java.util.prefs.Preferences;

public class MissionControl {

    private static final Logger logger = Logger.getLogger(MissionControl.class);
    private static DatabaseManager dbManager;
    private static PanelManager panelManager;
    private static JenkinsManager jenkinsManager;
    private static HaProxyManager proxyManager;

    private static List<ServerInfo> servers;
    private static List<ProxyInfo> proxies;

    public static void main(String[] args) {
        Thread.currentThread().setName("Main Thread");
        logger.info("Starting AuroraMC Mission Control...");

        Preferences prefs = Preferences.userNodeForPackage(MissionControl.class);
        String mysqlHost = prefs.get("mysqlHost", null);
        String mysqlPort = prefs.get("mysqlPort", null);
        String mysqlDb = prefs.get("mysqlDb", null);
        String mysqlUsername = prefs.get("mysqlUsername", null);
        String mysqlPassword = prefs.get("mysqlPassword", null);
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
            logger.info("Now, we need the MySQL database username?\n");
            mysqlUsername = scanner.nextLine();
            logger.info("Now, we need the MySQL database password?\n");
            mysqlPassword = scanner.nextLine();
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
        panelManager = new PanelManager(panelBaseURL, panelAPIKey);
        jenkinsManager = new JenkinsManager(ciBaseURL, ciAPIKey);

        logger.info("Loading current server/connection node configuration...");

        servers = dbManager.getAllServers();
        proxies = dbManager.getAllConnectionNodes();

        logger.info("Checking HaProxy and Pterodactyl configuration for mismatches...");



        logger.info("AuroraMC Mission Control successfully started.");
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
}
