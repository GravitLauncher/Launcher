package pro.gravit.launcher.server.authlib;

import pro.gravit.launcher.server.commands.InstallAuthLib;

import java.io.IOException;

@FunctionalInterface
public interface LibrariesHashFileModifier {
    byte[] apply(byte[] data, InstallAuthLib.InstallAuthlibContext context) throws IOException;
}
