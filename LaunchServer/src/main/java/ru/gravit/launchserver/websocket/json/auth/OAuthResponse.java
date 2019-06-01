package ru.gravit.launchserver.websocket.json.auth;

import com.vk.api.sdk.client.TransportClient;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import com.vk.api.sdk.objects.UserAuthResponse;
import io.netty.channel.ChannelHandlerContext;

import ru.gravit.launcher.events.request.AuthRequestEvent;
import ru.gravit.launcher.events.request.LogEvent;

import ru.gravit.launcher.profiles.PlayerProfile;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.auth.AuthProviderPair;
import ru.gravit.launchserver.auth.provider.AuthProviderResult;
import ru.gravit.launchserver.command.handler.OAuthTokenGet;
import ru.gravit.launchserver.manangers.OAuthManager;
import ru.gravit.launchserver.socket.Client;
import ru.gravit.launchserver.websocket.json.SimpleResponse;
import ru.gravit.launchserver.websocket.json.profile.ProfileByUUIDResponse;
import ru.gravit.utils.helper.LogHelper;

import static java.lang.Thread.sleep;


public class OAuthResponse extends SimpleResponse {

    TransportClient transportClient = new HttpTransportClient();
    VkApiClient vk = new VkApiClient(transportClient);

    public String code;

    @Override
    public void execute(ChannelHandlerContext ctx, Client clientData) {
        try {
            if(code == "false") {
                try {
                    throw new OAuthManager.OAuthException("Empty code");
                }catch (OAuthManager.OAuthException e) {
                    sendError(e.getMessage());
                }
            }else if(code == null){
                OAuthManager.stretchCache(ctx, clientData);
                int trays = 0;
                OAuthManager.Entry inUSE = null;
                while(trays < 15) {
                    trays++;
                    try {
                       inUSE = OAuthManager.findEntry(ctx);
                       AuthRequestEvent event = inUSE.getAuthRequestEvent();
                       sendResult(event);
                       break;
                    }catch (OAuthManager.OAuthException e){
                        sleep(5000);
                        e.printStackTrace();
                    }
                } if(inUSE == null) {
                    sendError("TimedOut");
                    throw new OAuthManager.OAuthException("TimedOut");
                }
            }else {
                UserAuthResponse authResponse = OAuthTokenGet.mToken(code, vk);

                UserActor actor = new UserActor(authResponse.getUserId(), authResponse.getAccessToken());
                sendResultAndClose(new LogEvent("Continue in Launcher"));
                AuthProviderResult ar;
                AuthProviderPair ap;
                if (LaunchServer.server.config.getAuthProviderPair("MySQLAuthProvider") != null) {
                    ar = LaunchServer.server.config.getAuthProviderPair("MySQLAuthProvider").provider.oauth(actor.getId());
                    ap = LaunchServer.server.config.getAuthProviderPair("MySQLAuthProvider");
                } else {
                    ar = LaunchServer.server.config.getAuthProviderPair().provider.oauth(actor.getId());
                    ap = LaunchServer.server.config.getAuthProviderPair();
                }
                LogHelper.subDebug("found account " + ar.username);

                PlayerProfile playerProfile = ProfileByUUIDResponse.getProfile(LaunchServer.server, ap.handler.auth(ar), ar.username, null, ap.textureProvider);
                AuthRequestEvent a = new AuthRequestEvent(playerProfile, ar.accessToken, ar.permissions);
                OAuthManager.stretchCache(ctx, a);
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendError(e.getMessage());
        }
    }

    @Override
    public String getType() {
        return "OAuthURL";
    }
}


