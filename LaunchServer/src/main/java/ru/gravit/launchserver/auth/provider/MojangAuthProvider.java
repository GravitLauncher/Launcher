package ru.gravit.launchserver.auth.provider;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.regex.Pattern;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.WriterConfig;

import ru.gravit.launchserver.LaunchServer;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.launcher.serialize.config.entry.BlockConfigEntry;

public final class MojangAuthProvider extends AuthProvider {
    private static final Pattern UUID_REGEX = Pattern.compile("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})");
    private static final URL URL;

    static {
        try {
            URL = new URL("https://authserver.com.mojang.com/authenticate");
        } catch (MalformedURLException e) {
            throw new InternalError(e);
        }
    }

    public static JsonObject makeJSONRequest(URL url, JsonObject request) throws IOException {
        // Make authentication request
        HttpURLConnection connection = IOHelper.newConnectionPost(url);
        connection.setRequestProperty("Content-Type", "application/json");
        try (OutputStream output = connection.getOutputStream()) {
            output.write(request.toString(WriterConfig.MINIMAL).getBytes(StandardCharsets.UTF_8));
        }
        connection.getResponseCode(); // Actually make request

        // Read response
        InputStream errorInput = connection.getErrorStream();
        try (InputStream input = errorInput == null ? connection.getInputStream() : errorInput) {
            String charset = connection.getContentEncoding();
            Charset charsetObject = charset == null ?
                    IOHelper.UNICODE_CHARSET : Charset.forName(charset);

            // Parse response
            String json = new String(IOHelper.read(input), charsetObject);
            return json.isEmpty() ? null : Json.parse(json).asObject();
        }
    }

    public MojangAuthProvider(BlockConfigEntry block, LaunchServer server) {
        super(block,server);
    }

    @Override
    public AuthProviderResult auth(String login, String password, String ip) throws Exception {
        JsonObject request = Json.object().
                add("agent", Json.object().add("name", "Minecraft").add("version", 1)).
                add("username", login).add("password", password);

        // Verify there's no error
        JsonObject response = makeJSONRequest(URL, request);
        if (response == null)
            authError("Empty com.mojang response");
        JsonValue errorMessage = response.get("errorMessage");
        if (errorMessage != null)
            authError(errorMessage.asString());

        // Parse JSON data
        JsonObject selectedProfile = response.get("selectedProfile").asObject();
        String username = selectedProfile.get("name").asString();
        String accessToken = response.get("clientToken").asString();
        UUID uuid = UUID.fromString(UUID_REGEX.matcher(selectedProfile.get("id").asString()).replaceFirst("$1-$2-$3-$4-$5"));
        String launcherToken = response.get("accessToken").asString();

        // We're done
        return new MojangAuthProviderResult(username, accessToken, uuid, launcherToken);
    }

    @Override
    public void close() {
        // Do nothing
    }
}
