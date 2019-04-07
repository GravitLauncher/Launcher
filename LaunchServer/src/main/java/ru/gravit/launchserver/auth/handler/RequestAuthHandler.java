package ru.gravit.launchserver.auth.handler;

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

/*
 * author sasha0552
 */

public final class RequestAuthHandler extends AuthHandler {
    // Из конфига строки
    private String url;
    private String urlGetUUID;
    private String urlUpdateAccessToken;
    private String urlUpdateServerID;
    private String response;

    // Поля
    private String username;
    private String uuid;
    private String accessToken;
    private String serverID;

    private transient Pattern pattern;

    @Override
    public void init() {
        //TODO добавить ошибки к строкам из конфига
        if (url == null) LogHelper.error("[Verify][AuthHandler] url cannot be null");
        if (response == null) LogHelper.error("[Verify][AuthHandler] response cannot be null");
        pattern = Pattern.compile(response);
    }

    @Override
    public void close() {
        // Ничего не делать
    }

    @Override
    public String uuidToUsername(UUID uuid) throws IOException {
        URL url;
        HttpURLConnection conn;
        BufferedReader rd;
        String line;
        String result = "";
        try {
            url = new URL(format("%s?uuid=%s",urlGetUUID,uuid));
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            while ((line = rd.readLine()) != null) {
                // Пусто
            }
            rd.close();
        } catch (Exception e) {
            LogHelper.error("[Request AuthHandler] Error get username by UUID");
        }
        return result;
    }
}
