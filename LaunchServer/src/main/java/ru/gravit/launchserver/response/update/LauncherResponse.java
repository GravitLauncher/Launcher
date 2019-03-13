package ru.gravit.launchserver.response.update;

import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;
import ru.gravit.launcher.serialize.SerializeLimits;
import ru.gravit.launcher.serialize.signed.DigestBytesHolder;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.response.Response;
import ru.gravit.launchserver.socket.Client;

import java.io.IOException;
import java.util.Arrays;

public final class LauncherResponse extends Response {

    public LauncherResponse(LaunchServer server, long session, HInput input, HOutput output, String ip, Client clientData) {
        super(server, session, input, output, ip, clientData);
    }

    @Override
    public void reply() throws IOException {
        // Resolve launcher binary
        DigestBytesHolder bytes = (input.readBoolean() ? server.launcherEXEBinary : server.launcherBinary).getBytes();
        if (bytes == null) {
            requestError("Missing launcher binary");
            return;
        }
        byte[] digest = input.readByteArray(SerializeLimits.MAX_DIGEST);
        if (!Arrays.equals(bytes.getDigest(), digest)) {
            writeNoError(output);
            output.writeBoolean(true);
            output.writeByteArray(bytes.getBytes(), 0);
            clientData.checkSign = false;
            return;
        }
        writeNoError(output);
        output.writeBoolean(false);
        clientData.checkSign = true;
    }
}
