package ru.gravit.launcher.request.websockets;

import com.google.gson.GsonBuilder;
import ru.gravit.launcher.Launcher;
import ru.gravit.launcher.events.request.ErrorRequestEvent;
import ru.gravit.launcher.request.RequestException;
import ru.gravit.launcher.request.ResultInterface;
import ru.gravit.utils.helper.LogHelper;

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
            synchronized(e)
            {
                e.wait();
                LogHelper.debug("WAIT OK");
            }
        }
        ResultInterface result = e.result;
        waitEventHandler.requests.remove(e);
        if(e.result.getType().equals("error"))
        {
            ErrorRequestEvent errorRequestEvent = (ErrorRequestEvent) e.result;
            throw new RequestException(errorRequestEvent.error);
        }
        return result;
    }
    public static void initWebSockets(String address)
    {
        service = new ClientWebSocketService(new GsonBuilder(), address, 5000);
        service.registerResults();
        service.registerRequests();
        service.registerHandler(waitEventHandler);
        try {
            if(!service.connectBlocking()) LogHelper.error("Error connecting");
            LogHelper.debug("Connect to %s",address);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    static {
        if(Launcher.getConfig().nettyPort != 0)
            initWebSockets(Launcher.getConfig().nettyAddress);
    }
}
