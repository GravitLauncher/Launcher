package pro.gravit.launchserver.auth.password;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.Launcher;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class JsonPasswordVerifier extends PasswordVerifier {
    private static transient final Logger logger = LogManager.getLogger();
    private transient final HttpClient client = HttpClient.newBuilder().build();
    public String url;
    public String bearerToken;

    @Override
    public boolean check(String encryptedPassword, String password) {
        JsonPasswordResponse response = jsonRequest(new JsonPasswordRequest(encryptedPassword, password), url, bearerToken, JsonPasswordResponse.class, client);
        if (response != null) {
            return response.success;
        }
        return false;
    }

    public static class JsonPasswordRequest {
        public String encryptedPassword;
        public String password;

        public JsonPasswordRequest(String encryptedPassword, String password) {
            this.encryptedPassword = encryptedPassword;
            this.password = password;
        }
    }

    public static class JsonPasswordResponse {
        public boolean success;
    }

    public static <T, R> R jsonRequest(T request, String url, String bearerToken, Class<R> clazz, HttpClient client) {
        HttpRequest.BodyPublisher publisher;
        if (request != null) {
            publisher = HttpRequest.BodyPublishers.ofString(Launcher.gsonManager.gson.toJson(request));
        } else {
            publisher = HttpRequest.BodyPublishers.noBody();
        }
        try {
            HttpRequest.Builder request1 = HttpRequest.newBuilder()
                    .method("POST", publisher)
                    .uri(new URI(url))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .header("Accept", "application/json")
                    .timeout(Duration.ofMillis(10000));
            if (bearerToken != null) {
                request1.header("Authorization", "Bearer ".concat(bearerToken));
            }
            HttpResponse<InputStream> response = client.send(request1.build(), HttpResponse.BodyHandlers.ofInputStream());
            int statusCode = response.statusCode();
            if (200 > statusCode || statusCode > 300) {
                if (statusCode >= 500) {
                    logger.error("JsonCoreProvider: {} return {}", url, statusCode);
                } else if (statusCode >= 300 && statusCode <= 400) {
                    logger.error("JsonCoreProvider: {} return {}, try redirect to {}. Redirects not supported!", url, statusCode, response.headers().firstValue("Location").orElse("Unknown"));
                } else if (statusCode == 403 || statusCode == 401) {
                    logger.error("JsonCoreProvider: {} return {}. Please set 'bearerToken'!", url, statusCode);
                }
                return null;
            }
            try (Reader reader = new InputStreamReader(response.body())) {
                return Launcher.gsonManager.gson.fromJson(reader, clazz);
            }
        } catch (Exception e) {
            return null;
        }
    }
}
