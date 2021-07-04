package pro.gravit.launcher.request.auth;

import pro.gravit.launcher.events.request.AdditionalDataRequestEvent;
import pro.gravit.launcher.request.Request;

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
