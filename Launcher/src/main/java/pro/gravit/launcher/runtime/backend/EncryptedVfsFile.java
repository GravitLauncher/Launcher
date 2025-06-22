package pro.gravit.launcher.runtime.backend;

import pro.gravit.launcher.base.Launcher;
import pro.gravit.launcher.base.vfs.Vfs;
import pro.gravit.launcher.base.vfs.VfsFile;
import pro.gravit.utils.helper.SecurityHelper;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class EncryptedVfsFile extends VfsFile {
    private VfsFile parent;
    private final String alg;
    private final SecretKeySpec sKeySpec;
    private final IvParameterSpec iKeySpec;

    public EncryptedVfsFile(VfsFile parent) {
        this.parent = parent;
        this.alg = "AES/CBC/PKCS5Padding";
        try {
            byte[] compat = SecurityHelper.getAESKey(Launcher.getConfig().runtimeEncryptKey.getBytes(StandardCharsets.UTF_8));
            sKeySpec = new SecretKeySpec(compat, "AES");
            iKeySpec = new IvParameterSpec("8u3d90ikr7o67lsq".getBytes());
        } catch (Exception e) {
            throw new SecurityException(e);
        }
    }

    @Override
    public InputStream getInputStream() {
        Cipher cipher;
        try {
            cipher = Cipher.getInstance(alg);
            cipher.init(Cipher.DECRYPT_MODE, sKeySpec, iKeySpec);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                 InvalidAlgorithmParameterException e) {
            throw new SecurityException(e);
        }
        return new BufferedInputStream(new CipherInputStream(parent.getInputStream(), cipher));
    }
}
