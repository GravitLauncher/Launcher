package ru.gravit.launcher.events.request;

import ru.gravit.launcher.LauncherNetworkAPI;
import ru.gravit.launcher.hasher.HashedDir;
import ru.gravit.launcher.request.ResultInterface;

public class UpdateRequestEvent implements ResultInterface {
    @LauncherNetworkAPI
    public HashedDir hdir;
    @Override
    public String getType() {
        return "update";
    }

    public UpdateRequestEvent(HashedDir hdir) {
        this.hdir = hdir;
    }
}
