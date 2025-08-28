package pro.gravit.launcher.base.events.request;

import pro.gravit.launcher.base.events.RequestEvent;

import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;

public class GetPublicKeyRequestEvent extends RequestEvent {
    public byte[] rsaPublicKey;
    public byte[] ecdsaPublicKey;

    public GetPublicKeyRequestEvent(byte[] rsaPublicKey, byte[] ecdsaPublicKey) {
        this.rsaPublicKey = rsaPublicKey;
        this.ecdsaPublicKey = ecdsaPublicKey;
    }

    public GetPublicKeyRequestEvent(RSAPublicKey rsaPublicKey, ECPublicKey ecdsaPublicKey) {
        this.rsaPublicKey = rsaPublicKey.getEncoded();
        this.ecdsaPublicKey = ecdsaPublicKey.getEncoded();
    }

    @Override
    public String getType() {
        return "getPublicKey";
    }
}
