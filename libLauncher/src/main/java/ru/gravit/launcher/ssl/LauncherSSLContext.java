package ru.gravit.launcher.ssl;

import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

public class LauncherSSLContext {
    public SSLServerSocketFactory ssf;
    public SSLSocketFactory sf;
    @SuppressWarnings("unused")
	private SSLContext sc;

    public LauncherSSLContext(KeyStore ks, String keypassword) throws NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException, KeyManagementException {
        TrustManager[] trustAllCerts = new TrustManager[]{
                new LauncherTrustManager()
        };
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory
                .getDefaultAlgorithm());
        kmf.init(ks, keypassword.toCharArray());
        SSLContext sc = SSLContext.getInstance("TLSv1.2");
        sc.init(kmf.getKeyManagers(), trustAllCerts, new SecureRandom());
        ssf = sc.getServerSocketFactory();
        sf = sc.getSocketFactory();
    }

    public LauncherSSLContext() throws NoSuchAlgorithmException, KeyManagementException {
        TrustManager[] trustAllCerts = new TrustManager[]{
                new LauncherTrustManager()
        };
        SSLContext sc = SSLContext.getInstance("TLSv1.2");
        sc.init(null, trustAllCerts, new SecureRandom());
        ssf = null;
        sf = sc.getSocketFactory();
    }
}
