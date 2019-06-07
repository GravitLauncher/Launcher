package pro.gravit.launchserver.websocket.json;

import java.util.UUID;

import io.netty.channel.ChannelHandlerContext;
import pro.gravit.launcher.events.RequestEvent;
import pro.gravit.launcher.events.request.ErrorRequestEvent;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.websocket.WebSocketService;

public abstract class SimpleResponse implements JsonResponseInterface {
    public UUID requestUUID;
    public transient LaunchServer server;
    public transient WebSocketService service;
    public transient ChannelHandlerContext ctx;
    public transient String ip;

    public void sendResult(RequestEvent result) {
        result.requestUUID = requestUUID;
        service.sendObject(ctx, result);
    }

    public void sendResultAndClose(RequestEvent result) {
        result.requestUUID = requestUUID;
        service.sendObjectAndClose(ctx, result);
    }

    public void sendError(String errorMessage) {
        ErrorRequestEvent event = new ErrorRequestEvent(errorMessage);
        event.requestUUID = requestUUID;
        service.sendObject(ctx, event);
    }

    @Override
    public void execute(ChannelHandlerContext ctx, Client client) throws Exception {

    }
}
