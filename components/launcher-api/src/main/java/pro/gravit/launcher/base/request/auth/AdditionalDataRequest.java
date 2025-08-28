package pro.gravit.launcher.base.request.auth;

import pro.gravit.launcher.base.events.request.AdditionalDataRequestEvent;
import pro.gravit.launcher.base.request.Request;

import java.util.UUID;

public class AdditionalDataRequest extends Request<AdditionalDataRequestEvent> {
    public String username;
    public UUID uuid;

    public AdditionalDataRequest(String username) {
        this.username = username;
    }

    public AdditionalDataRequest(UUID uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getType() {
        return "additionalData";
    }
}
