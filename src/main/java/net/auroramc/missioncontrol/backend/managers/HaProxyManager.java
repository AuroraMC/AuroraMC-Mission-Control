package net.auroramc.missioncontrol.backend.managers;

import net.auroramc.missioncontrol.MissionControl;
import net.auroramc.missioncontrol.entities.ProxyInfo;
import net.auroramc.missioncontrol.entities.ServerInfo;
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

public class HaProxyManager {

    private final String baseURL;
    private final String auth;

    public HaProxyManager(String baseURL, String auth) {
        MissionControl.getLogger().info("Loading Load Balancer manager...");
        this.auth = "Basic " + new String(Base64.getEncoder().encode(("missioncontrol:" + auth).getBytes(StandardCharsets.UTF_8)));
        this.baseURL = baseURL;

        MissionControl.getLogger().info("Sending test API request.");
        try {
            URL url = new URL(baseURL + "/info");
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
                JSONObject json = new JSONObject(content.toString());
                MissionControl.getLogger().info("Test API request succeeded, HaProxy API version " + json.getJSONObject("api").getString("version") + " detected.");
            }
        } catch (IOException e) {
            MissionControl.getLogger().log(Level.SEVERE,"Failed to send test API request. Stack trace:", e);
        }

        MissionControl.getLogger().info("Load Balancer manager successfully loaded.");
    }


    public JSONObject getBackendServers(ServerInfo.Network network) {
        return sendGetRequest("services/haproxy/configuration/servers?backend=" + network.name().toLowerCase(), null);
    }

    public void removeServer(String name, ServerInfo.Network network) {
        sendDeleteRequest("services/haproxy/configuration/servers/" + name + "?backend=" + network.name().toLowerCase(), null);
    }

    public void addServer(ProxyInfo proxyInfo) {
        JSONObject object = new JSONObject();
        object.put("address", proxyInfo.getIp());
        object.put("port", proxyInfo.getPort());
        object.put("name", proxyInfo.getUuid().toString());
        object.put("check-send-proxy", "enabled");
        object.put("send-proxy-v2", "enabled");
        object.put("check", "enabled");
        ServerInfo.Network network = proxyInfo.getNetwork();

        MissionControl.getLogger().info("Creating proxy on HaProxy using body: " + object);
        sendPostRequest("services/haproxy/configuration/servers?backend=" + network.name().toLowerCase(), object.toString());
    }

    private JSONObject sendPostRequest(String endpoint, String body) {
        try {
            URL url = new URL(baseURL + "/" + endpoint + ((endpoint.contains("?"))?"&version=2":"?version=2"));
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
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

                String line;
                StringBuilder content = new StringBuilder();

                while ((line = in.readLine()) != null) {

                    content.append(line);
                    content.append(System.lineSeparator());
                }
                return new JSONObject(content.toString());
            }
        } catch (IOException e) {
            MissionControl.getLogger().log(Level.SEVERE,"Failed to send API request. Stack trace:", e);
            return null;
        }
    }

    private JSONObject sendDeleteRequest(String endpoint, String body) {
        try {
            URL url = new URL(baseURL + "/" + endpoint + ((endpoint.contains("?"))?"&version=2":"?version=2"));
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("DELETE");
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

                String line;
                StringBuilder content = new StringBuilder();

                while ((line = in.readLine()) != null) {

                    content.append(line);
                    content.append(System.lineSeparator());
                }
                return new JSONObject(content.toString());
            }
        } catch (IOException e) {
            MissionControl.getLogger().log(Level.SEVERE,"Failed to send API request. Stack trace:", e);
            return null;
        }
    }

    private JSONObject sendGetRequest(String endpoint, String body) {
        try {
            URL url = new URL(baseURL + "/" + endpoint + ((endpoint.contains("?"))?"&version=2":"?version=2"));
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
