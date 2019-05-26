package ru.gravit.launchserver.command.handler;


import com.vk.api.sdk.client.ClientResponse;
import com.vk.api.sdk.client.TransportClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import com.vk.api.sdk.objects.UserAuthResponse;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.command.Command;
import com.vk.api.sdk.client.VkApiClient;
import ru.gravit.utils.helper.LogHelper;

import java.io.File;
import java.io.IOException;


public class OAuthTokenGet extends Command {

    TransportClient transportClient = new HttpTransportClient();
    VkApiClient vk = new VkApiClient(transportClient);

    protected OAuthTokenGet(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return "Code";
    }

    @Override
    public String getUsageDescription() {
        return "Возвращает access token в обмен на code";
    }

    @Override
    public void invoke(String... args) {
        String code = args[0];
        UserAuthResponse authResponse = mToken(code, vk);
        LogHelper.subInfo(authResponse.getAccessToken());
    }

    public static UserAuthResponse mToken(String code, VkApiClient vk) {
        UserAuthResponse authResponse= null;
        try {
            authResponse = vk.oAuth().
                    userAuthorizationCodeFlow(LaunchServer.server.config.OAuthAppID,
                            LaunchServer.server.config.OAuthAppSecret,
                            LaunchServer.server.config.getOAuthBackURL(),
                            code).execute();
            return authResponse;
        } catch (ApiException e) {
            e.printStackTrace();
        } catch (ClientException e) {
            e.printStackTrace();
        }
    }
}
