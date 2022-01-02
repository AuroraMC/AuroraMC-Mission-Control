/*
 * Copyright (c) 2021 AuroraMC Ltd. All Rights Reserved.
 */

package net.auroramc.missioncontrol.backend.communication.panel;

import net.auroramc.core.api.backend.communication.ServerCommunicationUtils;
import net.auroramc.missioncontrol.MissionControl;
import net.auroramc.missioncontrol.NetworkManager;
import net.auroramc.missioncontrol.NetworkMonitorRunnable;
import net.auroramc.missioncontrol.backend.util.Game;
import net.auroramc.missioncontrol.backend.util.MaintenanceMode;
import net.auroramc.missioncontrol.backend.util.Module;
import net.auroramc.missioncontrol.entities.ProxyInfo;
import net.auroramc.missioncontrol.entities.ServerInfo;
import net.auroramc.proxy.api.backend.communication.Protocol;
import net.auroramc.proxy.api.backend.communication.ProtocolMessage;
import net.auroramc.proxy.api.backend.communication.ProxyCommunicationUtils;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

public class PanelMessageHandler {

    private static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);

    public static String onMessage(String message) {
        List<String> args = new ArrayList<>(Arrays.asList(message.split(";")));
        String command = args.remove(0);
        switch (command.toLowerCase()) {
            case "createserver": {
                if (args.size() >= 2) {
                    ServerInfo.Network network;
                    try {
                        network = ServerInfo.Network.valueOf(args.get(0));
                    } catch (IllegalArgumentException e) {
                        return "An invalid network was inputted.";
                    }

                    Game game;
                    try {
                        game = Game.valueOf(args.get(1));
                    } catch (IllegalArgumentException e) {
                        return "An invalid game was inputted.";
                    }

                    String serverName = null;
                    List<String> extraArgs = null;

                    if (args.size() >= 3) {
                        serverName = args.get(2);
                    }

                    if (MissionControl.getServers().get(network).containsKey(serverName)) {
                        return "That server already exists on that network.";
                    }

                    if (args.size() >= 4) {
                        extraArgs = Arrays.asList(args.get(3).split(" "));
                    }
                    if (extraArgs != null && network == ServerInfo.Network.TEST) {
                        Map<Module, Integer> moduleBuilds = new HashMap<>();
                        Map<Module, String> moduleBranches = new HashMap<>();
                        for (String arg : extraArgs) {
                            String[] moduleArg = arg.split("=");
                            String[] branchArg = moduleArg[1].split(":");

                            String branch = branchArg[0];
                            int build = Integer.parseInt(branchArg[1]);
                            Module module;
                            try {
                                module = Module.valueOf(moduleArg[0].toUpperCase());
                            } catch (IllegalArgumentException e) {
                                return "Module '" + moduleArg[0] + "' does not exist.";
                            }

                            if (!game.getModules().contains(module)) {
                                continue;
                            }

                            if (!MissionControl.getJenkinsManager().branchExists(module, branch)) {
                                return "The branch '" + branch + "' does not exist for this module. If it should exist, make sure that the CI job has been executed before trying again.";
                            }

                            if (!MissionControl.getJenkinsManager().buildExists(module, branch, build)) {
                                return "The build '" + build + "' does not exist for this module/branch. If it should exist, make sure that the CI job has been executed before trying again.";
                            }

                            moduleBranches.put(module, branch);
                            moduleBuilds.put(module, build);
                        }

                        if (moduleBranches.size() != game.getModules().size()) {
                            return "You have not specified the branch and build number for all modules. Aborting.";
                        }

                        ServerInfo info = NetworkManager.createServer(serverName, game, true, network, moduleBuilds.getOrDefault(Module.CORE, -1), moduleBranches.get(Module.CORE), moduleBuilds.getOrDefault(Module.LOBBY, -1), moduleBranches.get(Module.LOBBY), moduleBuilds.getOrDefault(Module.BUILD, -1), moduleBranches.get(Module.BUILD), moduleBuilds.getOrDefault(Module.GAME, -1), moduleBranches.get(Module.GAME), moduleBuilds.getOrDefault(Module.ENGINE, -1), moduleBranches.get(Module.ENGINE), false);
                        if (info != null) {
                            return "Server '" + serverName + "' successfully created on network '" + network.name() + "'. Please give the server time to start up properly.";
                        } else {
                            return "Server creation failed.";
                        }
                    } else {
                        if (network == ServerInfo.Network.TEST) {
                            return "You must provide the server name and branch/build details for each module the server will deploy with when creating a server on the test network.";
                        }

                        if (serverName == null) {
                            int id = NetworkMonitorRunnable.findLowestAvailableServerID(game, network);
                            serverName = game.getServerCode() + "-" + id;
                        } else if (MissionControl.getServers().get(network).containsKey(serverName)) {
                            return "That server already exists on that network.";
                        }

                        ServerInfo info = NetworkManager.createServer(serverName, game, true, network, false);
                        if (info != null) {
                            return "Server '" + serverName + "' successfully created on network '" + network.name() + "'. Please give the server time to start up properly.";
                        } else {
                            return "Server creation failed.";
                        }
                    }
                } else {
                    return "The command executed does not have correct arguments. Please try again.";
                }
            }
            case "restartserver": {
                if (args.size() == 2) {
                    ServerInfo.Network network;
                    try {
                        network = ServerInfo.Network.valueOf(args.get(0));
                    } catch (IllegalArgumentException e) {
                        return "An invalid network was inputted.";
                    }

                    String name = args.get(1);
                    ServerInfo info = MissionControl.getServers().get(network).get(name);
                    if (info == null) {
                        return "That server does not exist!";
                    }

                    info.setStatus(ServerInfo.ServerStatus.PENDING_RESTART);

                    NetworkManager.removeServerFromRotation(info);
                    net.auroramc.core.api.backend.communication.ProtocolMessage protocolMessage = new net.auroramc.core.api.backend.communication.ProtocolMessage(net.auroramc.core.api.backend.communication.Protocol.EMERGENCY_SHUTDOWN, info.getName(), "restart", "Mission Control", "");
                    ServerCommunicationUtils.sendMessage(protocolMessage, network);
                    return "Restart request sent. Please be patient, it can take some time for a server to be ready to restart.";
                } else {
                    return "The command executed does not have correct arguments. Please try again.";
                }
            }
            case "closeserver": {
                if (args.size() == 2) {
                    ServerInfo.Network network;
                    try {
                        network = ServerInfo.Network.valueOf(args.get(0));
                    } catch (IllegalArgumentException e) {
                        return "An invalid network was inputted.";
                    }

                    String name = args.get(1);
                    ServerInfo info = MissionControl.getServers().get(network).get(name);
                    if (info == null) {
                        return "That server does not exist!";
                    }

                    NetworkManager.removeServerFromRotation(info);
                    net.auroramc.core.api.backend.communication.ProtocolMessage protocolMessage = new net.auroramc.core.api.backend.communication.ProtocolMessage(net.auroramc.core.api.backend.communication.Protocol.EMERGENCY_SHUTDOWN, info.getName(), "close", "Mission Control", "");
                    ServerCommunicationUtils.sendMessage(protocolMessage, network);
                    return "Close request sent. Please be patient, it can take some time for a server to be ready to close.";
                } else {
                    return "The command executed does not have correct arguments. Please try again.";
                }
            }
            case "createproxy": {
                if (args.size() >= 1) {
                    ServerInfo.Network network;
                    try {
                        network = ServerInfo.Network.valueOf(args.get(0));
                    } catch (IllegalArgumentException e) {
                        return "An invalid network was inputted.";
                    }
                    String arg = null;
                    if (args.size() == 2) {
                        arg = args.get(1);
                    }
                    if (arg != null && network == ServerInfo.Network.TEST) {
                        String[] branchArg = arg.split(":");

                        if (branchArg.length != 2) {
                            return "When creating proxies, the extra arg must be of format: branch:build";
                        }

                        String branch = branchArg[0];
                        int build = Integer.parseInt(branchArg[1]);

                        if (!MissionControl.getJenkinsManager().branchExists(Module.PROXY, branch)) {
                            return "The branch '" + branch + "' does not exist for the PROXY module. If it should exist, make sure that the CI job has been executed before trying again.";
                        }

                        if (!MissionControl.getJenkinsManager().buildExists(Module.PROXY, branch, build)) {
                            return "The build '" + build + "' does not exist for the PROXY module on this branch. If it should exist, make sure that the CI job has been executed before trying again.";
                        }

                        ProxyInfo info = NetworkManager.createProxy(network, true, build, branch, false);
                        if (info != null) {
                            return "Proxy '" + info.getUuid().toString() + "' successfully created on network '" + network.name() + "'.";
                        } else {
                            return "Failed to create a proxy.";
                        }
                    } else {
                        if (network == ServerInfo.Network.TEST) {
                            return "You must provide the branch/build details for the PROXY module the proxy will deploy with when creating a proxy on the test network.";
                        }

                        ProxyInfo info = NetworkManager.createProxy(network, true, false);
                        if (info != null) {
                            return "Proxy '" + info.getUuid().toString() + "' successfully created on network '" + network.name() + "'.";
                        } else {
                            return "Failed to create a proxy.";
                        }
                    }
                } else {
                    return "The command executed does not have correct arguments. Please try again.";
                }
            }
            case "restartproxy": {
                if (args.size() == 2) {
                    UUID uuid;

                    try {
                        uuid = UUID.fromString(args.get(1));
                    } catch (IllegalArgumentException e) {
                        return "That is not a valid UUID.";
                    }

                    ProxyInfo info = MissionControl.getProxies().get(uuid);

                    if (info == null) {
                        return "Proxy '" + uuid + "' does not exist.";
                    }

                    info.setStatus(ProxyInfo.ProxyStatus.RESTARTING);

                    ProtocolMessage protocolMessage = new ProtocolMessage(Protocol.EMERGENCY_SHUTDOWN, uuid.toString(), "restart", "Mission Control", "");
                    ProxyCommunicationUtils.sendMessage(protocolMessage);
                    return "Proxy restart has been queued. Please allow up to 5 minutes for the proxy to restart properly.";
                } else {
                    return "The command executed does not have correct arguments. Please try again.";
                }
            }
            case "closeproxy": {
                if (args.size() == 2) {
                    UUID uuid;

                    try {
                        uuid = UUID.fromString(args.get(1));
                    } catch (IllegalArgumentException e) {
                        return "That is not a valid UUID.";
                    }

                    ProxyInfo info = MissionControl.getProxies().get(uuid);

                    if (info == null) {
                        return "Proxy '" + uuid + "' does not exist.";
                    }

                    ProtocolMessage protocolMessage = new ProtocolMessage(Protocol.EMERGENCY_SHUTDOWN, uuid.toString(), "close", "Mission Control", "");
                    ProxyCommunicationUtils.sendMessage(protocolMessage);
                    return "Proxy close has been requested. Please allow up to 5 minutes for the proxy to close properly.";
                } else {
                    return "The command executed does not have correct arguments. Please try again.";
                }
            }
            case "disablenetwork": {
                if (args.size() == 1) {
                    ServerInfo.Network network;
                    try {
                        network = ServerInfo.Network.valueOf(args.get(0));
                    } catch (IllegalArgumentException e) {
                        return "An invalid network was inputted.";
                    }

                    if (network == ServerInfo.Network.TEST) {
                        return "You cannot disable server monitoring on the test network.";
                    }
                    if (!NetworkManager.isServerMonitoringEnabled(network)) {
                        return "Server monitoring is already disabled for network '" + network.name() + "'.";
                    }
                    NetworkManager.setServerManagerEnabled(network, false);
                    return "Server monitoring has been disabled for network '" + network.name() + "'.";

                } else {
                    return "The command executed does not have correct arguments. Please try again.";
                }
            }
            case "enablenetwork": {
                if (args.size() == 1) {
                    ServerInfo.Network network;
                    try {
                        network = ServerInfo.Network.valueOf(args.get(0));
                    } catch (IllegalArgumentException e) {
                        return "An invalid network was inputted.";
                    }

                    if (network == ServerInfo.Network.TEST) {
                        return "You cannot enable server monitoring on the test network.";
                    }
                    if (NetworkManager.isServerMonitoringEnabled(network)) {
                        return "Server monitoring is already enabled for network '" + network.name() + "'.";
                    }
                    NetworkManager.setServerManagerEnabled(network, true);
                    return "Server monitoring has been enabled for network '" + network.name() + "'.";

                } else {
                    return "The command executed does not have correct arguments. Please try again.";
                }
            }
            case "updatenetwork": {
                if (args.size() == 1) {
                    Map<Module, Integer> modulesToUpdate = new HashMap<>();
                    String[] args2 = args.get(0).split(" ");
                    for (String arg : args2) {
                        String[] moduleInfo = arg.split(":");
                        if (moduleInfo.length != 2) {
                            return "One of the arguments is formatted incorrectly. Please correct this error and try again.";
                        }
                        Module module;
                        try {
                            module = Module.valueOf(moduleInfo[0].toUpperCase());
                        } catch (IllegalArgumentException e) {
                            return "Module '" + moduleInfo[0] + "' does not exist.";
                        }
                        int build;
                        try {
                            build = Integer.parseInt(moduleInfo[1]);
                        } catch (NumberFormatException e) {
                            return "One of your build numbers is not in the correct format. Please fix this error and try again.";
                        }
                        modulesToUpdate.put(module, build);
                    }
                    NetworkManager.pushUpdate(modulesToUpdate, ServerInfo.Network.MAIN);
                    return "Initiated update for " + modulesToUpdate.size() + " modules.";
                } else {
                    return "The command executed does not have correct arguments. Please try again.";
                }
            }
            case "enablealpha": {
                if (NetworkManager.isAlphaEnabled()) {
                    return "The alpha network is already enabled.";
                }
                NetworkManager.setAlphaEnabled(true);
                return "The alpha network has been enabled.";
            }
            case "disablealpha": {
                if (!NetworkManager.isAlphaEnabled()) {
                    return "The alpha network is already disabled.";
                }
                NetworkManager.setAlphaEnabled(false);
                return "The alpha network has been disabled.";
            }
            case "updatealpha": {
                if (args.size() == 1) {
                    String[] args2 = args.get(0).split(" ");
                    Map<Module, Integer> modulesToUpdate = new HashMap<>();
                    for (String arg : args2) {
                        if (arg.matches("[A-Za-z]{1,10}=[A-Za-z0-9_-]{1,255}:[0-9]{1,10}")) {
                            String[] moduleArg = arg.split("=");
                            String[] branchArg = moduleArg[1].split(":");

                            String branch = branchArg[0];
                            int build;
                            try {
                                build = Integer.parseInt(branchArg[1]);
                            } catch (NumberFormatException e) {
                                return "One of your build numbers is not in the correct format. Please fix this error and try again.";
                            }
                            Module module;
                            try {
                                module = Module.valueOf(moduleArg[0].toUpperCase());
                            } catch (IllegalArgumentException e) {
                                return "Module '" + moduleArg[0] + "' does not exist.";
                            }

                            if (!MissionControl.getJenkinsManager().branchExists(module, branch)) {
                                return "The branch '" + branch + "' does not exist for this module. If it should exist, make sure that the CI job has been executed before trying again.";
                            }

                            if (!MissionControl.getJenkinsManager().buildExists(module, branch, build)) {
                                return "The build '" + build + "' does not exist for this module/branch. If it should exist, make sure that the CI job has been executed before trying again.";
                            }

                            NetworkManager.getAlphaBranches().put(module, branch);
                            modulesToUpdate.put(module, build);
                        } else {
                            return "Your argument is not formatted properly. It should be formatted as: branch:build";
                        }
                    }
                    NetworkManager.pushUpdate(modulesToUpdate, ServerInfo.Network.ALPHA);
                    return "Builds updated and network restart initiated.";
                } else {
                    return "The command executed does not have correct arguments. Please try again.";
                }
            }
            case "enablegame": {
                if (args.size() == 2) {
                    Game game;
                    try {
                        game = Game.valueOf(args.get(1));
                    } catch (IllegalArgumentException e) {
                        return "That is not a valid game.";
                    }

                    ServerInfo.Network network;
                    try {
                        network = ServerInfo.Network.valueOf(args.get(0));
                    } catch (IllegalArgumentException e) {
                        return "That is not a valid network.";
                    }

                    if (NetworkManager.isGameEnabled(game, network)) {
                        return "That game is already enabled!";
                    }

                    NetworkManager.enableGame(game, network);
                    return "Game '" + game.name() + "' has been enabled on network '" + network.name() + "'.";
                } else {
                    return "The command executed does not have correct arguments. Please try again.";
                }
            }
            case "disablegame": {
                if (args.size() == 2) {
                    Game game;
                    try {
                        game = Game.valueOf(args.get(1));
                    } catch (IllegalArgumentException e) {
                        return "That is not a valid game.";
                    }

                    ServerInfo.Network network;
                    try {
                        network = ServerInfo.Network.valueOf(args.get(0));
                    } catch (IllegalArgumentException e) {
                        return "That is not a valid network.";
                    }

                    if (!NetworkManager.isGameEnabled(game, network)) {
                        return "That game is already disabled!";
                    }

                    NetworkManager.disableGame(game, network);
                    return "Game '" + game.name() + "' has been disabled on network '" + network.name() + "'.";
                } else {
                    return "The command executed does not have correct arguments. Please try again.";
                }
            }
            case "monitorgame": {
                if (args.size() == 3) {
                    Game game;
                    try {
                        game = Game.valueOf(args.get(1));
                    } catch (IllegalArgumentException e) {
                        return "That is not a valid game.";
                    }

                    boolean monitor = Boolean.parseBoolean(args.get(2));

                    ServerInfo.Network network;
                    try {
                        network = ServerInfo.Network.valueOf(args.get(0));
                    } catch (IllegalArgumentException e) {
                        return "That is not a valid network.";
                    }

                    if (NetworkManager.isGameMonitored(game, network) == monitor) {
                        return "That game is already " + ((monitor)?"enabled":"disabled") + "!";
                    }

                    NetworkManager.setMonitored(game, network, monitor);
                    return "Monitoring for game '" + game.name() + "' has been " + ((monitor)?"enabled":"disabled") + " on network '" + network.name() + "'.";
                } else {
                    return "The command executed does not have correct arguments. Please try again.";
                }
            }
            case "maintenancemode": {
                if (args.size() == 2) {
                    ServerInfo.Network network;
                    try {
                        network = ServerInfo.Network.valueOf(args.get(0));
                    } catch (IllegalArgumentException e) {
                        return "That is not a valid network.";
                    }

                    if (args.get(1).equals("DISABLED")) {
                        if (NetworkManager.isMaintenance(network)) {
                            MissionControl.getDbManager().changeMaintenance(network, false);
                            NetworkManager.setMaintenance(network, false);
                            for (ProxyInfo info : MissionControl.getProxies().values().stream().filter(proxyInfo -> proxyInfo.getNetwork() == network).collect(Collectors.toList())) {
                                ProtocolMessage message1 = new ProtocolMessage(Protocol.UPDATE_MAINTENANCE_MODE, info.getUuid().toString(), "disable", "Mission Control", "");
                                ProxyCommunicationUtils.sendMessage(message1);
                            }
                            return "Maintenance mode has been disabled.";
                        } else {
                            return "Maintenance mode is not enabled on that network.";
                        }
                    }

                    MaintenanceMode mode = MaintenanceMode.valueOf(args.get(1));

                    if (NetworkManager.isMaintenance(network)) {
                        MissionControl.getDbManager().changeMaintenanceMode(network, mode);
                        NetworkManager.setMaintenanceMode(network, mode);
                        for (ProxyInfo info : MissionControl.getProxies().values().stream().filter(proxyInfo -> proxyInfo.getNetwork() == network).collect(Collectors.toList())) {
                            ProtocolMessage message1 = new ProtocolMessage(Protocol.UPDATE_MAINTENANCE_MODE, info.getUuid().toString(), "update", "Mission Control", mode.name());
                            ProxyCommunicationUtils.sendMessage(message1);
                        }
                        return "Maintenance mode has been updated.";
                    } else {
                        MissionControl.getDbManager().changeMaintenance(network, true);
                        NetworkManager.setMaintenance(network, true);
                        MissionControl.getDbManager().changeMaintenanceMode(network, mode);
                        NetworkManager.setMaintenanceMode(network, mode);
                        for (ProxyInfo info : MissionControl.getProxies().values().stream().filter(proxyInfo -> proxyInfo.getNetwork() == network).collect(Collectors.toList())) {
                            ProtocolMessage message1 = new ProtocolMessage(Protocol.UPDATE_MAINTENANCE_MODE, info.getUuid().toString(), "enable", "Mission Control", mode.name());
                            ProxyCommunicationUtils.sendMessage(message1);
                        }
                        return "Maintenance mode has been enabled.";
                    }
                } else {
                    return "The command executed does not have correct arguments. Please try again.";
                }
            }
            case "maintenancemotd": {
                if (args.size() == 2) {
                    ServerInfo.Network network;
                    try {
                        network = ServerInfo.Network.valueOf(args.get(0));
                    } catch (IllegalArgumentException e) {
                        return "That is not a valid network.";
                    }

                    NetworkManager.setMaintenanceMotd(network, args.get(1));
                    MissionControl.getDbManager().changeMaintenanceMotd(network, args.get(1));

                    for (ProxyInfo info : MissionControl.getProxies().values().stream().filter(proxyInfo -> proxyInfo.getNetwork() == network).collect(Collectors.toList())) {
                        ProtocolMessage message1 = new ProtocolMessage(Protocol.UPDATE_MOTD, info.getUuid().toString(), "maintenance", "Mission Control", args.get(1));
                        ProxyCommunicationUtils.sendMessage(message1);
                    }
                    return "Maintenance MOTD has been updated.";
                } else {
                    return "The command executed does not have correct arguments. Please try again.";
                }
            }
            case "normalmotd": {
                if (args.size() == 2) {
                    ServerInfo.Network network;
                    try {
                        network = ServerInfo.Network.valueOf(args.get(0));
                    } catch (IllegalArgumentException e) {
                        return "That is not a valid network.";
                    }

                    NetworkManager.setMotd(network, args.get(1));
                    MissionControl.getDbManager().changeMotd(network, args.get(1));

                    for (ProxyInfo info : MissionControl.getProxies().values().stream().filter(proxyInfo -> proxyInfo.getNetwork() == network).collect(Collectors.toList())) {
                        ProtocolMessage message1 = new ProtocolMessage(Protocol.UPDATE_MOTD, info.getUuid().toString(), "normal", "Mission Control", args.get(1));
                        ProxyCommunicationUtils.sendMessage(message1);
                    }
                    return "MOTD has been updated.";
                } else {
                    return "The command executed does not have correct arguments. Please try again.";
                }
            }
            case "updaterules": {
                for (ServerInfo.Network network : ServerInfo.Network.values()) {
                    for (ServerInfo info : MissionControl.getServers().get(network).values()) {
                        net.auroramc.core.api.backend.communication.ProtocolMessage msg = new net.auroramc.core.api.backend.communication.ProtocolMessage(net.auroramc.core.api.backend.communication.Protocol.UPDATE_RULES, info.getName(), "update", "MissionControl", "");
                        ServerCommunicationUtils.sendMessage(msg, network);
                    }
                }
                for (ProxyInfo info : MissionControl.getProxies().values()) {
                    ProtocolMessage msg = new ProtocolMessage(Protocol.UPDATE_RULES, info.getUuid().toString(), "update", "MissionControl", "");
                    ProxyCommunicationUtils.sendMessage(msg);
                }
                return "Rules have been updated on all servers and proxies.";
            }
            case "updatemaps": {
                for (ServerInfo.Network network : ServerInfo.Network.values()) {
                    for (ServerInfo info : MissionControl.getServers().get(network).values()) {
                        net.auroramc.core.api.backend.communication.ProtocolMessage msg = new net.auroramc.core.api.backend.communication.ProtocolMessage(net.auroramc.core.api.backend.communication.Protocol.UPDATE_MAPS, info.getName(), "update", "MissionControl", "");
                        ServerCommunicationUtils.sendMessage(msg, network);
                    }
                }
                return "Map lists have been updated on all servers.";
            }
        }
        return "The command executed was not recognised by Mission Control. Please try again.";
    }

}
