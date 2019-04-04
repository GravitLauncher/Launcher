package ru.gravit.launcher.request.auth;

import ru.gravit.launcher.*;
import ru.gravit.launcher.events.request.AuthRequestEvent;
import ru.gravit.launcher.profiles.PlayerProfile;
import ru.gravit.launcher.request.Request;
import ru.gravit.launcher.request.RequestType;
import ru.gravit.launcher.request.websockets.LegacyRequestBridge;
import ru.gravit.launcher.request.websockets.RequestInterface;
import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;
import ru.gravit.launcher.serialize.SerializeLimits;
import ru.gravit.utils.helper.SecurityHelper;
import ru.gravit.utils.helper.VerifyHelper;

import java.io.IOException;

public final class AuthRequest extends Request<AuthRequestEvent> implements RequestInterface {
    @LauncherNetworkAPI
    private final String login;
    @LauncherNetworkAPI
    private final byte[] encryptedPassword;
    @LauncherNetworkAPI
    private final String auth_id;
    @LauncherNetworkAPI
    private final HWID hwid;
    @LauncherNetworkAPI
    private final String customText;
    @LauncherNetworkAPI
    private final boolean getSession;
    @LauncherNetworkAPI
    private final ConnectTypes authType;

    public enum ConnectTypes {
        SERVER, CLIENT, BOT
    }

    @LauncherAPI
    public AuthRequest(LauncherConfig config, String login, byte[] password, HWID hwid) {
        super(config);
        this.login = VerifyHelper.verify(login, VerifyHelper.NOT_EMPTY, "Login can't be empty");
        this.encryptedPassword = password.clone();
        this.hwid = hwid;
        customText = "";
        auth_id = "";
        getSession = true;
        authType = ConnectTypes.CLIENT;
    }

    @LauncherAPI
    public AuthRequest(LauncherConfig config, String login, byte[] password, HWID hwid, String auth_id) {
        super(config);
        this.login = VerifyHelper.verify(login, VerifyHelper.NOT_EMPTY, "Login can't be empty");
        this.encryptedPassword = password.clone();
        this.hwid = hwid;
        this.auth_id = auth_id;
        customText = "";
        getSession = true;
        authType = ConnectTypes.CLIENT;
    }

    @LauncherAPI
    public AuthRequest(LauncherConfig config, String login, byte[] password, HWID hwid, String customText, String auth_id) {
        super(config);
        this.login = VerifyHelper.verify(login, VerifyHelper.NOT_EMPTY, "Login can't be empty");
        this.encryptedPassword = password.clone();
        this.hwid = hwid;
        this.auth_id = auth_id;
        this.customText = customText;
        getSession = true;
        authType = ConnectTypes.CLIENT;
    }

    @LauncherAPI
    public AuthRequest(String login, byte[] password, HWID hwid) {
        this(null, login, password, hwid);
    }

    public AuthRequest(String login, byte[] encryptedPassword, String auth_id, ConnectTypes authType) {
        this.login = login;
        this.encryptedPassword = encryptedPassword;
        this.auth_id = auth_id;
        this.authType = authType;
        this.hwid = null;
        this.customText = "";
        this.getSession = false;
    }

    @Override
    public AuthRequestEvent requestWebSockets() throws Exception {
        return (AuthRequestEvent) LegacyRequestBridge.sendRequest(this);
    }

    @LauncherAPI
    public AuthRequest(String login, byte[] password, HWID hwid, String auth_id) {
        this(null, login, password, hwid, auth_id);
    }

    @Override
    public Integer getLegacyType() {
        return RequestType.AUTH.getNumber();
    }

    /*public class EchoRequest implements RequestInterface
    {
        String echo;

        public EchoRequest(String echo) {
            this.echo = echo;
        }

        @Override
        public String getLegacyType() {
            return "echo";
        }
    }*/
    @Override
    protected AuthRequestEvent requestDo(HInput input, HOutput output) throws IOException {
        /*try {
            LegacyRequestBridge.sendRequest(new EchoRequest("Hello World!"));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/
        output.writeString(login, SerializeLimits.MAX_LOGIN);
        output.writeBoolean(Launcher.profile != null);
        if (Launcher.profile != null)
            output.writeString(Launcher.profile.getTitle(), SerializeLimits.MAX_CLIENT);
        output.writeString(auth_id, SerializeLimits.MAX_QUEUE_SIZE);
        output.writeString(hwid.getSerializeString(), 0);
        //output.writeLong(0);
        //output.writeLong(0);
        //output.writeLong(0);
        output.writeByteArray(encryptedPassword, SecurityHelper.CRYPTO_MAX_LENGTH);
        output.writeString(customText, SerializeLimits.MAX_CUSTOM_TEXT);
        output.flush();

        // Read UUID and access token
        readError(input);
        PlayerProfile pp = new PlayerProfile(input);
        String accessToken = input.readASCII(-SecurityHelper.TOKEN_STRING_LENGTH);
        ClientPermissions permissions = new ClientPermissions(input);
        String protectToken = input.readString(SerializeLimits.MAX_CUSTOM_TEXT);
        return new AuthRequestEvent(permissions, pp, accessToken, protectToken);
    }

    @Override
    public String getType() {
        return "auth";
    }
}
