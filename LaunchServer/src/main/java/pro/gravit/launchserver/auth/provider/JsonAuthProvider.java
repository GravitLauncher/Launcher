package pro.gravit.launchserver.auth.provider;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import pro.gravit.launcher.ClientPermissions;
import pro.gravit.launcher.HTTPRequest;
import pro.gravit.launcher.request.auth.AuthRequest;
import pro.gravit.launcher.request.auth.password.AuthPlainPassword;
import pro.gravit.launchserver.auth.AuthException;
import pro.gravit.utils.helper.SecurityHelper;

import java.io.IOException;
import java.net.URL;

public final class JsonAuthProvider extends AuthProvider {
    private static final Gson gson = new Gson();
    private URL url;
    private String apiKey;

    @Override
    public AuthProviderResult auth(String login, AuthRequest.AuthPasswordInterface password, String ip) throws IOException {
        if (!(password instanceof AuthPlainPassword)) throw new AuthException("This password type not supported");
        authRequest authRequest = new authRequest(login, ((AuthPlainPassword) password).password, ip, apiKey);
        JsonElement request = gson.toJsonTree(authRequest);
        JsonElement content = HTTPRequest.jsonRequest(request, url);
        if (!content.isJsonObject())
            return authError("Authentication server response is malformed");

        authResult result = gson.fromJson(content, authResult.class);
        if (result.username != null)
            return new AuthProviderResult(result.username, SecurityHelper.randomStringToken(), new ClientPermissions(result.permissions, result.flags));
        else if (result.error != null)
            return authError(result.error);
        else
            return authError("Authentication server response is malformed");
    }

    @Override
    public void close() {
        // pass
    }

    public static class authResult {
        String username;
        String error;
        long permissions;
        long flags;
    }

    public static class authRequest {
        final String username;
        final String password;
        final String ip;
        String apiKey;

        public authRequest(String username, String password, String ip) {
            this.username = username;
            this.password = password;
            this.ip = ip;
        }

        public authRequest(String username, String password, String ip, String apiKey) {
            this.username = username;
            this.password = password;
            this.ip = ip;
            this.apiKey = apiKey;
        }
    }
}
