package ru.gravit.launchserver.auth.provider;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.SecurityHelper;
import ru.gravit.utils.helper.VerifyHelper;
import ru.gravit.launcher.serialize.config.entry.BlockConfigEntry;
import ru.gravit.launcher.serialize.config.entry.StringConfigEntry;

public final class JsonAuthProvider extends AuthProvider {
    private static final int TIMEOUT = Integer.parseInt(
            System.getProperty("launcher.connection.timeout", Integer.toString(1500)));

    private final URL url;
    private final String userKeyName;
    private final String passKeyName;
    private final String ipKeyName;
    private final String responseUserKeyName;
    private final String responseErrorKeyName;

    JsonAuthProvider(BlockConfigEntry block) {
        super(block);
        String configUrl = block.getEntryValue("url", StringConfigEntry.class);
        userKeyName = VerifyHelper.verify(block.getEntryValue("userKeyName", StringConfigEntry.class),
                VerifyHelper.NOT_EMPTY, "Username key name can't be empty");
        passKeyName = VerifyHelper.verify(block.getEntryValue("passKeyName", StringConfigEntry.class),
                VerifyHelper.NOT_EMPTY, "Password key name can't be empty");
        ipKeyName = VerifyHelper.verify(block.getEntryValue("ipKeyName", StringConfigEntry.class),
                VerifyHelper.NOT_EMPTY, "IP key can't be empty");
        responseUserKeyName = VerifyHelper.verify(block.getEntryValue("responseUserKeyName", StringConfigEntry.class),
                VerifyHelper.NOT_EMPTY, "Response username key can't be empty");
        responseErrorKeyName = VerifyHelper.verify(block.getEntryValue("responseErrorKeyName", StringConfigEntry.class),
                VerifyHelper.NOT_EMPTY, "Response error key can't be empty");
        url = IOHelper.convertToURL(configUrl);
    }

    @Override
    public AuthProviderResult auth(String login, String password, String ip) throws IOException {
        JsonObject request = Json.object().add(userKeyName, login).add(passKeyName, password).add(ipKeyName, ip);
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
			return authError("Authentication server response is malformed");

        JsonObject response = content.asObject();
        String value;

        if ((value = response.getString(responseUserKeyName, null)) != null)
			return new AuthProviderResult(value, SecurityHelper.randomStringToken());
		else if ((value = response.getString(responseErrorKeyName, null)) != null)
			return authError(value);
		else
			return authError("Authentication server response is malformed");
    }

    @Override
    public void close() {
        // pass
    }
}
