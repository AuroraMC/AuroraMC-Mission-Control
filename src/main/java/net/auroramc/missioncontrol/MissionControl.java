package net.auroramc.missioncontrol;

import org.apache.log4j.Logger;

public class MissionControl {

    private static final Logger logger = Logger.getLogger(MissionControl.class);

    public static void main(String[] args) {
        Thread.currentThread().setName("Main Thread");
        logger.info("Starting AuroraMC Mission Control...");
    }

    public static Logger getLogger() {
        return logger;
    }
}
