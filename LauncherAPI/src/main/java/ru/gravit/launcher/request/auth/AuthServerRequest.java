package ru.gravit.launcher.request.auth;

import ru.gravit.launcher.ClientPermissions;
import ru.gravit.launcher.LauncherAPI;
import ru.gravit.launcher.LauncherConfig;
import ru.gravit.launcher.profiles.PlayerProfile;
import ru.gravit.launcher.request.Request;
import ru.gravit.launcher.request.RequestType;
import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;
import ru.gravit.launcher.serialize.SerializeLimits;
import ru.gravit.utils.helper.SecurityHelper;
import ru.gravit.utils.helper.VerifyHelper;

import java.io.IOException;

public final class AuthServerRequest extends Request<ClientPermissions> {
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
    private final String auth_id;
    private final String title;

    @LauncherAPI
    public AuthServerRequest(LauncherConfig config, String login, byte[] encryptedPassword) {
        super(config);
        this.login = VerifyHelper.verify(login, VerifyHelper.NOT_EMPTY, "Login can't be empty");
        this.encryptedPassword = encryptedPassword.clone();
        auth_id = "";
        title = "";
    }

    @LauncherAPI
    public AuthServerRequest(LauncherConfig config, String login, byte[] encryptedPassword, String auth_id) {
        super(config);
        this.login = VerifyHelper.verify(login, VerifyHelper.NOT_EMPTY, "Login can't be empty");
        this.encryptedPassword = encryptedPassword.clone();
        this.auth_id = auth_id;
        title = "";
    }

    @LauncherAPI
    public AuthServerRequest(LauncherConfig config, String login, byte[] encryptedPassword, String auth_id, String title) {
        super(config);
        this.login = VerifyHelper.verify(login, VerifyHelper.NOT_EMPTY, "Login can't be empty");
        this.encryptedPassword = encryptedPassword.clone();
        this.auth_id = auth_id;
        this.title = title;
    }

    @LauncherAPI
    public AuthServerRequest(String login, byte[] encryptedPassword) {
        this(null, login, encryptedPassword);
    }

    @LauncherAPI
    public AuthServerRequest(String login, byte[] encryptedPassword, String auth_id) {
        this(null, login, encryptedPassword, auth_id);
    }

    @Override
    public Integer getLegacyType() {
        return RequestType.SERVERAUTH.getNumber();
    }

    @Override
    protected ClientPermissions requestDo(HInput input, HOutput output) throws IOException {
        output.writeString(login, SerializeLimits.MAX_LOGIN);
        output.writeString(title, SerializeLimits.MAX_CLIENT);
        output.writeString(auth_id, SerializeLimits.MAX_QUEUE_SIZE);
        output.writeByteArray(encryptedPassword, SecurityHelper.CRYPTO_MAX_LENGTH);
        output.flush();

        // Read UUID and access token
        readError(input);
        return new ClientPermissions(input);
    }
}
