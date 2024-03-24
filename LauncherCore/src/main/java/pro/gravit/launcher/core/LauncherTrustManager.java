package pro.gravit.launcher.core;

import pro.gravit.utils.helper.LogHelper;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.*;
import java.util.*;

public class LauncherTrustManager {
    private final X509Certificate[] trustSigners;
    private final List<X509Certificate> trustCache = new ArrayList<>();

    @LauncherInject("launcher.certificatePinning")
    private static boolean isCertificatePinning;

    public LauncherTrustManager(X509Certificate[] trustSigners) {
        this.trustSigners = trustSigners;
        if (requireCustomTrustStore()) {
            injectCertificates();
        }
    }

    public LauncherTrustManager(List<byte[]> encodedCertificate) throws CertificateException {
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        trustSigners = encodedCertificate.stream().map((cert) -> {
            try (InputStream input = new ByteArrayInputStream(cert)) {
                return (X509Certificate) certFactory.generateCertificate(input);
            } catch (IOException | CertificateException e) {
                LogHelper.error(e);
                return null;
            }
        }).toArray(X509Certificate[]::new);
        if (requireCustomTrustStore()) {
            injectCertificates();
        }
    }

    private boolean requireCustomTrustStore() {
        return trustSigners != null && trustSigners.length != 0 && isCertificatePinning;
    }

