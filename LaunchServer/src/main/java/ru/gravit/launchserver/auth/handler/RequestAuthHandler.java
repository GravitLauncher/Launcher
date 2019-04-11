package ru.gravit.launchserver.auth.handler;

import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.net.URL;
import java.util.UUID;

public final class RequestAuthHandler extends CachedAuthHandler {
    private String url;
	private String secretKey;
	
	private String typeColumn;
	private String uuidColumn;
	private String usernameColumn;
	private String accessTokenColumn;
	private String serverIDColumn;
	private String secretKeyColumn;
	
	private String TypeGetUsername;
	private String TypeGetUUID;
	private String TypeGetAccessToken;
	private String TypeGetServerID;
	private String TypeSetAccessTokenAndUUID;
	private String TypeSetServerID;

    @Override
    public void init() {
        if (url == null)
            LogHelper.error("[Verify][AuthHandler] url cannot be null");
        if (secretKey == null)
            LogHelper.error("[Verify][AuthHandler] secretKey cannot be null");
		if (typeColumn == null)
            typeColumn = "type";
		if (uuidColumn == null)
            uuidColumn = "uuid";
		if (usernameColumn == null)
            usernameColumn = "username";
		if (accessTokenColumn == null)
            accessTokenColumn = "accessToken";
		if (serverIDColumn == null)
            serverIDColumn = "serverID";
		if (secretKeyColumn == null)
            secretKeyColumn = "secretKey";
		if (TypeGetUsername == null)
            TypeGetUsername = "GetUsername";
		if (TypeGetUUID == null)
            TypeGetUUID = "GetUUID";
		if (TypeGetAccessToken == null)
            TypeGetAccessToken = "GetAccessToken";
		if (TypeGetServerID == null)
            TypeGetServerID = "GetServerID";
		if (TypeSetAccessTokenAndUUID == null)
            TypeSetAccessTokenAndUUID = "SetAccessTokenAndUUID";
		if (TypeSetServerID == null)
            TypeSetServerID = "SetServerID";
    }

    @Override
    protected Entry fetchEntry(UUID uuid) throws IOException {
        String username = IOHelper.request(new URL(url + "?" + IOHelper.urlEncode(typeColumn) + "=" + TypeGetUsername + "&" + secretKeyColumn + "=" + IOHelper.urlEncode(secretKey) + "&" + IOHelper.urlEncode(uuidColumn) + "=" + IOHelper.urlEncode(uuid.toString())));
        String accessToken = IOHelper.request(new URL(url + "?" + IOHelper.urlEncode(typeColumn) + "=" + TypeGetAccessToken + "&" + secretKeyColumn + "=" + IOHelper.urlEncode(secretKey) + "&" + IOHelper.urlEncode(uuidColumn) + "=" + IOHelper.urlEncode(uuid.toString())));
        String serverID = IOHelper.request(new URL(url + "?" + IOHelper.urlEncode(typeColumn) + "=" + TypeGetServerID + "&" + secretKeyColumn + "=" + IOHelper.urlEncode(secretKey) + "&" + IOHelper.urlEncode(uuidColumn) + "=" + IOHelper.urlEncode(uuid.toString())));
        LogHelper.debug("[AuthHandler] Getted username: " + username);
		LogHelper.debug("[AuthHandler] Getted accessToken: " + accessToken);
		LogHelper.debug("[AuthHandler] Getted serverID: " + serverID);
		LogHelper.debug("[AuthHandler] Getted UUID: " + uuid);
		return query(uuid, username, accessToken, serverID);
    }

    @Override
    protected Entry fetchEntry(String username) throws IOException {
        String GettedUUID = IOHelper.request(new URL(url + "?" + IOHelper.urlEncode(typeColumn) + "=" + TypeGetUUID + "&" + secretKeyColumn + "=" + IOHelper.urlEncode(secretKey) + "&" + IOHelper.urlEncode(usernameColumn) + "=" + IOHelper.urlEncode(username)));
        UUID uuid = UUID.fromString(GettedUUID);
        String accessToken = IOHelper.request(new URL(url + "?" + IOHelper.urlEncode(typeColumn) + "=" + TypeGetAccessToken + "&" + secretKeyColumn + "=" + IOHelper.urlEncode(secretKey) + "&" + IOHelper.urlEncode(usernameColumn) + "=" + IOHelper.urlEncode(username)));
        String serverID = IOHelper.request(new URL(url + "?" + IOHelper.urlEncode(typeColumn) + "=" + TypeGetServerID + "&" + secretKeyColumn + "=" + IOHelper.urlEncode(secretKey) + "&" + IOHelper.urlEncode(usernameColumn) + "=" + IOHelper.urlEncode(username)));
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
        String response = IOHelper.request(new URL(url+ "?" + IOHelper.urlEncode(typeColumn) + "=" + TypeSetAccessTokenAndUUID + "&" + secretKeyColumn + "=" + IOHelper.urlEncode(secretKey) + "&" + IOHelper.urlEncode(uuidColumn) + "=" + IOHelper.urlEncode(uuid.toString()) + "&" + IOHelper.urlEncode(accessTokenColumn) + "=" + IOHelper.urlEncode(accessToken) + "&" + IOHelper.urlEncode(usernameColumn) + "=" + IOHelper.urlEncode(username)));
        LogHelper.debug("[AuthHandler] Set accessToken: " + accessToken);
		LogHelper.debug("[AuthHandler] Set UUID: " + uuid);
		LogHelper.debug("[AuthHandler] For this username: " + username);
        return response.equals("OK");
    }

    @Override
    protected boolean updateServerID(UUID uuid, String serverID) throws IOException {
        String response = IOHelper.request(new URL(url + "?" + IOHelper.urlEncode(typeColumn) + "=" + TypeSetServerID + "&" + secretKeyColumn + "=" + IOHelper.urlEncode(secretKey) + "&" + IOHelper.urlEncode(uuidColumn) + "=" + IOHelper.urlEncode(uuid.toString()) + "&" + IOHelper.urlEncode(serverIDColumn) + "=" + IOHelper.urlEncode(serverID)));
        LogHelper.debug("[AuthHandler] Set serverID: " + serverID);
		LogHelper.debug("[AuthHandler] For this UUID: " + uuid);
		return response.equals("OK");
    }

    @Override
    public void close() {}
}