package pro.gravit.launchserver.auth.protect;

import pro.gravit.launcher.events.request.GetSecureLevelInfoRequestEvent;
import pro.gravit.launcher.events.request.HardwareReportRequestEvent;
import pro.gravit.launcher.events.request.VerifySecureLevelKeyRequestEvent;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.Reconfigurable;
import pro.gravit.launchserver.auth.protect.hwid.HWIDException;
import pro.gravit.launchserver.auth.protect.hwid.HWIDProvider;
import pro.gravit.launchserver.auth.protect.interfaces.HardwareProtectHandler;
import pro.gravit.launchserver.auth.protect.interfaces.JoinServerProtectHandler;
import pro.gravit.launchserver.auth.protect.interfaces.SecureProtectHandler;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.auth.AuthResponse;
import pro.gravit.launchserver.socket.response.secure.HardwareReportResponse;
import pro.gravit.utils.command.Command;
import pro.gravit.utils.helper.LogHelper;

import java.util.HashMap;
import java.util.Map;

public class AdvancedProtectHandler extends StdProtectHandler implements SecureProtectHandler, HardwareProtectHandler, JoinServerProtectHandler, Reconfigurable {
    public boolean enableHardwareFeature;
    public HWIDProvider provider;
    private transient LaunchServer server;

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
        if (!enableHardwareFeature) {
            response.sendResult(new HardwareReportRequestEvent());
            return;
        }
        try {
            if (!client.isAuth || client.trustLevel == null || client.trustLevel.publicKey == null) {
                response.sendError("Access denied");
                return;
            }
            provider.normalizeHardwareInfo(response.hardware);
            LogHelper.debug("[HardwareInfo] HardwareInfo received");
            boolean needCreate = !provider.addPublicKeyToHardwareInfo(response.hardware, client.trustLevel.publicKey, client);
            LogHelper.debug("[HardwareInfo] HardwareInfo needCreate: %s", needCreate ? "true" : "false");
            if (needCreate)
                provider.createHardwareInfo(response.hardware, client.trustLevel.publicKey, client);
            client.trustLevel.hardwareInfo = response.hardware;
        } catch (HWIDException e) {
            throw new SecurityException(e.getMessage());
        }
        response.sendResult(new HardwareReportRequestEvent());
    }

    @Override
    public VerifySecureLevelKeyRequestEvent onSuccessVerify(Client client) {
        if (enableHardwareFeature) {
            if (provider == null) {
                LogHelper.warning("HWIDProvider null. HardwareInfo not checked!");
            } else {
                try {
                    client.trustLevel.hardwareInfo = provider.findHardwareInfoByPublicKey(client.trustLevel.publicKey, client);
                    if (client.trustLevel.hardwareInfo == null) //HWID not found?
                        return new VerifySecureLevelKeyRequestEvent(true);
                } catch (HWIDException e) {
                    throw new SecurityException(e.getMessage()); //Show banned message
                }
            }
            return new VerifySecureLevelKeyRequestEvent(false);
        }
        return new VerifySecureLevelKeyRequestEvent();
    }

    @Override
    public Map<String, Command> getCommands() {
        Map<String, Command> commands = new HashMap<>();
        if (provider instanceof Reconfigurable) {
            commands.putAll(((Reconfigurable) provider).getCommands());
        }
        return commands;
    }

    @Override
    public boolean onJoinServer(String serverID, String username, Client client) {
        return !enableHardwareFeature || (client.trustLevel != null && client.trustLevel.hardwareInfo != null);
    }

    @Override
    public void init(LaunchServer server) {
        if (provider != null)
            provider.init(server);
    }

    @Override
    public void close() {
        if (provider != null)
            provider.close();
    }
}
