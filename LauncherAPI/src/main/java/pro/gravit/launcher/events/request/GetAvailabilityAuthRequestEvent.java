package pro.gravit.launcher.events.request;

import java.util.List;

import pro.gravit.launcher.LauncherNetworkAPI;
import pro.gravit.launcher.events.RequestEvent;

public class GetAvailabilityAuthRequestEvent extends RequestEvent {
    public static class AuthAvailability {
        @LauncherNetworkAPI
        public String name;
        @LauncherNetworkAPI
        public String displayName;

        public AuthAvailability(String name, String displayName) {
            this.name = name;
            this.displayName = displayName;
        }
    }

    @LauncherNetworkAPI
    public List<AuthAvailability> list;

    public GetAvailabilityAuthRequestEvent(List<AuthAvailability> list) {
        this.list = list;
    }

    @Override
    public String getType() {
        return "getAvailabilityAuth";
    }
}
