package pro.gravit.launcher.client.api;

import pro.gravit.launcher.client.ClientLauncherMethods;
import pro.gravit.launcher.core.LauncherTrustManager;
import pro.gravit.launcher.client.utils.ApiBridgeService;

import java.security.cert.X509Certificate;

public class CertificateService {
    private CertificateService() {
        throw new UnsupportedOperationException();
    }

    public static CheckClassResultApi checkClass(Class<?> clazz) throws SecurityException {
        X509Certificate[] certificates = ClientLauncherMethods.getCertificates(clazz);
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
        X509Certificate[] certificates = ClientLauncherMethods.getCertificates(clazz);
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
            return switch (type) {
                case NOT_SIGNED -> CheckClassResultTypeApi.NOT_SIGNED;
                case SUCCESS -> CheckClassResultTypeApi.SUCCESS;
                case UNTRUSTED -> CheckClassResultTypeApi.UNTRUSTED;
                case UNVERIFED -> CheckClassResultTypeApi.UNVERIFED;
                case UNCOMPAT -> CheckClassResultTypeApi.UNCOMPAT;
            };
        }
    }
}
