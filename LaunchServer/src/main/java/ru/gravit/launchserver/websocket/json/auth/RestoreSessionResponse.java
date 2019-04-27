package ru.gravit.launchserver.websocket.json.auth;

import io.netty.channel.ChannelHandlerContext;
import ru.gravit.launcher.LauncherNetworkAPI;
import ru.gravit.launcher.events.request.RestoreSessionRequestEvent;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.socket.Client;
import ru.gravit.launchserver.websocket.WebSocketFrameHandler;
import ru.gravit.launchserver.websocket.json.SimpleResponse;

public class RestoreSessionResponse extends SimpleResponse {
    @LauncherNetworkAPI
    public long session;
    @Override
    public String getType() {
        return "restoreSession";
    }

    @Override
    public void execute(ChannelHandlerContext ctx, Client client) {
        Client rClient = LaunchServer.server.sessionManager.getClient(session);
        if(rClient == null)
        {
            sendError("Session invalid");
        }
        WebSocketFrameHandler frameHandler = ctx.pipeline().get(WebSocketFrameHandler.class);
        frameHandler.setClient(rClient);
        sendResult(new RestoreSessionRequestEvent());
    }
}
