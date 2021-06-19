package net.auroramc.missioncontrol.backend;

import com.cdancy.jenkins.rest.JenkinsClient;
import com.cdancy.jenkins.rest.domain.system.SystemInfo;
import com.mattmalec.pterodactyl4j.PteroBuilder;
import com.mattmalec.pterodactyl4j.application.entities.ApplicationServer;
import com.mattmalec.pterodactyl4j.application.entities.PteroApplication;
import net.auroramc.missioncontrol.MissionControl;

import java.util.List;

public class PanelManager {

    private final PteroApplication api;

    public PanelManager(String baseURL, String apiKey) {
        MissionControl.getLogger().info("Loading panel manager...");
        api = PteroBuilder.createApplication(baseURL, apiKey);

        MissionControl.getLogger().info("Sending test API request...");
        try {
            api.retrieveServers();
            MissionControl.getLogger().info("Test API request succeeded.");
        } catch (Exception e) {
            MissionControl.getLogger().error("Test API request failed. Stack trace:", e);
        }

        MissionControl.getLogger().info("Panel successfully loaded.");
    }

    public List<ApplicationServer> getAllServers() {
        return api.retrieveServers().execute();
    }

}
