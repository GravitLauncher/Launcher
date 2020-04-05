package pro.gravit.launchserver.socket.response.auth;

import io.netty.channel.ChannelHandlerContext;
import pro.gravit.launcher.ClientPermissions;
import pro.gravit.launcher.events.RequestEvent;
import pro.gravit.launcher.events.request.ExitRequestEvent;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.handlers.WebSocketFrameHandler;
import pro.gravit.launchserver.socket.response.SimpleResponse;

public class ExitResponse extends SimpleResponse {
    public boolean exitAll;
    public String username;

    @Override
    public String getType() {
        return "exit";
    }

    @Override
    public void execute(ChannelHandlerContext ctx, Client client) throws Exception {
        if (username != null && (!client.isAuth || client.permissions == null || !client.permissions.isPermission(ClientPermissions.PermissionConsts.ADMIN))) {
            sendError("Permissions denied");
            return;
        }
        if (username == null) {
            if (client.session == 0 && exitAll) {
                sendError("Session invalid");
                return;
            }
            WebSocketFrameHandler handler = ctx.pipeline().get(WebSocketFrameHandler.class);
            if (handler == null) {
                sendError("Exit internal error");
                return;
            }
            Client newClient = new Client(0);
            newClient.checkSign = client.checkSign;
            handler.setClient(newClient);
            if (client.session != 0) server.sessionManager.removeClient(client.session);
            if (exitAll) {
                service.channels.forEach((channel) -> {
                    if (channel == null || channel.pipeline() == null) return;
                    WebSocketFrameHandler wsHandler = channel.pipeline().get(WebSocketFrameHandler.class);
                    if (wsHandler == null || wsHandler == handler) return;
                    Client chClient = wsHandler.getClient();
                    if (client.isAuth && client.username != null) {
                        if (!chClient.isAuth || !client.username.equals(chClient.username)) return;
                    } else {
                        if (chClient.session != client.session) return;
                    }
                    Client newCusClient = new Client(0);
                    newCusClient.checkSign = chClient.checkSign;
                    wsHandler.setClient(newCusClient);
                    if (chClient.session != 0) server.sessionManager.removeClient(chClient.session);
                    ExitRequestEvent event = new ExitRequestEvent(ExitRequestEvent.ExitReason.SERVER);
                    event.requestUUID = RequestEvent.eventUUID;
                    wsHandler.service.sendObject(channel, event);
                });
            }
            sendResult(new ExitRequestEvent(ExitRequestEvent.ExitReason.CLIENT));
        } else {
            service.channels.forEach((channel -> {
                if (channel == null || channel.pipeline() == null) return;
                WebSocketFrameHandler wsHandler = channel.pipeline().get(WebSocketFrameHandler.class);
                if (wsHandler == null) return;
                Client chClient = wsHandler.getClient();
                if (!chClient.isAuth || !username.equals(chClient.username)) return;
                Client newCusClient = new Client(0);
                newCusClient.checkSign = chClient.checkSign;
                wsHandler.setClient(newCusClient);
                if (chClient.session != 0) server.sessionManager.removeClient(chClient.session);
                ExitRequestEvent event = new ExitRequestEvent(ExitRequestEvent.ExitReason.SERVER);
                event.requestUUID = RequestEvent.eventUUID;
                wsHandler.service.sendObject(channel, event);
            }));
            sendResult(new ExitRequestEvent(ExitRequestEvent.ExitReason.NO_EXIT));
        }
    }
}
