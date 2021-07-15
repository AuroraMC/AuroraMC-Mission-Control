package net.auroramc.missioncontrol;

import net.auroramc.core.api.backend.communication.Protocol;
import net.auroramc.core.api.backend.communication.ProtocolMessage;
import net.auroramc.core.api.backend.communication.ServerCommunicationUtils;
import net.auroramc.missioncontrol.backend.Module;
import net.auroramc.missioncontrol.entities.ProxyInfo;
import net.auroramc.missioncontrol.entities.ServerInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.stream.Collectors;

public class NetworkRestarterThread extends Thread {

    private final List<Module> modules;
    private final List<ServerInfo> serversToRestart = new ArrayList<>();
    private RestartMode serverRestartMode;
    private final List<ProxyInfo> proxiesToRestart = new ArrayList<>();
    private RestartMode proxyRestartMode;

    ArrayBlockingQueue<ServerInfo> serverQueue;
    ArrayBlockingQueue<ProxyInfo> proxyQueue;

    public NetworkRestarterThread(List<Module> modulesToRestart) {
        modules = modulesToRestart;
    }

    @Override
    public void run() {
        if (modules.contains(Module.PROXY)) {
            //Open and close proxies in batches of 10 to prevent using too many resources.
            proxiesToRestart.addAll(MissionControl.getProxies().values());
            proxyQueue = new ArrayBlockingQueue<>(10);
            if (proxiesToRestart.size() <= 10) {
                //As there are only 10 open, just restart them 1 at a time.
                proxyRestartMode = RestartMode.SOLO;
                ProxyInfo info = proxiesToRestart.remove(0);
                if (info != null) {
                    NetworkManager.removeProxyFromRotation(info);

                }
            } else {
                //Delete servers and create new ones in batches of 10. Create 10 connection nodes first tho.
                for (int i = 0;i < 10;i++) {
                    NetworkManager.createProxy();
                }
            }
        }
        if (modules.contains(Module.CORE)) {
            //Restart the entire network.

        } else {
            if (modules.contains(Module.BUILD)) {
                //Restart any build servers.
                List<ServerInfo> servers = MissionControl.getServers().values().stream().filter(server -> server.getServerType().getString("type").equalsIgnoreCase("build")).collect(Collectors.toList());
                for (ServerInfo info : servers) {
                    ProtocolMessage message = new ProtocolMessage(Protocol.SHUTDOWN, info.getName(), "update", "Mission Control", "");
                    ServerCommunicationUtils.sendMessage(message);
                }
            } else if (modules.contains(Module.ENGINE) || modules.contains(Module.GAME)) {
                //Restart any game servers.

            } else if (modules.contains(Module.LOBBY)) {
                //Restart any lobby servers.

            } else if (modules.contains(Module.EVENT)) {
                //Restart any event servers currently active. Does not include servers that have been turned into event servers.
                List<ServerInfo> servers = MissionControl.getServers().values().stream().filter(server -> server.getServerType().getString("type").equalsIgnoreCase("game") && server.getServerType().getBoolean("event")).collect(Collectors.toList());
                for (ServerInfo info : servers) {
                    ProtocolMessage message = new ProtocolMessage(Protocol.SHUTDOWN, info.getName(), "update", "Mission Control", "");
                    ServerCommunicationUtils.sendMessage(message);
                }
            }
        }
    }

    public void proxyCloseConfirm(ProxyInfo info) {

    }

    public void serverRestartConfirm(ServerInfo info) {

    }

    private enum RestartMode {BATCHES, SOLO}
}
