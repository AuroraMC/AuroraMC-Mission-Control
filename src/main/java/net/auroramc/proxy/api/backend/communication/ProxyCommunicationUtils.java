package net.auroramc.proxy.api.backend.communication;

import net.auroramc.core.api.backend.communication.IncomingProtocolMessageThread;
import net.auroramc.core.api.backend.communication.ProtocolMessage;
import net.auroramc.missioncontrol.MissionControl;
import net.auroramc.missioncontrol.entities.ProxyInfo;
import net.auroramc.missioncontrol.entities.ServerInfo;

import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.UUID;

public class ProxyCommunicationUtils {

    private static IncomingProtocolMessageThread task;

    public static void init() {
        if (task != null) {
            task.shutdown();
        }
        task = new IncomingProtocolMessageThread(35566);
        task.start();
    }

    public static UUID sendMessage(ProtocolMessage message) {
        ProxyInfo info = MissionControl.getProxies().get(UUID.fromString(message.getDestination()));
        if (info != null) {
            try (Socket socket = new Socket(info.getIp(), info.getProtocolPort())) {
                ObjectOutputStream outputStream = (ObjectOutputStream) socket.getOutputStream();
                outputStream.writeObject(message);
                outputStream.flush();
                return message.getUuid();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }

    public static void shutdown() {
        if (task != null) {
            task.shutdown();
            task = null;
        }
    }

}
