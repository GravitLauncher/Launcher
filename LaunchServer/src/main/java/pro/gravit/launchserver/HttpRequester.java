package pro.gravit.launchserver;

import com.google.gson.JsonElement;
import pro.gravit.launcher.Launcher;
import pro.gravit.launchserver.helper.HttpHelper;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Duration;

public class HttpRequester {
    private transient final HttpClient httpClient = HttpClient.newBuilder().build();

    public HttpRequester() {
    }

    public static class SimpleErrorHandler<T> implements HttpHelper.HttpJsonErrorHandler<T, SimpleError> {
        private final Type type;

        private SimpleErrorHandler(Type type) {
            this.type = type;
        }

        @Override
        public HttpHelper.HttpOptional<T, SimpleError> applyJson(JsonElement response, int statusCode) {
            if(statusCode < 200 || statusCode >= 300) {
                return new HttpHelper.HttpOptional<>(null, Launcher.gsonManager.gson.fromJson(response, SimpleError.class), statusCode);
            }
            return new HttpHelper.HttpOptional<>(Launcher.gsonManager.gson.fromJson(response, type), null, statusCode);
        }
    }

    public <T> SimpleErrorHandler<T> makeEH(Class<T> clazz) {
        return new SimpleErrorHandler<>(clazz);
    }

    public <T> HttpRequest get(String url, String token) {
        try {
            var requestBuilder = HttpRequest.newBuilder()
                    .method("GET", HttpRequest.BodyPublishers.noBody())
                    .uri(new URI(url))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .header("Accept", "application/json")
                    .timeout(Duration.ofMillis(10000));
            if(token != null) {
                requestBuilder.header("Authorization", "Bearer ".concat(token));
            }
            return requestBuilder.build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public <T> HttpRequest post(String url, T request, String token) {
        try {
            var requestBuilder = HttpRequest.newBuilder()
                    .method("POST", HttpRequest.BodyPublishers.ofString(Launcher.gsonManager.gson.toJson(request)))
                    .uri(new URI(url))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .header("Accept", "application/json")
                    .timeout(Duration.ofMillis(10000));
            if(token != null) {
                requestBuilder.header("Authorization", "Bearer ".concat(token));
            }
            return requestBuilder.build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public <T> HttpHelper.HttpOptional<T, SimpleError> send(HttpRequest request, Class<T> clazz) throws IOException {
        return HttpHelper.send(httpClient, request, makeEH(clazz));
    }

    public static class SimpleError {
        public String error;
        public int code;

        public SimpleError(String error) {
            this.error = error;
        }

        @Override
        public String toString() {
            return "SimpleError{" +
                    "error='" + error + '\'' +
                    ", code=" + code +
                    '}';
        }
    }
}
