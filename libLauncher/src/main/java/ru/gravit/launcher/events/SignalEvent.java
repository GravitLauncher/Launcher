package ru.gravit.launcher.events;

import ru.gravit.utils.event.EventInterface;

import java.util.UUID;

//Используется, что бы послать короткое сообщение, которое вмещается в int
public class SignalEvent implements EventInterface {
    private static final UUID uuid = UUID.fromString("edc3afa1-2726-4da3-95c6-7e6994b981e1");
    public int signal;

    public SignalEvent(int signal) {
        this.signal = signal;
    }

    @Override
    public UUID getUUID() {
        return uuid;
    }
}
