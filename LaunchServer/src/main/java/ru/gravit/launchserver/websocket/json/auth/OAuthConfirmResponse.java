package ru.gravit.launchserver.websocket.json.auth;

import com.vk.api.sdk.client.TransportClient;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import com.vk.api.sdk.objects.UserAuthResponse;
import io.netty.channel.ChannelHandlerContext;

import ru.gravit.launcher.events.request.OAuthConfirmRequestEvent;

import ru.gravit.launchserver.LaunchServer;

import ru.gravit.launchserver.command.handler.OAuthTokenGet;
import ru.gravit.launchserver.manangers.OAuthManager;
import ru.gravit.launchserver.socket.Client;
import ru.gravit.launchserver.websocket.json.SimpleResponse;
import ru.gravit.utils.HookException;


public class OAuthConfirmResponse extends SimpleResponse {

    TransportClient transportClient = new HttpTransportClient();
    VkApiClient vk = new VkApiClient(transportClient);

    public String code;

    @Override
    public void execute(ChannelHandlerContext ctx, Client clientData) throws Exception {
        try {
            if(code == null) {
                try {
                    throw new OAuthManager.OAuthException("Empty code");
                }catch (OAuthManager.OAuthException e) {
                    sendError(e.getMessage());
                }
            }
            //LogHelper.debug("code get");

            OAuthTokenGet.mToken(code, vk);
            int ID = authResponse.getUserId();

            OAuthConfirmRequestEvent result = new OAuthConfirmRequestEvent();
            result.str = "Continue in Launcher";
            sendResultAndClose(result);

        } catch (HookException e) {
            sendError(e.getMessage());
        }
    }

    @Override
    public String getType() {
        return "OAuthURL";
    }
}


