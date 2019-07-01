package pro.gravit.launcher.request.admin;

import pro.gravit.launcher.request.Request;
import pro.gravit.launcher.request.WebSocketEvent;
import pro.gravit.launcher.request.websockets.WebSocketRequest;

public class ProxyRequest extends Request<WebSocketEvent> implements WebSocketRequest {
    public WebSocketRequest response;
    public long session;
    public boolean isCheckSign;

    public ProxyRequest(WebSocketRequest response, long session) {
        this.response = response;
        this.session = session;
    }

    @Override
    public String getType() {
        return "proxy";
    }
}
