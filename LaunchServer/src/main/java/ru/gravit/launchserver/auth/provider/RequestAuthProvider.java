package ru.gravit.launchserver.auth.provider;

import ru.gravit.launcher.ClientPermissions;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.utils.helper.CommonHelper;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.LogHelper;
import ru.gravit.utils.helper.SecurityHelper;

import java.io.IOException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RequestAuthProvider extends AuthProvider {
    private String url;
    private transient Pattern pattern;
    private String response;
    private boolean usePermission;

    @Override
    public void init() {
        if (url == null) LogHelper.error("[Verify][AuthProvider] url cannot be null");
        if (response == null) LogHelper.error("[Verify][AuthProvider] response cannot be null");
        pattern = Pattern.compile(response);
    }

    @Override
    public AuthProviderResult auth(String login, String password, String ip) throws IOException {
        String currentResponse = IOHelper.request(new URL(getFormattedURL(login, password, ip)));

        // Match username
        Matcher matcher = pattern.matcher(currentResponse);
        return matcher.matches() && matcher.groupCount() >= 1 ?
                new AuthProviderResult(matcher.group("username"), SecurityHelper.randomStringToken(), usePermission ? new ClientPermissions(Long.getLong(matcher.group("permission"))) : LaunchServer.server.config.permissionsHandler.getPermissions(login)) :
                authError(currentResponse);
    }

    @Override
    public void close() {
        // Do nothing
    }

    private String getFormattedURL(String login, String password, String ip) {
        return CommonHelper.replace(url, "login", IOHelper.urlEncode(login), "password", IOHelper.urlEncode(password), "ip", IOHelper.urlEncode(ip));
    }
}
