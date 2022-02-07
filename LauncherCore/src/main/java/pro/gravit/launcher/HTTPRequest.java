package pro.gravit.launcher;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import pro.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class HTTPRequest {
    private static final int TIMEOUT = 10000;

    private HTTPRequest() {
    }

    public static JsonElement jsonRequest(JsonElement request, URL url) throws IOException {
        return jsonRequest(request, "POST", url);
    }

    public static JsonElement jsonRequest(JsonElement request, String method, URL url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoInput(true);
        if (request != null) connection.setDoOutput(true);
        connection.setRequestMethod(method);
        if (request != null) connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        if (request != null) connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/45.0.2454.85 Safari/537.36");
        connection.setRequestProperty("Accept", "application/json");
        if (TIMEOUT > 0)
            connection.setConnectTimeout(TIMEOUT);
        if (request != null)
            try (OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8)) {
                writer.write(request.toString());
                writer.flush();
            }

        InputStreamReader reader;
        int statusCode = connection.getResponseCode();

        if (200 <= statusCode && statusCode < 300)
            reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8);
        else
            reader = new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8);
        try {
            return JsonParser.parseReader(reader);
        } catch (Exception e) {
            if (200 > statusCode || statusCode > 300) {
                LogHelper.error("JsonRequest failed. Server response code %d", statusCode);
                throw new IOException(e);
            }
            return null;
        }
    }
}
