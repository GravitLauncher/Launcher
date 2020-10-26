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
        if(session == null) {
            sendError("Session invalid");
            return;
        }
        final Client[] rClient = {null};
        service.forEachActiveChannels((channel, handler) -> {
            Client c = handler.getClient();
            if(c != null && session.equals(c.session)) {
                rClient[0] = c;
            }
        });
        if(rClient[0] == null) {
            rClient[0] = server.sessionManager.getClient(session);
        }
        if (rClient[0] == null) {
            sendError("Session invalid");
            return;
        }
        WebSocketFrameHandler frameHandler = ctx.pipeline().get(WebSocketFrameHandler.class);
        frameHandler.setClient(rClient[0]);
        if (needUserInfo) {
            sendResult(new RestoreSessionRequestEvent(CurrentUserResponse.collectUserInfoFromClient(rClient[0])));
        } else {
            sendResult(new RestoreSessionRequestEvent());
        }
    }
}
