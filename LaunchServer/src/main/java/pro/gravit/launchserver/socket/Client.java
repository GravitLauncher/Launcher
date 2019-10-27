package pro.gravit.launchserver.socket;

import pro.gravit.launcher.ClientPermissions;
import pro.gravit.launcher.profiles.ClientProfile;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.AuthProviderPair;
import pro.gravit.utils.helper.LogHelper;

public class Client {
    public long session;
    public String auth_id;
    public long timestamp;
    public final Type type;
    public ClientProfile profile;
    public boolean isAuth;
    public boolean checkSign;
    public boolean isSecure;
    public ClientPermissions permissions;
    public String username;
    public String verifyToken;
    public transient LogHelper.OutputEnity logOutput;

    public transient AuthProviderPair auth;

    public Client(long session) {
        this.session = session;
        timestamp = System.currentTimeMillis();
        type = Type.USER;
        isAuth = false;
        permissions = ClientPermissions.DEFAULT;
        username = "";
        checkSign = false;
    }

    //Данные авторизации
    public void up() {
        timestamp = System.currentTimeMillis();
    }

    public void updateAuth(LaunchServer server) {
        if (!isAuth) return;
        if (auth_id.isEmpty()) auth = server.config.getAuthProviderPair();
        else auth = server.config.getAuthProviderPair(auth_id);
    }

    public enum Type {
        SERVER,
        USER
    }
}
