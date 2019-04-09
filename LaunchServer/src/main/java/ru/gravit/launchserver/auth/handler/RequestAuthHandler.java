package ru.gravit.launchserver.auth.handler;

import ru.gravit.utils.helper.CommonHelper;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.UUID;

public final class RequestAuthHandler extends CachedAuthHandler {
    private String url;

    @Override
    public void init() {
        if (url == null) LogHelper.error("[Verify][AuthHandler] url cannot be null");
    }

    @Override
    protected Entry fetchEntry(UUID uuid) throws IOException {
        String username = IOHelper.request(new URL(CommonHelper.replace(url, "type", "GetUsername", "uuid", IOHelper.urlEncode(uuid.toString()))));
        String accessToken = IOHelper.request(new URL(CommonHelper.replace(url, "type", "GetAccessToken", "uuid", IOHelper.urlEncode(uuid.toString()))));
        String serverID = IOHelper.request(new URL(CommonHelper.replace(url, "type", "GetServerID", "uuid", IOHelper.urlEncode(uuid.toString()))));
        return new Entry(uuid, username, accessToken, serverID);
    }

    @Override
    protected Entry fetchEntry(String username) throws IOException {
        String GettedUUID = IOHelper.request(new URL(CommonHelper.replace(url, "type", "GetUUID", "username", IOHelper.urlEncode(username))));
        UUID uuid = UUID.fromString(GettedUUID);
        String accessToken = IOHelper.request(new URL(CommonHelper.replace(url, "type", "GetAccessToken", "username", IOHelper.urlEncode(username))));
        String serverID = IOHelper.request(new URL(CommonHelper.replace(url, "type", "GetServerID", "username", IOHelper.urlEncode(username))));
        return new Entry(uuid, username, accessToken, serverID);
    }

    @Override
    protected boolean updateAuth(UUID uuid, String username, String accessToken) throws IOException {
        boolean isUUIDupdated = updUUID(uuid, username).equals("OK");
        boolean isAccessTokenUpdated = updAccessToken(accessToken, username).equals("OK");
        return isUUIDupdated && isAccessTokenUpdated;
    }

    @Override
    protected boolean updateServerID(UUID uuid, String serverID) throws IOException {
        return updServerID(uuid, serverID).equals("OK");
    }

    protected String updAccessToken(final String accessToken, final String username) throws IOException {
        return IOHelper.request(new URL(CommonHelper.replace(url, "type", "SetAccessToken", "username", IOHelper.urlEncode(Objects.toString(username)), "uuid", IOHelper.urlEncode(Objects.toString(accessToken)))));
    }

    protected String updServerID(final UUID uuid, final String serverID) throws IOException {
        return IOHelper.request(new URL(CommonHelper.replace(url, "type", "SetServerID", "uuid", IOHelper.urlEncode(Objects.toString(uuid)), "ServerID", IOHelper.urlEncode(serverID))));
    }

    protected String updUUID(final UUID uuid, final String username) throws IOException {
        return IOHelper.request(new URL(CommonHelper.replace(url, "type", "SetUUID", "username", IOHelper.urlEncode(Objects.toString(username)), "uuid", IOHelper.urlEncode(Objects.toString(uuid)))));
    }

    @Override
    public void close() {}
}