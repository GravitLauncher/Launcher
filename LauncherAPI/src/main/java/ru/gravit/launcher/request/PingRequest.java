package ru.gravit.launcher.request;

import ru.gravit.launcher.LauncherAPI;
import ru.gravit.launcher.LauncherConfig;
import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;
import ru.gravit.launcher.serialize.SerializeLimits;

import java.io.IOException;

public final class PingRequest extends Request<Void> {

    @LauncherAPI
    public PingRequest() {
        this(null);
    }

    @LauncherAPI
    public PingRequest(LauncherConfig config) {
        super(config);
    }
}
