package ru.gravit.launcher.request.auth;

import java.io.IOException;

import ru.gravit.launcher.LauncherAPI;
import ru.gravit.launcher.LauncherConfig;
import ru.gravit.launcher.client.ClientLauncher;
import ru.gravit.utils.helper.JVMHelper;
import ru.gravit.utils.helper.SecurityHelper;
import ru.gravit.utils.helper.VerifyHelper;
import ru.gravit.launcher.profiles.PlayerProfile;
import ru.gravit.launcher.request.Request;
import ru.gravit.launcher.request.RequestType;
import ru.gravit.launcher.request.auth.AuthRequest.Result;
import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;
import ru.gravit.launcher.serialize.SerializeLimits;
import ru.zaxar163.GuardBind;

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

    @LauncherAPI
    public AuthRequest(LauncherConfig config, String login, byte[] encryptedPassword) {
        super(config);
        this.login = VerifyHelper.verify(login, VerifyHelper.NOT_EMPTY, "Login can't be empty");
        this.encryptedPassword = encryptedPassword.clone();
        auth_id = 0;
    }
    @LauncherAPI
    public AuthRequest(LauncherConfig config, String login, byte[] encryptedPassword,int auth_id) {
        super(config);
        this.login = VerifyHelper.verify(login, VerifyHelper.NOT_EMPTY, "Login can't be empty");
        this.encryptedPassword = encryptedPassword.clone();
        this.auth_id = auth_id;
    }

    @LauncherAPI
    public AuthRequest(String login, byte[] encryptedPassword) {
        this(null, login, encryptedPassword);
    }
    @LauncherAPI
    public AuthRequest(String login, byte[] encryptedPassword,int auth_id) {
        this(null, login, encryptedPassword,auth_id);
    }

    @Override
    public Integer getType() {
        return RequestType.AUTH.getNumber();
    }

    @Override
    protected Result requestDo(HInput input, HOutput output) throws IOException {
        output.writeString(login, SerializeLimits.MAX_LOGIN);
        output.writeString(ClientLauncher.profile.getTitle(), SerializeLimits.MAX_CLIENT);
        output.writeInt(auth_id);
        output.writeLong(JVMHelper.OS_TYPE == JVMHelper.OS.MUSTDIE ? GuardBind.avnGetHddId() : 0);
        output.writeLong(JVMHelper.OS_TYPE == JVMHelper.OS.MUSTDIE ? GuardBind.avnGetCpuid() : 0);
        output.writeLong(JVMHelper.OS_TYPE == JVMHelper.OS.MUSTDIE ? GuardBind.avnGetSmbiosId() : 0);
        output.writeByteArray(encryptedPassword, SecurityHelper.CRYPTO_MAX_LENGTH);
        output.flush();

        // Read UUID and access token
        readError(input);
        PlayerProfile pp = new PlayerProfile(input);
        String accessToken = input.readASCII(-SecurityHelper.TOKEN_STRING_LENGTH);
        return new Result(pp, accessToken);
    }
}
