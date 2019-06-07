package pro.gravit.launcher.request.websockets;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLException;

import com.google.gson.GsonBuilder;

import pro.gravit.launcher.events.request.ErrorRequestEvent;
import pro.gravit.launcher.request.Request;
import pro.gravit.launcher.request.RequestException;
import pro.gravit.launcher.request.ResultInterface;
import pro.gravit.utils.helper.CommonHelper;
import pro.gravit.utils.helper.JVMHelper;
import pro.gravit.utils.helper.LogHelper;

public class StandartClientWebSocketService extends ClientWebSocketService {
    public WaitEventHandler waitEventHandler = new WaitEventHandler();

    public StandartClientWebSocketService(GsonBuilder gsonBuilder, String address, int i) throws SSLException {
        super(null, address, i);
    }

    public class RequestFuture implements Future<ResultInterface> {
        public final WaitEventHandler.ResultEvent event;
        public boolean isCanceled = false;

        @SuppressWarnings("rawtypes")
        public RequestFuture(RequestInterface request) throws IOException {
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
        public ResultInterface get() throws InterruptedException, ExecutionException {
            if (isCanceled) return null;
            while (!event.ready) {
                synchronized (event) {
                    event.wait();
                }
            }
            ResultInterface result = event.result;
            waitEventHandler.requests.remove(event);
            if (event.result.getType().equals("error") || event.result.getType().equals("exception")) {
                ErrorRequestEvent errorRequestEvent = (ErrorRequestEvent) event.result;
                throw new ExecutionException(new RequestException(errorRequestEvent.error));
            }
            return result;
        }

        @Override
        public ResultInterface get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException {
            if (isCanceled) return null;
            while (!event.ready) {
                synchronized (event) {
                    event.wait(timeout);
                }
            }
            ResultInterface result = event.result;
            waitEventHandler.requests.remove(event);
            if (event.result.getType().equals("error") || event.result.getType().equals("exception")) {
                ErrorRequestEvent errorRequestEvent = (ErrorRequestEvent) event.result;
                throw new ExecutionException(new RequestException(errorRequestEvent.error));
            }
            return result;
        }
    }

    public ResultInterface sendRequest(RequestInterface request) throws IOException, InterruptedException {
        RequestFuture future = new RequestFuture(request);
        ResultInterface result;
        try {
            result = future.get();
        } catch (ExecutionException e) {
            throw (RequestException) e.getCause();
        }
        return result;
    }

    public RequestFuture asyncSendRequest(RequestInterface request) throws IOException {
        return new RequestFuture(request);
    }

    public static StandartClientWebSocketService initWebSockets(String address, boolean async) {
        StandartClientWebSocketService service;
        try {
            service = new StandartClientWebSocketService(CommonHelper.newBuilder(), address, 5000);
        } catch (SSLException e) {
            LogHelper.error(e);
            return null;
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
