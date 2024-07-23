package pro.gravit.launcher.base.request.websockets;

import pro.gravit.launcher.base.request.Request;
import pro.gravit.launcher.base.request.RequestException;
import pro.gravit.launcher.base.request.RequestService;
import pro.gravit.launcher.base.request.WebSocketEvent;

import java.util.concurrent.CompletableFuture;

public class VoidRequestService implements RequestService {
    private final Throwable ex;

    public VoidRequestService(Throwable ex) {
        this.ex = ex;
    }

    public VoidRequestService() {
        this.ex = null;
    }

    @Override
    public <T extends WebSocketEvent> CompletableFuture<T> request(Request<T> request) {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(ex != null ? ex : new RequestException("Connection fail"));
        return future;
    }

    @Override
    public void open() {

    }

    @Override
    public void registerEventHandler(EventHandler handler) {

    }

    @Override
    public void unregisterEventHandler(EventHandler handler) {

    }

    @Override
    public boolean isClosed() {
        return true;
    }
}
