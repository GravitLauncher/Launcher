package pro.gravit.launcher.profiles;

import pro.gravit.launcher.serialize.HOutput;
import pro.gravit.launcher.serialize.stream.StreamObject;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.SecurityHelper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;

public final class Texture extends StreamObject {
    private static final SecurityHelper.DigestAlgorithm DIGEST_ALGO = SecurityHelper.DigestAlgorithm.SHA256;

    // Instance

    public final String url;

    public final byte[] digest;

    public final Map<String, String> metadata;

    public Texture(String url, boolean cloak) throws IOException {
        this.url = IOHelper.verifyURL(url);

        // Fetch texture
        byte[] texture;
        try (InputStream input = IOHelper.newInput(new URL(url))) {
            texture = IOHelper.read(input);
        }
        try (ByteArrayInputStream input = new ByteArrayInputStream(texture)) {
            IOHelper.readTexture(input, cloak); // Verify texture
        }

        // Get digest of texture
        digest = SecurityHelper.digest(DIGEST_ALGO, new URL(url));
        metadata = null; // May be auto-detect?
    }

    public Texture(String url, Path local, boolean cloak) throws IOException {
        this.url = IOHelper.verifyURL(url);
        try (InputStream input = IOHelper.newInput(local)) {
            IOHelper.readTexture(input, cloak); // Verify texture
        }
        this.digest = SecurityHelper.digest(DIGEST_ALGO, local);
        this.metadata = null;
    }


    public Texture(String url, byte[] digest) {
        this.url = IOHelper.verifyURL(url);
        this.digest = digest == null ? new byte[0] : digest;
        this.metadata = null;
    }

    public Texture(String url, byte[] digest, Map<String, String> metadata) {
        this.url = url;
        this.digest = digest == null ? new byte[0] : digest;
        this.metadata = metadata;
    }

    @Override
    public void write(HOutput output) throws IOException {
        output.writeASCII(url, 2048);
        output.writeByteArray(digest, -DIGEST_ALGO.bytes);
    }

    @Override
    public String toString() {
        return "Texture{" +
                "url='" + url + '\'' +
                ", digest=" + Arrays.toString(digest) +
                ", metadata=" + metadata +
                '}';
    }
}
