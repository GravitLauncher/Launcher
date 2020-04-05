package pro.gravit.launchserver.binary;

import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.CMSTypedData;
import pro.gravit.launchserver.helper.SignHelper;
import pro.gravit.utils.helper.IOHelper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


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

    private static final String MANIFEST_FN = "META-INF/MANIFEST.MF";
    private static final String DIGEST_HASH = SignHelper.hashFunctionName + "-Digest";
    private final String SIG_FN;
    private final String SIG_KEY_FN;
    private final ZipOutputStream zos;

    private final Map<String, String> manifestAttributes;
    private final Map<String, String> fileDigests;
    private final Map<String, String> sectionDigests;
    private final Supplier<CMSSignedDataGenerator> gen;
    private String manifestHash;
    private String manifestMainHash;

    public SignerJar(ZipOutputStream out, Supplier<CMSSignedDataGenerator> gen, String sig_fn, String sig_key_fn) {
        zos = out;
        this.gen = gen;
        manifestAttributes = new LinkedHashMap<>();
        fileDigests = new LinkedHashMap<>();
        sectionDigests = new LinkedHashMap<>();
        SIG_FN = "META-INF/".concat(sig_fn);
        SIG_KEY_FN = "META-INF/".concat(sig_key_fn);
    }

    /**
     * Adds a file to the JAR. The file is immediately added to the zipped output stream. This method cannot be called once
     * the stream is closed.
     *
     * @param filename name of the file to add (use forward slash as a path separator)
     * @param contents contents of the file
     * @throws IOException
     * @throws NullPointerException if any of the arguments is {@code null}
     */
    public void addFileContents(String filename, byte[] contents) throws IOException {
        addFileContents(filename, new ByteArrayInputStream(contents));
    }

    /**
     * Adds a file to the JAR. The file is immediately added to the zipped output stream. This method cannot be called once
     * the stream is closed.
     *
     * @param filename name of the file to add (use forward slash as a path separator)
     * @param contents contents of the file
     * @throws IOException
     * @throws NullPointerException if any of the arguments is {@code null}
     */
    public void addFileContents(String filename, InputStream contents) throws IOException {
        addFileContents(IOHelper.newZipEntry(filename), contents);
    }

    /**
     * Adds a file to the JAR. The file is immediately added to the zipped output stream. This method cannot be called once
     * the stream is closed.
     *
     * @param entry    name of the file to add (use forward slash as a path separator)
     * @param contents contents of the file
     * @throws IOException
     * @throws NullPointerException if any of the arguments is {@code null}
     */
    public void addFileContents(ZipEntry entry, byte[] contents) throws IOException {
        addFileContents(entry, new ByteArrayInputStream(contents));
    }

    /**
     * Adds a file to the JAR. The file is immediately added to the zipped output stream. This method cannot be called once
     * the stream is closed.
     *
     * @param entry    name of the file to add (use forward slash as a path separator)
     * @param contents contents of the file
     * @throws IOException
     * @throws NullPointerException if any of the arguments is {@code null}
     */
    public void addFileContents(ZipEntry entry, InputStream contents) throws IOException {
        zos.putNextEntry(entry);
        SignHelper.HashingOutputStream out = new SignHelper.HashingNonClosingOutputStream(zos, SignHelper.hasher());
        IOHelper.transfer(contents, out);
        zos.closeEntry();
        fileDigests.put(entry.getName(), Base64.getEncoder().encodeToString(out.digest()));
    }

    /**
     * Adds a header to the manifest of the JAR.
     *
     * @param name  name of the attribute, it is placed into the main section of the manifest file
     * @param value value of the attribute
     */
    public void addManifestAttribute(String name, String value) {
        manifestAttributes.put(name, value);
    }


    /**
     * Closes the JAR file by writing the manifest and signature data to it and finishing the ZIP entries. It closes the
     * underlying stream.
     *
     * @throws IOException
     * @throws RuntimeException if the signing goes wrong
     */
    @Override
    public void close() throws IOException {
        finish();
        zos.close();
    }


    /**
     * Finishes the JAR file by writing the manifest and signature data to it and finishing the ZIP entries. It leaves the
     * underlying stream open.
     *
     * @throws IOException
     * @throws RuntimeException if the signing goes wrong
     */
    public void finish() throws IOException {
        writeManifest();
        byte[] sig = writeSigFile();
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
        return Base64.getEncoder().encodeToString(SignHelper.hasher().digest(ob));
    }

    /**
     * Helper for {@link #writeManifest()} that creates the digest of the main section.
     */
    private String hashMainSection(Attributes attributes) throws IOException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().putAll(attributes);
        SignHelper.HashingOutputStream o = new SignHelper.HashingNonClosingOutputStream(SignHelper.NULL, SignHelper.hasher());
        manifest.write(o);
        return Base64.getEncoder().encodeToString(o.digest());
    }

    /**
     * Returns the CMS signed data.
     */
    private byte[] signSigFile(byte[] sigContents) throws Exception {
        CMSSignedDataGenerator gen = this.gen.get();
        CMSTypedData cmsData = new CMSProcessableByteArray(sigContents);
        CMSSignedData signedData = gen.generate(cmsData, false);
        return signedData.getEncoded();
    }

    /**
     * Writes the manifest to the JAR. It also calculates the digests that are required to be placed in the the signature
     * file.
     *
     * @throws IOException
     */
    private void writeManifest() throws IOException {
        zos.putNextEntry(IOHelper.newZipEntry(MANIFEST_FN));
        Manifest man = new Manifest();

        // main section
        Attributes mainAttributes = man.getMainAttributes();
        mainAttributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");

        for (Map.Entry<String, String> entry : manifestAttributes.entrySet())
            mainAttributes.put(new Attributes.Name(entry.getKey()), entry.getValue());

        // individual files sections
        Attributes.Name digestAttr = new Attributes.Name(DIGEST_HASH);
        for (Map.Entry<String, String> entry : fileDigests.entrySet()) {
            Attributes attributes = new Attributes();
            man.getEntries().put(entry.getKey(), attributes);
            attributes.put(digestAttr, entry.getValue());
            sectionDigests.put(entry.getKey(), hashEntrySection(entry.getKey(), attributes));
        }

        SignHelper.HashingOutputStream out = new SignHelper.HashingNonClosingOutputStream(zos, SignHelper.hasher());
        man.write(out);
        zos.closeEntry();

        manifestHash = Base64.getEncoder().encodeToString(out.digest());
        manifestMainHash = hashMainSection(man.getMainAttributes());
    }

    /**
     * Writes the .SIG file to the JAR.
     *
     * @return the contents of the file as bytes
     */
    private byte[] writeSigFile() throws IOException {
        zos.putNextEntry(IOHelper.newZipEntry(SIG_FN));
        Manifest man = new Manifest();
        // main section
        Attributes mainAttributes = man.getMainAttributes();
        mainAttributes.put(Attributes.Name.SIGNATURE_VERSION, "1.0");
        mainAttributes.put(new Attributes.Name(DIGEST_HASH + "-Manifest"), manifestHash);
        mainAttributes.put(new Attributes.Name(DIGEST_HASH + "-Manifest-Main-Attributes"), manifestMainHash);

        // individual files sections
        Attributes.Name digestAttr = new Attributes.Name(DIGEST_HASH);
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
     * @throws IOException
     * @throws RuntimeException if the signing failed
     */
    private void writeSignature(byte[] sigFile) throws IOException {
        zos.putNextEntry(IOHelper.newZipEntry(SIG_KEY_FN));
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