package pro.gravit.launchserver.helper;

import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.SignerInfoGenerator;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DigestCalculatorProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.util.Store;
import pro.gravit.utils.helper.IOHelper;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SignHelper {

    public static final OutputStream NULL = OutputStream.nullOutputStream();
    public static final String hashFunctionName = "SHA-256";

    private SignHelper() {
    }

    /**
     * Creates the KeyStore with given algo.
     */
    public static KeyStore getStore(Path file, String storepass, String algo) throws IOException {
        try {
            KeyStore st = KeyStore.getInstance(algo);
            st.load(IOHelper.newInput(file), storepass != null ? storepass.toCharArray() : null);
            return st;
        } catch (NoSuchAlgorithmException | CertificateException | KeyStoreException e) {
            throw new IOException(e);
        }
    }

    /**
     * Creates the beast that can actually sign the data (for JKS, for other make it).
     */
    public static CMSSignedDataGenerator createSignedDataGenerator(KeyStore keyStore, String keyAlias, String signAlgo, String keyPassword) throws KeyStoreException, OperatorCreationException, CertificateEncodingException, UnrecoverableKeyException, NoSuchAlgorithmException, CMSException {
        List<Certificate> certChain = new ArrayList<>(Arrays.asList(keyStore.getCertificateChain(keyAlias)));
        @SuppressWarnings("rawtypes")
        Store certStore = new JcaCertStore(certChain);
        Certificate cert = keyStore.getCertificate(keyAlias);
        PrivateKey privateKey = (PrivateKey) keyStore.getKey(keyAlias, keyPassword != null ? keyPassword.toCharArray() : null);
        ContentSigner signer = new JcaContentSignerBuilder(signAlgo).setProvider("BC").build(privateKey);
        CMSSignedDataGenerator generator = new CMSSignedDataGenerator();
        DigestCalculatorProvider dcp = new JcaDigestCalculatorProviderBuilder().setProvider("BC").build();
        SignerInfoGenerator sig = new JcaSignerInfoGeneratorBuilder(dcp).build(signer, (X509Certificate) cert);
        generator.addSignerInfoGenerator(sig);
        generator.addCertificates(certStore);
        return generator;
    }

    public static CMSSignedDataGenerator createSignedDataGenerator(PrivateKey privateKey, Certificate cert, List<Certificate> certChain, String signAlgo) throws OperatorCreationException, CertificateEncodingException, CMSException {
        @SuppressWarnings("rawtypes")
        Store certStore = new JcaCertStore(certChain);
        ContentSigner signer = new JcaContentSignerBuilder(signAlgo).setProvider("BC").build(privateKey);
        CMSSignedDataGenerator generator = new CMSSignedDataGenerator();
        DigestCalculatorProvider dcp = new JcaDigestCalculatorProviderBuilder().setProvider("BC").build();
        SignerInfoGenerator sig = new JcaSignerInfoGeneratorBuilder(dcp).build(signer, (X509Certificate) cert);
        generator.addSignerInfoGenerator(sig);
        generator.addCertificates(certStore);
        return generator;
    }

    public static MessageDigest hasher() {
        try {
            return MessageDigest.getInstance(hashFunctionName);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    /**
     * Helper output stream that also sends the data to the given.
     */
    public static class HashingOutputStream extends OutputStream {
        public final OutputStream out;
        public final MessageDigest hasher;

        public HashingOutputStream(OutputStream out, MessageDigest hasher) {
            this.out = out;
            this.hasher = hasher;
        }

        @Override
        public void close() throws IOException {
            out.close();
        }

        @Override
        public void flush() throws IOException {
            out.flush();
        }

        @Override
        public void write(byte[] b) throws IOException {
            out.write(b);
            hasher.update(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            out.write(b, off, len);
            hasher.update(b, off, len);
        }

        @Override
        public void write(int b) throws IOException {
            out.write(b);
            hasher.update((byte) b);
        }

        public byte[] digest() {
            return hasher.digest();
        }
    }

    /**
     * Helper output stream that also sends the data to the given.
     */
    public static class HashingNonClosingOutputStream extends HashingOutputStream {
        public HashingNonClosingOutputStream(OutputStream out, MessageDigest hasher) {
            super(out, hasher);
        }

        @Override
        public void close() {
            // Do nothing
        }
    }
}
