package pro.gravit.launchserver.manangers;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.crypto.util.PrivateKeyInfoFactory;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.bc.BcECContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.bouncycastle.util.io.pem.PemWriter;
import pro.gravit.launcher.LauncherTrustManager;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.JVMHelper;
import pro.gravit.utils.helper.SecurityHelper;

import java.io.*;
import java.math.BigInteger;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CertificateManager {
    public final int validDays = 60;
    public final int minusHours = 6;
    private transient final Logger logger = LogManager.getLogger();
    public X509CertificateHolder ca;
    public AsymmetricKeyParameter caKey;
    public X509CertificateHolder server;
    public AsymmetricKeyParameter serverKey;
    public LauncherTrustManager trustManager;
    public String orgName;

    public X509CertificateHolder generateCertificate(String subjectName, PublicKey subjectPublicKey) throws OperatorCreationException {
        SubjectPublicKeyInfo subjectPubKeyInfo = SubjectPublicKeyInfo.getInstance(subjectPublicKey.getEncoded());
        BigInteger serial = BigInteger.valueOf(SecurityHelper.newRandom().nextLong());
        Date startDate = Date.from(Instant.now().minus(minusHours, ChronoUnit.HOURS));
        Date endDate = Date.from(startDate.toInstant().plus(validDays, ChronoUnit.DAYS));

        X500NameBuilder subject = new X500NameBuilder();
        subject.addRDN(BCStyle.CN, subjectName);
        subject.addRDN(BCStyle.O, orgName);
        X509v3CertificateBuilder v3CertGen = new X509v3CertificateBuilder(ca.getSubject(), serial,
                startDate, endDate, subject.build(), subjectPubKeyInfo);

        AlgorithmIdentifier sigAlgId = ca.getSignatureAlgorithm();
        AlgorithmIdentifier digAlgId = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId);
        ContentSigner sigGen = new BcECContentSignerBuilder(sigAlgId, digAlgId).build(caKey);

        return v3CertGen.build(sigGen);
    }

    public void generateCA() throws NoSuchAlgorithmException, IOException, OperatorCreationException, InvalidAlgorithmParameterException {
        ECGenParameterSpec ecGenSpec = new ECGenParameterSpec("secp384k1");
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(ecGenSpec, SecurityHelper.newRandom());
        KeyPair pair = generator.generateKeyPair();
        LocalDateTime startDate = LocalDate.now().atStartOfDay();

        X500NameBuilder subject = new X500NameBuilder();
        subject.addRDN(BCStyle.CN, orgName.concat(" CA"));
        subject.addRDN(BCStyle.O, orgName);

        X509v3CertificateBuilder builder = new X509v3CertificateBuilder(
                subject.build(),
                new BigInteger("0"),
                Date.from(startDate.atZone(ZoneId.systemDefault()).toInstant()),
                Date.from(startDate.plusDays(3650).atZone(ZoneId.systemDefault()).toInstant()),
                new X500Name("CN=ca"),
                SubjectPublicKeyInfo.getInstance(pair.getPublic().getEncoded()));
        JcaContentSignerBuilder csBuilder = new JcaContentSignerBuilder("SHA256WITHECDSA");
        ContentSigner signer = csBuilder.build(pair.getPrivate());
        ca = builder.build(signer);
        caKey = PrivateKeyFactory.createKey(pair.getPrivate().getEncoded());
    }

    public KeyPair generateKeyPair() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException {
        ECGenParameterSpec ecGenSpec = new ECGenParameterSpec("secp384k1");
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(ecGenSpec, SecurityHelper.newRandom());
        return generator.generateKeyPair();
    }

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

    @Deprecated
    public void checkClass(Class<?> clazz, LauncherTrustManager.CheckMode mode) throws SecurityException {
        if (trustManager == null) return;
        X509Certificate[] certificates = JVMHelper.getCertificates(clazz);
        if (certificates == null) {
            if (mode == LauncherTrustManager.CheckMode.EXCEPTION_IN_NOT_SIGNED)
                throw new SecurityException(String.format("Class %s not signed", clazz.getName()));
            else if (mode == LauncherTrustManager.CheckMode.WARN_IN_NOT_SIGNED)
                logger.warn("Class {} not signed", clazz.getName());
            return;
        }
        try {
            trustManager.checkCertificatesSuccess(certificates, trustManager::stdCertificateChecker);
        } catch (Exception e) {
            throw new SecurityException(e);
        }
    }

    public LauncherTrustManager.CheckClassResult checkClass(Class<?> clazz) {
        X509Certificate[] certificates = JVMHelper.getCertificates(clazz);
        return trustManager.checkCertificates(certificates, trustManager::stdCertificateChecker);
    }
}
