package pro.gravit.launcher.events.request;

import java.util.UUID;

import pro.gravit.launcher.LauncherNetworkAPI;
import pro.gravit.launcher.events.RequestEvent;
import pro.gravit.launcher.profiles.ClientProfile;


public class SetProfileRequestEvent extends RequestEvent {
    @SuppressWarnings("unused")
	private static final UUID uuid = UUID.fromString("08c0de9e-4364-4152-9066-8354a3a48541");
    @LauncherNetworkAPI
    public ClientProfile newProfile;

    public SetProfileRequestEvent(ClientProfile newProfile) {
        this.newProfile = newProfile;
    }

    @Override
    public String getType() {
        return "setProfile";
    }
}
