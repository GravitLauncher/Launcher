package pro.gravit.launcher.request;

import pro.gravit.launcher.request.websockets.StandartClientWebSocketService;

public final class PingRequest extends Request<WebSocketEvent> {

    @Override
    protected WebSocketEvent requestDo(StandartClientWebSocketService service) {
        return null;
    }

    @Override
    public String getType() {
        return null;
    }
}
