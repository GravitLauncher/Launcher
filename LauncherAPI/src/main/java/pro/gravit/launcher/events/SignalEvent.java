package pro.gravit.launcher.events;

import pro.gravit.launcher.LauncherNetworkAPI;

//Используется, что бы послать короткое сообщение, которое вмещается в int
public class SignalEvent {
    @LauncherNetworkAPI
    public int signal;

    public SignalEvent(int signal) {
        this.signal = signal;
    }
}
