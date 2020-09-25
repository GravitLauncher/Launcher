package pro.gravit.launchserver.command.basic;

import org.bouncycastle.cert.X509CertificateHolder;
import pro.gravit.launcher.request.auth.password.AuthPlainPassword;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.AuthException;
import pro.gravit.launchserver.auth.AuthProviderPair;
import pro.gravit.launchserver.command.Command;
import pro.gravit.launchserver.socket.handlers.NettyServerSocketHandler;
import pro.gravit.utils.helper.CommonHelper;
import pro.gravit.utils.helper.LogHelper;

import java.nio.file.Paths;
import java.security.KeyPair;

public class TestCommand extends Command {
    private NettyServerSocketHandler handler = null;

    public TestCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return null;
    }

    @Override
    public String getUsageDescription() {
        return "Test command. Only developer!";
    }

    @Override
    public void invoke(String... args) throws Exception {
        verifyArgs(args, 1);
        if (handler == null)
            handler = new NettyServerSocketHandler(server);
        if (args[0].equals("start")) {
            CommonHelper.newThread("Netty Server", true, handler).start();
        }
        if (args[0].equals("stop")) {
            handler.close();
        }
        if (args[0].equals("genCA")) {
            server.certificateManager.generateCA();
            server.certificateManager.writePrivateKey(Paths.get("ca.key"), server.certificateManager.caKey);
            server.certificateManager.writeCertificate(Paths.get("ca.crt"), server.certificateManager.ca);
        }
        if (args[0].equals("readCA")) {
            server.certificateManager.ca = server.certificateManager.readCertificate(Paths.get("ca.crt"));
            server.certificateManager.caKey = server.certificateManager.readPrivateKey(Paths.get("ca.key"));
        }
        if (args[0].equals("genCert")) {
            verifyArgs(args, 2);
            String name = args[1];
            KeyPair pair = server.certificateManager.generateKeyPair();
            X509CertificateHolder cert = server.certificateManager.generateCertificate(name, pair.getPublic());
            server.certificateManager.writePrivateKey(Paths.get(name.concat(".key")), pair.getPrivate());
            server.certificateManager.writeCertificate(Paths.get(name.concat(".crt")), cert);
        }
        if (args[0].equals("authstresser")) {
            AuthProviderPair pair = server.config.getAuthProviderPair();
            AuthPlainPassword plainPassword = new AuthPlainPassword("test");
            Runnable runnable = () -> {
                long startTime = System.currentTimeMillis();
                for (int i = 0; i < 100000; ++i) {
                    try {
                        pair.provider.auth("Test", plainPassword, "127.0.0.1");
                    } catch (AuthException ignored) {

                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    if ((i % 10000) == 0) {
                        LogHelper.info("Completed %d requests", i);
                    }
                }
                LogHelper.info("Completed all requests. Time %d ms", System.currentTimeMillis() - startTime);
            };
            for (int i = 0; i < 7; ++i) {
                CommonHelper.newThread(String.format("Stresser #%d", i), true, runnable).start();
            }
        }
    }
}
