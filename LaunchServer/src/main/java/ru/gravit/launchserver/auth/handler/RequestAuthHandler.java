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
        String uuidTOstring = uuid.toString();
        return IOHelper.request(new URL(CommonHelper.replace(url, "type", "GetUsername", "uuid", IOHelper.urlEncode(uuidTOstring))));
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
