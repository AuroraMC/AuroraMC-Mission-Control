/*
 * Copyright (c) 2021 AuroraMC Ltd. All Rights Reserved.
 */

package net.auroramc.missioncontrol.backend.communication.panel;

import net.auroramc.core.api.backend.communication.ServerCommunicationUtils;
import net.auroramc.missioncontrol.MissionControl;
import net.auroramc.missioncontrol.NetworkManager;
import net.auroramc.missioncontrol.NetworkMonitorRunnable;
import net.auroramc.missioncontrol.backend.util.Game;
import net.auroramc.missioncontrol.backend.util.Module;
import net.auroramc.missioncontrol.entities.ProxyInfo;
import net.auroramc.missioncontrol.entities.ServerInfo;
import net.auroramc.proxy.api.backend.communication.Protocol;
import net.auroramc.proxy.api.backend.communication.ProtocolMessage;
import net.auroramc.proxy.api.backend.communication.ProxyCommunicationUtils;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

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

                    NetworkManager.removeProxyFromRotation(info);
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

                    NetworkManager.removeProxyFromRotation(info);
                    ProtocolMessage protocolMessage = new ProtocolMessage(Protocol.EMERGENCY_SHUTDOWN, uuid.toString(), "close", "Mission Control", "");
                    ProxyCommunicationUtils.sendMessage(protocolMessage);
                    return "Proxy close has been requested. Please allow up to 5 minutes for the proxy to close properly.";
                } else {
                    return "The command executed does not have correct arguments. Please try again.";
                }
            }
        }
        return "The command executed was not recognised by Mission Control. Please try again.";
    }

}
