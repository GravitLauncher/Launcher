package ru.gravit.launcher.events.request;

import ru.gravit.launcher.request.ResultInterface;

public class RestoreSessionRequestEvent implements ResultInterface {
    @Override
    public String getType() {
        return "restoreSession";
    }
}
