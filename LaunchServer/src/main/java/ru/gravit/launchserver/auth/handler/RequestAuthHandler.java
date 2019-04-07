package ru.gravit.launchserver.auth.handler;

import ru.gravit.launchserver.auth.provider.AuthProviderResult;
import ru.gravit.utils.helper.CommonHelper;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.LogHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;

public final class RequestAuthHandler extends AuthHandler {
    // Из конфига строки
    private String urlGetUUID;
    private String urlGetAll;
    private String urlGetUsername;
    private String urlUpdateAccessToken;
    private String urlUpdateServerID;
    private String response;

    private transient Pattern pattern;

    @Override
    public void init() {
        //TODO добавить ошибки к строкам из конфига
        if (url == null) LogHelper.error("[Verify][AuthHandler] url cannot be null");
        if (response == null) LogHelper.error("[Verify][AuthHandler] response cannot be null");
        pattern = Pattern.compile(response);
    }

    @Override
    public UUID auth(AuthProviderResult authResult) throws IOException {
        //TODO
    }

    @Override
    public UUID checkServer(String username, String serverID) throws IOException {
        //TODO
    }

    @Override
    public boolean joinServer(String username, String accessToken, String serverID) throws IOException {
        // TODO
    }

    @Override
    public UUID usernameToUUID(String username) throws IOException {
        String currentResponse = IOHelper.request(new URL(CommonHelper.replace(urlGetUUID, "username", IOHelper.urlEncode("username"))));

        Matcher matcher = pattern.matcher(currentResponse);

        // TODO
    }

    @Override
    public String uuidToUsername(UUID uuid) throws IOException {
        String currentResponse = IOHelper.request(new URL(CommonHelper.replace(urlGetUsername, "uuid", IOHelper.urlEncode("uuid"))));

        Matcher matcher = pattern.matcher(currentResponse);

        // TODO
    }

    @Override
    public void close() {
        // Ничего не делать
    }
}
