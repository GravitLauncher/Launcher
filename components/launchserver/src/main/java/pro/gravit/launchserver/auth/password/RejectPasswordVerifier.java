package pro.gravit.launchserver.auth.password;

public class RejectPasswordVerifier extends PasswordVerifier {
    @Override
    public boolean check(String encryptedPassword, String password) {
        return false;
    }
}
