package pro.gravit.launcher.request.auth.password;

import pro.gravit.launcher.request.auth.AuthRequest;

public class AuthSignaturePassword implements AuthRequest.AuthPasswordInterface {
    public byte[] signature;
    public byte[] publicKey;
    public byte[] salt;

    @Override
    public boolean check() {
        return true;
    }
}
