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
        @LauncherNetworkAPI
        public final AuthType firstType;
        @LauncherNetworkAPI
        public final AuthType secondType;

        public AuthAvailability(String name, String displayName, AuthType firstType, AuthType secondType) {
            this.name = name;
            this.displayName = displayName;
            this.firstType = firstType;
            this.secondType = secondType;
        }


        public enum AuthType
        {
            @LauncherNetworkAPI
            PASSWORD,
            @LauncherNetworkAPI
            KEY,
            @LauncherNetworkAPI
            TOTP,
            @LauncherNetworkAPI
            OAUTH,
            @LauncherNetworkAPI
            NONE,
            @LauncherNetworkAPI
            OTHER
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
