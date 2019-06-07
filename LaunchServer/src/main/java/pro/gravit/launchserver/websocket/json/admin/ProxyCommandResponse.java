package pro.gravit.launchserver.websocket.json.admin;

import io.netty.channel.ChannelHandlerContext;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.websocket.json.JsonResponseInterface;
import pro.gravit.launchserver.websocket.json.SimpleResponse;

public class ProxyCommandResponse extends SimpleResponse {
    public JsonResponseInterface response;
    public long session;
    public boolean isCheckSign;

    @Override
    public void execute(ChannelHandlerContext ctx, Client client) throws Exception {
        if (!client.proxy) {
            sendError("Proxy server error");
            return;
        }
        Client real_client = server.sessionManager.getOrNewClient(session);
        real_client.checkSign = isCheckSign;
        response.execute(ctx, real_client);
    }

    @Override
    public String getType() {
        return "proxy";
    }
}
