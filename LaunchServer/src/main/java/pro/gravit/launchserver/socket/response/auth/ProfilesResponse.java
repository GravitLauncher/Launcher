package pro.gravit.launchserver.socket.response.auth;

import io.netty.channel.ChannelHandlerContext;
import pro.gravit.launcher.base.events.request.ProfilesRequestEvent;
import pro.gravit.launcher.base.profiles.ClientProfile;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.protect.interfaces.ProfilesProtectHandler;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.SimpleResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ProfilesResponse extends SimpleResponse {
    @Deprecated
    public static List<ClientProfile> getListVisibleProfiles(LaunchServer server, Client client) {
        List<ClientProfile> profileList;
        Set<ClientProfile> serverProfiles = server.getProfiles();
        if (server.config.protectHandler instanceof ProfilesProtectHandler protectHandler) {
            profileList = new ArrayList<>(4);
            for (ClientProfile profile : serverProfiles) {
                if (protectHandler.canGetProfile(profile, client)) {
                    profileList.add(profile);
                }
            }
        } else {
            profileList = List.copyOf(serverProfiles);
        }
        return profileList;
    }

    @Override
    public String getType() {
        return "profiles";
    }

    @Override
    public void execute(ChannelHandlerContext ctx, Client client) {
        if (server.config.protectHandler instanceof ProfilesProtectHandler profilesProtectHandler && !profilesProtectHandler.canGetProfiles(client)) {
            sendError("Access denied");
            return;
        }
        sendResult(new ProfilesRequestEvent(server.config.profileProvider.getProfiles(client)));
    }
}
