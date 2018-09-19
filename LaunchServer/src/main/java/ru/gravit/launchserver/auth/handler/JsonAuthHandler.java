package ru.gravit.launchserver.auth.handler;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import ru.gravit.launchserver.auth.provider.AuthProviderResult;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.VerifyHelper;
import ru.gravit.launcher.serialize.config.entry.BlockConfigEntry;
import ru.gravit.launcher.serialize.config.entry.StringConfigEntry;

@SuppressWarnings("unused")
public class JsonAuthHandler extends AuthHandler {

    private static final int TIMEOUT = 10;
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
        url = IOHelper.convertToURL(configUrl);
        urlCheckServer = IOHelper.convertToURL(configUrlCheckServer);
        urlJoinServer = IOHelper.convertToURL(configUrlJoinServer);
        urlUsernameToUUID = IOHelper.convertToURL(configUrlUsernameUUID);
        urlUUIDToUsername = IOHelper.convertToURL(configUrlUUIDUsername);
    }

    @Override
    public UUID auth(AuthProviderResult authResult) throws IOException {
        JsonObject request = Json.object().add(userKeyName, authResult.username).add(accessTokenKeyName, authResult.accessToken);
        JsonObject result = jsonRequest(request, url);
        String value;
        if ((value = result.getString(uuidKeyName, null)) != null)
            return UUID.fromString(value);
        throw new IOException("Service error");
    }

    @Override
    public UUID checkServer(String username, String serverID) throws IOException {
        JsonObject request = Json.object().add(userKeyName, username).add(serverIDKeyName, serverID);
        JsonObject result = jsonRequest(request, urlCheckServer);
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
        jsonRequest(request, urlJoinServer);
        return request.getString(responseErrorKeyName,null).equals("OK");
    }

    @Override
    public UUID usernameToUUID(String username) throws IOException {
        JsonObject request = Json.object().add(userKeyName, username);
        JsonObject result = jsonRequest(request, urlUsernameToUUID);
        String value;
        if ((value = result.getString(uuidKeyName, null)) != null)
            return UUID.fromString(value);
        throw new IOException("Service error");
    }

    @Override
    public String uuidToUsername(UUID uuid) throws IOException {
        JsonObject request = Json.object().add(uuidKeyName, uuid.toString());
        JsonObject result = jsonRequest(request, urlUUIDToUsername);
        String value;
        if ((value = result.getString(userKeyName, null)) != null)
            return value;
        throw new IOException("Service error");
    }

    public JsonObject jsonRequest(JsonObject request, URL url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setRequestProperty("Accept", "application/json");
        if (TIMEOUT > 0)
			connection.setConnectTimeout(TIMEOUT);

        OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), Charset.forName("UTF-8"));
        writer.write(request.toString());
        writer.flush();
        writer.close();

        InputStreamReader reader;
        int statusCode = connection.getResponseCode();

        if (200 <= statusCode && statusCode < 300)
			reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8);
		else
			reader = new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8);
        JsonValue content = Json.parse(reader);
        if (!content.isObject())
			authError("Authentication server response is malformed");

        JsonObject response = content.asObject();
        String value;

        if ((value = response.getString(responseErrorKeyName, null)) != null)
			authError(value);
        return response;
    }
}
