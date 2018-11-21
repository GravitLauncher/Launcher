package ru.gravit.launchserver.binary;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.CMSTypedData;
import org.bouncycastle.cms.SignerInfoGenerator;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DigestCalculatorProvider;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.util.Store;

import ru.gravit.utils.helper.IOHelper;


/**
 * Generator of signed Jars. It stores some data in memory therefore it is not suited for creation of large files. The
 * usage:
 * <pre>
 * KeyStore keystore = KeyStore.getInstance("JKS");
 * keyStore.load(keystoreStream, "keystorePassword");
 * SignerJar jar = new SignerJar(out, keyStore, "keyAlias", "keyPassword");
 * signedJar.addManifestAttribute("Main-Class", "com.example.MainClass");
 * signedJar.addManifestAttribute("Application-Name", "Example");
 * signedJar.addManifestAttribute("Permissions", "all-permissions");
 * signedJar.addManifestAttribute("Codebase", "*");
 * signedJar.addFileContents("com/example/MainClass.class", clsData);
 * signedJar.addFileContents("JNLP-INF/APPLICATION.JNLP", generateJnlpContents());
 * signedJar.close();
 * </pre>
 */
public class SignerJar implements AutoCloseable {
    /**
     * Helper output stream that also sends the data to the given {@link com.google.common.hash.Hasher}.
     */
    static class HashingOutputStream extends OutputStream {
        private final OutputStream out;
        private final MessageDigest hasher;

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
    }

    private static final String MANIFEST_FN = "META-INF/MANIFEST.MF";
    private static final String SIG_FN = "META-INF/SIGNUMO.SF";

    private static final String SIG_RSA_FN = "META-INF/SIGNUMO.RSA";

    private static final String hashFunctionName = "SHA-256";

    public static KeyStore getStore(Path file, String storepass, String algo) throws IOException {
        try {
            KeyStore st = KeyStore.getInstance(algo);
            st.load(IOHelper.newInput(file), storepass != null ? storepass.toCharArray() : null);
            return st;
        } catch (NoSuchAlgorithmException | CertificateException | KeyStoreException e) {
            throw new IOException(e);
        }
    }

