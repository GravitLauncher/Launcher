package pro.gravit.launcher.request.admin;

import pro.gravit.launcher.request.Request;
import pro.gravit.launcher.request.ResultInterface;
import pro.gravit.launcher.request.websockets.RequestInterface;

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
