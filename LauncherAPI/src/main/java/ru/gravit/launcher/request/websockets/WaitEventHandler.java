package ru.gravit.launcher.request.websockets;

import java.util.HashSet;

public class WaitEventHandler implements ClientWebSocketService.EventHandler {
    public HashSet<ResultEvent> requests;
    @Override
    public void process(ResultInterface result) {
        for(ResultEvent r : requests)
        {
            if(r.type.equals(result.getType()))
            {
                synchronized (r)
                {
                    r.result = result;
                    r.ready = true;
                    r.notifyAll();
                }
            }
        }
    }
    public class ResultEvent
    {
        public ResultInterface result;
        public String type;
        public boolean ready;
    }
}
