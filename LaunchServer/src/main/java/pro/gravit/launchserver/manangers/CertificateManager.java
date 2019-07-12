package pro.gravit.launchserver.manangers;


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
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.SecurityHelper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;

public class CertificateManager {
    public X509CertificateHolder ca;
    public AsymmetricKeyParameter caKey;

    public X509CertificateHolder server;
    public AsymmetricKeyParameter serverKey;


    //public X509CertificateHolder server;
    //public AsymmetricKeyParameter serverKey;

    public int validDays = 60;
    public int minusHours = 6;

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
        ECGenParameterSpec  ecGenSpec = new ECGenParameterSpec("secp384r1");
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(ecGenSpec, SecurityHelper.newRandom());
        KeyPair pair = generator.generateKeyPair();
        LocalDateTime startDate = LocalDate.now().atStartOfDay();

        X500NameBuilder subject = new X500NameBuilder();
        subject.addRDN(BCStyle.CN, orgName.concat(" CA"));
        subject.addRDN(BCStyle.O, orgName);

        X509v3CertificateBuilder builder= new X509v3CertificateBuilder(
                subject.build(),
                new BigInteger("0"),
                Date.from(startDate.atZone(ZoneId.systemDefault()).toInstant()),
                Date.from(startDate.plusDays(3650).atZone(ZoneId.systemDefault()).toInstant()),
                new X500Name("CN=ca"),
                SubjectPublicKeyInfo.getInstance(pair.getPublic().getEncoded()));
        JcaContentSignerBuilder csBuilder= new JcaContentSignerBuilder("SHA256WITHECDSA");
        ContentSigner signer = csBuilder.build(pair.getPrivate());
        ca = builder.build(signer);
        caKey = PrivateKeyFactory.createKey(pair.getPrivate().getEncoded());
    }

    public KeyPair generateKeyPair() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException {
        ECGenParameterSpec  ecGenSpec = new ECGenParameterSpec("secp384r1");
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(ecGenSpec, SecurityHelper.newRandom());
        return generator.generateKeyPair();
    }

    public void writePrivateKey(Path file, PrivateKey privateKey) throws IOException {
        try (PemWriter writer = new PemWriter(IOHelper.newWriter(file))) {
            writer.writeObject(new PemObject("PRIVATE KEY", privateKey.getEncoded()));
        }
    }

    public void writePrivateKey(Path file, AsymmetricKeyParameter key) throws IOException {
        PrivateKeyInfo info = PrivateKeyInfoFactory.createPrivateKeyInfo(key);
        try (PemWriter writer = new PemWriter(IOHelper.newWriter(file))) {
            writer.writeObject(new PemObject("PRIVATE KEY", info.getEncoded()));
        }
    }

    public void writeCertificate(Path file, X509CertificateHolder holder) throws IOException {
        try (PemWriter writer = new PemWriter(IOHelper.newWriter(file))) {
            writer.writeObject(new PemObject("CERTIFICATE", holder.toASN1Structure().getEncoded()));
        }
    }

    public AsymmetricKeyParameter readPrivateKey(Path file) throws IOException {
        AsymmetricKeyParameter ret;
        try(PemReader reader = new PemReader(IOHelper.newReader(file)))
        {
            byte[] bytes = reader.readPemObject().getContent();
            try(ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes))
            {

                ret = PrivateKeyFactory.createKey(inputStream);
            }
        }
        return ret;
    }

    public X509CertificateHolder readCertificate(Path file) throws IOException {
        X509CertificateHolder ret;
        try(PemReader reader = new PemReader(IOHelper.newReader(file)))
        {
            byte[] bytes = reader.readPemObject().getContent();
            ret = new X509CertificateHolder(bytes);
        }
        return ret;
    }
}
