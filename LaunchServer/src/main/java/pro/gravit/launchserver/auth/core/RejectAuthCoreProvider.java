package pro.gravit.launchserver.auth.core;

import pro.gravit.launcher.request.auth.AuthRequest;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.AuthException;
import pro.gravit.launchserver.socket.response.auth.AuthResponse;

import java.io.IOException;
import java.util.UUID;

public class RejectAuthCoreProvider extends AuthCoreProvider {
    @Override
    public User getUserByUsername(String username) {
        return null;
    }

    @Override
    public User getUserByUUID(UUID uuid) {
        return null;
    }

    @Override
    public void verifyAuth(AuthResponse.AuthContext context) throws AuthException {
        throw new AuthException("Please configure AuthCoreProvider");
    }

    @Override
    public PasswordVerifyReport verifyPassword(User user, AuthRequest.AuthPasswordInterface password) {
        return PasswordVerifyReport.FAILED;
    }

    @Override
    public void init(LaunchServer server) {

    }

    @Override
    protected boolean updateAuth(User user, String accessToken) throws IOException {
        return false;
    }

    @Override
    protected boolean updateServerID(User user, String serverID) throws IOException {
        return false;
    }

    @Override
    public void close() throws IOException {

    }
}
