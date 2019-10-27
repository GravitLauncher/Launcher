package pro.gravit.launcher.events.request;

import pro.gravit.launcher.LauncherNetworkAPI;
import pro.gravit.launcher.events.RequestEvent;

import java.util.List;

public class GetAvailabilityAuthRequestEvent extends RequestEvent {
    public static class AuthAvailability {
        @LauncherNetworkAPI
        public final String name;
        @LauncherNetworkAPI
        public final String displayName;

        public AuthAvailability(String name, String displayName) {
            this.name = name;
            this.displayName = displayName;
        }
    }

    @LauncherNetworkAPI
    public final List<AuthAvailability> list;

    public GetAvailabilityAuthRequestEvent(List<AuthAvailability> list) {
        this.list = list;
    }

    @Override
    public String getType() {
        return "getAvailabilityAuth";
    }
}
