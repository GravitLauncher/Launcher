package pro.gravit.launcher.request.auth;

import pro.gravit.launcher.events.request.RestoreRequestEvent;
import pro.gravit.launcher.request.Request;

import java.util.Map;

public class RestoreRequest extends Request<RestoreRequestEvent> {
    public String authId;
    public String accessToken;
    public Map<String, String> extended;
    public boolean needUserInfo;

    public RestoreRequest() {
    }

    public RestoreRequest(String authId, String accessToken, Map<String, String> extended, boolean needUserInfo) {
        this.authId = authId;
        this.accessToken = accessToken;
        this.extended = extended;
        this.needUserInfo = needUserInfo;
    }

    @Override
    public String getType() {
        return "restore";
    }
}
