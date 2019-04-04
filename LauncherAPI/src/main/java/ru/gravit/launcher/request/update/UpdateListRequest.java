package ru.gravit.launcher.request.update;

import ru.gravit.launcher.LauncherAPI;
import ru.gravit.launcher.LauncherConfig;
import ru.gravit.launcher.events.request.UpdateListRequestEvent;
import ru.gravit.launcher.request.Request;
import ru.gravit.launcher.request.RequestType;
import ru.gravit.launcher.request.websockets.LegacyRequestBridge;
import ru.gravit.launcher.request.websockets.RequestInterface;
import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;
import ru.gravit.utils.helper.IOHelper;

import java.io.IOException;
import java.util.HashSet;

public final class UpdateListRequest extends Request<UpdateListRequestEvent> implements RequestInterface {
    @LauncherAPI
    public UpdateListRequest() {
        this(null);
    }

    @LauncherAPI
    public UpdateListRequest(LauncherConfig config) {
        super(config);
    }

    @Override
    public UpdateListRequestEvent requestWebSockets() throws Exception {
        return (UpdateListRequestEvent) LegacyRequestBridge.sendRequest(this);
    }

    @Override
    public String getType() {
        return "updateList";
    }
}
