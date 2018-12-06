package ru.gravit.launchserver.response.update;

import java.io.IOException;
import java.util.Arrays;

import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;
import ru.gravit.launcher.serialize.signed.DigestBytesHolder;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.response.Response;
import ru.gravit.launchserver.socket.Client;

public final class LauncherResponse extends Response {

    public LauncherResponse(LaunchServer server, long session, HInput input, HOutput output, String ip) {
        super(server, session, input, output, ip);
    }

    @Override
    public void reply() throws IOException {
        // Resolve launcher binary
        DigestBytesHolder bytes = (input.readBoolean() ? server.launcherEXEBinary : server.launcherBinary).getBytes();
        if (bytes == null) {
            requestError("Missing launcher binary");
            return;
        }
        Client client = server.sessionManager.getOrNewClient(session);
        byte[] digest = input.readByteArray(0);
        if (!Arrays.equals(bytes.getDigest(), digest)) {
            writeNoError(output);
            output.writeBoolean(true);
            output.writeByteArray(bytes.getBytes(), 0);
            client.checkSign = false;
            return;
        }
        writeNoError(output);
        output.writeBoolean(false);
        client.checkSign = true;
    }
}
