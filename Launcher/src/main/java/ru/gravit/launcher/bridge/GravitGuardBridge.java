package ru.gravit.launcher.bridge;

import ru.gravit.launcher.LauncherAPI;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

@LauncherAPI
public class GravitGuardBridge {
    @LauncherAPI
    public static native void callGuard();

    @LauncherAPI
    public static int sendHTTPRequest(String strurl) throws IOException {
        URL url = new URL(strurl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Language", "en-US");
        return connection.getResponseCode();
    }
}
