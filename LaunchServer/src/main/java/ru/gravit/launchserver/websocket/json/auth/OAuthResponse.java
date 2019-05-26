package ru.gravit.launchserver.websocket.json.auth;

import io.netty.channel.ChannelHandlerContext;
import ru.gravit.launcher.OshiHWID;
import ru.gravit.launcher.events.request.OAuthRequestEvent;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.manangers.OAuthManager;
import ru.gravit.launchserver.socket.Client;
import ru.gravit.launchserver.websocket.json.SimpleResponse;
import ru.gravit.utils.HookException;
import ru.gravit.utils.helper.LogHelper;

import java.net.MalformedURLException;
import java.net.URL;

public class OAuthResponse extends SimpleResponse {

    public OshiHWID hwid;

    @Override
    public void execute(ChannelHandlerContext ctx, Client clientData) throws Exception {
        try {
            OAuthManager.stretchCache(clientData, ctx);
            OAuthRequestEvent result = new OAuthRequestEvent();
            result.URL = getAcsessURL();
            sendResult(result);
        } catch (HookException e) {
            sendError(e.getMessage());
        }
    }
    public URL getAcsessURL() throws MalformedURLException {

       String url =  "http://oauth.vk.com/authorize?client_id=" + LaunchServer.server.config.OAuthAppID + "&display=page&scope=offline&response_type=code&v=5.69&redirect_uri=" + LaunchServer.server.config.getOAuthBackURL();
        return new URL(url);
    }

    @Override
    public String getType() {
        return "oauth";
    }

}
