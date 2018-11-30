package ru.gravit.launcher.request.auth;

import java.io.IOException;

import ru.gravit.launcher.Launcher;
import ru.gravit.launcher.LauncherAPI;
import ru.gravit.launcher.LauncherConfig;
import ru.gravit.launcher.LauncherHWIDInterface;
import ru.gravit.utils.helper.SecurityHelper;
import ru.gravit.utils.helper.VerifyHelper;
import ru.gravit.launcher.profiles.PlayerProfile;
import ru.gravit.launcher.request.Request;
import ru.gravit.launcher.request.RequestType;
import ru.gravit.launcher.request.auth.AuthRequest.Result;
import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;
import ru.gravit.launcher.serialize.SerializeLimits;

public final class AuthRequest extends Request<Result> {
    public static final class Result {
        @LauncherAPI
        public final PlayerProfile pp;
        @LauncherAPI
        public final String accessToken;

        private Result(PlayerProfile pp, String accessToken) {
            this.pp = pp;
            this.accessToken = accessToken;
        }
    }

    private final String login;

    private final byte[] encryptedPassword;
    private final int auth_id;
    private final LauncherHWIDInterface hwid;

    @LauncherAPI
    public AuthRequest(LauncherConfig config, String login, byte[] encryptedPassword, LauncherHWIDInterface hwid) {
        super(config);
        this.login = VerifyHelper.verify(login, VerifyHelper.NOT_EMPTY, "Login can't be empty");
        this.encryptedPassword = encryptedPassword.clone();
        this.hwid = hwid;
        auth_id = 0;
    }

    @LauncherAPI
    public AuthRequest(LauncherConfig config, String login, byte[] encryptedPassword, LauncherHWIDInterface hwid, int auth_id) {
        super(config);
        this.login = VerifyHelper.verify(login, VerifyHelper.NOT_EMPTY, "Login can't be empty");
        this.encryptedPassword = encryptedPassword.clone();
        this.hwid = hwid;
        this.auth_id = auth_id;
    }

    @LauncherAPI
    public AuthRequest(String login, byte[] encryptedPassword, LauncherHWIDInterface hwid) {
        this(null, login, encryptedPassword,hwid);
    }

    @LauncherAPI
    public AuthRequest(String login, byte[] encryptedPassword, LauncherHWIDInterface hwid, int auth_id) {
        this(null, login, encryptedPassword, hwid, auth_id);
    }

    @Override
    public Integer getType() {
        return RequestType.AUTH.getNumber();
    }

    @Override
    protected Result requestDo(HInput input, HOutput output) throws IOException {
        output.writeString(login, SerializeLimits.MAX_LOGIN);
        output.writeBoolean(Launcher.profile != null);
        if (Launcher.profile != null)
            output.writeString(Launcher.profile.getTitle(), SerializeLimits.MAX_CLIENT);
        output.writeInt(auth_id);
        output.writeString(hwid.getHWID().getSerializeString(),0);
        //output.writeLong(0);
        //output.writeLong(0);
        //output.writeLong(0);
        output.writeByteArray(encryptedPassword, SecurityHelper.CRYPTO_MAX_LENGTH);
        output.flush();

        // Read UUID and access token
        readError(input);
        PlayerProfile pp = new PlayerProfile(input);
        String accessToken = input.readASCII(-SecurityHelper.TOKEN_STRING_LENGTH);
        return new Result(pp, accessToken);
    }
}