    private static MessageDigest hasher() {
        try {
            return MessageDigest.getInstance(hashFunctionName);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    private final ZipOutputStream zos;

    private final KeyStore keyStore;

    private final String keyAlias;

    private final String password;
    public final Map<String, String> manifestAttributes;
    private String manifestHash;
    private String manifestMainHash;

    private final Map<String, String> fileDigests;

    private final Map<String, String> sectionDigests;

    /**
     * Constructor.
     *
     * @param out         the output stream to write JAR data to
     * @param keyStore    the key store to load given key from
     * @param keyAlias    the name of the key in the store, this key is used to sign the JAR
     * @param keyPassword the password to access the key
     */
    public SignerJar(OutputStream out, KeyStore keyStore, String keyAlias, String keyPassword) {
        zos = new ZipOutputStream(out);
        this.keyStore = keyStore;
        this.keyAlias = keyAlias;
        password = keyPassword;

        manifestAttributes = new LinkedHashMap<>();
        fileDigests = new LinkedHashMap<>();
        sectionDigests = new LinkedHashMap<>();
    }

    /**
     * Adds a file to the JAR. The file is immediately added to the zipped output stream. This method cannot be called once
     * the stream is closed.
     *
     * @param filename name of the file to add (use forward slash as a path separator)
     * @param contents contents of the file
     * @throws java.io.IOException
     * @throws NullPointerException if any of the arguments is {@code null}
     */
    public void addFileContents(String filename, byte[] contents) throws IOException {
        zos.putNextEntry(new ZipEntry(filename));
        zos.write(contents);
        zos.closeEntry();

        String hashCode64 = Base64.getEncoder().encodeToString(hasher().digest(contents));
        fileDigests.put(filename, hashCode64);
    }

    /**
     * Adds a file to the JAR. The file is immediately added to the zipped output stream. This method cannot be called once
     * the stream is closed.
     *
     * @param filename name of the file to add (use forward slash as a path separator)
     * @param contents contents of the file
     * @throws java.io.IOException
     * @throws NullPointerException if any of the arguments is {@code null}
     */
    public void addFileContents(String filename, InputStream contents) throws IOException {
        zos.putNextEntry(new ZipEntry(filename));
        byte[] arr = IOHelper.toByteArray(contents);
        zos.write(arr);
        zos.closeEntry();

        String hashCode64 = Base64.getEncoder().encodeToString(hasher().digest(arr));
        fileDigests.put(filename, hashCode64);
    }

    /**
     * Adds a file to the JAR. The file is immediately added to the zipped output stream. This method cannot be called once
     * the stream is closed.
     *
     * @param entry    name of the file to add (use forward slash as a path separator)
     * @param contents contents of the file
     * @throws java.io.IOException
     * @throws NullPointerException if any of the arguments is {@code null}
     */
    public void addFileContents(ZipEntry entry, byte[] contents) throws IOException {
        zos.putNextEntry(entry);
        zos.write(contents);
        zos.closeEntry();

        String hashCode64 = Base64.getEncoder().encodeToString(hasher().digest(contents));
        fileDigests.put(entry.getName(), hashCode64);
    }

    /**
     * Adds a file to the JAR. The file is immediately added to the zipped output stream. This method cannot be called once
     * the stream is closed.
     *
     * @param entry    name of the file to add (use forward slash as a path separator)
     * @param contents contents of the file
     * @throws java.io.IOException
     * @throws NullPointerException if any of the arguments is {@code null}
     */
    public void addFileContents(ZipEntry entry, InputStream contents) throws IOException {
        zos.putNextEntry(entry);
        byte[] arr = IOHelper.toByteArray(contents);
        zos.write(arr);
        zos.closeEntry();

        String hashCode64 = Base64.getEncoder().encodeToString(hasher().digest(arr));
        fileDigests.put(entry.getName(), hashCode64);
    }

    /**
     * Adds a header to the manifest of the JAR.
     *
     * @param name  name of the attribute, it is placed into the main section of the manifest file, it cannot be longer
     *              than {@value #MANIFEST_ATTR_MAX_LEN} bytes (in utf-8 encoding)
     * @param value value of the attribute
     */
    public void addManifestAttribute(String name, String value) {
        manifestAttributes.put(name.trim(), value.trim());
    }


    /**
     * Closes the JAR file by writing the manifest and signature data to it and finishing the ZIP entries. It closes the
     * underlying stream.
     *
     * @throws java.io.IOException
     * @throws RuntimeException    if the signing goes wrong
     */
    @Override
    public void close() throws IOException {
        finish();
        zos.close();
    }

    /**
     * Creates the beast that can actually sign the data.
     */
    private CMSSignedDataGenerator createSignedDataGenerator() throws Exception {
        Security.addProvider(new BouncyCastleProvider());

        List<Certificate> certChain = new ArrayList<>(Arrays.asList(keyStore.getCertificateChain(keyAlias)));
        Store certStore = new JcaCertStore(certChain);
        Certificate cert = keyStore.getCertificate(keyAlias);
        PrivateKey privateKey = (PrivateKey) keyStore.getKey(keyAlias, password != null ? password.toCharArray() : null);
        ContentSigner signer = new JcaContentSignerBuilder("SHA256WITHRSA").setProvider("BC").build(privateKey);
        CMSSignedDataGenerator generator = new CMSSignedDataGenerator();
        DigestCalculatorProvider dcp = new JcaDigestCalculatorProviderBuilder().setProvider("BC").build();
        SignerInfoGenerator sig = new JcaSignerInfoGeneratorBuilder(dcp).build(signer, (X509Certificate) cert);
        generator.addSignerInfoGenerator(sig);
        generator.addCertificates(certStore);
        return generator;
    }


    /**
     * Finishes the JAR file by writing the manifest and signature data to it and finishing the ZIP entries. It leaves the
     * underlying stream open.
     *
     * @throws java.io.IOException
     * @throws RuntimeException    if the signing goes wrong
     */
    public void finish() throws IOException {
        writeManifest();
        byte sig[] = writeSigFile();
        writeSignature(sig);
        zos.finish();
    }

    public ZipOutputStream getZos() {
        return zos;
    }

    /**
     * Helper for {@link #writeManifest()} that creates the digest of one entry.
     */
    private String hashEntrySection(String name, Attributes attributes) throws IOException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        manifest.write(o);
        int emptyLen = o.toByteArray().length;

        manifest.getEntries().put(name, attributes);

        manifest.write(o);
        byte[] ob = o.toByteArray();
        ob = Arrays.copyOfRange(ob, emptyLen, ob.length);
        return Base64.getEncoder().encodeToString(hasher().digest(ob));
    }

    /**
     * Helper for {@link #writeManifest()} that creates the digest of the main section.
     */
    private String hashMainSection(Attributes attributes) throws IOException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().putAll(attributes);
        MessageDigest hasher = hasher();
        SignerJar.HashingOutputStream o = new SignerJar.HashingOutputStream(new OutputStream() {
            @Override
            public String toString() {
                return "NullOutputStream";
            }

            /** Discards the specified byte array. */
            @Override
            public void write(byte[] b) {
            }

            /** Discards the specified byte array. */
            @Override
            public void write(byte[] b, int off, int len) {
            }

            /** Discards the specified byte. */
            @Override
            public void write(int b) {
            }
        }, hasher);
        manifest.write(o);
        return Base64.getEncoder().encodeToString(hasher.digest());
    }

