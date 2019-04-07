package ru.gravit.launchserver.auth.handler;

import ru.gravit.launchserver.auth.provider.AuthProviderResult;
import ru.gravit.utils.helper.CommonHelper;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.net.URL;
import java.util.UUID;

public final class RequestAuthHandler extends AuthHandler {
    // Из конфига строки
    private String urlGetAll;
    private String urlGetUUID;
    private String urlGetUsername;
    private String urlUpdateAccessToken;
    private String urlUpdateServerID;
    // TODO Обьеденить все

    @Override
    public void init() {
        if (urlGetAll == null) LogHelper.error("[Verify][AuthHandler] urlGetAll cannot be null");
        if (urlGetUUID == null) LogHelper.error("[Verify][AuthHandler] urlGetUUID cannot be null");
        if (urlGetUsername == null) LogHelper.error("[Verify][AuthHandler] urlGetUsername cannot be null");
    }

    @Override
    public UUID auth(AuthProviderResult authResult) throws IOException {
        //TODO вот тут не понял поэтому пусть пока что возращает рандом uuid
        return UUID.randomUUID();
    }

    @Override
    public UUID checkServer(String username, String serverID) throws IOException {
        String currentResponse = IOHelper.request(new URL(CommonHelper.replace(urlGetUUID, "type", "GetUUID", "username", IOHelper.urlEncode(username))));
        UUID stringTOuuid = UUID.fromString(currentResponse);
        return stringTOuuid;
    }

    @Override
    public boolean joinServer(String username, String accessToken, String serverID) throws IOException {
        String currentResponse = IOHelper.request(new URL(CommonHelper.replace(urlGetAll, "type", "GetAll", "username", IOHelper.urlEncode(username), "accessToken", IOHelper.urlEncode(accessToken), "serverID", IOHelper.urlEncode(serverID))));
        String[] joinServerParams = currentResponse.split(":");
        if (joinServerParams[0] == username && joinServerParams[1] == accessToken) {
            if (joinServerParams[2] == serverID || joinServerParams == null) {
                return true;
            }
        }
        return false;
    }

    @Override
    public UUID usernameToUUID(String username) throws IOException {
        String currentResponse = IOHelper.request(new URL(CommonHelper.replace(urlGetUUID, "type", "GetUUID", "username", IOHelper.urlEncode(username))));
        UUID stringTOuuid = UUID.fromString(currentResponse);
        return stringTOuuid;
    }

    @Override
    public String uuidToUsername(UUID uuid) throws IOException {
        String uuidTOstring = uuid.toString();
        String currentResponse = IOHelper.request(new URL(CommonHelper.replace(urlGetUsername, "type", "GetUsername", "uuid", IOHelper.urlEncode(uuidTOstring))));
        return currentResponse;
    }

    @Override
    public void close() {
        // Ничего не делать
    }
}