    private void injectCertificates() {
        try {
            // Получение списка всех существующих и действительных сертификатов из стандартного KeyStore JVM
            final Map<String, Certificate> jdkTrustStore = getDefaultKeyStore();
            // Создание нового KeyStore с дополнительными сертификатами.
            final KeyStore mergedTrustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            mergedTrustStore.load(null, new char[0]);

            // добавление дополнительных сертификатов в новый KeyStore
            Arrays.stream(trustSigners).forEach(cert -> setCertificateEntry(mergedTrustStore, "injected-certificate" + UUID.randomUUID(), cert));

            // добавление стандартных сертификатов в новый KeyStore
            jdkTrustStore.keySet().forEach(key -> setCertificateEntry(mergedTrustStore, key, jdkTrustStore.get(key)));

            // Context initialization. In case of failure, the process is allowed to be interrupted, but certificates will not be added
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(mergedTrustStore);
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagerFactory.getTrustManagers(), null);

            // Setting the default context
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
            LogHelper.info("Successfully injected certificates to truststore");
        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException | IOException | CertificateException e) {
            LogHelper.error("Error while modify existing keystore");
        }
    }

    /**
     * Получение набора стандартных сертификатов, вшитых в текущую сессию JVM
     */
    private static Map<String, Certificate> getDefaultKeyStore() {
        // init existing keystore
        final Map<String, Certificate> jdkTrustStore = new HashMap<>();
        try {
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            Path ksPath = Paths.get(System.getProperty("java.home"), "lib", "security", "cacerts");
            keyStore.load(Files.newInputStream(ksPath), "changeit".toCharArray());

            // getting all JDK/JRE certificates
            extractAllCertsAndPutInMap(keyStore, jdkTrustStore);
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
            LogHelper.warning("Error while loading existing keystore");
        }
        return jdkTrustStore;
    }

    /**
     * Retrieve existing certificates from the standard KeyStore of the current JVM session. The process should not be interrupted in case of failure
     */
    private static void extractAllCertsAndPutInMap(KeyStore keyStore, Map<String, Certificate> placeToExport) {
        try {
            Collections.list(keyStore.aliases()).forEach(key -> extractCertAndPutInMap(keyStore, key, placeToExport));
        } catch (KeyStoreException e) {
            LogHelper.error("Error during extraction certificates from default keystore");
        }
    }

    /**
     * Добавление сертификата с именем name в KeyStore. Не должно прерывать общий процесс инъекции сертификатов, в случае неудачи.
     */
    private static void setCertificateEntry(KeyStore keyStore, String name, Certificate cert) {
        try {
            keyStore.setCertificateEntry(name, cert);
        } catch (KeyStoreException e) {
            LogHelper.warning("Something went wrong while adding certificate " + name);
        }
    }

    /**
     * Retrieve an existing certificate from the standard KeyStore of the current JVM session. The process should not be interrupted in case of failure
     */
    private static void extractCertAndPutInMap(KeyStore keyStoreFromExtract, String key, Map<String, Certificate> placeToExtract) {
        try {
            if (keyStoreFromExtract.containsAlias(key)) {
                placeToExtract.put(key, keyStoreFromExtract.getCertificate(key));
            }
        } catch (KeyStoreException e) {
            LogHelper.warning("Error while extracting certificate " + key);
        }
    }

    public CheckClassResult checkCertificates(X509Certificate[] certs, CertificateChecker checker) {
        if (certs == null) return new CheckClassResult(CheckClassResultType.NOT_SIGNED, null, null);
        X509Certificate rootCert = certs[certs.length - 1];
        X509Certificate endCert = certs[0];
        for (int i = 0; i < certs.length; ++i) {
            X509Certificate cert = certs[i];
            if (trustCache.contains(cert)) {
                //Добавляем в кеш все проверенные сертификаты
                trustCache.addAll(Arrays.asList(certs).subList(0, i));
                return new CheckClassResult(CheckClassResultType.SUCCESS, endCert, rootCert);
            }
            X509Certificate signer = (i + 1 < certs.length) ? certs[i + 1] : null;
            try {
                cert.checkValidity();
            } catch (Exception e) {
                return new CheckClassResult(CheckClassResultType.UNVERIFED, endCert, rootCert, e);
            }

            if (signer != null) {
                try {
                    cert.verify(signer.getPublicKey());
                } catch (Exception e) {
                    return new CheckClassResult(CheckClassResultType.UNVERIFED, endCert, rootCert, e);
                }
            } else {
                try {
                    if (isTrusted(cert)) {
                        continue;
                    } else {
                        return new CheckClassResult(CheckClassResultType.UNTRUSTED, endCert, rootCert);
                    }
                } catch (CertificateEncodingException e) {
                    return new CheckClassResult(CheckClassResultType.UNVERIFED, endCert, rootCert, e);
                }
            }
            try {
                checker.check(cert, signer, i);
            } catch (Exception e) {
                return new CheckClassResult(CheckClassResultType.UNCOMPAT, endCert, rootCert, e);
            }
        }
        Collections.addAll(trustCache, certs);
        return new CheckClassResult(CheckClassResultType.SUCCESS, endCert, rootCert);
    }

    public void checkCertificatesSuccess(X509Certificate[] certs, CertificateChecker checker) throws Exception {
        CheckClassResult result = checkCertificates(certs, checker);
        if (result.type == CheckClassResultType.SUCCESS) return;
        if (result.exception != null) throw result.exception;
        throw new SecurityException(result.type.name());
    }

    public boolean isTrusted(X509Certificate certificate) throws CertificateEncodingException {
        //Java API не дает возможности вызвать getFingerprint
        //Oracle использует хак с кастом к sun.security.x509.X509CertImpl для проверки равенства сертификатов
        //Мы пойдем более медленным путем
        for (X509Certificate cert : trustSigners) {
            if (cert.getSerialNumber().equals(certificate.getSerialNumber()) //Проверка serialNumber (быстро)
                    && Arrays.equals(cert.getEncoded(), certificate.getEncoded())) //Полная проверка (медленно)
            {
                return true;
            }
        }
        return false;
    }

    public X509Certificate[] getTrusted() {
        return Arrays.copyOf(trustSigners, trustSigners.length); // AntiModify orig array!!!
    }

    public void isCertificateCodeSign(X509Certificate certificate) {
        //if(!certificate.getKeyUsage()[0]) throw new SecurityException("Certificate keyUsage \"digitalSignature\" check failed");
        List<String> extended;
        try {
            extended = certificate.getExtendedKeyUsage();
            if (extended == null) throw new SecurityException("Certificate extendedKeyUsage null");
            boolean isCodeSign = false;
            for (String s : extended) {
                if (s.equals("1.3.6.1.5.5.7.3.3")) {
                    isCodeSign = true;
                    break;
                }
            }
            if (!isCodeSign) throw new SecurityException("Certificate extendedKeyUsage codeSign checkFailed");
        } catch (CertificateParsingException e) {
            throw new SecurityException(e);
        }
    }

    public void isCertificateCA(X509Certificate certificate) {
        if (certificate.getBasicConstraints() < 0) throw new SecurityException("This certificate not CA");
    }

    public void stdCertificateChecker(X509Certificate cert, X509Certificate signer, int number) {
        if (number == 0)
            isCertificateCodeSign(cert);
        else
            isCertificateCA(cert);
    }

    public enum CheckClassResultType {
        NOT_SIGNED,
        SUCCESS,
        UNTRUSTED,
        UNVERIFED,
        UNCOMPAT
    }

    public interface CertificateChecker {
        void check(X509Certificate cert, X509Certificate signer, int number) throws SecurityException;
    }

    public static class CheckClassResult {
        public final CheckClassResultType type;
        public final X509Certificate endCertificate;
        public final X509Certificate rootCertificate;
        public final Exception exception;

        public CheckClassResult(CheckClassResultType type, X509Certificate endCertificate, X509Certificate rootCertificate) {
            this.type = type;
            this.endCertificate = endCertificate;
            this.rootCertificate = rootCertificate;
            exception = null;
        }

        public CheckClassResult(CheckClassResultType type, X509Certificate endCertificate, X509Certificate rootCertificate, Exception exception) {
            this.type = type;
            this.endCertificate = endCertificate;
            this.rootCertificate = rootCertificate;
            this.exception = exception;
        }

        public CheckClassResult(CheckClassResult orig) {
            this.type = orig.type;
            this.exception = orig.exception;
            this.rootCertificate = orig.rootCertificate;
            this.endCertificate = orig.endCertificate;
        }
    }
}
