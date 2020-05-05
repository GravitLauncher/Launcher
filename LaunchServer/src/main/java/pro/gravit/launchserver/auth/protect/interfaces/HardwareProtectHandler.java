package pro.gravit.launchserver.auth.protect.interfaces;

import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.secure.HardwareReportResponse;

public interface HardwareProtectHandler {
    void onHardwareReport(HardwareReportResponse response, Client client);
}
