package ru.gravit.launchserver.auth.handler;

import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.net.URL;
import java.util.UUID;

public final class RequestAuthHandler extends CachedAuthHandler {
    private String url;
	private String secretKey;

    @Override
    public void init() {
        if (url == null)
            LogHelper.error("[Verify][AuthHandler] url cannot be null");
        if (secretKey == null)
            LogHelper.error("[Verify][AuthHandler] secretKey cannot be null");
    }

    @Override
    protected Entry fetchEntry(UUID uuid) throws IOException {
        String username = IOHelper.request(new URL(url + "?type=GetUsername&secretKey=" + IOHelper.urlEncode(secretKey) + "&uuid=" + IOHelper.urlEncode(uuid.toString())));
        String accessToken = IOHelper.request(new URL(url + "?type=GetAccessToken&secretKey=" + IOHelper.urlEncode(secretKey) + "&uuid=" + IOHelper.urlEncode(uuid.toString())));
        String serverID = IOHelper.request(new URL(url + "?type=GetServerID&secretKey=" + IOHelper.urlEncode(secretKey) + "&uuid=" + IOHelper.urlEncode(uuid.toString())));

        LogHelper.debug("[AuthHandler] Getted username: " + username);
		LogHelper.debug("[AuthHandler] Getted accessToken: " + accessToken);
		LogHelper.debug("[AuthHandler] Getted serverID: " + serverID);
		LogHelper.debug("[AuthHandler] Getted UUID: " + uuid);

		return query(uuid, username, accessToken, serverID);
    }

    @Override
    protected Entry fetchEntry(String username) throws IOException {
        String GettedUUID = IOHelper.request(new URL(url + "?type=GetUUID&secretKey=" + IOHelper.urlEncode(secretKey) + "&username=" + IOHelper.urlEncode(username)));
        UUID uuid = UUID.fromString(GettedUUID);
        String accessToken = IOHelper.request(new URL(url + "?type=GetAccessToken&secretKey=" + IOHelper.urlEncode(secretKey) + "&username=" + IOHelper.urlEncode(username)));
        String serverID = IOHelper.request(new URL(url + "?type=GetServerID&secretKey=" + IOHelper.urlEncode(secretKey) + "&username=" + IOHelper.urlEncode(username)));

        LogHelper.debug("[AuthHandler] Getted username: " + username);
		LogHelper.debug("[AuthHandler] Getted accessToken: " + accessToken);
		LogHelper.debug("[AuthHandler] Getted serverID: " + serverID);
		LogHelper.debug("[AuthHandler] Getted UUID: " + uuid);

		return query(uuid, username, accessToken, serverID);
    }

    private Entry query(UUID uuid, String username, String accessToken, String serverID) {
        return new Entry(uuid, username, accessToken, serverID);
    }

    @Override
    protected boolean updateAuth(UUID uuid, String username, String accessToken) throws IOException {
        String response0 = IOHelper.request(new URL(url+ "?type=SetUUID&secretKey=" + IOHelper.urlEncode(secretKey) + "&uuid=" + IOHelper.urlEncode(uuid.toString()) + "&username=" + IOHelper.urlEncode(username)));
        String response1 = IOHelper.request(new URL(url+ "?type=SetAccessToken&secretKey=" + IOHelper.urlEncode(secretKey) + "&accessToken=" + IOHelper.urlEncode(accessToken) + "&username=" + IOHelper.urlEncode(username)));

        LogHelper.debug("[AuthHandler] Set accessToken: " + accessToken);
		LogHelper.debug("[AuthHandler] Set UUID: " + uuid);
		LogHelper.debug("[AuthHandler] For this username: " + username);

        return response0.equals("OK") && response1.equals("OK");
    }

    @Override
    protected boolean updateServerID(UUID uuid, String serverID) throws IOException {
        String response = IOHelper.request(new URL(url + "?type=SetServerID&secretKey=" + IOHelper.urlEncode(secretKey) + "&uuid=" + IOHelper.urlEncode(uuid.toString()) + "&serverID=" + IOHelper.urlEncode(serverID)));

        LogHelper.debug("[AuthHandler] Set serverID: " + serverID);
		LogHelper.debug("[AuthHandler] For this UUID: " + uuid);

		return response.equals("OK");
    }

    @Override
    public void close() {}
}