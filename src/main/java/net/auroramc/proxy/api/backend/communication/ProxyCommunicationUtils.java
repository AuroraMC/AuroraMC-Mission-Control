package net.auroramc.proxy.api.backend.communication;

import net.auroramc.missioncontrol.MissionControl;
import net.auroramc.missioncontrol.entities.ProxyInfo;

import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.UUID;
import java.util.logging.Level;

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
            MissionControl.getLogger().log(Level.FINEST, "Sending protocol message to " + info.getIp() + ":" + info.getProtocolPort());
            try (Socket socket = new Socket(info.getIp(), info.getProtocolPort())) {
                ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
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
