package pro.gravit.launchserver.socket.response.auth;

import io.netty.channel.ChannelHandlerContext;
import pro.gravit.launcher.base.events.request.SetProfileRequestEvent;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.SimpleResponse;
import pro.gravit.utils.HookException;

import java.util.UUID;

public class SetProfileResponse extends SimpleResponse {
    public UUID uuid;
    public String tag;

    @Override
    public String getType() {
        return "setProfile";
    }

    @Override
    public void execute(ChannelHandlerContext ctx, Client client) {
        try {
            server.authHookManager.setProfileHook.hook(this, client);
        } catch (HookException e) {
            sendError(e.getMessage());
        }
        var profile = server.config.profilesProvider.get(uuid, tag);
        if(profile == null) {
            sendError("Profile not found");
            return;
        }
        client.profile = profile;
        sendResult(new SetProfileRequestEvent(profile.getProfile(), profile.getTag()));
    }

    @Override
    public ThreadSafeStatus getThreadSafeStatus() {
        return ThreadSafeStatus.READ_WRITE;
    }
}