    /**
     * Returns the CMS signed data.
     */
    private byte[] signSigFile(byte[] sigContents) throws Exception {
        CMSSignedDataGenerator gen = createSignedDataGenerator();
        CMSTypedData cmsData = new CMSProcessableByteArray(sigContents);
        CMSSignedData signedData = gen.generate(cmsData, true);
        return signedData.getEncoded();
    }

    /**
     * Writes the manifest to the JAR. It also calculates the digests that are required to be placed in the the signature
     * file.
     *
     * @throws java.io.IOException
     */
    private void writeManifest() throws IOException {
        zos.putNextEntry(new ZipEntry(MANIFEST_FN));
        Manifest man = new Manifest();

        // main section
        Attributes mainAttributes = man.getMainAttributes();
        mainAttributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");

        for (Map.Entry<String, String> entry : manifestAttributes.entrySet())
            mainAttributes.put(new Attributes.Name(entry.getKey()), entry.getValue());

        // individual files sections
        Attributes.Name digestAttr = new Attributes.Name(hashFunctionName + "-Digest");
        for (Map.Entry<String, String> entry : fileDigests.entrySet()) {
            Attributes attributes = new Attributes();
            man.getEntries().put(entry.getKey(), attributes);
            attributes.put(digestAttr, entry.getValue());
            sectionDigests.put(entry.getKey(), hashEntrySection(entry.getKey(), attributes));
        }

        MessageDigest hasher = hasher();
        OutputStream out = new SignerJar.HashingOutputStream(zos, hasher);
        man.write(out);
        zos.closeEntry();

        manifestHash = Base64.getEncoder().encodeToString(hasher.digest());
        manifestMainHash = hashMainSection(man.getMainAttributes());
    }

    /**
     * Writes the .SIG file to the JAR.
     *
     * @return the contents of the file as bytes
     */
    private byte[] writeSigFile() throws IOException {
        zos.putNextEntry(new ZipEntry(SIG_FN));
        Manifest man = new Manifest();
        // main section
        Attributes mainAttributes = man.getMainAttributes();
        mainAttributes.put(Attributes.Name.SIGNATURE_VERSION, "1.0");
        mainAttributes.put(new Attributes.Name(hashFunctionName + "-Digest-Manifest"), manifestHash);
        mainAttributes.put(new Attributes.Name(hashFunctionName + "-Digest-Manifest-Main-Attributes"), manifestMainHash);

        // individual files sections
        Attributes.Name digestAttr = new Attributes.Name(hashFunctionName + "-Digest");
        for (Map.Entry<String, String> entry : sectionDigests.entrySet()) {
            Attributes attributes = new Attributes();
            man.getEntries().put(entry.getKey(), attributes);
            attributes.put(digestAttr, entry.getValue());
        }

        man.write(zos);
        zos.closeEntry();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        man.write(baos);
        return baos.toByteArray();
    }

    /**
     * Signs the .SIG file and writes the signature (.RSA file) to the JAR.
     *
     * @throws java.io.IOException
     * @throws RuntimeException    if the signing failed
     */
    private void writeSignature(byte[] sigFile) throws IOException {
        zos.putNextEntry(new ZipEntry(SIG_RSA_FN));
        try {
            byte[] signature = signSigFile(sigFile);
            zos.write(signature);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Signing failed.", e);
        }
        zos.closeEntry();
    }
}