package ru.gravit.launchserver.auth.handler;

import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.net.URL;
import java.util.UUID;

public final class RequestAuthHandler extends CachedAuthHandler {
    private String url;

    @Override
    public void init() {
        if (url == null) LogHelper.error("[Verify][AuthHandler] url cannot be null");
    }

    @Override
    protected Entry fetchEntry(UUID uuid) throws IOException {
        String username = IOHelper.request(new URL(url + "?type=GetUsername&uuid=" + IOHelper.urlEncode(uuid.toString())));
        String accessToken = IOHelper.request(new URL(url + "?type=GetAccessToken&uuid=" + IOHelper.urlEncode(uuid.toString())));
        String serverID = IOHelper.request(new URL(url + "?type=GetServerID&uuid=" + IOHelper.urlEncode(uuid.toString())));
        return new Entry(uuid, username, accessToken, serverID);
    }

    @Override
    protected Entry fetchEntry(String username) throws IOException {
        String GettedUUID = IOHelper.request(new URL(url + "?type=GetUUID&username=" + IOHelper.urlEncode(username)));
        UUID uuid = UUID.fromString(GettedUUID);
        String accessToken = IOHelper.request(new URL(url + "?type=GetAccessToken&username=" + IOHelper.urlEncode(username)));
        String serverID = IOHelper.request(new URL(url + "?type=GetServerID&username=" + IOHelper.urlEncode(username)));
        return new Entry(uuid, username, accessToken, serverID);
    }

    @Override
    protected boolean updateAuth(UUID uuid, String username, String accessToken) throws IOException {
        String response0 = IOHelper.request(new URL(url+ "?type=SetUUID&uuid=" + IOHelper.urlEncode(uuid.toString()) + "&username=" + IOHelper.urlEncode(username)));
        String response1 = IOHelper.request(new URL(url+ "?type=SetAccessToken&accessToken=" + IOHelper.urlEncode(accessToken) + "&username=" + IOHelper.urlEncode(username)));
        if (response0 == "OK" && response1 == "OK") {
            return true;
        }
        return false;
    }

    @Override
    protected boolean updateServerID(UUID uuid, String serverID) throws IOException {
        String response = IOHelper.request(new URL(url + "?type=SetServerID&uuid=" + IOHelper.urlEncode(uuid.toString()) + "&serverID=" + IOHelper.urlEncode(serverID)));
        if (response == "OK") {
            return true;
        }
        return false;
    }

    @Override
    public void close() {}
}