package pro.gravit.launcher.events.request;

import pro.gravit.launcher.events.RequestEvent;

public class ServerStatusRequestEvent extends RequestEvent {
    public final String projectName;
    public long totalJavaMemory;
    public long freeJavaMemory;

    //Latency
    public long shortLatency; //Millis
    public long middleLatency; //Millis
    public long longLatency; //Millis
    public long latency;

    public ServerStatusRequestEvent(String projectName) {
        this.projectName = projectName;
    }

    @Override
    public String getType() {
        return "serverStatus";
    }
}
