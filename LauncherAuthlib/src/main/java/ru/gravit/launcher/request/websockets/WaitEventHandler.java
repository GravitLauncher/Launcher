package ru.gravit.launcher.request.websockets;

import ru.gravit.launcher.events.RequestEvent;
import ru.gravit.launcher.request.ResultInterface;
import ru.gravit.utils.helper.LogHelper;

import java.util.HashSet;
import java.util.UUID;

public class WaitEventHandler implements ClientWebSocketService.EventHandler {
    public HashSet<ResultEvent> requests = new HashSet<>();

    @Override
    public void process(ResultInterface result) {
        LogHelper.debug("Processing event %s type", result.getType());
        UUID checkUUID = null;
        if(result instanceof RequestEvent)
        {
            RequestEvent event = (RequestEvent) result;
            checkUUID = event.requestUUID;
            if(checkUUID != null)
            LogHelper.debug("Event UUID: %s found", checkUUID.toString());
        }
        for (ResultEvent r : requests) {
            if(r.uuid != null)
                LogHelper.debug("Request UUID found: %s", r.uuid.toString());
            if( (r.uuid != null && r.uuid.equals(checkUUID)) || ( checkUUID == null && (r.type.equals(result.getType()) || result.getType().equals("error") )) ) {
                LogHelper.debug("Event %s type", r.type);
                synchronized (r) {
                    r.result = result;
                    r.ready = true;
                    r.notifyAll();
                }
            }
        }
    }

    public static class ResultEvent {
        public ResultInterface result;
        public UUID uuid;
        public String type;
        public boolean ready;
    }
}
