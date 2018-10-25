package ru.gravit.launchserver.response.update;

import java.io.IOException;
import java.util.Collection;

import ru.gravit.launcher.serialize.signed.DigestBytesHolder;
import ru.gravit.launchserver.binary.LauncherBinary;
import ru.gravit.utils.helper.SecurityHelper;
import ru.gravit.launcher.profiles.ClientProfile;
import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;
import ru.gravit.launcher.serialize.signed.SignedBytesHolder;
import ru.gravit.launcher.serialize.signed.SignedObjectHolder;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.response.Response;

public final class LegacyLauncherResponse extends Response {

    public LegacyLauncherResponse(LaunchServer server, long session, HInput input, HOutput output, String ip) {
        super(server, session, input, output, ip);
    }

    @Override
    public void reply() throws IOException {
        // Resolve launcher binary
        LauncherBinary bytes = (input.readBoolean() ? server.launcherEXEBinary : server.launcherBinary);
        if (bytes == null) {
            requestError("Missing launcher binary");
            return;
        }
        writeNoError(output);

        // Update launcher binary
        output.writeByteArray(bytes.getSign(), -SecurityHelper.RSA_KEY_LENGTH);
        output.flush();
        if (input.readBoolean()) {
            output.writeByteArray(bytes.getBytes().getBytes(), 0);
            return; // Launcher will be restarted
        }
        requestError("You must update");
    }
}
