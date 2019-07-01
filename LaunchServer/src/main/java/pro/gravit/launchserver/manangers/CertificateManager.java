package pro.gravit.launchserver.manangers;


import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.bc.BcECContentSignerBuilder;
import pro.gravit.utils.helper.SecurityHelper;

import java.math.BigInteger;
import java.security.PublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

public class CertificateManager {
    public X509CertificateHolder ca;
    public AsymmetricKeyParameter caKey;

    //public X509CertificateHolder server;
    //public AsymmetricKeyParameter serverKey;

    public int validDays = 0;
    public int minusHours = 6;

    public X509CertificateHolder generateCertificate(String subjectName, PublicKey subjectPublicKey) throws OperatorCreationException {
        SubjectPublicKeyInfo subjectPubKeyInfo = SubjectPublicKeyInfo.getInstance(subjectPublicKey);
        BigInteger serial = BigInteger.valueOf(SecurityHelper.newRandom().nextLong());
        Date startDate = Date.from(Instant.now().minus(minusHours, ChronoUnit.HOURS));
        Date endDate = Date.from(startDate.toInstant().plus(validDays, ChronoUnit.DAYS));

        X500NameBuilder subject = new X500NameBuilder();
        subject.addRDN(BCStyle.CN, subjectName);
        X509v3CertificateBuilder v3CertGen = new X509v3CertificateBuilder(ca.getSubject(), serial,
                startDate, endDate, subject.build(), subjectPubKeyInfo);

        AlgorithmIdentifier sigAlgId = ca.getSignatureAlgorithm();
        AlgorithmIdentifier digAlgId = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId);
        ContentSigner sigGen = new BcECContentSignerBuilder(sigAlgId, digAlgId).build(caKey);

        return v3CertGen.build(sigGen);
    }
}
