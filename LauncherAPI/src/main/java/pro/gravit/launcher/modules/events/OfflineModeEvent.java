package pro.gravit.launcher.modules.events;

import pro.gravit.launcher.modules.LauncherModule;
import pro.gravit.launcher.request.RequestService;

public class OfflineModeEvent extends LauncherModule.Event {
    public RequestService service;

    public OfflineModeEvent(RequestService service) {
        this.service = service;
    }
}
