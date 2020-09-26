package pro.gravit.launchserver.socket.response.profile;

import io.netty.channel.ChannelHandlerContext;
import pro.gravit.launcher.events.request.BatchProfileByUsernameRequestEvent;
import pro.gravit.launcher.profiles.PlayerProfile;
import pro.gravit.launchserver.auth.AuthProviderPair;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.SimpleResponse;

import java.util.UUID;

public class BatchProfileByUsername extends SimpleResponse {
    Entry[] list;

    @Override
    public String getType() {
        return "batchProfileByUsername";
    }

    @Override
    public void execute(ChannelHandlerContext ctx, Client client) throws Exception {
        BatchProfileByUsernameRequestEvent result = new BatchProfileByUsernameRequestEvent();
        if (list == null) {
            sendError("Invalid request");
            return;
        }
        result.playerProfiles = new PlayerProfile[list.length];
        for (int i = 0; i < list.length; ++i) {
            UUID uuid;
            AuthProviderPair pair = client.auth;
            if (pair == null) {
                pair = server.config.getAuthProviderPair();
            }
            uuid = pair.handler.usernameToUUID(list[i].username);
            result.playerProfiles[i] = ProfileByUUIDResponse.getProfile(uuid, list[i].username, list[i].client, pair.textureProvider);
        }
        sendResult(result);
    }

    static class Entry {
        String username;
        String client;
    }
}
