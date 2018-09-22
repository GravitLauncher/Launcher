package ru.gravit.launcher.ssl;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

public class LauncherKeyStore {
    public static KeyStore getKeyStore(String keystore, String password) throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
        KeyStore ks = KeyStore.getInstance("JKS");
        try (InputStream ksIs = new FileInputStream(keystore)) {
            ks.load(ksIs, password.toCharArray());
        }
        return ks;
    }
}
