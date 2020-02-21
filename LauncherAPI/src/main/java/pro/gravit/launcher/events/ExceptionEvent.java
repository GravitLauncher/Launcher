package pro.gravit.launcher.events;

import pro.gravit.launcher.LauncherNetworkAPI;

public class ExceptionEvent extends RequestEvent {
    public ExceptionEvent(Exception e) {
        this.message = e.getMessage();
        this.clazz = e.getClass().getName();
    }

    @LauncherNetworkAPI
    public final String message;
    @LauncherNetworkAPI
    public final String clazz;

    @Override
    public String getType() {
        return "exception";
    }
}
