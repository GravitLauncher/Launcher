package pro.gravit.launchserver.hibernate;

import pro.gravit.utils.helper.LogHelper;
import pro.gravit.utils.helper.SecurityHelper;

import javax.persistence.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(unique = true)
    public String username;
    @Column(name = "password")
    private byte[] password;
    private String password_salt;
    public void setPassword(String password)
    {
        password_salt = SecurityHelper.randomStringAESKey();
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            LogHelper.error(e);
            return;
        }
        this.password = digest.digest(password.concat(password_salt).getBytes(StandardCharsets.UTF_8));
    }
    public boolean verifyPassword(String password)
    {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            LogHelper.error(e);
            return false;
        }
        byte[] enpassword = digest.digest(password.concat(password_salt).getBytes(StandardCharsets.UTF_8));
        LogHelper.info(Arrays.toString(enpassword));
        LogHelper.info(Arrays.toString(this.password));
        return Arrays.equals(enpassword, this.password);
    }
}
