package pro.gravit.launchserver.socket;

import pro.gravit.launcher.serialize.HInput;
import pro.gravit.launcher.serialize.HOutput;

public class SocketContext {
    public HInput input;
    public HOutput output;
    public long session;
    public String ip;
    public Integer type;
    public Client client;
}
