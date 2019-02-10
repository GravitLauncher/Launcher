package ru.gravit.launcher.request.auth;

import ru.gravit.launcher.*;
import ru.gravit.launcher.profiles.PlayerProfile;
import ru.gravit.launcher.request.Request;
import ru.gravit.launcher.request.RequestType;
import ru.gravit.launcher.request.auth.AuthRequest.Result;
import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;
import ru.gravit.launcher.serialize.SerializeLimits;
import ru.gravit.utils.helper.SecurityHelper;
import ru.gravit.utils.helper.VerifyHelper;

import java.io.IOException;

public final class AuthRequest extends Request<Result> {
    public static final class Result {
        @LauncherAPI
        public final PlayerProfile pp;
        @LauncherAPI
        public final String accessToken;
        @LauncherAPI
        public final ClientPermissions permissions;

        private Result(PlayerProfile pp, String accessToken, ClientPermissions permissions) {
            this.pp = pp;
            this.accessToken = accessToken;
            this.permissions = permissions;
        }
    }

    private final String login;

    private final byte[] encryptedPassword;
    private final int auth_id;
    private final HWID hwid;
    private final String customText;

    @LauncherAPI
    public AuthRequest(LauncherConfig config, String login, byte[] encryptedPassword, HWID hwid) {
        super(config);
        this.login = VerifyHelper.verify(login, VerifyHelper.NOT_EMPTY, "Login can't be empty");
        this.encryptedPassword = encryptedPassword.clone();
        this.hwid = hwid;
        customText = "";
        auth_id = 0;
    }
    @LauncherAPI
    public AuthRequest(LauncherConfig config, String login, byte[] encryptedPassword, HWID hwid, String customText) {
        super(config);
        this.login = VerifyHelper.verify(login, VerifyHelper.NOT_EMPTY, "Login can't be empty");
        this.encryptedPassword = encryptedPassword.clone();
        this.hwid = hwid;
        this.customText = customText;
        auth_id = 0;
    }

    @LauncherAPI
    public AuthRequest(LauncherConfig config, String login, byte[] encryptedPassword, HWID hwid, int auth_id) {
        super(config);
        this.login = VerifyHelper.verify(login, VerifyHelper.NOT_EMPTY, "Login can't be empty");
        this.encryptedPassword = encryptedPassword.clone();
        this.hwid = hwid;
        this.auth_id = auth_id;
        customText = "";
    }
    @LauncherAPI
    public AuthRequest(LauncherConfig config, String login, byte[] encryptedPassword, HWID hwid, String customText, int auth_id) {
        super(config);
        this.login = VerifyHelper.verify(login, VerifyHelper.NOT_EMPTY, "Login can't be empty");
        this.encryptedPassword = encryptedPassword.clone();
        this.hwid = hwid;
        this.auth_id = auth_id;
        this.customText = customText;
    }

    @LauncherAPI
    public AuthRequest(String login, byte[] encryptedPassword, HWID hwid) {
        this(null, login, encryptedPassword, hwid);
    }

    @LauncherAPI
    public AuthRequest(String login, byte[] encryptedPassword, HWID hwid, int auth_id) {
        this(null, login, encryptedPassword, hwid, auth_id);
    }

    @Override
    public Integer getType() {
        return RequestType.AUTH.getNumber();
    }
    /*public class EchoRequest implements RequestInterface
    {
        String echo;

        public EchoRequest(String echo) {
            this.echo = echo;
        }

        @Override
        public String getType() {
            return "echo";
        }
    }*/
    @Override
    protected Result requestDo(HInput input, HOutput output) throws IOException {
        /*try {
            LegacyRequestBridge.sendRequest(new EchoRequest("Hello World!"));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/
        output.writeString(login, SerializeLimits.MAX_LOGIN);
        output.writeBoolean(Launcher.profile != null);
        if (Launcher.profile != null)
            output.writeString(Launcher.profile.getTitle(), SerializeLimits.MAX_CLIENT);
        output.writeInt(auth_id);
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
        return new Result(pp, accessToken, permissions);
    }
}
