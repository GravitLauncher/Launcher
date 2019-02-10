package ru.gravit.launcher.request.websockets;

import ru.gravit.launcher.request.ResultInterface;
import ru.gravit.utils.helper.LogHelper;

import java.util.HashSet;

public class WaitEventHandler implements ClientWebSocketService.EventHandler {
    public HashSet<ResultEvent> requests = new HashSet<>();
    @Override
    public void process(ResultInterface result) {
        LogHelper.debug("Processing event %s type", result.getType());
        for(ResultEvent r : requests)
        {
            LogHelper.subDebug("Processing %s", r.type);
            if(r.type.equals(result.getType()) || result.getType().equals("error"))
            {
                LogHelper.debug("Event %s type", r.type);
                synchronized (r)
                {
                    r.result = result;
                    r.ready = true;
                    r.notifyAll();
                }
            }
        }
    }
    public static class ResultEvent
    {
        public ResultInterface result;
        public String type;
        public boolean ready;
    }
}
