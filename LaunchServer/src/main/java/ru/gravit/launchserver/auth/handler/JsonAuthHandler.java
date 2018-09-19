package ru.gravit.launchserver.auth.handler;

import java.io.IOException;
import java.net.URL;
import java.util.UUID;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import ru.gravit.launchserver.auth.provider.AuthProviderResult;
import ru.gravit.utils.HTTPRequest;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.VerifyHelper;
import ru.gravit.launcher.serialize.config.entry.BlockConfigEntry;
import ru.gravit.launcher.serialize.config.entry.StringConfigEntry;

@SuppressWarnings("unused")
public class JsonAuthHandler extends AuthHandler {

    private final URL url;
    private final URL urlCheckServer;
    private final URL urlJoinServer;
    private final URL urlUsernameToUUID;
    private final URL urlUUIDToUsername;
    private final String userKeyName;
    private final String serverIDKeyName;
    private final String accessTokenKeyName;
    private final String uuidKeyName;
    private final String responseErrorKeyName;
    private final String responseOKKeyName;

    protected JsonAuthHandler(BlockConfigEntry block) {
        super(block);
        String configUrl = block.getEntryValue("url", StringConfigEntry.class);
        String configUrlCheckServer = block.getEntryValue("urlCheckServer", StringConfigEntry.class);
        String configUrlJoinServer = block.getEntryValue("urlJoinServer", StringConfigEntry.class);
        String configUrlUsernameUUID = block.getEntryValue("urlUsernameToUUID", StringConfigEntry.class);
        String configUrlUUIDUsername = block.getEntryValue("urlUUIDToUsername", StringConfigEntry.class);
        userKeyName = VerifyHelper.verify(block.getEntryValue("userKeyName", StringConfigEntry.class),
                VerifyHelper.NOT_EMPTY, "Username key name can't be empty");
        serverIDKeyName = VerifyHelper.verify(block.getEntryValue("serverIDKeyName", StringConfigEntry.class),
                VerifyHelper.NOT_EMPTY, "ServerID key name can't be empty");
        uuidKeyName = VerifyHelper.verify(block.getEntryValue("UUIDKeyName", StringConfigEntry.class),
                VerifyHelper.NOT_EMPTY, "UUID key name can't be empty");
        accessTokenKeyName = VerifyHelper.verify(block.getEntryValue("accessTokenKeyName", StringConfigEntry.class),
                VerifyHelper.NOT_EMPTY, "AccessToken key name can't be empty");
        responseErrorKeyName = VerifyHelper.verify(block.getEntryValue("responseErrorKeyName", StringConfigEntry.class),
                VerifyHelper.NOT_EMPTY, "Response error key can't be empty");
        responseOKKeyName = VerifyHelper.verify(block.getEntryValue("responseOKKeyName", StringConfigEntry.class),
                VerifyHelper.NOT_EMPTY, "Response okay key can't be empty");
        url = IOHelper.convertToURL(configUrl);
        urlCheckServer = IOHelper.convertToURL(configUrlCheckServer);
        urlJoinServer = IOHelper.convertToURL(configUrlJoinServer);
        urlUsernameToUUID = IOHelper.convertToURL(configUrlUsernameUUID);
        urlUUIDToUsername = IOHelper.convertToURL(configUrlUUIDUsername);
    }

    @Override
    public UUID auth(AuthProviderResult authResult) throws IOException {
        JsonObject request = Json.object().add(userKeyName, authResult.username).add(accessTokenKeyName, authResult.accessToken);
        JsonObject result = jsonRequestChecked(request, url);
        String value;
        if ((value = result.getString(uuidKeyName, null)) != null)
            return UUID.fromString(value);
        throw new IOException("Service error");
    }

    @Override
    public UUID checkServer(String username, String serverID) throws IOException {
        JsonObject request = Json.object().add(userKeyName, username).add(serverIDKeyName, serverID);
        JsonObject result = jsonRequestChecked(request, urlCheckServer);
        String value;
        if ((value = result.getString(uuidKeyName, null)) != null)
			return UUID.fromString(value);
		throw new IOException("Service error");
    }

    @Override
    public void close() {

    }
    @Override
    public boolean joinServer(String username, String accessToken, String serverID) throws IOException {
        JsonObject request = Json.object().add(userKeyName, username).add(serverIDKeyName, serverID).add(accessTokenKeyName, accessToken);
        HTTPRequest.jsonRequest(request, urlJoinServer);
        return request.getString(responseOKKeyName,null).equals("OK");
    }

    @Override
    public UUID usernameToUUID(String username) throws IOException {
        JsonObject request = Json.object().add(userKeyName, username);
        JsonObject result = jsonRequestChecked(request, urlUsernameToUUID);
        String value;
        if ((value = result.getString(uuidKeyName, null)) != null)
            return UUID.fromString(value);
        throw new IOException("Service error");
    }

    @Override
    public String uuidToUsername(UUID uuid) throws IOException {
        JsonObject request = Json.object().add(uuidKeyName, uuid.toString());
        JsonObject result = jsonRequestChecked(request, urlUUIDToUsername);
        String value;
        if ((value = result.getString(userKeyName, null)) != null)
            return value;
        throw new IOException("Service error");
    }

    public JsonObject jsonRequestChecked(JsonObject object,URL url) throws IOException {
        JsonValue result = HTTPRequest.jsonRequest(object,url);
        if (!result.isObject())
            authError("Authentication server response is malformed");

        JsonObject response = result.asObject();
        String value;

        if ((value = response.getString(responseErrorKeyName, null)) != null)
            authError(value);
        return response;
    }
}
