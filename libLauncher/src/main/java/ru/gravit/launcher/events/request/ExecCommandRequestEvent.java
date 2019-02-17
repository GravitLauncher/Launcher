package ru.gravit.launcher.events.request;

import ru.gravit.launcher.request.ResultInterface;

public class ExecCommandRequestEvent implements ResultInterface {
    @Override
    public String getType() {
        return "execCmd";
    }
    boolean success;

    public ExecCommandRequestEvent(boolean success) {
        this.success = success;
    }
}
