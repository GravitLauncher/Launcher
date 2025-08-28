package pro.gravit.launcher.base.modules.events;

import pro.gravit.launcher.base.modules.LauncherModule;
import pro.gravit.launcher.base.request.RequestService;

public class OfflineModeEvent extends LauncherModule.Event {
    public RequestService service;

    public OfflineModeEvent(RequestService service) {
        this.service = service;
    }
}
