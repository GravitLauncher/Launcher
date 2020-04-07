package pro.gravit.launchserver.auth.protect;

import pro.gravit.launcher.profiles.ClientProfile;
import pro.gravit.launchserver.auth.protect.interfaces.ProfilesProtectHandler;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.auth.AuthResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StdProtectHandler extends ProtectHandler implements ProfilesProtectHandler {
    public Map<String, List<String>> profileWhitelist = new HashMap<>();
    public List<String> allowUpdates = new ArrayList<>();

    @Override
    public boolean allowGetAccessToken(AuthResponse.AuthContext context) {
        return (context.authType == AuthResponse.ConnectTypes.CLIENT) && context.client.checkSign;
    }

    @Override
    public void checkLaunchServerLicense() {

    }

    @Override
    public boolean canGetProfile(ClientProfile profile, Client client) {
        return canChangeProfile(profile, client);
    }

    @Override
    public boolean canChangeProfile(ClientProfile profile, Client client) {
        return client.isAuth && client.username != null && isWhitelisted(profile.getTitle(), client.username);
    }

    @Override
    public boolean canGetUpdates(String updatesDirName, Client client) {
        return client.profile != null && (client.profile.getDir().equals(updatesDirName) || client.profile.getAssetDir().equals(updatesDirName) || allowUpdates.contains(updatesDirName));
    }

    public boolean isWhitelisted(String profileTitle, String username) {
        List<String> allowedUsername = profileWhitelist.get(profileTitle);
        if (allowedUsername == null) return true;
        return allowedUsername.contains(username);
    }
}
