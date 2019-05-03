package ru.gravit.launchserver.websocket.json.admin;

import io.netty.channel.ChannelHandlerContext;
import ru.gravit.launchserver.socket.Client;
import ru.gravit.launchserver.websocket.json.JsonResponseInterface;
import ru.gravit.launchserver.websocket.json.SimpleResponse;

public class ProxyCommandResponse extends SimpleResponse {
    public JsonResponseInterface response;
    public long session;
    @Override
    public void execute(ChannelHandlerContext ctx, Client client) throws Exception {
        if(!client.proxy) {
            sendError("Proxy server error");
            return;
        }
        Client real_client = server.sessionManager.getOrNewClient(session);
        response.execute(ctx, real_client);
    }

    @Override
    public String getType() {
        return "proxy";
    }
}
