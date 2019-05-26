package ru.gravit.launcher.ssl;

import javax.net.ssl.X509TrustManager;

public class LauncherTrustManager implements X509TrustManager {
    public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
    }

    public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
    }

    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
        return null;
    }
}
