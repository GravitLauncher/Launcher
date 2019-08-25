package pro.gravit.launcher;

import java.io.IOException;
import java.nio.file.Path;
import java.security.spec.InvalidKeySpecException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import pro.gravit.launcher.test.utils.EXENonWarningLauncherBinary;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.utils.helper.LogHelper;

public class StartTest {
    @TempDir
    public Path dir;

    @BeforeAll
    public static void prepare() {
        LogHelper.removeStdOutput();
        LaunchServer.defaultLauncherEXEBinaryClass = EXENonWarningLauncherBinary.class;
    }

    @Test
    public void checkLaunchServerStarts() {
        /*try {
            LaunchServer srv = new LaunchServer(dir, true, new String[]{"checkInstall"});
            srv.run();
            srv.commandHandler.eval(new String[]{"checkInstall"}, false);
            srv.close();
        } catch (InvalidKeySpecException | IOException e) {
            throw new RuntimeException(e);
        }*/
    }
}
