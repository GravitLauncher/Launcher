package pro.gravit.launchserver.socket.response.auth;

import io.netty.channel.ChannelHandlerContext;
import pro.gravit.launcher.LauncherNetworkAPI;
import pro.gravit.launcher.events.request.RestoreSessionRequestEvent;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.handlers.WebSocketFrameHandler;
import pro.gravit.launchserver.socket.response.SimpleResponse;

import java.util.UUID;

public class RestoreSessionResponse extends SimpleResponse {
    @LauncherNetworkAPI
    public UUID session;
    public boolean needUserInfo;

    @Override
    public String getType() {
        return "restoreSession";
    }

    @Override
    public void execute(ChannelHandlerContext ctx, Client client) throws Exception {
        Client rClient = server.sessionManager.getClient(session);
        if (rClient == null) {
            sendError("Session invalid");
            return;
        }
        WebSocketFrameHandler frameHandler = ctx.pipeline().get(WebSocketFrameHandler.class);
        frameHandler.setClient(rClient);
        if(needUserInfo)
        {
            sendResult(new RestoreSessionRequestEvent(CurrentUserResponse.collectUserInfoFromClient(rClient)));
        }
        else
        {
            sendResult(new RestoreSessionRequestEvent());
        }
    }
}
