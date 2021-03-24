package pro.gravit.launcher.request.management;

import pro.gravit.launcher.events.request.PingServerReportRequestEvent;
import pro.gravit.launcher.request.Request;

import java.util.List;

public class PingServerReportRequest extends Request<PingServerReportRequestEvent> {
    public final String name;
    public final PingServerReport data;

    public PingServerReportRequest(String name, PingServerReport data) {
        this.name = name;
        this.data = data;
    }

    @Override
    public String getType() {
        return "pingServerReport";
    }

    public static class PingServerReport {
        public final String name;
        public final int maxPlayers; // player slots
        public final int playersOnline;
        //Server addional info
        public double tps; //Server tps
        public List<UsernameInfo> users;

        public PingServerReport(String name, int maxPlayers, int playersOnline) {
            this.name = name;
            this.maxPlayers = maxPlayers;
            this.playersOnline = playersOnline;
        }

        public static class UsernameInfo {
            public final String username;

            public UsernameInfo(String username) {
                this.username = username;
            }
        }
    }
}
