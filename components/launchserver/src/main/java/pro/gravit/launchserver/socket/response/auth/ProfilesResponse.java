package pro.gravit.launchserver.socket.response.auth;

import io.netty.channel.ChannelHandlerContext;
import pro.gravit.launcher.base.events.request.ProfilesRequestEvent;
import pro.gravit.launcher.base.profiles.ClientProfile;
import pro.gravit.launcher.base.profiles.ClientProfileBuilder;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.profiles.ProfilesProvider;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.SimpleResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ProfilesResponse extends SimpleResponse {

    public static List<ClientProfile> getListVisibleProfiles(LaunchServer server, Client client) {
        Set<ProfilesProvider.UncompletedProfile> serverProfiles = server.config.profilesProvider.getProfiles(client);
        List<ClientProfile> profiles = new ArrayList<>();
        for(var uncompleted : serverProfiles) {
            if(uncompleted instanceof ProfilesProvider.CompletedProfile completed) {
                profiles.add(completed.getProfile());
            } else {
                profiles.add(new ClientProfileBuilder()
                                .setUuid(uncompleted.getUuid())
                                .setTitle(uncompleted.getName())
                                .setInfo(uncompleted.getDescription())
                        .createClientProfile());
            }
        }
        return profiles;
    }

    @Override
    public String getType() {
        return "profiles";
    }

    @Override
    public void execute(ChannelHandlerContext ctx, Client client) {
        sendResult(new ProfilesRequestEvent(getListVisibleProfiles(server, client)));
    }
}
