package ru.gravit.launchserver.socket.websocket.json.auth;

import io.netty.channel.ChannelHandlerContext;
import ru.gravit.launcher.LauncherNetworkAPI;
import ru.gravit.launcher.events.request.ErrorRequestEvent;
import ru.gravit.launcher.events.request.RestoreSessionRequestEvent;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.socket.Client;
import ru.gravit.launchserver.socket.websocket.WebSocketFrameHandler;
import ru.gravit.launchserver.socket.websocket.WebSocketService;
import ru.gravit.launchserver.socket.websocket.json.JsonResponseInterface;

public class RestoreSessionResponse implements JsonResponseInterface {
    @LauncherNetworkAPI
    public long session;
    @Override
    public String getType() {
        return "restoreSession";
    }

    @Override
    public void execute(WebSocketService service, ChannelHandlerContext ctx, Client client) throws Exception {
        Client rClient = LaunchServer.server.sessionManager.getClient(session);
        if(rClient == null)
        {
            service.sendObject(ctx, new ErrorRequestEvent("Session invalid"));
        }
        WebSocketFrameHandler frameHandler = ctx.pipeline().get(WebSocketFrameHandler.class);
        frameHandler.setClient(rClient);
        service.sendObject(ctx, new RestoreSessionRequestEvent());
    }
}
