package pro.gravit.launcher.events.request;

import pro.gravit.launcher.LauncherNetworkAPI;
import pro.gravit.launcher.events.RequestEvent;

import java.util.List;

public class GetAvailabilityAuthRequestEvent extends RequestEvent {
    @LauncherNetworkAPI
    public final List<AuthAvailability> list;
    @LauncherNetworkAPI
    public final long features;

    public GetAvailabilityAuthRequestEvent(List<AuthAvailability> list) {
        this.list = list;
        this.features = ServerFeature.FEATURE_SUPPORT.val;
    }

    public GetAvailabilityAuthRequestEvent(List<AuthAvailability> list, long features) {
        this.list = list;
        this.features = features;
    }

    @Override
    public String getType() {
        return "getAvailabilityAuth";
    }

    public enum ServerFeature {
        FEATURE_SUPPORT(1);
        public final int val;

        ServerFeature(int val) {
            this.val = val;
        }
    }

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


        public enum AuthType {
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
}
