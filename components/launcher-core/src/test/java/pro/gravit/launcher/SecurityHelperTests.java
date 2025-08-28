package pro.gravit.launcher;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import pro.gravit.utils.helper.SecurityHelper;

import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class SecurityHelperTests {
    @Test
    public void aesLegacyTest() throws Exception {
        byte[] bytes = SecurityHelper.randomBytes(24);
        byte[] seed = SecurityHelper.randomBytes(32);
        byte[] encrypted = SecurityHelper.encrypt(seed, bytes);
        byte[] decrypted = SecurityHelper.decrypt(seed, encrypted);
        Assertions.assertArrayEquals(bytes, decrypted);
    }


    @Test
    public void aesStreamTest() throws Exception {
        byte[] bytes = SecurityHelper.randomBytes(128);
        byte[] seed = SecurityHelper.randomAESKey();
        byte[] encrypted;
        ByteArrayOutputStream s = new ByteArrayOutputStream();
        try(OutputStream o = new CipherOutputStream(s, SecurityHelper.newAESEncryptCipher(seed))) {
            try(ByteArrayInputStream i = new ByteArrayInputStream(bytes)) {
                i.transferTo(o);
            }
        }
        encrypted = s.toByteArray();
        byte[] decrypted;
        ;
        try(InputStream i = new CipherInputStream(new ByteArrayInputStream(encrypted), SecurityHelper.newAESDecryptCipher(seed))) {
            ByteArrayOutputStream s2 = new ByteArrayOutputStream();
            try(s2) {
                i.transferTo(s2);
            }
            decrypted = s2.toByteArray();
        }
        Assertions.assertArrayEquals(bytes, decrypted);
    }
}
