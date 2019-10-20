package pro.gravit.launchserver.binary.tasks;

import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.operator.OperatorCreationException;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.binary.SignerJar;
import pro.gravit.launchserver.config.LaunchServerConfig;
import pro.gravit.launchserver.helper.SignHelper;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateEncodingException;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class SignJarTask implements LauncherBuildTask {

    private final LaunchServerConfig.JarSignerConf config;
    private final LaunchServer srv;

    public SignJarTask(LaunchServerConfig.JarSignerConf config, LaunchServer srv) {
        this.config = config;
        this.srv = srv;
    }

    @Override
    public String getName() {
        return "SignJar";
    }

    @Override
    public Path process(Path inputFile) throws IOException {
        Path toRet = srv.launcherBinary.nextPath("signed");
        KeyStore c = SignHelper.getStore(new File(config.keyStore).toPath(), config.keyStorePass, config.keyStoreType);
        try (SignerJar output = new SignerJar(new ZipOutputStream(IOHelper.newOutput(toRet)), () -> this.gen(c),
                config.manifestFileSfName, config.manifestFileName);
             ZipInputStream input = new ZipInputStream(IOHelper.newInput(inputFile))) {
            //input.getManifest().getMainAttributes().forEach((a, b) -> output.addManifestAttribute(a.toString(), b.toString())); // may not work such as after Radon.
            ZipEntry e = input.getNextEntry();
            while (e != null) {
               if ("META-INF/MANIFEST.MF".equals(e.getName()) || "/META-INF/MANIFEST.MF".equals(e.getName())) {
               	 Manifest m = new Manifest(input);
               	 m.getMainAttributes().forEach((a, b) -> output.addManifestAttribute(a.toString(), b.toString()));
               	 e = input.getNextEntry();
               	 continue;
               }
                output.addFileContents(IOHelper.newZipEntry(e), input);
                e = input.getNextEntry();
            }
        }
        return toRet;
    }

    @Override
    public boolean allowDelete() {
        return true;
    }
    
    public CMSSignedDataGenerator gen(KeyStore c) {
    	try {
			return SignHelper.createSignedDataGenerator(c,
			        config.keyAlias, config.signAlgo, config.keyPass);
		} catch (CertificateEncodingException | UnrecoverableKeyException | KeyStoreException
				| OperatorCreationException | NoSuchAlgorithmException | CMSException e) {
			LogHelper.error(e);
			return null;
		}
    }
}
