package pro.gravit.launchserver.manangers;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.crypto.util.PrivateKeyInfoFactory;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.bouncycastle.util.io.pem.PemWriter;
import pro.gravit.launcher.LauncherTrustManager;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.JVMHelper;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

public class CertificateManager {
    private transient final Logger logger = LogManager.getLogger();
    public LauncherTrustManager trustManager;

    public void writePrivateKey(Path file, PrivateKey privateKey) throws IOException {
        writePrivateKey(IOHelper.newWriter(file), privateKey);
    }

    public void writePrivateKey(Writer writer, PrivateKey privateKey) throws IOException {
        try (PemWriter writer1 = new PemWriter(writer)) {
            writer1.writeObject(new PemObject("PRIVATE KEY", privateKey.getEncoded()));
        }
    }

    public void writePrivateKey(Path file, AsymmetricKeyParameter key) throws IOException {
        writePrivateKey(IOHelper.newWriter(file), key);
    }

    public void writePrivateKey(Writer writer, AsymmetricKeyParameter key) throws IOException {
        PrivateKeyInfo info = PrivateKeyInfoFactory.createPrivateKeyInfo(key);
        try (PemWriter writer1 = new PemWriter(writer)) {
            writer1.writeObject(new PemObject("PRIVATE KEY", info.getEncoded()));
        }
    }

    public void writeCertificate(Path file, X509CertificateHolder holder) throws IOException {
        writeCertificate(IOHelper.newWriter(file), holder);
    }

    public void writeCertificate(Writer writer, X509CertificateHolder holder) throws IOException {
        try (PemWriter writer1 = new PemWriter(writer)) {
            writer1.writeObject(new PemObject("CERTIFICATE", holder.toASN1Structure().getEncoded()));
        }
    }

    public AsymmetricKeyParameter readPrivateKey(Path file) throws IOException {
        return readPrivateKey(IOHelper.newReader(file));
    }

    public AsymmetricKeyParameter readPrivateKey(Reader reader) throws IOException {
        AsymmetricKeyParameter ret;
        try (PemReader reader1 = new PemReader(reader)) {
            byte[] bytes = reader1.readPemObject().getContent();
            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes)) {

                ret = PrivateKeyFactory.createKey(inputStream);
            }
        }
        return ret;
    }

    public X509CertificateHolder readCertificate(Path file) throws IOException {
        return readCertificate(IOHelper.newReader(file));
    }

    public X509CertificateHolder readCertificate(Reader reader) throws IOException {
        X509CertificateHolder ret;
        try (PemReader reader1 = new PemReader(reader)) {
            byte[] bytes = reader1.readPemObject().getContent();
            ret = new X509CertificateHolder(bytes);
        }
        return ret;
    }

    public void readTrustStore(Path dir) throws IOException, CertificateException {
        if (!IOHelper.isDir(dir)) {
            Files.createDirectories(dir);
            try {
                URL inBuildCert = IOHelper.getResourceURL("pro/gravit/launchserver/defaults/BuildCertificate.crt");
                try (OutputStream outputStream = IOHelper.newOutput(dir.resolve("BuildCertificate.crt"));
                     InputStream inputStream = IOHelper.newInput(inBuildCert)) {
                    IOHelper.transfer(inputStream, outputStream);
                }
            } catch (NoSuchFileException ignored) {

            }

        } else {
            if (IOHelper.exists(dir.resolve("GravitCentralRootCA.crt"))) {
                logger.warn("Found old default certificate - 'GravitCentralRootCA.crt'. Delete...");
                Files.delete(dir.resolve("GravitCentralRootCA.crt"));
            }
        }
        List<X509Certificate> certificates = new ArrayList<>();
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        IOHelper.walk(dir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toFile().getName().endsWith(".crt")) {
                    try (InputStream inputStream = IOHelper.newInput(file)) {
                        certificates.add((X509Certificate) certFactory.generateCertificate(inputStream));
                    } catch (CertificateException e) {
                        throw new IOException(e);
                    }
                }
                return super.visitFile(file, attrs);
            }
        }, false);
        trustManager = new LauncherTrustManager(certificates.toArray(new X509Certificate[0]));
    }

    public LauncherTrustManager.CheckClassResult checkClass(Class<?> clazz) {
        X509Certificate[] certificates = JVMHelper.getCertificates(clazz);
        return trustManager.checkCertificates(certificates, trustManager::stdCertificateChecker);
    }
}
