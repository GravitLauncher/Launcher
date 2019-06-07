package pro.gravit.launchserver.websocket.json.auth;

import java.util.Collection;

import io.netty.channel.ChannelHandlerContext;
import pro.gravit.launcher.events.request.SetProfileRequestEvent;
import pro.gravit.launcher.profiles.ClientProfile;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.websocket.json.SimpleResponse;
import pro.gravit.utils.HookException;

public class SetProfileResponse extends SimpleResponse {
    public String client;

    @Override
    public String getType() {
        return "setProfile";
    }

    @Override
    public void execute(ChannelHandlerContext ctx, Client client) {
        if (!client.isAuth) {
            sendError("Access denied");
            return;
        }
        try {
            server.authHookManager.setProfileHook.hook(this, client);
        } catch (HookException e) {
            sendError(e.getMessage());
        }
        Collection<ClientProfile> profiles = server.getProfiles();
        for (ClientProfile p : profiles) {
            if (p.getTitle().equals(this.client)) {
                if (!p.isWhitelistContains(client.username)) {
                    sendError(server.config.whitelistRejectString);
                    return;
                }
                client.profile = p;
                sendResult(new SetProfileRequestEvent(p));
                return;
            }
        }
        sendError("Profile not found");
    }
}
