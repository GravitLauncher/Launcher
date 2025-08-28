package pro.gravit.launcher.runtime.backend;

import pro.gravit.launcher.runtime.client.DirBridge;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;
import pro.gravit.utils.helper.SecurityHelper;

import java.io.IOException;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;

public class ECKeyHolder {
    public ECPublicKey publicKey;
    public ECPrivateKey privateKey;

    public ECPublicKey getClientPublicKey() {
        return publicKey;
    }

    public byte[] sign(byte[] bytes) {
        return SecurityHelper.sign(bytes, privateKey);
    }

    public void readKeys() throws IOException, InvalidKeySpecException {
        if (privateKey != null || publicKey != null) return;
        Path dir = DirBridge.dir;
        Path publicKeyFile = dir.resolve("public.key");
        Path privateKeyFile = dir.resolve("private.key");
        if (IOHelper.isFile(publicKeyFile) && IOHelper.isFile(privateKeyFile)) {
            LogHelper.info("Reading EC keypair");
            publicKey = SecurityHelper.toPublicECDSAKey(IOHelper.read(publicKeyFile));
            privateKey = SecurityHelper.toPrivateECDSAKey(IOHelper.read(privateKeyFile));
        } else {
            LogHelper.info("Generating EC keypair");
            KeyPair pair = SecurityHelper.genECDSAKeyPair(new SecureRandom());
            publicKey = (ECPublicKey) pair.getPublic();
            privateKey = (ECPrivateKey) pair.getPrivate();

            // Write key pair list
            LogHelper.info("Writing EC keypair list");
            IOHelper.write(publicKeyFile, publicKey.getEncoded());
            IOHelper.write(privateKeyFile, privateKey.getEncoded());
        }
    }
}
