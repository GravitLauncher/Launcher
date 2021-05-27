package pro.gravit.launcher.request.auth;

import pro.gravit.launcher.LauncherNetworkAPI;
import pro.gravit.launcher.events.request.AuthRequestEvent;
import pro.gravit.launcher.request.Request;
import pro.gravit.launcher.request.auth.password.*;
import pro.gravit.launcher.request.websockets.WebSocketRequest;
import pro.gravit.utils.ProviderMap;
import pro.gravit.utils.helper.VerifyHelper;

public final class AuthRequest extends Request<AuthRequestEvent> implements WebSocketRequest {
    public static final ProviderMap<AuthPasswordInterface> providers = new ProviderMap<>();
    private static boolean registerProviders = false;
    @LauncherNetworkAPI
    private final String login;
    @LauncherNetworkAPI
    private final AuthPasswordInterface password;
    @LauncherNetworkAPI
    private final String auth_id;
    @LauncherNetworkAPI
    private final boolean getSession;
    @LauncherNetworkAPI
    private final ConnectTypes authType;

    @Deprecated
    public AuthRequest(String login, byte[] password) {
        this.login = VerifyHelper.verify(login, VerifyHelper.NOT_EMPTY, "Login can't be empty");
        this.password = new AuthECPassword(password.clone());
        auth_id = "";
        getSession = true;
        authType = ConnectTypes.CLIENT;
    }

    @Deprecated
    public AuthRequest(String login, byte[] password, String auth_id) {
        this.login = VerifyHelper.verify(login, VerifyHelper.NOT_EMPTY, "Login can't be empty");
        this.password = new AuthECPassword(password.clone());
        this.auth_id = auth_id;
        getSession = true;
        authType = ConnectTypes.CLIENT;
    }

    @Deprecated
    public AuthRequest(String login, byte[] encryptedPassword, String auth_id, ConnectTypes authType) {
        this.login = login;
        this.password = new AuthECPassword(encryptedPassword.clone());
        this.auth_id = auth_id;
        this.authType = authType;
        this.getSession = false;
    }

    public AuthRequest(String login, String password, String auth_id, ConnectTypes authType) {
        this.login = login;
        this.password = new AuthPlainPassword(password);
        this.auth_id = auth_id;
        this.authType = authType;
        this.getSession = false;
    }

    public AuthRequest(String login, AuthPasswordInterface password, String auth_id, boolean getSession, ConnectTypes authType) {
        this.login = login;
        this.password = password;
        this.auth_id = auth_id;
        this.getSession = getSession;
        this.authType = authType;
    }

    @SuppressWarnings("deprecation")
    public static void registerProviders() {
        if (!registerProviders) {
            providers.register("plain", AuthPlainPassword.class);
            providers.register("rsa2", AuthRSAPassword.class);
            providers.register("rsa", AuthECPassword.class);
            providers.register("aes", AuthAESPassword.class);
            providers.register("2fa", Auth2FAPassword.class);
            providers.register("multi", AuthMultiPassword.class);
            providers.register("signature", AuthSignaturePassword.class);
            providers.register("totp", AuthTOTPPassword.class);
            providers.register("oauth", AuthOAuthPassword.class);
            providers.register("code", AuthCodePassword.class);
            registerProviders = true;
        }
    }

    @Override
    public String getType() {
        return "auth";
    }

    public enum ConnectTypes {
        @Deprecated
        @LauncherNetworkAPI
        SERVER,
        @LauncherNetworkAPI
        CLIENT,
        @LauncherNetworkAPI
        API
    }

    public interface AuthPasswordInterface {
        boolean check();
    }
}
