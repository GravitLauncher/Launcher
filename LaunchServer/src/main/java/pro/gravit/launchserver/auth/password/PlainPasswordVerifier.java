package pro.gravit.launchserver.auth.password;

public class PlainPasswordVerifier extends PasswordVerifier {
    @Override
    public boolean check(String encryptedPassword, String password) {
        return encryptedPassword.equals(password);
    }

    @Override
    public String encrypt(String password) {
        return super.encrypt(password);
    }
}
