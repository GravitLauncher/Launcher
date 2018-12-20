package ru.gravit.launcher.events;

import ru.gravit.utils.event.EventInterface;

import java.util.UUID;

//Набор стандартных событий
public class ControlEvent implements EventInterface {
    private static final UUID uuid = UUID.fromString("f1051a64-0cd0-4ed8-8430-d856a196e91f");

    public enum ControlCommand {
        STOP, START, PAUSE, CONTINUE, CRASH
    }

    public ControlEvent(ControlCommand signal) {
        this.signal = signal;
    }

    public ControlCommand signal;

    @Override
    public UUID getUUID() {
        return uuid;
    }
}
