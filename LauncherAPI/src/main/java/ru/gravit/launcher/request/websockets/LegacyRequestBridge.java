package ru.gravit.launcher.request.websockets;

import ru.gravit.launcher.request.ResultInterface;

import java.io.IOException;

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
            e.wait();
        }
        ResultInterface result = e.result;
        waitEventHandler.requests.remove(e);
        return result;
    }
}
