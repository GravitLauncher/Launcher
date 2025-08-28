package pro.gravit.launchserver.command.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.WebSocketService;
import pro.gravit.launchserver.socket.handlers.WebSocketFrameHandler;
import pro.gravit.utils.helper.IOHelper;

import java.util.Base64;

public class ClientsCommand extends Command {
    private transient final Logger logger = LogManager.getLogger();

    public ClientsCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return null;
    }

    @Override
    public String getUsageDescription() {
        return "Show all connected clients";
    }

    @Override
    public void invoke(String... args) {
        WebSocketService service = server.nettyServerSocketHandler.nettyServer.service;
        service.channels.forEach((channel -> {
            WebSocketFrameHandler frameHandler = channel.pipeline().get(WebSocketFrameHandler.class);
            if (frameHandler == null) {
                logger.info("Channel {}", IOHelper.getIP(channel.remoteAddress()));
                return;
            }
            Client client = frameHandler.getClient();
            String ip = frameHandler.context.ip != null ? frameHandler.context.ip : IOHelper.getIP(channel.remoteAddress());
            if (!client.isAuth)
                logger.info("Channel {} | connectUUID {} | checkSign {}", ip, frameHandler.getConnectUUID(), client.checkSign ? "true" : "false");
            else {
                logger.info("Client name {} | ip {} | connectUUID {}", client.username == null ? "null" : client.username, ip, frameHandler.getConnectUUID());
                logger.info("userUUID: {}", client.uuid == null ? "null" : client.uuid.toString());
                logger.info("OAuth session {}", client.sessionObject == null ? "null" : client.sessionObject);
                logger.info("Data: checkSign {} | auth_id {}", client.checkSign ? "true" : "false",
                        client.auth_id);
            }
            if (client.trustLevel != null) {
                logger.info("trustLevel | key {} | pubkey {}", client.trustLevel.keyChecked ? "checked" : "unchecked", client.trustLevel.publicKey == null ? "null" : new String(Base64.getEncoder().encode(client.trustLevel.publicKey)));
            }
            if (client.permissions != null) {
                logger.info("Permissions: {}", client.permissions.toString());
            }
        }));
    }
}
