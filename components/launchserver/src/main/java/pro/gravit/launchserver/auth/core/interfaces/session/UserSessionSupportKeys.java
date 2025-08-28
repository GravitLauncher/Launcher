package pro.gravit.launchserver.auth.core.interfaces.session;

import java.security.PrivateKey;
import java.security.PublicKey;

public interface UserSessionSupportKeys {
    ClientProfileKeys getClientProfileKeys();

    record ClientProfileKeys(PublicKey publicKey, PrivateKey privateKey, byte[] signature /* V2 */, long expiresAt,
                             long refreshedAfter) {

    }
}
