package pro.gravit.launchserver.socket;

import pro.gravit.launcher.ClientPermissions;
import pro.gravit.launcher.profiles.ClientProfile;
import pro.gravit.launcher.request.secure.HardwareReportRequest;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.AuthProviderPair;
import pro.gravit.launchserver.dao.User;
import pro.gravit.launchserver.socket.response.auth.AuthResponse;
import pro.gravit.utils.helper.LogHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Client {
    public UUID session;
    public String auth_id;
    public long timestamp;
    public AuthResponse.ConnectTypes type;
    public ClientProfile profile;
    public boolean isAuth;
    public boolean checkSign;
    public ClientPermissions permissions;
    public String username;
    public TrustLevel trustLevel;

    public transient AuthProviderPair auth;

    public transient User daoObject;

    public transient Map<String, Object> properties;

    public Client(UUID session) {
        this.session = session;
        timestamp = System.currentTimeMillis();
        type = null;
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

    public static class TrustLevel {
        public byte[] verifySecureKey;
        public boolean keyChecked;
        public byte[] publicKey;
        public HardwareReportRequest.HardwareInfo hardwareInfo;
    }
    @SuppressWarnings("unchecked")
    public<T> T getProperty(String name)
    {
        if(properties == null) properties = new HashMap<>();
        return (T) properties.get(name);
    }

    public<T> void setProperty(String name, T object)
    {
        if(properties == null) properties = new HashMap<>();
        properties.put(name, object);
    }
}
