package ru.gravit.launcher.events.request;

import ru.gravit.launcher.profiles.ClientProfile;
import ru.gravit.launcher.request.ResultInterface;
import ru.gravit.utils.event.EventInterface;

import java.util.UUID;

public class SetProfileRequestEvent implements ResultInterface, EventInterface {
    private static final UUID uuid = UUID.fromString("08c0de9e-4364-4152-9066-8354a3a48541");
    public ClientProfile newProfile;

    public SetProfileRequestEvent(ClientProfile newProfile) {
        this.newProfile = newProfile;
    }

    @Override
    public String getType() {
        return "setProfile";
    }

    @Override
    public UUID getUUID() {
        return uuid;
    }
}
