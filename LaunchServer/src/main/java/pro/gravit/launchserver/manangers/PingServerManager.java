package pro.gravit.launchserver.manangers;

import pro.gravit.launcher.profiles.ClientProfile;
import pro.gravit.launcher.request.management.PingServerReportRequest;
import pro.gravit.launchserver.LaunchServer;

import java.util.HashMap;
import java.util.Map;

public class PingServerManager {
    public static final long REPORT_EXPIRED_TIME = 20 * 1000;
    public final Map<String, ServerInfoEntry> map = new HashMap<>();
    private final LaunchServer server;

    public PingServerManager(LaunchServer server) {
        this.server = server;
    }

    public void syncServers() {
        server.getProfiles().forEach((p) -> {
            for (ClientProfile.ServerProfile sp : p.getServers()) {
                ServerInfoEntry entry = map.get(sp.name);
                if (entry == null) {
                    map.put(sp.name, new ServerInfoEntry(p));
                }
            }
        });
    }

    public boolean updateServer(String name, PingServerReportRequest.PingServerReport report) {
        ServerInfoEntry entry = map.get(name);
        if (entry == null)
            return false;
        else {
            entry.lastReportTime = System.currentTimeMillis();
            entry.lastReport = report;
            return true;
        }
    }

    public static class ServerInfoEntry {
        public final ClientProfile profile;
        public PingServerReportRequest.PingServerReport lastReport;
        public long lastReportTime;

        public ServerInfoEntry(ClientProfile profile, PingServerReportRequest.PingServerReport lastReport) {
            this.lastReport = lastReport;
            this.profile = profile;
            this.lastReportTime = System.currentTimeMillis();
        }

        public ServerInfoEntry(ClientProfile profile) {
            this.profile = profile;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - lastReportTime > REPORT_EXPIRED_TIME;
        }
    }
}
