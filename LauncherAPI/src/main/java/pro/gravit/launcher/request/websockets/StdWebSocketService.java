package pro.gravit.launcher.request.websockets;

import pro.gravit.launcher.events.ExceptionEvent;
import pro.gravit.launcher.events.RequestEvent;
import pro.gravit.launcher.events.request.ErrorRequestEvent;
import pro.gravit.launcher.request.Request;
import pro.gravit.launcher.request.RequestException;
import pro.gravit.launcher.request.WebSocketEvent;
import pro.gravit.utils.helper.JVMHelper;
import pro.gravit.utils.helper.LogHelper;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

public class StdWebSocketService extends ClientWebSocketService {
    @SuppressWarnings("rawtypes")
    private final ConcurrentHashMap<UUID, CompletableFuture> futureMap = new ConcurrentHashMap<>();
    private final HashSet<EventHandler> eventHandlers = new HashSet<>();

    public StdWebSocketService(String address) throws SSLException {
        super(address);
    }

    public static StdWebSocketService initWebSockets(String address, boolean async) {
        StdWebSocketService service;
        try {
            service = new StdWebSocketService(address);
        } catch (SSLException e) {
            throw new SecurityException(e);
        }
        service.registerResults();
        service.registerRequests();
        if (!async) {
            try {
                service.open();
            } catch (Exception e) {
                LogHelper.error(e);
            }
        } else {
            service.openAsync(() -> {
            });
        }
        JVMHelper.RUNTIME.addShutdownHook(new Thread(() -> {
            try {
                //if(service.isOpen())
                //    service.closeBlocking();
                service.close();
            } catch (InterruptedException e) {
                LogHelper.error(e);
            }
        }));
        return service;
    }

    public void registerEventHandler(EventHandler handler) {
        eventHandlers.add(handler);
    }

    public void unregisterEventHandler(EventHandler handler) {
        eventHandlers.remove(handler);
    }

    public <T extends WebSocketEvent> void processEventHandlers(T event) {
        for (EventHandler handler : eventHandlers) {
            if (handler.eventHandle(event)) return;
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends WebSocketEvent> void eventHandle(T webSocketEvent) {
        if (webSocketEvent instanceof RequestEvent) {
            RequestEvent event = (RequestEvent) webSocketEvent;
            if (event.requestUUID == null) {
                LogHelper.warning("Request event type %s.requestUUID is null", event.getType() == null ? "null" : event.getType());
                return;
            }
            if (event.requestUUID.equals(RequestEvent.eventUUID)) {
                processEventHandlers(webSocketEvent);
                return;
            }
            @SuppressWarnings("rawtypes")
            CompletableFuture future = futureMap.get(event.requestUUID);
            if (future != null) {
                if (event instanceof ErrorRequestEvent) {
                    future.completeExceptionally(new RequestException(((ErrorRequestEvent) event).error));
                } else if (event instanceof ExceptionEvent) {
                    future.completeExceptionally(new RequestException(
                            String.format("LaunchServer internal error: %s %s", ((ExceptionEvent) event).clazz, ((ExceptionEvent) event).message)));
                } else
                    future.complete(event);
                futureMap.remove(event.requestUUID);
            } else {
                processEventHandlers(event);
                return;
            }
        }
        //
        processEventHandlers(webSocketEvent);
    }

    public <T extends WebSocketEvent> CompletableFuture<T> request(Request<T> request) throws IOException {
        CompletableFuture<T> result = new CompletableFuture<>();
        futureMap.put(request.requestUUID, result);
        sendObject(request, WebSocketRequest.class);
        return result;
    }

    public <T extends WebSocketEvent> T requestSync(Request<T> request) throws IOException {
        try {
            return request(request).get();
        } catch (InterruptedException e) {
            throw new RequestException("Request interrupted");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException)
                throw (IOException) e.getCause();
            else {
                throw new RequestException(cause);
            }
        }
    }
}
