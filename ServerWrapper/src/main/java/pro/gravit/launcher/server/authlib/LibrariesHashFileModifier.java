package pro.gravit.launcher.server.authlib;

import java.io.IOException;

@FunctionalInterface
public interface LibrariesHashFileModifier {
    byte[] apply(byte[] data, InstallAuthlib.InstallAuthlibContext context) throws IOException;
}
