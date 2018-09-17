package ru.gravit.utils.websocket;

import ru.gravit.launcher.serialize.HInput;

@FunctionalInterface
public interface MessageInterface {
    void request(HInput input);
}
