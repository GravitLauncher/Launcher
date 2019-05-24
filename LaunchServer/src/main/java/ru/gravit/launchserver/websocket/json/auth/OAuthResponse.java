package ru.gravit.launchserver.websocket.json.auth;

import io.netty.channel.ChannelHandlerContext;
import ru.gravit.launcher.OshiHWID;
import ru.gravit.launcher.events.request.OAuthRequestEvent;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.socket.Client;
import ru.gravit.launchserver.websocket.json.SimpleResponse;
import ru.gravit.utils.HookException;

import java.net.MalformedURLException;
import java.net.URL;

public class OAuthResponse extends SimpleResponse {

    public OshiHWID hwid;

    @Override
    public void execute(ChannelHandlerContext ctx, Client clientData) throws Exception {
        try {
            OAuthRequestEvent result = new OAuthRequestEvent();
            result.URL = getAcsessURL();
            sendResult(result);
        } catch (HookException e) {
            sendError(e.getMessage());
        }
    }
    public URL getAcsessURL() throws MalformedURLException {

       String url =  "https://oauth.vk.com/authorize?client_id=" + LaunchServer.server.config.OAuthAppID + "&display=page&response_type=code&v=5.69&redirect_uri=" + LaunchServer.server.config.getOAuthBackURL();
        return new URL(url);
    }

    @Override
    public String getType() {
        return "oauth";
    }

}
