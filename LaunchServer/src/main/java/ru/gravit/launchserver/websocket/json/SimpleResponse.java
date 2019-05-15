package ru.gravit.launchserver.websocket.json;

import io.netty.channel.ChannelHandlerContext;
import ru.gravit.launcher.events.RequestEvent;
import ru.gravit.launcher.events.request.ErrorRequestEvent;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.socket.Client;
import ru.gravit.launchserver.websocket.WebSocketService;

import java.util.UUID;

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
