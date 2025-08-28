package pro.gravit.launcher.runtime.utils;

import pro.gravit.launcher.runtime.LauncherEngine;
import pro.gravit.launcher.core.LauncherInject;
import pro.gravit.launcher.base.request.update.LauncherRequest;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;
import pro.gravit.utils.helper.SecurityHelper;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static pro.gravit.launcher.base.Downloader.makeSSLSocketFactory;

public class LauncherUpdater {
    @LauncherInject("launcher.certificatePinning")
    private static boolean isCertificatePinning;

    public static void nothing() {

    }

    private static Path getLauncherPath() {
        Path pathToCore = IOHelper.getCodeSource(IOHelper.class);
        Path pathToApi = IOHelper.getCodeSource(LauncherRequest.class);
        Path pathToSelf = IOHelper.getCodeSource(LauncherUpdater.class);
        if (pathToCore.equals(pathToApi) && pathToCore.equals(pathToSelf)) {
            return pathToCore;
        } else {
            throw new SecurityException("Found split-jar launcher");
        }
    }

    public static Path prepareUpdate(URL url) throws Exception {
        Path pathToLauncher = getLauncherPath();
        Path tempFile = Files.createTempFile("launcher-update-", ".jar");
        URLConnection connection = url.openConnection();
        if (isCertificatePinning) {
            HttpsURLConnection connection1 = (HttpsURLConnection) connection;
            try {
                connection1.setSSLSocketFactory(makeSSLSocketFactory());
            } catch (NoSuchAlgorithmException | CertificateException | KeyStoreException | KeyManagementException e) {
                throw new IOException(e);
            }
        }
        try (InputStream in = connection.getInputStream()) {
            IOHelper.transfer(in, tempFile);
        }
        if (Arrays.equals(SecurityHelper.digest(SecurityHelper.DigestAlgorithm.MD5, tempFile),
                SecurityHelper.digest(SecurityHelper.DigestAlgorithm.MD5, pathToLauncher)))
            throw new IOException("Invalid update (launcher needs update, but link has old launcher), check LaunchServer config...");
        return tempFile;
    }

    public static void restart() {
        List<String> args = new ArrayList<>(8);
        args.add(IOHelper.resolveJavaBin(null).toString());
        args.add("-jar");
        args.add(IOHelper.getCodeSource(LauncherUpdater.class).toString());
        ProcessBuilder builder = new ProcessBuilder(args.toArray(new String[0]));
        builder.inheritIO();
        try {
            builder.start();
        } catch (IOException e) {
            LogHelper.error(e);
        }
        LauncherEngine.forceExit(0);
    }
}
