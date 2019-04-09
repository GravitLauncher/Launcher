package ru.gravit.launchserver.auth.handler;

import ru.gravit.utils.helper.CommonHelper;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.UUID;

public final class RequestAuthHandler extends CachedAuthHandler {
    private String url;

    @Override
    public void init() {
        if (url == null) LogHelper.error("[Verify][AuthHandler] url cannot be null");
    }

    @Override
    protected Entry fetchEntry(UUID uuid) throws IOException {
        // Входные данные - uuid     ^, выходные - username
        String uuidTOstring = uuid.toString();
        //        вот запрос например этот выглядит так: localhost/auth.php?type =  GetUsername &  uuid =         переменная uuidTOstring
        return IOHelper.request(new URL(CommonHelper.replace(url, "type", "GetUsername", "uuid", IOHelper.urlEncode(uuidTOstring))));
    }

    @Override
    protected Entry fetchEntry(String username) throws IOException {
        // тут надо с точностью наоборот как выше, вместо uuid входные данные username
        return null;
    }

    @Override
    protected boolean updateAuth(UUID uuid, String username, String accessToken) throws IOException {
        return false;
        // тут и ниже пока не трогай
    }

    @Override
    protected boolean updateServerID(UUID uuid, String serverID) throws IOException {
        return false;
    }


    /*
    @Override
    public UUID usernameToUUID(String username) throws IOException {
        String currentResponse = IOHelper.request(new URL(CommonHelper.replace(url, "type", "GetUUID", "username", IOHelper.urlEncode(username))));
        UUID stringTOuuid = UUID.fromString(currentResponse);
        return stringTOuuid;
    }

    @Override
    public String uuidToUsername(UUID uuid) throws IOException {
        String uuidTOstring = uuid.toString();
        String currentResponse = IOHelper.request(new URL(CommonHelper.replace(url, "type", "GetUsername", "uuid", IOHelper.urlEncode(uuidTOstring))));
        return currentResponse;
    }
    */
    @Override
    public void close() {
        // Ничего не делать
    }
}
