package pro.gravit.launcher.base.events.request;

import pro.gravit.launcher.base.events.RequestEvent;

import java.security.PrivateKey;
import java.security.PublicKey;

public class FetchClientProfileKeyRequestEvent extends RequestEvent {
    public byte[] publicKey;
    public byte[] privateKey;
    public byte[] signature /* V2 */;
    public long expiresAt;
    public long refreshedAfter;

    public FetchClientProfileKeyRequestEvent(byte[] publicKey, byte[] privateKey, byte[] signature, long expiresAt, long refreshedAfter) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.signature = signature;
        this.expiresAt = expiresAt;
        this.refreshedAfter = refreshedAfter;
    }

    public FetchClientProfileKeyRequestEvent(PublicKey publicKey, PrivateKey privateKey, byte[] signature, long expiresAt, long refreshedAfter) {
        this.publicKey = publicKey.getEncoded();
        this.privateKey = privateKey.getEncoded();
        this.signature = signature;
        this.expiresAt = expiresAt;
        this.refreshedAfter = refreshedAfter;
    }

    @Override
    public String getType() {
        return "clientProfileKey";
    }
}
