package pro.gravit.launchserver.socket.response.management;

import io.netty.channel.ChannelHandlerContext;
import pro.gravit.launcher.base.events.request.GetPublicKeyRequestEvent;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.SimpleResponse;

public class GetPublicKeyResponse extends SimpleResponse {
    @Override
    public String getType() {
        return "getPublicKey";
    }

    @Override
    public void execute(ChannelHandlerContext ctx, Client client) {
        sendResult(new GetPublicKeyRequestEvent(server.keyAgreementManager.rsaPublicKey, server.keyAgreementManager.ecdsaPublicKey));
    }
}
