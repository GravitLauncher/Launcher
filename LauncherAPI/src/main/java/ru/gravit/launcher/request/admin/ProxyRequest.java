package ru.gravit.launcher.request.admin;

import ru.gravit.launcher.request.Request;
import ru.gravit.launcher.request.ResultInterface;
import ru.gravit.launcher.request.websockets.RequestInterface;

public class ProxyRequest extends Request<ResultInterface> implements RequestInterface {
    public RequestInterface response;
    public long session;
    public boolean isCheckSign;

    public ProxyRequest(RequestInterface response, long session) {
        this.response = response;
        this.session = session;
    }

    @Override
    public String getType() {
        return "proxy";
    }
}
