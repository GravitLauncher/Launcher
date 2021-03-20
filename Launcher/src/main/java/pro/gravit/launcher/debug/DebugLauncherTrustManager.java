package pro.gravit.launcher.debug;

import pro.gravit.launcher.LauncherTrustManager;

import java.security.cert.X509Certificate;

public class DebugLauncherTrustManager extends LauncherTrustManager {
    private final TrustDebugMode mode;

    public DebugLauncherTrustManager(X509Certificate[] trustSigners) {
        super(trustSigners);
        this.mode = null;
    }

    public DebugLauncherTrustManager() {
        super(new X509Certificate[0]);
        this.mode = null;
    }

    public DebugLauncherTrustManager(TrustDebugMode mode) {
        super(new X509Certificate[0]);
        this.mode = mode;
    }

    @Override
    public CheckClassResult checkCertificates(X509Certificate[] certs, CertificateChecker checker) {
        if (mode == TrustDebugMode.TRUST_ALL) return new CheckClassResult(CheckClassResultType.SUCCESS, null, null);
        return super.checkCertificates(certs, checker);
    }

    public enum TrustDebugMode {
        TRUST_ALL
    }
}
