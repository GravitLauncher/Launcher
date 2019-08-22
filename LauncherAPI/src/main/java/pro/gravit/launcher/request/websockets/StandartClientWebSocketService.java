package pro.gravit.launcher.request.websockets;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLException;

import pro.gravit.launcher.events.request.ErrorRequestEvent;
import pro.gravit.launcher.request.Request;
import pro.gravit.launcher.request.RequestException;
import pro.gravit.launcher.request.WebSocketEvent;
import pro.gravit.utils.helper.JVMHelper;
import pro.gravit.utils.helper.LogHelper;

public class StandartClientWebSocketService extends ClientWebSocketService {
    public WaitEventHandler waitEventHandler = new WaitEventHandler();

    public StandartClientWebSocketService(String address) throws SSLException {
        super(address);
    }

    public class RequestFuture implements Future<WebSocketEvent> {
        public final WaitEventHandler.ResultEvent event;
        public boolean isCanceled = false;

        @SuppressWarnings("rawtypes")
        public RequestFuture(WebSocketRequest request) throws IOException {
            event = new WaitEventHandler.ResultEvent();
            event.type = request.getType();
            if (request instanceof Request) {
                event.uuid = ((Request) request).requestUUID;
            }
            waitEventHandler.requests.add(event);
            sendObject(request);
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            waitEventHandler.requests.remove(event);
            isCanceled = true;
            return true;
        }

        @Override
        public boolean isCancelled() {
            return isCanceled;
        }

        @Override
        public boolean isDone() {
            return event.ready;
        }

        @Override
        public WebSocketEvent get() throws InterruptedException, ExecutionException {
            if (isCanceled) return null;
            synchronized (event) {
                while (!event.ready) {
                    event.wait();
                }
            }
            WebSocketEvent result = event.result;
            waitEventHandler.requests.remove(event);
            if (event.result.getType().equals("error") || event.result.getType().equals("exception")) {
                ErrorRequestEvent errorRequestEvent = (ErrorRequestEvent) event.result;
                throw new ExecutionException(new RequestException(errorRequestEvent.error));
            }
            return result;
        }

        @Override
        public WebSocketEvent get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException {
            if (isCanceled) return null;
            synchronized (event) {
                while (!event.ready) {
                    event.wait(timeout);
                }
            }
            WebSocketEvent result = event.result;
            waitEventHandler.requests.remove(event);
            if (event.result.getType().equals("error") || event.result.getType().equals("exception")) {
                ErrorRequestEvent errorRequestEvent = (ErrorRequestEvent) event.result;
                throw new ExecutionException(new RequestException(errorRequestEvent.error));
            }
            return result;
        }
    }

    public WebSocketEvent sendRequest(WebSocketRequest request) throws IOException, InterruptedException {
        RequestFuture future = new RequestFuture(request);
        WebSocketEvent result;
        try {
            result = future.get();
        } catch (ExecutionException e) {
            throw (RequestException) e.getCause();
        }
        return result;
    }

    public RequestFuture asyncSendRequest(WebSocketRequest request) throws IOException {
        return new RequestFuture(request);
    }

    public static StandartClientWebSocketService initWebSockets(String address, boolean async) {
        StandartClientWebSocketService service;
        try {
            service = new StandartClientWebSocketService(address);
        } catch (SSLException e) {
            throw new SecurityException(e);
        }
        service.registerResults();
        service.registerRequests();
        service.registerHandler(service.waitEventHandler);
        if (!async) {
            try {
                service.open();
                LogHelper.debug("Connect to %s", address);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                service.open();
            } catch (Exception e) {
                e.printStackTrace();
            }
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
}
