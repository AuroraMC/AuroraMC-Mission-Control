/*
 * Copyright (c) 2021 AuroraMC Ltd. All Rights Reserved.
 */

package net.auroramc.missioncontrol.backend.managers;

import com.mattmalec.pterodactyl4j.DataType;
import com.mattmalec.pterodactyl4j.EnvironmentValue;
import com.mattmalec.pterodactyl4j.PteroBuilder;
import com.mattmalec.pterodactyl4j.application.entities.ApplicationAllocation;
import com.mattmalec.pterodactyl4j.application.entities.ApplicationServer;
import com.mattmalec.pterodactyl4j.application.entities.Node;
import com.mattmalec.pterodactyl4j.application.entities.PteroApplication;
import com.mattmalec.pterodactyl4j.client.entities.PteroClient;
import net.auroramc.missioncontrol.MissionControl;
import net.auroramc.missioncontrol.NetworkManager;
import net.auroramc.missioncontrol.backend.util.MemoryAllocation;
import net.auroramc.missioncontrol.backend.util.Module;
import net.auroramc.missioncontrol.entities.ProxyInfo;
import net.auroramc.missioncontrol.entities.ServerInfo;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class PanelManager {

    private final PteroApplication api;
    private final PteroClient apiClient;
    private final String jenkinsApiKey;


    private final String mysqlHost, mysqlPort, mysqlUsername, mysqlPassword, mysqlDb, redisHost, redisAuth;

    public PanelManager(String baseURL, String apiKey, String apiUserKey, String jenkinsApiKey, String mysqlHost, String mysqlPort, String mysqlDb, String mysqlUsername, String mysqlPassword, String redisHost, String redisAuth) {
        MissionControl.getLogger().fine("Loading panel manager...");
        api = PteroBuilder.createApplication(baseURL, apiKey);
        apiClient = PteroBuilder.createClient(baseURL, apiUserKey);
        this.jenkinsApiKey = jenkinsApiKey;

        this.mysqlHost = mysqlHost;
        this.mysqlPort = mysqlPort;
        this.mysqlDb = mysqlDb;
        this.mysqlUsername = mysqlUsername;
        this.mysqlPassword = mysqlPassword;
        this.redisHost = redisHost;
        this.redisAuth = redisAuth;

        MissionControl.getLogger().fine("Sending test API request...");
        try {
            api.retrieveServers().execute();
            MissionControl.getLogger().fine("Test API request succeeded.");
        } catch (Exception e) {
            MissionControl.getLogger().log(Level.SEVERE,"Test Pterodactyl API request failed. Stack trace:", e);
        }

        MissionControl.getLogger().fine("Panel successfully loaded.");
    }

    public List<ApplicationServer> getAllServers() {
        return api.retrieveServers().stream().collect(Collectors.toList());
    }

    public void deleteServer(String name, ServerInfo.Network network) {
        api.retrieveServersByName(name + "-" + network.name(), false).execute().get(0).getController().delete(true).execute();
    }

    public List<ApplicationAllocation> getAvailableAllocations() {
        return api.retrieveAllocations().execute().stream().filter(allocation -> !allocation.getServer().isPresent()).collect(Collectors.toList());
    }

    public List<Node> getAllNodes() {
        return api.retrieveNodes().execute();
    }

    public void deleteServer(ApplicationServer server) {
        server.getController().delete(true);
    }

    public void createServer(ServerInfo serverInfo, MemoryAllocation assignedMemory) {

        Map<String, EnvironmentValue<?>> environment = new HashMap<>();
        environment.put("CORE_VERSION", EnvironmentValue.ofString(serverInfo.getBuildNumber() + ""));
        if (serverInfo.getNetwork() != ServerInfo.Network.MAIN) {
            environment.put("CORE_BRANCH", EnvironmentValue.ofString(NetworkManager.getAlphaBranches().get(Module.CORE) + ""));
        }
        if (serverInfo.getLobbyBuildNumber() != -1) {
            environment.put("LOBBY_VERSION", EnvironmentValue.ofString(serverInfo.getLobbyBuildNumber() + ""));
            if (serverInfo.getNetwork() != ServerInfo.Network.MAIN) {
                environment.put("LOBBY_BRANCH", EnvironmentValue.ofString(NetworkManager.getAlphaBranches().get(Module.LOBBY) + ""));
            }
        } else {
            environment.put("LOBBY_VERSION", EnvironmentValue.ofString("-1"));
            if (serverInfo.getNetwork() != ServerInfo.Network.MAIN) {
                environment.put("LOBBY_BRANCH", EnvironmentValue.ofString(NetworkManager.getAlphaBranches().get(Module.LOBBY) + ""));
            }
        }
        if (serverInfo.getBuildBuildNumber() != -1) {
            environment.put("BUILD_VERSION", EnvironmentValue.ofString(serverInfo.getBuildBuildNumber() + ""));
            if (serverInfo.getNetwork() != ServerInfo.Network.MAIN) {
                environment.put("BUILD_BRANCH", EnvironmentValue.ofString(NetworkManager.getAlphaBranches().get(Module.BUILD) + ""));
            }
        } else {
            environment.put("BUILD_VERSION", EnvironmentValue.ofString("-1"));
            if (serverInfo.getNetwork() != ServerInfo.Network.MAIN) {
                environment.put("BUILD_BRANCH", EnvironmentValue.ofString(NetworkManager.getAlphaBranches().get(Module.LOBBY) + ""));
            }
        }
        if (serverInfo.getGameBuildNumber() != -1) {
            environment.put("GAME_VERSION", EnvironmentValue.ofString(serverInfo.getGameBuildNumber() + ""));
            if (serverInfo.getNetwork() != ServerInfo.Network.MAIN) {
                environment.put("GAME_BRANCH", EnvironmentValue.ofString(NetworkManager.getAlphaBranches().get(Module.GAME) + ""));
            }
        } else {
            environment.put("GAME_VERSION", EnvironmentValue.ofString("-1"));
            if (serverInfo.getNetwork() != ServerInfo.Network.MAIN) {
                environment.put("GAME_BRANCH", EnvironmentValue.ofString(NetworkManager.getAlphaBranches().get(Module.LOBBY) + ""));
            }
        }
        if (serverInfo.getEngineBuildNumber() != -1) {
            environment.put("ENGINE_VERSION", EnvironmentValue.ofString(serverInfo.getEngineBuildNumber() + ""));
            if (serverInfo.getNetwork() != ServerInfo.Network.MAIN) {
                environment.put("ENGINE_BRANCH", EnvironmentValue.ofString(NetworkManager.getAlphaBranches().get(Module.ENGINE) + ""));
            }
        } else {
            environment.put("ENGINE_VERSION", EnvironmentValue.ofString("-1"));
            if (serverInfo.getNetwork() != ServerInfo.Network.MAIN) {
                environment.put("ENGINE_BRANCH", EnvironmentValue.ofString(NetworkManager.getAlphaBranches().get(Module.LOBBY) + ""));
            }
        }
        if (serverInfo.getEngineBuildNumber() != -1) {
            environment.put("DUELS_VERSION", EnvironmentValue.ofString(serverInfo.getDuelsBuildNumber() + ""));
            if (serverInfo.getNetwork() != ServerInfo.Network.MAIN) {
                environment.put("DUELS_BRANCH", EnvironmentValue.ofString(NetworkManager.getAlphaBranches().get(Module.DUELS) + ""));
            }
        } else {
            environment.put("DUELS_VERSION", EnvironmentValue.ofString("-1"));
            if (serverInfo.getNetwork() != ServerInfo.Network.MAIN) {
                environment.put("DUELS_BRANCH", EnvironmentValue.ofString(NetworkManager.getAlphaBranches().get(Module.DUELS) + ""));
            }
        }
        environment.put("JENKINS_KEY", EnvironmentValue.ofString(jenkinsApiKey));
        environment.put("SERVER_NAME", EnvironmentValue.ofString(serverInfo.getName()));

        //Adding in database details.
        environment.put("MYSQL_HOST", EnvironmentValue.ofString(mysqlHost));
        environment.put("MYSQL_PORT", EnvironmentValue.ofString(mysqlPort));
        environment.put("MYSQL_DB", EnvironmentValue.ofString(mysqlDb));
        environment.put("MYSQL_USERNAME", EnvironmentValue.ofString(mysqlUsername));
        environment.put("MYSQL_PASSWORD", EnvironmentValue.ofString(mysqlPassword));
        environment.put("REDIS_HOST", EnvironmentValue.ofString(redisHost));
        environment.put("REDIS_AUTH", EnvironmentValue.ofString(redisAuth));
        environment.put("NETWORK", EnvironmentValue.ofString(serverInfo.getNetwork().name()));

        ApplicationServer server = api.createServer()
                .setName(serverInfo.getName() + "-" + serverInfo.getNetwork().name())
                .setDescription("Server")
                .setOwner(api.retrieveUserById(6).execute())
                .setEgg(api.retrieveEggById(api.retrieveNestById(1).execute(), ((serverInfo.getNetwork() != ServerInfo.Network.MAIN)?19:16)).execute())
                .setAllocations(api.retrieveAllocations().execute().stream().filter(allocation -> allocation.getPort().equals(serverInfo.getPort() + "") && allocation.getIP().equalsIgnoreCase(serverInfo.getIp())).collect(Collectors.toList()).get(0), api.retrieveAllocations().execute().stream().filter(allocation -> allocation.getPort().equals(serverInfo.getProtocolPort() + "") && allocation.getIP().equalsIgnoreCase(serverInfo.getIp())).collect(Collectors.toList()).get(0),api.retrieveAllocations().execute().stream().filter(allocation -> allocation.getPort().equals(serverInfo.getPort() + "") && allocation.getIP().equalsIgnoreCase("127.0.0.1")).collect(Collectors.toList()).get(0), api.retrieveAllocations().execute().stream().filter(allocation -> allocation.getPort().equals(serverInfo.getProtocolPort() + "") && allocation.getIP().equalsIgnoreCase("127.0.0.1")).collect(Collectors.toList()).get(0))
                .setDatabases(0)
                .setCPU(0)
                .setDisk(5, DataType.GB)
                .setMemory(assignedMemory.getMegaBytes(), DataType.MB)
                .setDockerImage("quay.io/pterodactyl/core:java")
                .startOnCompletion(true)
                .skipScripts(false)
                .setEnvironment(environment).execute();
    }

    public void createPathfinderServer(ServerInfo serverInfo, MemoryAllocation assignedMemory) {

        Map<String, EnvironmentValue<?>> environment = new HashMap<>();
        environment.put("CORE_VERSION", EnvironmentValue.ofString(serverInfo.getBuildNumber() + ""));
        if (serverInfo.getNetwork() != ServerInfo.Network.MAIN) {
            environment.put("CORE_BRANCH", EnvironmentValue.ofString(NetworkManager.getAlphaBranches().get(Module.CORE) + ""));
        }
        environment.put("PATHFINDER_VERSION", EnvironmentValue.ofString(serverInfo.getBuildNumber() + ""));
        environment.put("PATHFINDER_TYPE", EnvironmentValue.ofString(serverInfo.getServerType().getString("pathfinder_type") + ""));
        if (serverInfo.getNetwork() != ServerInfo.Network.MAIN) {
            environment.put("PATHFINDER_BRANCH", EnvironmentValue.ofString(NetworkManager.getAlphaBranches().get(Module.CORE) + ""));
        }

        environment.put("JENKINS_KEY", EnvironmentValue.ofString(jenkinsApiKey));
        environment.put("SERVER_NAME", EnvironmentValue.ofString(serverInfo.getName()));

        //Adding in database details.
        environment.put("MYSQL_HOST", EnvironmentValue.ofString(mysqlHost));
        environment.put("MYSQL_PORT", EnvironmentValue.ofString(mysqlPort));
        environment.put("MYSQL_DB", EnvironmentValue.ofString(mysqlDb));
        environment.put("MYSQL_USERNAME", EnvironmentValue.ofString(mysqlUsername));
        environment.put("MYSQL_PASSWORD", EnvironmentValue.ofString(mysqlPassword));
        environment.put("REDIS_HOST", EnvironmentValue.ofString(redisHost));
        environment.put("REDIS_AUTH", EnvironmentValue.ofString(redisAuth));
        environment.put("NETWORK", EnvironmentValue.ofString(serverInfo.getNetwork().name()));

        ApplicationServer server = api.createServer()
                .setName(serverInfo.getName() + "-" + serverInfo.getNetwork().name())
                .setDescription("Server")
                .setOwner(api.retrieveUserById(6).execute())
                .setEgg(api.retrieveEggById(api.retrieveNestById(1).execute(), ((serverInfo.getNetwork() != ServerInfo.Network.MAIN)?35:34)).execute())
                .setAllocations(api.retrieveAllocations().execute().stream().filter(allocation -> allocation.getPort().equals(serverInfo.getPort() + "") && allocation.getIP().equalsIgnoreCase(serverInfo.getIp())).collect(Collectors.toList()).get(0), api.retrieveAllocations().execute().stream().filter(allocation -> allocation.getPort().equals(serverInfo.getProtocolPort() + "") && allocation.getIP().equalsIgnoreCase(serverInfo.getIp())).collect(Collectors.toList()).get(0),api.retrieveAllocations().execute().stream().filter(allocation -> allocation.getPort().equals(serverInfo.getPort() + "") && allocation.getIP().equalsIgnoreCase("127.0.0.1")).collect(Collectors.toList()).get(0), api.retrieveAllocations().execute().stream().filter(allocation -> allocation.getPort().equals(serverInfo.getProtocolPort() + "") && allocation.getIP().equalsIgnoreCase("127.0.0.1")).collect(Collectors.toList()).get(0))
                .setDatabases(0)
                .setCPU(0)
                .setDisk(5, DataType.GB)
                .setMemory(assignedMemory.getMegaBytes(), DataType.MB)
                .setDockerImage("quay.io/pterodactyl/core:java")
                .startOnCompletion(true)
                .skipScripts(false)
                .setEnvironment(environment).execute();
    }

    public void createServer(ServerInfo serverInfo, MemoryAllocation assignedMemory, ApplicationAllocation allocation, ApplicationAllocation protocolAllocation, ApplicationAllocation altAllocation, ApplicationAllocation altProtocolAllocation) {

        Map<String, EnvironmentValue<?>> environment = new HashMap<>();
        environment.put("CORE_VERSION", EnvironmentValue.ofString(serverInfo.getBuildNumber() + ""));
        if (serverInfo.getNetwork() != ServerInfo.Network.MAIN) {
            environment.put("CORE_BRANCH", EnvironmentValue.ofString(NetworkManager.getAlphaBranches().get(Module.CORE) + ""));
        }
        if (serverInfo.getLobbyBuildNumber() != -1) {
            environment.put("LOBBY_VERSION", EnvironmentValue.ofString(serverInfo.getLobbyBuildNumber() + ""));
            if (serverInfo.getNetwork() != ServerInfo.Network.MAIN) {
                environment.put("LOBBY_BRANCH", EnvironmentValue.ofString(NetworkManager.getAlphaBranches().get(Module.LOBBY) + ""));
            }
        } else {
            environment.put("LOBBY_VERSION", EnvironmentValue.ofString("-1"));
            if (serverInfo.getNetwork() != ServerInfo.Network.MAIN) {
                environment.put("LOBBY_BRANCH", EnvironmentValue.ofString(NetworkManager.getAlphaBranches().get(Module.LOBBY) + ""));
            }
        }
        if (serverInfo.getBuildBuildNumber() != -1) {
            environment.put("BUILD_VERSION", EnvironmentValue.ofString(serverInfo.getBuildBuildNumber() + ""));
            if (serverInfo.getNetwork() != ServerInfo.Network.MAIN) {
                environment.put("BUILD_BRANCH", EnvironmentValue.ofString(NetworkManager.getAlphaBranches().get(Module.BUILD) + ""));
            }
        } else {
            environment.put("BUILD_VERSION", EnvironmentValue.ofString("-1"));
            if (serverInfo.getNetwork() != ServerInfo.Network.MAIN) {
                environment.put("BUILD_BRANCH", EnvironmentValue.ofString(NetworkManager.getAlphaBranches().get(Module.BUILD) + ""));
            }
        }
        if (serverInfo.getGameBuildNumber() != -1) {
            environment.put("GAME_VERSION", EnvironmentValue.ofString(serverInfo.getGameBuildNumber() + ""));
            if (serverInfo.getNetwork() != ServerInfo.Network.MAIN) {
                environment.put("GAME_BRANCH", EnvironmentValue.ofString(NetworkManager.getAlphaBranches().get(Module.GAME) + ""));
            }
        } else {
            environment.put("GAME_VERSION", EnvironmentValue.ofString("-1"));
            if (serverInfo.getNetwork() != ServerInfo.Network.MAIN) {
                environment.put("GAME_BRANCH", EnvironmentValue.ofString(NetworkManager.getAlphaBranches().get(Module.GAME) + ""));
            }
        }
        if (serverInfo.getEngineBuildNumber() != -1) {
            environment.put("ENGINE_VERSION", EnvironmentValue.ofString(serverInfo.getEngineBuildNumber() + ""));
            if (serverInfo.getNetwork() != ServerInfo.Network.MAIN) {
                environment.put("ENGINE_BRANCH", EnvironmentValue.ofString(NetworkManager.getAlphaBranches().get(Module.ENGINE) + ""));
            }
        } else {
            environment.put("ENGINE_VERSION", EnvironmentValue.ofString("-1"));
            if (serverInfo.getNetwork() != ServerInfo.Network.MAIN) {
                environment.put("ENGINE_BRANCH", EnvironmentValue.ofString(NetworkManager.getAlphaBranches().get(Module.ENGINE) + ""));
            }
        }
        if (serverInfo.getEngineBuildNumber() != -1) {
            environment.put("DUELS_VERSION", EnvironmentValue.ofString(serverInfo.getDuelsBuildNumber() + ""));
            if (serverInfo.getNetwork() != ServerInfo.Network.MAIN) {
                environment.put("DUELS_BRANCH", EnvironmentValue.ofString(NetworkManager.getAlphaBranches().get(Module.DUELS) + ""));
            }
        } else {
            environment.put("DUELS_VERSION", EnvironmentValue.ofString("-1"));
            if (serverInfo.getNetwork() != ServerInfo.Network.MAIN) {
                environment.put("DUELS_BRANCH", EnvironmentValue.ofString(NetworkManager.getAlphaBranches().get(Module.DUELS) + ""));
            }
        }
        environment.put("JENKINS_KEY", EnvironmentValue.ofString(jenkinsApiKey));
        environment.put("SERVER_NAME", EnvironmentValue.ofString(serverInfo.getName()));

        //Adding in database details.
        environment.put("MYSQL_HOST", EnvironmentValue.ofString(mysqlHost));
        environment.put("MYSQL_PORT", EnvironmentValue.ofString(mysqlPort));
        environment.put("MYSQL_DB", EnvironmentValue.ofString(mysqlDb));
        environment.put("MYSQL_USERNAME", EnvironmentValue.ofString(mysqlUsername));
        environment.put("MYSQL_PASSWORD", EnvironmentValue.ofString(mysqlPassword));
        environment.put("REDIS_HOST", EnvironmentValue.ofString(redisHost));
        environment.put("REDIS_AUTH", EnvironmentValue.ofString(redisAuth));
        environment.put("NETWORK", EnvironmentValue.ofString(serverInfo.getNetwork().name()));

        ApplicationServer server = api.createServer()
                .setName(serverInfo.getName() + "-" + serverInfo.getNetwork().name())
                .setDescription("Server")
                .setOwner(api.retrieveUserById(6).execute())
                .setEgg(api.retrieveEggById(api.retrieveNestById(1).execute(), ((serverInfo.getNetwork() != ServerInfo.Network.MAIN)?19:16)).execute())
                .setAllocations(allocation, protocolAllocation, altAllocation, altProtocolAllocation)
                .setDatabases(0)
                .setCPU(0)
                .setDisk(5, DataType.GB)
                .setMemory(assignedMemory.getMegaBytes(), DataType.MB)
                .setDockerImage("quay.io/pterodactyl/core:java")
                .startOnCompletion(true)
                .skipScripts(false)
                .setEnvironment(environment).execute();

    }

    public void createPathfinderServer(ServerInfo serverInfo, MemoryAllocation assignedMemory, ApplicationAllocation allocation, ApplicationAllocation protocolAllocation, ApplicationAllocation altAllocation, ApplicationAllocation altProtocolAllocation) {

        Map<String, EnvironmentValue<?>> environment = new HashMap<>();
        environment.put("CORE_VERSION", EnvironmentValue.ofString(serverInfo.getBuildNumber() + ""));
        if (serverInfo.getNetwork() != ServerInfo.Network.MAIN) {
            environment.put("CORE_BRANCH", EnvironmentValue.ofString(NetworkManager.getAlphaBranches().get(Module.CORE) + ""));
        }

        environment.put("PATHFINDER_VERSION", EnvironmentValue.ofString(serverInfo.getBuildNumber() + ""));
        environment.put("PATHFINDER_TYPE", EnvironmentValue.ofString(serverInfo.getServerType().getString("pathfinder_type") + ""));
        if (serverInfo.getNetwork() != ServerInfo.Network.MAIN) {
            environment.put("PATHFINDER_BRANCH", EnvironmentValue.ofString(NetworkManager.getAlphaBranches().get(Module.CORE) + ""));
        }
        environment.put("JENKINS_KEY", EnvironmentValue.ofString(jenkinsApiKey));
        environment.put("SERVER_NAME", EnvironmentValue.ofString(serverInfo.getName()));

        //Adding in database details.
        environment.put("MYSQL_HOST", EnvironmentValue.ofString(mysqlHost));
        environment.put("MYSQL_PORT", EnvironmentValue.ofString(mysqlPort));
        environment.put("MYSQL_DB", EnvironmentValue.ofString(mysqlDb));
        environment.put("MYSQL_USERNAME", EnvironmentValue.ofString(mysqlUsername));
        environment.put("MYSQL_PASSWORD", EnvironmentValue.ofString(mysqlPassword));
        environment.put("REDIS_HOST", EnvironmentValue.ofString(redisHost));
        environment.put("REDIS_AUTH", EnvironmentValue.ofString(redisAuth));
        environment.put("PATHFINDER_KEY", EnvironmentValue.ofString(MissionControl.getAuthKey()));
        environment.put("NETWORK", EnvironmentValue.ofString(serverInfo.getNetwork().name()));

        ApplicationServer server = api.createServer()
                .setName(serverInfo.getName() + "-" + serverInfo.getNetwork().name())
                .setDescription("Server")
                .setOwner(api.retrieveUserById(6).execute())
                .setEgg(api.retrieveEggById(api.retrieveNestById(1).execute(), ((serverInfo.getNetwork() != ServerInfo.Network.MAIN)?35:34)).execute())
                .setAllocations(allocation, protocolAllocation, altAllocation, altProtocolAllocation)
                .setDatabases(0)
                .setCPU(0)
                .setDisk(5, DataType.GB)
                .setMemory(assignedMemory.getMegaBytes(), DataType.MB)
                .setDockerImage("quay.io/pterodactyl/core:java")
                .startOnCompletion(true)
                .skipScripts(false)
                .setEnvironment(environment).execute();

    }

    public void createServer(ServerInfo serverInfo, MemoryAllocation assignedMemory, ApplicationAllocation allocation, ApplicationAllocation protocolAllocation, ApplicationAllocation altAllocation, ApplicationAllocation altProtocolAllocation, String coreBranch, String lobbybranch, String buildBranch, String gameBranch, String engineBranch, String duelsBranch) {

        Map<String, EnvironmentValue<?>> environment = new HashMap<>();
        environment.put("CORE_VERSION", EnvironmentValue.ofString(serverInfo.getBuildNumber() + ""));
        if (serverInfo.getNetwork() != ServerInfo.Network.MAIN) {
            environment.put("CORE_BRANCH", EnvironmentValue.ofString(coreBranch));
        }
        if (serverInfo.getLobbyBuildNumber() != -1) {
            environment.put("LOBBY_VERSION", EnvironmentValue.ofString(serverInfo.getLobbyBuildNumber() + ""));
            if (serverInfo.getNetwork() != ServerInfo.Network.MAIN) {
                environment.put("LOBBY_BRANCH", EnvironmentValue.ofString(lobbybranch));
            }
        } else {
            environment.put("LOBBY_VERSION", EnvironmentValue.ofString("-1"));
            if (serverInfo.getNetwork() != ServerInfo.Network.MAIN) {
                environment.put("LOBBY_BRANCH", EnvironmentValue.ofString("na"));
            }
        }
        if (serverInfo.getBuildBuildNumber() != -1) {
            environment.put("BUILD_VERSION", EnvironmentValue.ofString(serverInfo.getBuildBuildNumber() + ""));
            if (serverInfo.getNetwork() != ServerInfo.Network.MAIN) {
                environment.put("BUILD_BRANCH", EnvironmentValue.ofString(buildBranch));
            }
        } else {
            environment.put("BUILD_VERSION", EnvironmentValue.ofString("-1"));
            if (serverInfo.getNetwork() != ServerInfo.Network.MAIN) {
                environment.put("BUILD_BRANCH", EnvironmentValue.ofString("na"));
            }
        }
        if (serverInfo.getGameBuildNumber() != -1) {
            environment.put("GAME_VERSION", EnvironmentValue.ofString(serverInfo.getGameBuildNumber() + ""));
            if (serverInfo.getNetwork() != ServerInfo.Network.MAIN) {
                environment.put("GAME_BRANCH", EnvironmentValue.ofString(gameBranch));
            }
        } else {
            environment.put("GAME_VERSION", EnvironmentValue.ofString("-1"));
            if (serverInfo.getNetwork() != ServerInfo.Network.MAIN) {
                environment.put("GAME_BRANCH", EnvironmentValue.ofString("na"));
            }
        }
        if (serverInfo.getEngineBuildNumber() != -1) {
            environment.put("ENGINE_VERSION", EnvironmentValue.ofString(serverInfo.getEngineBuildNumber() + ""));
            if (serverInfo.getNetwork() != ServerInfo.Network.MAIN) {
                environment.put("ENGINE_BRANCH", EnvironmentValue.ofString(engineBranch));
            }
        } else {
            environment.put("ENGINE_VERSION", EnvironmentValue.ofString("-1"));
            if (serverInfo.getNetwork() != ServerInfo.Network.MAIN) {
                environment.put("ENGINE_BRANCH", EnvironmentValue.ofString("na"));
            }
        }
        if (serverInfo.getDuelsBuildNumber() != -1) {
            environment.put("DUELS_VERSION", EnvironmentValue.ofString(serverInfo.getDuelsBuildNumber() + ""));
            if (serverInfo.getNetwork() != ServerInfo.Network.MAIN) {
                environment.put("DUELS_BRANCH", EnvironmentValue.ofString(duelsBranch));
            }
        } else {
            environment.put("DUELS_VERSION", EnvironmentValue.ofString("-1"));
            if (serverInfo.getNetwork() != ServerInfo.Network.MAIN) {
                environment.put("DUELS_BRANCH", EnvironmentValue.ofString("na"));
            }
        }
        environment.put("JENKINS_KEY", EnvironmentValue.ofString(jenkinsApiKey));
        environment.put("SERVER_NAME", EnvironmentValue.ofString(serverInfo.getName()));

        //Adding in database details.
        environment.put("MYSQL_HOST", EnvironmentValue.ofString(mysqlHost));
        environment.put("MYSQL_PORT", EnvironmentValue.ofString(mysqlPort));
        environment.put("MYSQL_DB", EnvironmentValue.ofString(mysqlDb));
        environment.put("MYSQL_USERNAME", EnvironmentValue.ofString(mysqlUsername));
        environment.put("MYSQL_PASSWORD", EnvironmentValue.ofString(mysqlPassword));
        environment.put("REDIS_HOST", EnvironmentValue.ofString(redisHost));
        environment.put("REDIS_AUTH", EnvironmentValue.ofString(redisAuth));
        environment.put("NETWORK", EnvironmentValue.ofString(serverInfo.getNetwork().name()));

        ApplicationServer server = api.createServer()
                .setName(serverInfo.getName() + "-" + serverInfo.getNetwork().name())
                .setDescription("Server")
                .setOwner(api.retrieveUserById(6).execute())
                .setEgg(api.retrieveEggById(api.retrieveNestById(1).execute(), ((serverInfo.getNetwork() != ServerInfo.Network.MAIN)?19:16)).execute())
                .setAllocations(allocation, protocolAllocation, altAllocation, altProtocolAllocation)
                .setDatabases(0)
                .setCPU(0)
                .setDisk(5, DataType.GB)
                .setMemory(assignedMemory.getMegaBytes(), DataType.MB)
                .setDockerImage("quay.io/pterodactyl/core:java")
                .startOnCompletion(true)
                .skipScripts(false)
                .setEnvironment(environment).execute();
    }

    public void createPathfinderServer(ServerInfo serverInfo, MemoryAllocation assignedMemory, ApplicationAllocation allocation, ApplicationAllocation protocolAllocation, ApplicationAllocation altAllocation, ApplicationAllocation altProtocolAllocation, String coreBranch, String pathfinderBranch) {

        Map<String, EnvironmentValue<?>> environment = new HashMap<>();
        environment.put("CORE_VERSION", EnvironmentValue.ofString(serverInfo.getBuildNumber() + ""));
        if (serverInfo.getNetwork() != ServerInfo.Network.MAIN) {
            environment.put("CORE_BRANCH", EnvironmentValue.ofString(coreBranch));
        }

        environment.put("PATHFINDER_VERSION", EnvironmentValue.ofString(serverInfo.getBuildNumber() + ""));
        environment.put("PATHFINDER_TYPE", EnvironmentValue.ofString(serverInfo.getServerType().getString("pathfinder_type") + ""));
        if (serverInfo.getNetwork() != ServerInfo.Network.MAIN) {
            environment.put("PATHFINDER_BRANCH", EnvironmentValue.ofString(pathfinderBranch));
        }

        environment.put("JENKINS_KEY", EnvironmentValue.ofString(jenkinsApiKey));
        environment.put("SERVER_NAME", EnvironmentValue.ofString(serverInfo.getName()));

        //Adding in database details.
        environment.put("MYSQL_HOST", EnvironmentValue.ofString(mysqlHost));
        environment.put("MYSQL_PORT", EnvironmentValue.ofString(mysqlPort));
        environment.put("MYSQL_DB", EnvironmentValue.ofString(mysqlDb));
        environment.put("MYSQL_USERNAME", EnvironmentValue.ofString(mysqlUsername));
        environment.put("MYSQL_PASSWORD", EnvironmentValue.ofString(mysqlPassword));
        environment.put("REDIS_HOST", EnvironmentValue.ofString(redisHost));
        environment.put("REDIS_AUTH", EnvironmentValue.ofString(redisAuth));
        environment.put("NETWORK", EnvironmentValue.ofString(serverInfo.getNetwork().name()));
        environment.put("PATHFINDER_KEY", EnvironmentValue.ofString(MissionControl.getAuthKey()));

        ApplicationServer server = api.createServer()
                .setName(serverInfo.getName() + "-" + serverInfo.getNetwork().name())
                .setDescription("Server")
                .setOwner(api.retrieveUserById(6).execute())
                .setEgg(api.retrieveEggById(api.retrieveNestById(1).execute(), ((serverInfo.getNetwork() != ServerInfo.Network.MAIN)?35:34)).execute())
                .setAllocations(allocation, protocolAllocation, altAllocation, altProtocolAllocation)
                .setDatabases(0)
                .setCPU(0)
                .setDisk(5, DataType.GB)
                .setMemory(assignedMemory.getMegaBytes(), DataType.MB)
                .setDockerImage("quay.io/pterodactyl/core:java")
                .startOnCompletion(true)
                .skipScripts(false)
                .setEnvironment(environment).execute();
    }

    public void updateServer(ServerInfo info) {
        Map<String, EnvironmentValue<?>> environment = new HashMap<>();
        environment.put("CORE_VERSION", EnvironmentValue.ofString(info.getBuildNumber() + ""));
        if (info.getNetwork() != ServerInfo.Network.MAIN) {
            environment.put("CORE_BRANCH", EnvironmentValue.ofString(NetworkManager.getAlphaBranches().get(Module.CORE) + ""));
        }
        if (info.getLobbyBuildNumber() != -1) {
            environment.put("LOBBY_VERSION", EnvironmentValue.ofString(info.getLobbyBuildNumber() + ""));
            if (info.getNetwork() != ServerInfo.Network.MAIN) {
                environment.put("LOBBY_BRANCH", EnvironmentValue.ofString(NetworkManager.getAlphaBranches().get(Module.LOBBY) + ""));
            }
        }
        if (info.getBuildBuildNumber() != -1) {
            environment.put("BUILD_VERSION", EnvironmentValue.ofString(info.getBuildBuildNumber() + ""));
            if (info.getNetwork() != ServerInfo.Network.MAIN) {
                environment.put("BUILD_BRANCH", EnvironmentValue.ofString(NetworkManager.getAlphaBranches().get(Module.BUILD) + ""));
            }
        }
        if (info.getGameBuildNumber() != -1) {
            environment.put("GAME_VERSION", EnvironmentValue.ofString(info.getGameBuildNumber() + ""));
            if (info.getNetwork() != ServerInfo.Network.MAIN) {
                environment.put("GAME_BRANCH", EnvironmentValue.ofString(NetworkManager.getAlphaBranches().get(Module.GAME) + ""));
            }
        }
        if (info.getEngineBuildNumber() != -1) {
            environment.put("ENGINE_VERSION", EnvironmentValue.ofString(info.getEngineBuildNumber() + ""));
            if (info.getNetwork() != ServerInfo.Network.MAIN) {
                environment.put("ENGINE_BRANCH", EnvironmentValue.ofString(NetworkManager.getAlphaBranches().get(Module.ENGINE) + ""));
            }
        }
        if (info.getDuelsBuildNumber() != -1) {
            environment.put("DUELS_VERSION", EnvironmentValue.ofString(info.getDuelsBuildNumber() + ""));
            if (info.getNetwork() != ServerInfo.Network.MAIN) {
                environment.put("DUELS_BRANCH", EnvironmentValue.ofString(NetworkManager.getAlphaBranches().get(Module.DUELS) + ""));
            }
        }
        environment.put("JENKINS_KEY", EnvironmentValue.ofString(jenkinsApiKey));
        environment.put("SERVER_NAME", EnvironmentValue.ofString(info.getName()));

        //Adding in database details.
        environment.put("MYSQL_HOST", EnvironmentValue.ofString(mysqlHost));
        environment.put("MYSQL_PORT", EnvironmentValue.ofString(mysqlPort));
        environment.put("MYSQL_DB", EnvironmentValue.ofString(mysqlDb));
        environment.put("MYSQL_USERNAME", EnvironmentValue.ofString(mysqlUsername));
        environment.put("MYSQL_PASSWORD", EnvironmentValue.ofString(mysqlPassword));
        environment.put("REDIS_HOST", EnvironmentValue.ofString(redisHost));
        environment.put("REDIS_AUTH", EnvironmentValue.ofString(redisAuth));
        environment.put("NETWORK", EnvironmentValue.ofString(info.getNetwork().name()));

        api.retrieveServersByName(info.getName() + "-" + info.getNetwork().name(), false).execute().get(0).getStartupManager().setEnvironment(environment).execute();
        runSetup(info,0);
        startServer(info, 0);
    }

    public void updatePathfinderServer(ServerInfo info) {
        Map<String, EnvironmentValue<?>> environment = new HashMap<>();
        environment.put("CORE_VERSION", EnvironmentValue.ofString(info.getBuildNumber() + ""));
        if (info.getNetwork() != ServerInfo.Network.MAIN) {
            environment.put("CORE_BRANCH", EnvironmentValue.ofString(NetworkManager.getAlphaBranches().get(Module.CORE) + ""));
        }

        environment.put("PATHFINDER_VERSION", EnvironmentValue.ofString(info.getBuildNumber() + ""));
        environment.put("PATHFINDER_TYPE", EnvironmentValue.ofString(info.getServerType().getString("pathfinder_type") + ""));
        if (info.getNetwork() != ServerInfo.Network.MAIN) {
            environment.put("PATHFINDER_BRANCH", EnvironmentValue.ofString(NetworkManager.getAlphaBranches().get(Module.CORE) + ""));
        }

        environment.put("JENKINS_KEY", EnvironmentValue.ofString(jenkinsApiKey));
        environment.put("SERVER_NAME", EnvironmentValue.ofString(info.getName()));

        //Adding in database details.
        environment.put("MYSQL_HOST", EnvironmentValue.ofString(mysqlHost));
        environment.put("MYSQL_PORT", EnvironmentValue.ofString(mysqlPort));
        environment.put("MYSQL_DB", EnvironmentValue.ofString(mysqlDb));
        environment.put("MYSQL_USERNAME", EnvironmentValue.ofString(mysqlUsername));
        environment.put("MYSQL_PASSWORD", EnvironmentValue.ofString(mysqlPassword));
        environment.put("REDIS_HOST", EnvironmentValue.ofString(redisHost));
        environment.put("REDIS_AUTH", EnvironmentValue.ofString(redisAuth));
        environment.put("NETWORK", EnvironmentValue.ofString(info.getNetwork().name()));
        environment.put("PATHFINDER_KEY", EnvironmentValue.ofString(MissionControl.getAuthKey()));

        api.retrieveServersByName(info.getName() + "-" + info.getNetwork().name(), false).execute().get(0).getStartupManager().setEnvironment(environment).execute();
        runSetup(info,0);
        startServer(info, 0);
    }

    private void runSetup(ServerInfo info, int attempts) {
        try {
            api.retrieveServersByName(info.getName() + "-" + info.getNetwork().name(), false).execute().get(0).getStartupManager().setSkipScripts(false).execute();
            api.retrieveServersByName(info.getName() + "-" + info.getNetwork().name(), false).delay(5, TimeUnit.SECONDS).execute().get(0).getController().reinstall().execute();
        } catch (Exception e) {
            if (attempts > 5) {
                e.printStackTrace();
                return;
            }
            runSetup(info, attempts + 1);
        }
    }

    private void startServer(ServerInfo info, int attempts) {
        try {
            apiClient.retrieveServersByName(info.getName() + "-" + info.getNetwork().name(), false).delay(15, TimeUnit.SECONDS).execute().get(0).start().delay(15, TimeUnit.SECONDS).execute();
        } catch (Exception e) {
            if (attempts > 5) {
                e.printStackTrace();
                return;
            }
            startServer(info, attempts + 1);
        }
    }

    public void updateProxy(ProxyInfo info) {
        Map<String, EnvironmentValue<?>> environment = new HashMap<>();
        environment.put("CORE_VERSION", EnvironmentValue.ofString(info.getBuildNumber() + ""));
        if (info.getNetwork() != ServerInfo.Network.MAIN) {
            environment.put("CORE_BRANCH", EnvironmentValue.ofString(NetworkManager.getAlphaBranches().get(Module.PROXY) + ""));
        }
        environment.put("JENKINS_KEY", EnvironmentValue.ofString(jenkinsApiKey));
        environment.put("PROXY_UUID", EnvironmentValue.ofString(info.getUuid().toString()));

        //Adding in database details.
        environment.put("MYSQL_HOST", EnvironmentValue.ofString(mysqlHost));
        environment.put("MYSQL_PORT", EnvironmentValue.ofString(mysqlPort));
        environment.put("MYSQL_DB", EnvironmentValue.ofString(mysqlDb));
        environment.put("MYSQL_USERNAME", EnvironmentValue.ofString(mysqlUsername));
        environment.put("MYSQL_PASSWORD", EnvironmentValue.ofString(mysqlPassword));
        environment.put("REDIS_HOST", EnvironmentValue.ofString(redisHost));
        environment.put("REDIS_AUTH", EnvironmentValue.ofString(redisAuth));
        environment.put("NETWORK", EnvironmentValue.ofString(info.getNetwork().name()));
        environment.put("PATHFINDER_KEY", EnvironmentValue.ofString(MissionControl.getAuthKey()));

        api.retrieveServersByName(info.getUuid().toString() + "-" + info.getNetwork().name(), false).execute().get(0).getStartupManager().setEnvironment(environment).execute();
        runProxySetup(info, 0);
        startProxy(info, 0);
    }

    private void runProxySetup(ProxyInfo info, int attempts) {
        try {
            api.retrieveServersByName(info.getUuid() + "-" + info.getNetwork().name(), false).execute().get(0).getStartupManager().setSkipScripts(false).execute();
            api.retrieveServersByName(info.getUuid() + "-" + info.getNetwork().name(), false).delay(5, TimeUnit.SECONDS).execute().get(0).getController().reinstall().execute();
        } catch (Exception e) {
            if (attempts > 5) {
                e.printStackTrace();
                return;
            }
            runProxySetup(info, attempts + 1);
        }
    }

    private void startProxy(ProxyInfo info, int attempts) {
        try {
            apiClient.retrieveServersByName(info.getUuid() + "-" + info.getNetwork().name(), false).delay(15, TimeUnit.SECONDS).execute().get(0).start().delay(15, TimeUnit.SECONDS).execute();
        } catch (Exception e) {
            if (attempts > 5) {
                e.printStackTrace();
                return;
            }
            startProxy(info, attempts + 1);
        }
    }

    public void closeServer(String name, ServerInfo.Network network) {
        apiClient.retrieveServersByName(name + "-" + network.name(), false).execute().get(0).stop().execute();

    }

    public void openServer(String name, ServerInfo.Network network) {
        apiClient.retrieveServersByName(name + "-" + network.name(), false).delay(15, TimeUnit.SECONDS).execute().get(0).start().delay(15, TimeUnit.SECONDS).execute();
    }

    public void createProxy(ProxyInfo info) {
        Map<String, EnvironmentValue<?>> environment = new HashMap<>();
        environment.put("CORE_VERSION", EnvironmentValue.ofString(info.getBuildNumber() + ""));
        if (info.getNetwork() != ServerInfo.Network.MAIN) {
            environment.put("CORE_BRANCH", EnvironmentValue.ofString(NetworkManager.getAlphaBranches().get(Module.PROXY) + ""));
        }
        environment.put("JENKINS_KEY", EnvironmentValue.ofString(jenkinsApiKey));
        environment.put("PROXY_UUID", EnvironmentValue.ofString(info.getUuid().toString()));

        //Adding in database details.
        environment.put("MYSQL_HOST", EnvironmentValue.ofString(mysqlHost));
        environment.put("MYSQL_PORT", EnvironmentValue.ofString(mysqlPort));
        environment.put("MYSQL_DB", EnvironmentValue.ofString(mysqlDb));
        environment.put("MYSQL_USERNAME", EnvironmentValue.ofString(mysqlUsername));
        environment.put("MYSQL_PASSWORD", EnvironmentValue.ofString(mysqlPassword));
        environment.put("REDIS_HOST", EnvironmentValue.ofString(redisHost));
        environment.put("REDIS_AUTH", EnvironmentValue.ofString(redisAuth));
        environment.put("NETWORK", EnvironmentValue.ofString(info.getNetwork().name()));
        environment.put("PATHFINDER_KEY", EnvironmentValue.ofString(MissionControl.getAuthKey()));



        ApplicationServer server = api.createServer()
                .setName(info.getUuid().toString() + "-" + info.getNetwork().name())
                .setDescription("Server")
                .setOwner(api.retrieveUserById(6).execute())
                .setEgg(api.retrieveEggById(api.retrieveNestById(1).execute(), ((info.getNetwork() != ServerInfo.Network.MAIN)?18:15)).execute())
                .setAllocations(api.retrieveAllocations().execute().stream().filter(allocation -> allocation.getPort().equals(info.getPort() + "") && allocation.getIP().equalsIgnoreCase(info.getIp())).collect(Collectors.toList()).get(0), api.retrieveAllocations().execute().stream().filter(allocation -> allocation.getPort().equals(info.getProtocolPort() + "") && allocation.getIP().equalsIgnoreCase(info.getIp())).collect(Collectors.toList()).get(0), api.retrieveAllocations().execute().stream().filter(allocation -> allocation.getPort().equals(info.getProtocolPort() + "") && allocation.getIP().equalsIgnoreCase("127.0.0.1")).collect(Collectors.toList()).get(0))
                .setDatabases(0)
                .setCPU(0)
                .setDisk(5, DataType.GB)
                .setMemory(MemoryAllocation.PROXY.getMegaBytes(), DataType.MB)
                .setDockerImage("quay.io/pterodactyl/core:java")
                .startOnCompletion(true)
                .skipScripts(false)
                .setEnvironment(environment).execute();
    }

    public void createProxy(ProxyInfo info, ApplicationAllocation allocation, ApplicationAllocation protocolAllocation, ApplicationAllocation altProtocolAllocation) {
        Map<String, EnvironmentValue<?>> environment = new HashMap<>();
        environment.put("CORE_VERSION", EnvironmentValue.ofString(info.getBuildNumber() + ""));
        if (info.getNetwork() != ServerInfo.Network.MAIN) {
            environment.put("CORE_BRANCH", EnvironmentValue.ofString(NetworkManager.getAlphaBranches().get(Module.PROXY) + ""));
        }
        environment.put("JENKINS_KEY", EnvironmentValue.ofString(jenkinsApiKey));
        environment.put("PROXY_UUID", EnvironmentValue.ofString(info.getUuid().toString()));

        //Adding in database details.
        environment.put("MYSQL_HOST", EnvironmentValue.ofString(mysqlHost));
        environment.put("MYSQL_PORT", EnvironmentValue.ofString(mysqlPort));
        environment.put("MYSQL_DB", EnvironmentValue.ofString(mysqlDb));
        environment.put("MYSQL_USERNAME", EnvironmentValue.ofString(mysqlUsername));
        environment.put("MYSQL_PASSWORD", EnvironmentValue.ofString(mysqlPassword));
        environment.put("REDIS_HOST", EnvironmentValue.ofString(redisHost));
        environment.put("REDIS_AUTH", EnvironmentValue.ofString(redisAuth));
        environment.put("NETWORK", EnvironmentValue.ofString(info.getNetwork().name()));
        environment.put("PATHFINDER_KEY", EnvironmentValue.ofString(MissionControl.getAuthKey()));
        MissionControl.getLogger().info("Creating proxy with IP: " + allocation.getIP() + ":" + allocation.getPort());

        ApplicationServer server = api.createServer()
                .setName(info.getUuid().toString() + "-" + info.getNetwork().name())
                .setDescription("Server")
                .setOwner(api.retrieveUserById(6).execute())
                .setEgg(api.retrieveEggById(api.retrieveNestById(1).execute(), ((info.getNetwork() != ServerInfo.Network.MAIN)?18:15)).execute())
                .setAllocations(allocation, Arrays.asList(protocolAllocation, altProtocolAllocation))
                .setDatabases(0)
                .setCPU(0)
                .setDisk(5, DataType.GB)
                .setMemory(MemoryAllocation.PROXY.getMegaBytes(), DataType.MB)
                .setDockerImage("quay.io/pterodactyl/core:java")
                .startOnCompletion(true)
                .skipScripts(false)
                .setEnvironment(environment).execute();
    }

    public void createProxy(ProxyInfo info, ApplicationAllocation allocation, ApplicationAllocation protocolAllocation, ApplicationAllocation altProtocolAllocation, String branch) {
        Map<String, EnvironmentValue<?>> environment = new HashMap<>();
        environment.put("CORE_VERSION", EnvironmentValue.ofString(info.getBuildNumber() + ""));
        if (info.getNetwork() != ServerInfo.Network.MAIN) {
            environment.put("CORE_BRANCH", EnvironmentValue.ofString(branch));
        }
        environment.put("JENKINS_KEY", EnvironmentValue.ofString(jenkinsApiKey));
        environment.put("PROXY_UUID", EnvironmentValue.ofString(info.getUuid().toString()));

        //Adding in database details.
        environment.put("MYSQL_HOST", EnvironmentValue.ofString(mysqlHost));
        environment.put("MYSQL_PORT", EnvironmentValue.ofString(mysqlPort));
        environment.put("MYSQL_DB", EnvironmentValue.ofString(mysqlDb));
        environment.put("MYSQL_USERNAME", EnvironmentValue.ofString(mysqlUsername));
        environment.put("MYSQL_PASSWORD", EnvironmentValue.ofString(mysqlPassword));
        environment.put("REDIS_HOST", EnvironmentValue.ofString(redisHost));
        environment.put("REDIS_AUTH", EnvironmentValue.ofString(redisAuth));
        environment.put("NETWORK", EnvironmentValue.ofString(info.getNetwork().name()));
        environment.put("PATHFINDER_KEY", EnvironmentValue.ofString(MissionControl.getAuthKey()));
        MissionControl.getLogger().info("Creating proxy with IP: " + allocation.getIP() + ":" + allocation.getPort());

        ApplicationServer server = api.createServer()
                .setName(info.getUuid().toString() + "-" + info.getNetwork().name())
                .setDescription("Server")
                .setOwner(api.retrieveUserById(6).execute())
                .setEgg(api.retrieveEggById(api.retrieveNestById(1).execute(), ((info.getNetwork() != ServerInfo.Network.MAIN)?18:15)).execute())
                .setAllocations(allocation, Arrays.asList(protocolAllocation, altProtocolAllocation))
                .setDatabases(0)
                .setCPU(0)
                .setDisk(5, DataType.GB)
                .setMemory(MemoryAllocation.PROXY.getMegaBytes(), DataType.MB)
                .setDockerImage("quay.io/pterodactyl/core:java")
                .startOnCompletion(true)
                .skipScripts(false)
                .setEnvironment(environment).execute();
    }

}
