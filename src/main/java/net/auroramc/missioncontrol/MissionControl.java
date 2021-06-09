package net.auroramc.missioncontrol;

import net.auroramc.missioncontrol.backend.DatabaseManager;
import org.apache.log4j.Logger;

import java.util.Scanner;
import java.util.prefs.Preferences;

public class MissionControl {

    private static final Logger logger = Logger.getLogger(MissionControl.class);
    private static DatabaseManager dbManager;

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
        String panelBaseURL = prefs.get("panelBaseURL", null);
        String loadBalancerBaseURL = prefs.get("loadBalancerBaseURL", null);

        if (mysqlHost == null || mysqlPort == null || mysqlDb == null || mysqlUsername == null || mysqlPassword == null || redisHost == null || redisAuth == null || ciBaseURL == null || panelBaseURL == null || loadBalancerBaseURL == null) {
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
            logger.info("Now, we need the base URL for the Pterodactyl API?\n");
            panelBaseURL = scanner.nextLine();
            logger.info("Now, we need the base URL for the HaProxy API?\n");
            loadBalancerBaseURL = scanner.nextLine();
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
            prefs.put("panelBaseURL", panelBaseURL);
            prefs.put("loadBalancerBaseURL", loadBalancerBaseURL);
        }

        dbManager = new DatabaseManager(mysqlHost, mysqlPort, mysqlDb, mysqlUsername, mysqlPassword, redisHost, redisAuth);

        logger.info("AuroraMC Mission Control successfully started.");
    }

    public static Logger getLogger() {
        return logger;
    }

    public static DatabaseManager getDbManager() {
        return dbManager;
    }
}
