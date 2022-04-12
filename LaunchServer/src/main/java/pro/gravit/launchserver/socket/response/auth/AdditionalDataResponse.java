package pro.gravit.launchserver.socket.response.auth;

import io.netty.channel.ChannelHandlerContext;
import pro.gravit.launcher.ClientPermissions;
import pro.gravit.launcher.events.request.AdditionalDataRequestEvent;
import pro.gravit.launchserver.auth.AuthProviderPair;
import pro.gravit.launchserver.auth.core.User;
import pro.gravit.launchserver.auth.core.interfaces.user.UserSupportAdditionalData;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.SimpleResponse;

import java.util.Map;
import java.util.UUID;

@SuppressWarnings("deprecation")
public class AdditionalDataResponse extends SimpleResponse {
    public String username;
    public UUID uuid;

    @Override
    public String getType() {
        return "additionalData";
    }

    @Override
    public void execute(ChannelHandlerContext ctx, Client client) throws Exception {
        if (!client.isAuth) {
            sendError("Access denied");
            return;
        }
        AuthProviderPair pair = client.auth;
        if (username == null && uuid == null) {
            Map<String, String> properties;
            User user = client.getUser();
            if (user instanceof UserSupportAdditionalData userSupport) {
                if (client.permissions.hasPerm("launchserver.request.addionaldata.privileged")) {
                    properties = userSupport.getPropertiesMap();
                } else {
                    properties = userSupport.getPropertiesMapUnprivilegedSelf();
                }
            } else {
                properties = Map.of();
            }
            sendResult(new AdditionalDataRequestEvent(properties));
            return;
        }
        User user;
        if (username != null) {
            user = pair.core.getUserByUsername(username);
        } else {
            user = pair.core.getUserByUUID(uuid);
        }
        if (!(user instanceof UserSupportAdditionalData userSupport)) {
            sendResult(new AdditionalDataRequestEvent(Map.of()));
            return;
        }
        Map<String, String> properties;
        if (client.permissions.hasPerm("launchserver.request.addionaldata.privileged")) {
            properties = userSupport.getPropertiesMap();
        } else {
            properties = userSupport.getPropertiesMapUnprivileged();
        }
        sendResult(new AdditionalDataRequestEvent(properties));
    }
}
