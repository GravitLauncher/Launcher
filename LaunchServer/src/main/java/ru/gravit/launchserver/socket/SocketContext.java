package ru.gravit.launchserver.socket;

import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;

public class SocketContext {
    HInput input;
    HOutput output;
    long session;
    String ip;
    Integer type;
}
