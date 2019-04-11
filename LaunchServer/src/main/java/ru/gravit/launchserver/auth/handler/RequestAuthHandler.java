package ru.gravit.launchserver.auth.handler;

import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.net.URL;
import java.util.UUID;

public final class RequestAuthHandler extends CachedAuthHandler {
    private String url;
	private String secretKey;
	
	private String typeLine;
	private String uuidLine;
	private String usernameLine;
	private String accessTokenLine;
	private String serverIDLine;
	private String secretKeyLine;
	
	private String TypeFetchByUUID;
	private String TypeFetchByUsername;
	private String TypeSetAccessTokenAndUUID;
	private String TypeSetServerID;
	private String splitSymbol;

    @Override
    public void init() {
        if (url == null)
            LogHelper.error("[Verify][AuthHandler] url cannot be null");
        if (secretKey == null)
            LogHelper.error("[Verify][AuthHandler] secretKey cannot be null");
		// Default
		if (typeLine == null)
            typeLine = "type";
		if (uuidLine == null)
            uuidLine = "uuid";
		if (usernameLine == null)
            usernameLine = "username";
		if (accessTokenLine == null)
            accessTokenLine = "accessToken";
		if (serverIDLine == null)
            serverIDLine = "serverID";
		if (secretKeyLine == null)
            secretKeyLine = "secretKey";
		if (TypeFetchByUUID == null)
            TypeFetchByUUID = "FetchByUUID";
		if (TypeFetchByUsername == null)
            TypeFetchByUsername = "FetchByUsername";
		if (TypeSetAccessTokenAndUUID == null)
            TypeSetAccessTokenAndUUID = "SetAccessTokenAndUUID";
		if (TypeSetServerID == null)
            TypeSetServerID = "SetServerID";
		if (splitSymbol == null)
            splitSymbol = ":";
    }

    @Override
    protected Entry fetchEntry(UUID uuid) throws IOException {
        String response = IOHelper.request(new URL(url + "?" + IOHelper.urlEncode(typeLine) + "=" + TypeFetchByUUID + "&" + secretKeyLine + "=" + IOHelper.urlEncode(secretKey) + "&" + IOHelper.urlEncode(uuidLine) + "=" + IOHelper.urlEncode(uuid.toString())));
		String[] parts = response.split(splitSymbol);
		String username = parts[0];
        String accessToken = parts[1];
		String serverID = parts[2];
        LogHelper.debug("[AuthHandler] Getted username: " + username);
		LogHelper.debug("[AuthHandler] Getted accessToken: " + accessToken);
		LogHelper.debug("[AuthHandler] Getted serverID: " + serverID);
		LogHelper.debug("[AuthHandler] Getted UUID: " + uuid);
		return query(uuid, username, accessToken, serverID);
    }

    @Override
    protected Entry fetchEntry(String username) throws IOException {
        String response = IOHelper.request(new URL(url + "?" + IOHelper.urlEncode(typeLine) + "=" + TypeFetchByUsername + "&" + secretKeyLine + "=" + IOHelper.urlEncode(secretKey) + "&" + IOHelper.urlEncode(usernameLine) + "=" + IOHelper.urlEncode(username)));
		String[] parts = response.split(splitSymbol);
		UUID uuid = UUID.fromString(parts[0]);
        String accessToken = parts[1];
		String serverID = parts[2];
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
        String response = IOHelper.request(new URL(url+ "?" + IOHelper.urlEncode(typeLine) + "=" + TypeSetAccessTokenAndUUID + "&" + secretKeyLine + "=" + IOHelper.urlEncode(secretKey) + "&" + IOHelper.urlEncode(uuidLine) + "=" + IOHelper.urlEncode(uuid.toString()) + "&" + IOHelper.urlEncode(accessTokenLine) + "=" + IOHelper.urlEncode(accessToken) + "&" + IOHelper.urlEncode(usernameLine) + "=" + IOHelper.urlEncode(username)));
        LogHelper.debug("[AuthHandler] Set accessToken: " + accessToken);
		LogHelper.debug("[AuthHandler] Set UUID: " + uuid);
		LogHelper.debug("[AuthHandler] For this username: " + username);
        return response.equals("OK");
    }

    @Override
    protected boolean updateServerID(UUID uuid, String serverID) throws IOException {
        String response = IOHelper.request(new URL(url + "?" + IOHelper.urlEncode(typeLine) + "=" + TypeSetServerID + "&" + secretKeyLine + "=" + IOHelper.urlEncode(secretKey) + "&" + IOHelper.urlEncode(uuidLine) + "=" + IOHelper.urlEncode(uuid.toString()) + "&" + IOHelper.urlEncode(serverIDLine) + "=" + IOHelper.urlEncode(serverID)));
        LogHelper.debug("[AuthHandler] Set serverID: " + serverID);
		LogHelper.debug("[AuthHandler] For this UUID: " + uuid);
		return response.equals("OK");
    }

    @Override
    public void close() {}
}