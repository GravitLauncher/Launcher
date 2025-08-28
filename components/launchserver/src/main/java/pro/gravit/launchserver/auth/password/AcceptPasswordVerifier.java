package pro.gravit.launchserver.auth.password;

public class AcceptPasswordVerifier extends PasswordVerifier {
    @Override
    public boolean check(String encryptedPassword, String password) {
        return true;
    }

    @Override
    public String encrypt(String password) {
        return "";
    }
}
