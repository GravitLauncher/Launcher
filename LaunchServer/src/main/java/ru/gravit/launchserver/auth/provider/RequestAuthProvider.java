package ru.gravit.launchserver.auth.provider;

import java.io.IOException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.gravit.launcher.helper.CommonHelper;
import ru.gravit.launcher.helper.IOHelper;
import ru.gravit.launcher.helper.SecurityHelper;
import ru.gravit.launcher.serialize.config.entry.BlockConfigEntry;
import ru.gravit.launcher.serialize.config.entry.StringConfigEntry;

public final class RequestAuthProvider extends AuthProvider {
    private final String url;
    private final Pattern response;

    public RequestAuthProvider(BlockConfigEntry block) {
        super(block);
        url = block.getEntryValue("url", StringConfigEntry.class);
        response = Pattern.compile(block.getEntryValue("response", StringConfigEntry.class));

        // Verify is valid URL
        IOHelper.verifyURL(getFormattedURL("urlAuthLogin", "urlAuthPassword", "127.0.0.1"));
    }

    @Override
    public AuthProviderResult auth(String login, String password, String ip) throws IOException {
        String currentResponse = IOHelper.request(new URL(getFormattedURL(login, password, ip)));

        // Match username
        Matcher matcher = response.matcher(currentResponse);
        return matcher.matches() && matcher.groupCount() >= 1 ?
                new AuthProviderResult(matcher.group("username"), SecurityHelper.randomStringToken()) :
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
