package pro.gravit.launcher.base.request.auth;

import pro.gravit.launcher.core.LauncherNetworkAPI;
import pro.gravit.launcher.base.events.request.SetProfileRequestEvent;
import pro.gravit.launcher.base.profiles.ClientProfile;
import pro.gravit.launcher.base.request.Request;
import pro.gravit.launcher.base.request.websockets.WebSocketRequest;

import java.util.UUID;

public class SetProfileRequest extends Request<SetProfileRequestEvent> implements WebSocketRequest {
    @LauncherNetworkAPI
    public final UUID uuid;
    public final String tag;

    public SetProfileRequest(UUID uuid, String tag) {
        this.uuid = uuid;
        this.tag = tag;
    }

    public SetProfileRequest(ClientProfile profile) {
        this.uuid = profile.getUUID();
        this.tag = null;
    }

    @Override
    public String getType() {
        return "setProfile";
    }
}
