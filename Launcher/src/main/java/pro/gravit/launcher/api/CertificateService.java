package pro.gravit.launcher.api;

import pro.gravit.launcher.LauncherTrustManager;
import pro.gravit.launcher.utils.ApiBridgeService;

import java.security.cert.X509Certificate;

import static pro.gravit.launcher.LauncherEngine.getCertificates;

public class CertificateService {
    private CertificateService() {
        throw new UnsupportedOperationException();
    }

    public static CheckClassResultApi checkClass(Class<?> clazz) throws SecurityException {
        X509Certificate[] certificates = getCertificates(clazz);
        if (certificates == null) {
            return new CheckClassResultApi(CheckClassResultTypeApi.NOT_SIGNED, null, null);
        }
        try {
            return CheckClassResultApi.fromCheckClassResult(ApiBridgeService.checkCertificates(certificates));
        } catch (Exception e) {
            throw new SecurityException(e);
        }
    }

    public static void checkClassSuccess(Class<?> clazz) {
        X509Certificate[] certificates = getCertificates(clazz);
        if (certificates == null) {
            throw new SecurityException(String.format("Class %s not signed", clazz.getName()));
        }
        try {
            ApiBridgeService.checkCertificatesSuccess(certificates);
        } catch (Exception e) {
            throw new SecurityException(e);
        }
    }

    public enum CheckClassResultTypeApi {
        NOT_SIGNED,
        SUCCESS,
        UNTRUSTED,
        UNVERIFED,
        UNCOMPAT,
        UNKNOWN
    }

    public static class CheckClassResultApi {
        public final CheckClassResultTypeApi type;
        public final X509Certificate endCertificate;
        public final X509Certificate rootCertificate;
        public final Exception exception;

        private CheckClassResultApi(CheckClassResultTypeApi type, X509Certificate endCertificate, X509Certificate rootCertificate) {
            this.type = type;
            this.endCertificate = endCertificate;
            this.rootCertificate = rootCertificate;
            exception = null;
        }

        private CheckClassResultApi(CheckClassResultTypeApi type, X509Certificate endCertificate, X509Certificate rootCertificate, Exception exception) {
            this.type = type;
            this.endCertificate = endCertificate;
            this.rootCertificate = rootCertificate;
            this.exception = exception;
        }

        private CheckClassResultApi(CheckClassResultApi orig) {
            this.type = orig.type;
            this.exception = orig.exception;
            this.rootCertificate = orig.rootCertificate;
            this.endCertificate = orig.endCertificate;
        }

        private static CheckClassResultApi fromCheckClassResult(LauncherTrustManager.CheckClassResult result) {
            if (result == null) return null;
            return new CheckClassResultApi(fromType(result.type), result.endCertificate, result.rootCertificate, result.exception);
        }

        private static CheckClassResultTypeApi fromType(LauncherTrustManager.CheckClassResultType type) {
            if (type == null) return null;
            switch (type) {
                case NOT_SIGNED:
                    return CheckClassResultTypeApi.NOT_SIGNED;
                case SUCCESS:
                    return CheckClassResultTypeApi.SUCCESS;
                case UNTRUSTED:
                    return CheckClassResultTypeApi.UNTRUSTED;
                case UNVERIFED:
                    return CheckClassResultTypeApi.UNVERIFED;
                case UNCOMPAT:
                    return CheckClassResultTypeApi.UNCOMPAT;
                default:
                    return CheckClassResultTypeApi.UNKNOWN;
            }
        }
    }
}
