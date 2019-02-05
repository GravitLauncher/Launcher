package ru.gravit.launcher.request.websockets;

import com.google.gson.GsonBuilder;
import ru.gravit.launcher.Launcher;
import ru.gravit.launcher.request.ResultInterface;

import javax.websocket.ContainerProvider;
import javax.websocket.WebSocketContainer;
import java.io.IOException;
import java.net.URI;

public class LegacyRequestBridge {
    public static WaitEventHandler waitEventHandler = new WaitEventHandler();
    public static ClientWebSocketService service;
    public static ResultInterface sendRequest(RequestInterface request) throws IOException, InterruptedException {
        WaitEventHandler.ResultEvent e = new WaitEventHandler.ResultEvent();
        e.type = request.getType();
        waitEventHandler.requests.add(e);
        service.sendObject(request);
        while(!e.ready)
        {
            synchronized(e)
            {
                e.wait();
            }
        }
        ResultInterface result = e.result;
        waitEventHandler.requests.remove(e);
        return result;
    }
    public static void initWebSockets(String address, int port)
    {
        service = new ClientWebSocketService(new GsonBuilder());
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            container.connectToServer(service, new URI("ws://".concat(address).concat(":".concat(String.valueOf(port)).concat("/api"))));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    static {
        if(Launcher.getConfig().nettyPort != 0)
            initWebSockets(Launcher.getConfig().address.getHostName(),Launcher.getConfig().nettyPort);
    }
}
