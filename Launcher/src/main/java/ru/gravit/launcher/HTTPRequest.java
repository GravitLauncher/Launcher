package ru.gravit.launcher;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import ru.gravit.utils.helper.IOHelper;

public class HTTPRequest {
    public static int sendCrashreport(String strurl, byte[] data) throws IOException {
        URL url = new URL(strurl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type",
                "application/x-www-form-urlencoded");
        connection.setRequestProperty("Content-Length",
                Integer.toString(data.length));
        connection.setRequestProperty("Content-Language", "en-US");
        OutputStream outputStream = connection.getOutputStream();
        outputStream.write(data);
        outputStream.close();
        return connection.getResponseCode();
    }

    public static int sendCrashreport(String strurl, String data) throws IOException {
        return sendCrashreport(strurl, data.getBytes(IOHelper.UNICODE_CHARSET));
    }
}
