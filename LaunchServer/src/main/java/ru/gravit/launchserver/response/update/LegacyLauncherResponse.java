package ru.gravit.launchserver.response.update;

import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.binary.LauncherBinary;
import ru.gravit.launchserver.response.Response;
import ru.gravit.launchserver.socket.Client;
import ru.gravit.utils.helper.SecurityHelper;

import java.io.IOException;

public final class LegacyLauncherResponse extends Response {

    public LegacyLauncherResponse(LaunchServer server, long session, HInput input, HOutput output, String ip, Client clientData) {
        super(server, session, input, output, ip, clientData);
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
