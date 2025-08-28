package pro.gravit.launcher.base.request;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public interface RequestService {
    <T extends WebSocketEvent> CompletableFuture<T> request(Request<T> request);
    void connect() throws Exception;

    void registerEventHandler(EventHandler handler);

    void unregisterEventHandler(EventHandler handler);

    default <T extends WebSocketEvent> T requestSync(Request<T> request) {
        try {
            return request(request).get();
        } catch (InterruptedException e) {
            throw new RequestException("Request interrupted");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw new RequestException(cause);
        }
    }

    boolean isClosed();

    @FunctionalInterface
    interface EventHandler {
        /**
         * @param event processing event
         * @param <T>   event type
         * @return false - continue, true - stop
         */
        <T extends WebSocketEvent> boolean eventHandle(T event);
    }
}

