package pro.gravit.launcher.base;

import com.google.gson.JsonElement;
import pro.gravit.launcher.base.request.RequestException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.function.Function;

public final class HttpHelper {
    private HttpHelper() {
        throw new UnsupportedOperationException();
    }

    public static <T, E> HttpOptional<T, E> send(HttpClient client, HttpRequest request, HttpErrorHandler<T, E> handler) throws IOException {
        try {
            var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            return handler.apply(response);
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }

    public static <T, E> CompletableFuture<HttpOptional<T, E>> sendAsync(HttpClient client, HttpRequest request, HttpErrorHandler<T, E> handler) {
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream()).thenApply(handler::apply);
    }

    public static <T> HttpResponse.BodyHandler<T> ofJsonResult(Class<T> type) {
        return ofJsonResult((Type) type);
    }

    public static <T> HttpResponse.BodyHandler<T> ofJsonResult(Type type) {
        return new JsonBodyHandler<>(HttpResponse.BodyHandlers.ofInputStream(), (input) -> {
            try (Reader reader = new InputStreamReader(input)) {
                return Launcher.gsonManager.gson.fromJson(reader, type);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static <T> HttpRequest.BodyPublisher jsonBodyPublisher(T obj) {
        return HttpRequest.BodyPublishers.ofString(Launcher.gsonManager.gson.toJson(obj));
    }


    public interface HttpErrorHandler<T, E> {
        HttpOptional<T, E> apply(HttpResponse<InputStream> response);
    }

    public interface HttpJsonErrorHandler<T, E> extends HttpErrorHandler<T, E> {
        HttpOptional<T, E> applyJson(JsonElement response, int statusCode);

        default HttpOptional<T, E> apply(HttpResponse<InputStream> response) {
            String contentType = response.headers()
                    .firstValue("Content-Type")
                    .orElse("unknown");

            if (!contentType.toLowerCase(Locale.ROOT).contains("json")) {
                throw new RuntimeException(
                        "Expected a JSON response, but the server returned Content-Type '%s' (HTTP %d)."
                                .formatted(contentType, response.statusCode())
                );
            }

            try (Reader reader = new InputStreamReader(response.body(), StandardCharsets.UTF_8)) {
                JsonElement element = Launcher.gsonManager.gson.fromJson(reader, JsonElement.class);
                return applyJson(element, response.statusCode());
            } catch (Exception e) {
                throw new RuntimeException(
                        "Failed to process JSON response from the server (HTTP %d)."
                                .formatted(response.statusCode()),
                        e
                );
            }
        }
    }

    public static class HttpOptional<T, E> {
        protected final T result;
        protected final E error;
        protected final int statusCode;

        public HttpOptional(T result, E error, int statusCode) {
            this.result = result;
            this.error = error;
            this.statusCode = statusCode;
        }

        public T result() {
            return result;
        }

        public E error() {
            return error;
        }

        public int statusCode() {
            return statusCode;
        }

        public boolean isSuccessful() {
            return statusCode >= 200 && statusCode < 300;
        }

        public T getOrThrow() throws RequestException {
            if (isSuccessful()) {
                return result;
            } else {
                throw new RequestException(error == null ? "statusCode %d".formatted(statusCode) : error.toString());
            }
        }
    }

    public static final class BasicJsonHttpErrorHandler<T> implements HttpJsonErrorHandler<T, Void> {
        private final Class<T> type;

        public BasicJsonHttpErrorHandler(Class<T> type) {
            this.type = type;
        }

        @Override
        public HttpOptional<T, Void> applyJson(JsonElement response, int statusCode) {
            return new HttpOptional<>(Launcher.gsonManager.gson.fromJson(response, type), null, statusCode);
        }
    }

    private static class JsonBodyHandler<T> implements HttpResponse.BodyHandler<T> {
        private final HttpResponse.BodyHandler<InputStream> delegate;
        private final Function<InputStream, T> func;

        private JsonBodyHandler(HttpResponse.BodyHandler<InputStream> delegate, Function<InputStream, T> func) {
            this.delegate = delegate;
            this.func = func;
        }

        @Override
        public HttpResponse.BodySubscriber<T> apply(HttpResponse.ResponseInfo responseInfo) {
            return new JsonBodySubscriber<>(delegate.apply(responseInfo), func);
        }
    }

    private static class JsonBodySubscriber<T> implements HttpResponse.BodySubscriber<T> {
        private final HttpResponse.BodySubscriber<InputStream> delegate;
        private final Function<InputStream, T> func;

        private JsonBodySubscriber(HttpResponse.BodySubscriber<InputStream> delegate, Function<InputStream, T> func) {
            this.delegate = delegate;
            this.func = func;
        }

        @Override
        public CompletionStage<T> getBody() {
            return delegate.getBody().thenApply(func);
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            delegate.onSubscribe(subscription);
        }

        @Override
        public void onNext(List<ByteBuffer> item) {
            delegate.onNext(item);
        }

        @Override
        public void onError(Throwable throwable) {
            delegate.onError(throwable);
        }

        @Override
        public void onComplete() {
            delegate.onComplete();
        }
    }
}
