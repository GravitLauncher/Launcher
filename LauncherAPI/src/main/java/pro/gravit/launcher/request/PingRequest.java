package pro.gravit.launcher.request;

import pro.gravit.launcher.request.websockets.StandartClientWebSocketService;

public final class PingRequest extends Request<ResultInterface> {

    @Override
    protected ResultInterface requestDo(StandartClientWebSocketService service) throws Exception {
        return null;
    }

    @Override
    public String getType() {
        return null;
    }
}
