package pro.gravit.launchserver.auth.provider;

import pro.gravit.launcher.ClientPermissions;
import pro.gravit.launcher.request.auth.AuthRequest;
import pro.gravit.launcher.request.auth.password.AuthPlainPassword;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.AuthException;
import pro.gravit.utils.helper.CommonHelper;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.SecurityHelper;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RequestAuthProvider extends AuthProvider {
    private transient final HttpClient client = HttpClient.newBuilder()
            .build();
    public String url;
    public transient Pattern pattern;
    public String response;
    public boolean flagsEnabled;
    public boolean usePermission = true;
    public int timeout = 5000;

    @Override
    public void init(LaunchServer srv) {
        super.init(srv);
        if (url == null) throw new RuntimeException("[Verify][AuthProvider] url cannot be null");
        if (response == null) throw new RuntimeException("[Verify][AuthProvider] response cannot be null");
        pattern = Pattern.compile(response);

    }

    @Override
    public AuthProviderResult auth(String login, AuthRequest.AuthPasswordInterface password, String ip) throws IOException, URISyntaxException, InterruptedException {
        if (!(password instanceof AuthPlainPassword)) throw new AuthException("This password type not supported");
        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                .uri(new URI(getFormattedURL(login, ((AuthPlainPassword) password).password, ip)))
                .header("User-Agent", IOHelper.USER_AGENT)
                .timeout(Duration.ofMillis(timeout))
                .GET()
                .build(), HttpResponse.BodyHandlers.ofString());

        // Match username
        String currentResponse = response.body();
        Matcher matcher = pattern.matcher(currentResponse);
        return matcher.matches() && matcher.groupCount() >= 1 ?
                new AuthProviderResult(matcher.group("username"), SecurityHelper.randomStringToken(), new ClientPermissions(
                        usePermission ? Long.parseLong(matcher.group("permissions")) : 0, flagsEnabled ? Long.parseLong(matcher.group("flags")) : 0)) :
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
