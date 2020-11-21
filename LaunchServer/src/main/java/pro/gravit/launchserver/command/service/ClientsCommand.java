package pro.gravit.launchserver.command.service;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.WebSocketService;
import pro.gravit.launchserver.socket.handlers.WebSocketFrameHandler;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

import java.util.Base64;

public class ClientsCommand extends Command {
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
            Client client = frameHandler.getClient();
            String ip = frameHandler.context.ip != null ? frameHandler.context.ip : IOHelper.getIP(channel.remoteAddress());
            if (!client.isAuth)
                LogHelper.info("Channel %s | connectUUID %s | checkSign %s", ip, frameHandler.getConnectUUID(), client.checkSign ? "true" : "false");
            else {
                LogHelper.info("Client name %s | ip %s | connectUUID %s", client.username == null ? "null" : client.username, ip, frameHandler.getConnectUUID());
                LogHelper.subInfo("userUUID: %s | session %s", client.uuid == null ? "null" : client.uuid.toString(), client.session == null ? "null" : client.session);
                LogHelper.subInfo("Data: checkSign %s | auth_id %s", client.checkSign ? "true" : "false",
                        client.auth_id);
                if(client.trustLevel != null) {
                    LogHelper.subInfo("trustLevel | key %s | pubkey %s", client.trustLevel.keyChecked ? "checked" : "unchecked", client.trustLevel.publicKey == null ? "null" : Base64.getEncoder().encode(client.trustLevel.publicKey));
                }
                LogHelper.subInfo("Permissions: %s (permissions %d | flags %d)", client.permissions == null ? "null" : client.permissions.toString(), client.permissions == null ? 0 : client.permissions.permissions, client.permissions == null ? 0 : client.permissions.flags);
            }
        }));
    }
}
