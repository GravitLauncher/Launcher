package pro.gravit.launchserver.auth.protect;

import pro.gravit.launcher.events.request.GetSecureLevelInfoRequestEvent;
import pro.gravit.launcher.events.request.HardwareReportRequestEvent;
import pro.gravit.launcher.events.request.VerifySecureLevelKeyRequestEvent;
import pro.gravit.launchserver.auth.protect.interfaces.HardwareProtectHandler;
import pro.gravit.launchserver.auth.protect.interfaces.SecureProtectHandler;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.auth.AuthResponse;
import pro.gravit.launchserver.socket.response.secure.HardwareReportResponse;

public class AdvancedProtectHandler extends StdProtectHandler implements SecureProtectHandler, HardwareProtectHandler {
    public boolean enableHardwareFeature;
    @Override
    public boolean allowGetAccessToken(AuthResponse.AuthContext context) {
        return (context.authType == AuthResponse.ConnectTypes.CLIENT) && context.client.checkSign;
    }

    @Override
    public void checkLaunchServerLicense() {

    }

    @Override
    public GetSecureLevelInfoRequestEvent onGetSecureLevelInfo(GetSecureLevelInfoRequestEvent event) {
        return event;
    }

    @Override
    public boolean allowGetSecureLevelInfo(Client client) {
        return client.checkSign;
    }

    @Override
    public void onHardwareReport(HardwareReportResponse response, Client client) {
        if(!enableHardwareFeature)
        {
            response.sendResult(new HardwareReportRequestEvent());
            return;
        }

        response.sendResult(new HardwareReportRequestEvent());
    }

    @Override
    public VerifySecureLevelKeyRequestEvent onSuccessVerify(Client client) {
        if(enableHardwareFeature)
        {
            return new VerifySecureLevelKeyRequestEvent(true);
        }
        return new VerifySecureLevelKeyRequestEvent();
    }
}
