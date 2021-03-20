package pro.gravit.launcher;

import pro.gravit.utils.helper.LogHelper;

import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;

public final class CertificatePinningTrustManager {
    @LauncherInject("launchercore.certificates")
    private static List<byte[]> secureConfigCertificates;
    private static X509Certificate[] certs = null;
    private volatile static TrustManagerFactory INSTANCE;

    private static X509Certificate[] getInternalCertificates() {
        CertificateFactory certFactory = null;
        try {
            certFactory = CertificateFactory.getInstance("X.509");
        } catch (CertificateException e) {
            return new X509Certificate[0];
        }
        CertificateFactory finalCertFactory = certFactory;
        return secureConfigCertificates.stream().map((cert) -> {
            try (InputStream input = new ByteArrayInputStream(cert)) {
                return (X509Certificate) finalCertFactory.generateCertificate(input);
            } catch (IOException | CertificateException e) {
                LogHelper.error(e);
                return null;
            }
        }).toArray(X509Certificate[]::new);
    }

    public static X509Certificate[] getCertificates() {
        if (certs == null) certs = getInternalCertificates();
        return Arrays.copyOf(certs, certs.length);
    }

    public static TrustManagerFactory getTrustManager() throws KeyStoreException, NoSuchAlgorithmException, IOException, CertificateException {
        if (INSTANCE != null) return INSTANCE;
        if (certs == null) certs = getInternalCertificates();
        TrustManagerFactory factory = TrustManagerFactory.getInstance("X.509");
        KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
        keystore.load(null, null);

        int i = 1;
        for (X509Certificate cert : certs) {
            String alias = Integer.toString(i);
            keystore.setCertificateEntry(alias, cert);
            i++;
        }
        factory.init(keystore);
        INSTANCE = factory;
        return factory;
    }
}
