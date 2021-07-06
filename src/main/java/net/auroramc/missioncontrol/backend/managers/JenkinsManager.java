package net.auroramc.missioncontrol.backend.managers;

import com.cdancy.jenkins.rest.JenkinsClient;
import com.cdancy.jenkins.rest.domain.system.SystemInfo;
import net.auroramc.missioncontrol.MissionControl;

public class JenkinsManager {

    public JenkinsManager(String baseURL, String apiKey) {
        MissionControl.getLogger().info("Loading Jenkins manager...");
        JenkinsClient client = JenkinsClient.builder()
                .endPoint(baseURL)
                .credentials("missioncontrol:" + apiKey)
                .build();

        MissionControl.getLogger().info("Sending test API request...");
        try {
            SystemInfo systemInfo = client.api().systemApi().systemInfo();
            String s = systemInfo.jenkinsVersion();
            MissionControl.getLogger().info("Test API request succeeded, Jenkins version " + s + " detected.");
        } catch (Exception e) {
            MissionControl.getLogger().error("Test API request failed. Stack trace:", e);
        }

        MissionControl.getLogger().info("Jenkins successfully loaded.");
    }

}
