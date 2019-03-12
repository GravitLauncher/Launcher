package ru.gravit.launchserver.socket;

import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;

public class SocketContext {
    public HInput input;
    public HOutput output;
    public long session;
    public String ip;
    public Integer type;
    public Client client;
}
