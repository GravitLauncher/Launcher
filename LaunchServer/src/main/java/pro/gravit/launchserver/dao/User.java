package pro.gravit.launchserver.dao;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import pro.gravit.launcher.ClientPermissions;
import pro.gravit.utils.helper.LogHelper;
import pro.gravit.utils.helper.SecurityHelper;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(unique = true)
    public String username;
    @Column(unique = true)
    public UUID uuid;
    @Column(name = "password")
    private byte[] password;
    private String accessToken;
    public String serverID;
    public String email;
    private String password_salt;
    public long permissions;
    //TODO: заменить EAGER на LASY и придумать способ сохранить сессию
    // [ERROR] org.hibernate.LazyInitializationException:
    // failed to lazily initialize a collection of role: pro.gravit.launchserver.dao.User.hwids, could not initialize proxy - no Session
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    public Collection<UserHWID> hwids;
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
    public ClientPermissions getPermissions()
    {
        return new ClientPermissions(permissions);
    }
    public void setPermissions(ClientPermissions permissions)
    {
        this.permissions = permissions.toLong();
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
}
