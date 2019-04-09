package ru.gravit.launchserver.auth.handler;

import ru.gravit.utils.helper.CommonHelper;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class RequestAuthHandler extends CachedAuthHandler {
    private transient String url;

    @Override
    public void init() {
        if (url == null) LogHelper.error("[Verify][AuthHandler] url cannot be null");
    }

    @Override
    protected Entry fetchEntry(UUID uuid) throws IOException {
        throw new UnsupportedOperationException("Произошол троллинг...");
    }

    @Override
    protected Entry fetchEntry(String username) throws IOException {
        throw new UnsupportedOperationException("Произошол троллинг...");
    }

    @Override
    protected boolean updateAuth(UUID uuid, String username, String accessToken) throws IOException {
        throw new UnsupportedOperationException("Произошол троллинг...");
    }

    @Override
    protected boolean updateServerID(UUID uuid, String serverID) throws IOException {
        return proceedServerID(uuid, serverID).equals("ok");
    }

    protected String proceedServerID(final UUID uuid, final String serverID) throws IOException {
        final String type = "SetServerID";
        final String encodedUUID = IOHelper.urlEncode(Objects.toString(uuid));
        final String encodedID = IOHelper.urlEncode(serverID);
        final URL formattedUrl = new URL(
                CommonHelper.replace(url,
                        "type",
                        type,
                        "uuid",
                        encodedUUID,
                        "ServerID",
                        encodedID));

        return IOHelper.request(formattedUrl);
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
