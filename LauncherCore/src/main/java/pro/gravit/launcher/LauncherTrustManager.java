package pro.gravit.launcher;

import pro.gravit.utils.helper.LogHelper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class LauncherTrustManager {
    private final X509Certificate[] trustSigners;
    private final List<X509Certificate> trustCache = new ArrayList<>();

    public LauncherTrustManager(X509Certificate[] trustSigners) {
        this.trustSigners = trustSigners;
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
    }

    public void checkCertificate(X509Certificate[] certs, CertificateChecker checker) throws CertificateException, NoSuchProviderException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        if (certs == null) throw new SecurityException("Object not signed");
        for (int i = 0; i < certs.length; ++i) {
            X509Certificate cert = certs[i];
            if (trustCache.contains(cert)) {
                //Добавляем в кеш все проверенные сертификаты
                trustCache.addAll(Arrays.asList(certs).subList(0, i));
                return;
            }
            X509Certificate signer = (i + 1 < certs.length) ? certs[i + 1] : null;
            cert.checkValidity();
            if (signer != null) {
                cert.verify(signer.getPublicKey());
            } else {
                if (!isTrusted(cert)) {
                    throw new CertificateException(String.format("Certificate %s is not signed by a trusted signer", cert.getSubjectDN().getName()));
                }
            }
            checker.check(cert, signer, i);
        }
        Collections.addAll(trustCache, certs);
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

    public enum CheckMode {
        EXCEPTION_IN_NOT_SIGNED, WARN_IN_NOT_SIGNED, NONE_IN_NOT_SIGNED
    }

    public interface CertificateChecker {
        void check(X509Certificate cert, X509Certificate signer, int number) throws SecurityException;
    }
}
