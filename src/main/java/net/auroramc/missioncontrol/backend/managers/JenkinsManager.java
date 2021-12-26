/*
 * Copyright (c) 2021 AuroraMC Ltd. All Rights Reserved.
 */

package net.auroramc.missioncontrol.backend.managers;

import net.auroramc.missioncontrol.MissionControl;
import net.auroramc.missioncontrol.backend.util.Module;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.logging.Level;

public class JenkinsManager {

    private final String baseURL;
    private final String auth;

    public JenkinsManager(String baseURL, String apiKey) {
        MissionControl.getLogger().fine("Loading Jenkins manager...");
        this.baseURL = baseURL;
        this.auth = "Basic " + new String(Base64.getEncoder().encode(("missioncontrol:" + apiKey).getBytes(StandardCharsets.UTF_8)));

        try {
            URL url = new URL(baseURL + "/api/json?pretty=true");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("Accept", "application/json");


            con.setRequestProperty("Authorization", this.auth);

            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()))) {

                String line;
                StringBuilder content = new StringBuilder();

                while ((line = in.readLine()) != null) {

                    content.append(line);
                    content.append(System.lineSeparator());
                }
                String version = con.getHeaderField("X-Jenkins");
                JSONObject json = new JSONObject(content.toString());
                MissionControl.getLogger().info("Test Jenkins API request succeeded, Jenkins version " + version + " detected.");
            }
        } catch (IOException e) {
            MissionControl.getLogger().log(Level.SEVERE,"Failed to send test Jenkins API request. Stack trace:", e);
        }

        MissionControl.getLogger().fine("Jenkins successfully loaded.");
    }

    public boolean branchExists(Module module, String branch) {
        return sendGetRequest("job/" + module.getDevCIName() + "/job/" + branch + "/api/json?pretty=true", null) != null;
    }

    public boolean buildExists(Module module, String branch, int build) {
        return sendGetRequest("job/" + module.getDevCIName() + "/job/" + branch + "/" + build + "/api/json?pretty=true", null) != null;
    }

    private JSONObject sendGetRequest(String endpoint, String body) {
        try {
            URL url = new URL(baseURL + "/" + endpoint);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Content-Type", "application/json; utf-8");
            con.setRequestProperty("Authorization", auth);
            con.setRequestProperty("Accept", "application/json");

            if (body != null) {
                con.setDoOutput(true);

                try(OutputStream os = con.getOutputStream()) {
                    byte[] input = body.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
            }

            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()))) {

                if (con.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return null;
                }
                String line;
                StringBuilder content = new StringBuilder();

                while ((line = in.readLine()) != null) {

                    content.append(line);
                    content.append(System.lineSeparator());
                }
                JSONObject json = new JSONObject(content.toString());
                return json;
            }
        } catch (IOException e) {
            MissionControl.getLogger().log(Level.SEVERE,"Failed to send API request. Stack trace:", e);
            return null;
        }
    }

}
