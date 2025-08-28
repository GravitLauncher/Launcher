package pro.gravit.launchserver.socket.response.auth;

import io.netty.channel.ChannelHandlerContext;
import pro.gravit.launcher.base.events.request.FetchClientProfileKeyRequestEvent;
import pro.gravit.launchserver.auth.core.UserSession;
import pro.gravit.launchserver.auth.core.interfaces.session.UserSessionSupportKeys;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.SimpleResponse;

public class FetchClientProfileKeyResponse extends SimpleResponse {
    @Override
    public String getType() {
        return "clientProfileKey";
    }

    @Override
    public void execute(ChannelHandlerContext ctx, Client client) {
        if (!client.isAuth || client.type != AuthResponse.ConnectTypes.CLIENT) {
            sendError("Permissions denied");
            return;
        }
        UserSession session = client.sessionObject;
        UserSessionSupportKeys.ClientProfileKeys keys;
        if (session instanceof UserSessionSupportKeys support) {
            keys = support.getClientProfileKeys();
        } else {
            keys = server.authManager.createClientProfileKeys(client.uuid);
        }
        sendResult(new FetchClientProfileKeyRequestEvent(keys.publicKey(), keys.privateKey(), keys.signature(), keys.expiresAt(), keys.refreshedAfter()));
    }
}
