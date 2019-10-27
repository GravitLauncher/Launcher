package pro.gravit.launchserver.binary.tasks;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.binary.SignerJar;
import pro.gravit.launchserver.config.LaunchServerConfig;
import pro.gravit.launchserver.helper.SignHelper;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class SignJarTask implements LauncherBuildTask {

    private final LaunchServerConfig.JarSignerConf config;
    private final LaunchServer srv;

    public SignJarTask(LaunchServerConfig.JarSignerConf config, LaunchServer srv) {
        this.config = config;
        this.srv = srv;
    }

    @Override
    public String getName() {
        return "SignJar";
    }

    @Override
    public Path process(Path inputFile) throws IOException {
        Path toRet = srv.launcherBinary.nextPath("signed");
        sign(config, inputFile, toRet);
        return toRet;
    }

    public void sign(LaunchServerConfig.JarSignerConf config, Path inputFile, Path signedFile) throws IOException {
        if(config.enabled) stdSign(config, inputFile, signedFile);
        else autoSign(inputFile, signedFile);
    }

    private void stdSign(LaunchServerConfig.JarSignerConf config, Path inputFile, Path signedFile) throws IOException {
        KeyStore c = SignHelper.getStore(new File(config.keyStore).toPath(), config.keyStorePass, config.keyStoreType);
        try (SignerJar output = new SignerJar(new ZipOutputStream(IOHelper.newOutput(signedFile)), () -> SignJarTask.gen(config, c),
                config.metaInfSfName, config.metaInfKeyName);
             ZipInputStream input = new ZipInputStream(IOHelper.newInput(inputFile))) {
            //input.getManifest().getMainAttributes().forEach((a, b) -> output.addManifestAttribute(a.toString(), b.toString())); // may not work such as after Radon.
            ZipEntry e = input.getNextEntry();
            while (e != null) {
                if ("META-INF/MANIFEST.MF".equals(e.getName()) || "/META-INF/MANIFEST.MF".equals(e.getName())) {
                    Manifest m = new Manifest(input);
                    m.getMainAttributes().forEach((a, b) -> output.addManifestAttribute(a.toString(), b.toString()));
                    e = input.getNextEntry();
                    continue;
                }
                output.addFileContents(IOHelper.newZipEntry(e), input);
                e = input.getNextEntry();
            }
        }
    }
    private void autoSign(Path inputFile, Path signedFile) throws IOException {
        try (SignerJar output = new SignerJar(new ZipOutputStream(IOHelper.newOutput(signedFile)), () -> {
            try {
                return genCertificate(srv.config.projectName, srv.publicKey, srv.privateKey);
            } catch (OperatorCreationException | CertificateException | CMSException e) {
                throw new InternalError(e);
            }
        },
                "AUTOGEN.SF", "AUTOGEN.EC");
             ZipInputStream input = new ZipInputStream(IOHelper.newInput(inputFile))) {
            //input.getManifest().getMainAttributes().forEach((a, b) -> output.addManifestAttribute(a.toString(), b.toString())); // may not work such as after Radon.
            ZipEntry e = input.getNextEntry();
            while (e != null) {
                if ("META-INF/MANIFEST.MF".equals(e.getName()) || "/META-INF/MANIFEST.MF".equals(e.getName())) {
                    Manifest m = new Manifest(input);
                    m.getMainAttributes().forEach((a, b) -> output.addManifestAttribute(a.toString(), b.toString()));
                    e = input.getNextEntry();
                    continue;
                }
                output.addFileContents(IOHelper.newZipEntry(e), input);
                e = input.getNextEntry();
            }
        }
    }

    @Override
    public boolean allowDelete() {
        return true;
    }
    
    public static CMSSignedDataGenerator gen(LaunchServerConfig.JarSignerConf config, KeyStore c) {
    	try {
			return SignHelper.createSignedDataGenerator(c,
			        config.keyAlias, config.signAlgo, config.keyPass);
		} catch (CertificateEncodingException | UnrecoverableKeyException | KeyStoreException
				| OperatorCreationException | NoSuchAlgorithmException | CMSException e) {
			LogHelper.error(e);
			return null;
		}
    }
    public static CMSSignedDataGenerator genCertificate(String projectName, ECPublicKey publicKey, ECPrivateKey privateKey) throws OperatorCreationException, CertificateException, CMSException {

        X500NameBuilder subject = new X500NameBuilder();
        subject.addRDN(BCStyle.CN, projectName.concat(" Autogenerated"));
        subject.addRDN(BCStyle.O, projectName);
        LocalDateTime startDate = LocalDate.now().atStartOfDay();
        X509v3CertificateBuilder builder = new X509v3CertificateBuilder(
                subject.build(),
                new BigInteger("0"),
                Date.from(startDate.atZone(ZoneId.systemDefault()).toInstant()),
                Date.from(startDate.plusDays(3650).atZone(ZoneId.systemDefault()).toInstant()),
                new X500Name("CN=ca"),
                SubjectPublicKeyInfo.getInstance(publicKey.getEncoded()));
        JcaContentSignerBuilder csBuilder = new JcaContentSignerBuilder("SHA256WITHECDSA");
        ContentSigner signer = csBuilder.build(privateKey);
        X509CertificateHolder certificate = builder.build(signer);
        X509Certificate x509Certificate = new JcaX509CertificateConverter().setProvider( "BC" )
                .getCertificate( certificate );
        ArrayList<Certificate> chain = new ArrayList<>();
        chain.add(x509Certificate);
        return SignHelper.createSignedDataGenerator(privateKey, x509Certificate, chain, "SHA256WITHECDSA");
    }
}
