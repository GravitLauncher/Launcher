package ru.gravit.launcher.events.request;

import ru.gravit.launcher.LauncherNetworkAPI;
import ru.gravit.launcher.events.RequestEvent;
import ru.gravit.launcher.request.ResultInterface;

import java.util.List;

public class GetAvailabilityAuthRequestEvent extends RequestEvent {
    public static class AuthAvailability
    {
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
